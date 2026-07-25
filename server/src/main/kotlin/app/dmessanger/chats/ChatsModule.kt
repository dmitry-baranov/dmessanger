package app.dmessanger.chats

import app.dmessanger.auth.AuthService
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import java.sql.ResultSet
import java.time.Instant
import java.util.UUID

@RestController
@RequestMapping("/api/v1/chats")
class ChatsController(
    private val authService: AuthService,
    private val chatService: ChatService,
) {
    @GetMapping
    fun list(request: HttpServletRequest): List<ChatSummaryResponse> {
        val user = authService.currentUser(request.getHeader(HttpHeaders.AUTHORIZATION))
        return chatService.listChats(user.id)
    }

    @GetMapping("/{chatId}")
    fun get(request: HttpServletRequest, @PathVariable chatId: UUID): ChatDetailsResponse {
        val user = authService.currentUser(request.getHeader(HttpHeaders.AUTHORIZATION))
        return chatService.getChat(user.id, chatId)
    }

    @PostMapping("/direct")
    fun createDirect(
        request: HttpServletRequest,
        @Valid @RequestBody body: CreateDirectChatRequest,
    ): ChatDetailsResponse {
        val user = authService.currentUser(request.getHeader(HttpHeaders.AUTHORIZATION))
        return chatService.createDirectChat(user.id, body.memberUserId)
    }
}

@Service
class ChatService(
    private val chats: ChatRepository,
) {
    fun listChats(userId: UUID): List<ChatSummaryResponse> = chats.findSummaries(userId).map { summary ->
        ChatSummaryResponse(
            id = summary.id,
            type = summary.type,
            title = summary.title,
            lastMessagePreview = summary.lastMessageCiphertext?.let { ciphertext ->
                decodeDevPreview(ciphertext, summary.lastMessageEncryptionMetadata)
            },
            lastEventAt = summary.lastEventAt,
            unreadCount = summary.unreadCount,
            isOnline = false,
        )
    }

    fun getChat(userId: UUID, chatId: UUID): ChatDetailsResponse {
        val chat = chats.findDetailsForMember(chatId = chatId, userId = userId)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Chat not found")
        return chat.toResponse()
    }

    @Transactional
    fun createDirectChat(currentUserId: UUID, memberUserId: UUID): ChatDetailsResponse {
        if (currentUserId == memberUserId) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Direct chat requires another user")
        }
        return chats.findDirectBetween(currentUserId, memberUserId)?.toResponse()
            ?: chats.createDirect(createdBy = currentUserId, memberUserId = memberUserId).toResponse()
    }

    private fun ChatDetailsRecord.toResponse(): ChatDetailsResponse = ChatDetailsResponse(
        id = id,
        type = type,
        title = title,
        createdAt = createdAt,
        updatedAt = updatedAt,
        members = members.map { member ->
            ChatMemberResponse(
                userId = member.userId,
                login = member.login,
                displayName = member.displayName,
                role = member.role,
            )
        },
    )

    private fun decodeDevPreview(ciphertext: ByteArray, metadata: String?): String {
        if (metadata?.contains("dev-plaintext-base64") != true) {
            return "Encrypted message"
        }
        return ciphertext.toString(Charsets.UTF_8)
    }
}

@Repository
class ChatRepository(
    private val jdbc: NamedParameterJdbcTemplate,
) {
    fun findSummaries(userId: UUID): List<ChatSummaryRecord> = jdbc.query(
        """
        SELECT
            c.id,
            c.type::text AS type,
            COALESCE(
                c.title,
                NULLIF(string_agg(DISTINCT CASE WHEN u.id <> :userId THEN u.display_name END, ', '), ''),
                'Saved messages'
            ) AS title,
            COALESCE(last_message.created_at, c.updated_at, c.created_at) AS last_event_at,
            last_message.ciphertext AS last_message_ciphertext,
            last_message.encryption_metadata::text AS last_message_encryption_metadata,
            (COUNT(DISTINCT mr.message_id) FILTER (WHERE mr.status <> 'READ'))::int AS unread_count
        FROM chats c
        JOIN chat_members cm_self ON cm_self.chat_id = c.id
            AND cm_self.user_id = :userId
            AND cm_self.removed_at IS NULL
        JOIN chat_members cm_all ON cm_all.chat_id = c.id AND cm_all.removed_at IS NULL
        JOIN users u ON u.id = cm_all.user_id
        LEFT JOIN LATERAL (
            SELECT m.created_at, m.ciphertext, m.encryption_metadata
            FROM messages m
            WHERE m.chat_id = c.id
            ORDER BY m.created_at DESC
            LIMIT 1
        ) last_message ON true
        LEFT JOIN message_recipients mr ON mr.user_id = :userId
            AND mr.status <> 'READ'
            AND mr.message_id IN (SELECT id FROM messages WHERE chat_id = c.id)
        GROUP BY c.id, c.type, c.title, c.created_at, c.updated_at,
            last_message.created_at, last_message.ciphertext, last_message.encryption_metadata
        ORDER BY COALESCE(last_message.created_at, c.updated_at, c.created_at) DESC
        """.trimIndent(),
        mapOf("userId" to userId),
    ) { rs, _ -> rs.toChatSummaryRecord() }

    fun findDetailsForMember(chatId: UUID, userId: UUID): ChatDetailsRecord? {
        if (!isMember(chatId, userId)) {
            return null
        }
        return findDetails(chatId)
    }

    fun findDirectBetween(firstUserId: UUID, secondUserId: UUID): ChatDetailsRecord? {
        val chatId = jdbc.query(
            """
            SELECT c.id
            FROM chats c
            JOIN chat_members first_member ON first_member.chat_id = c.id
                AND first_member.user_id = :firstUserId
                AND first_member.removed_at IS NULL
            JOIN chat_members second_member ON second_member.chat_id = c.id
                AND second_member.user_id = :secondUserId
                AND second_member.removed_at IS NULL
            WHERE c.type = 'DIRECT'
            ORDER BY c.created_at ASC
            LIMIT 1
            """.trimIndent(),
            mapOf("firstUserId" to firstUserId, "secondUserId" to secondUserId),
        ) { rs, _ -> rs.getObject("id", UUID::class.java) }.singleOrNull()
        return chatId?.let(::findDetails)
    }

    fun createDirect(createdBy: UUID, memberUserId: UUID): ChatDetailsRecord {
        val chat = jdbc.query(
            """
            INSERT INTO chats (type, created_by)
            VALUES ('DIRECT', :createdBy)
            RETURNING id
            """.trimIndent(),
            mapOf("createdBy" to createdBy),
        ) { rs, _ -> rs.getObject("id", UUID::class.java) }.single()

        jdbc.update(
            """
            INSERT INTO chat_members (chat_id, user_id, role)
            VALUES
                (:chatId, :createdBy, 'OWNER'),
                (:chatId, :memberUserId, 'MEMBER')
            """.trimIndent(),
            mapOf("chatId" to chat, "createdBy" to createdBy, "memberUserId" to memberUserId),
        )
        return findDetails(chat) ?: error("Created chat was not found")
    }

    fun ensureDevDirectChat(firstLogin: String, secondLogin: String) {
        val users = jdbc.query(
            """
            SELECT id, login
            FROM users
            WHERE login IN (:firstLogin, :secondLogin)
            """.trimIndent(),
            mapOf("firstLogin" to firstLogin, "secondLogin" to secondLogin),
        ) { rs, _ -> rs.getString("login") to rs.getObject("id", UUID::class.java) }.toMap()

        val firstUserId = users[firstLogin]
        val secondUserId = users[secondLogin]
        if (firstUserId != null && secondUserId != null && findDirectBetween(firstUserId, secondUserId) == null) {
            createDirect(createdBy = firstUserId, memberUserId = secondUserId)
        }
    }

    fun isMember(chatId: UUID, userId: UUID): Boolean = jdbc.queryForObject(
        """
        SELECT EXISTS(
            SELECT 1
            FROM chat_members
            WHERE chat_id = :chatId
              AND user_id = :userId
              AND removed_at IS NULL
        )
        """.trimIndent(),
        mapOf("chatId" to chatId, "userId" to userId),
        Boolean::class.java,
    ) == true

    private fun findDetails(chatId: UUID): ChatDetailsRecord? {
        val chat = jdbc.query(
            """
            SELECT id, type::text AS type, title, created_at, updated_at
            FROM chats
            WHERE id = :chatId
            """.trimIndent(),
            mapOf("chatId" to chatId),
        ) { rs, _ -> rs.toChatRecord() }.singleOrNull() ?: return null

        val members = jdbc.query(
            """
            SELECT cm.user_id, u.login, u.display_name, cm.role::text AS role
            FROM chat_members cm
            JOIN users u ON u.id = cm.user_id
            WHERE cm.chat_id = :chatId AND cm.removed_at IS NULL
            ORDER BY cm.joined_at ASC
            """.trimIndent(),
            mapOf("chatId" to chatId),
        ) { rs, _ -> rs.toChatMemberRecord() }

        return ChatDetailsRecord(
            id = chat.id,
            type = chat.type,
            title = chat.title ?: members.joinToString(", ") { it.displayName },
            createdAt = chat.createdAt,
            updatedAt = chat.updatedAt,
            members = members,
        )
    }

    private fun ResultSet.toChatSummaryRecord(): ChatSummaryRecord = ChatSummaryRecord(
        id = getObject("id", UUID::class.java),
        type = getString("type"),
        title = getString("title"),
        lastEventAt = getTimestamp("last_event_at").toInstant(),
        lastMessageCiphertext = getBytes("last_message_ciphertext"),
        lastMessageEncryptionMetadata = getString("last_message_encryption_metadata"),
        unreadCount = getInt("unread_count"),
    )

    private fun ResultSet.toChatRecord(): ChatRecord = ChatRecord(
        id = getObject("id", UUID::class.java),
        type = getString("type"),
        title = getString("title"),
        createdAt = getTimestamp("created_at").toInstant(),
        updatedAt = getTimestamp("updated_at").toInstant(),
    )

    private fun ResultSet.toChatMemberRecord(): ChatMemberRecord = ChatMemberRecord(
        userId = getObject("user_id", UUID::class.java),
        login = getString("login"),
        displayName = getString("display_name"),
        role = getString("role"),
    )
}

data class CreateDirectChatRequest(
    val memberUserId: UUID,
)

data class ChatSummaryResponse(
    val id: UUID,
    val type: String,
    val title: String,
    val lastMessagePreview: String?,
    val lastEventAt: Instant,
    val unreadCount: Int,
    val isOnline: Boolean,
)

data class ChatDetailsResponse(
    val id: UUID,
    val type: String,
    val title: String,
    val createdAt: Instant,
    val updatedAt: Instant,
    val members: List<ChatMemberResponse>,
)

data class ChatMemberResponse(
    val userId: UUID,
    val login: String,
    val displayName: String,
    val role: String,
)

data class ChatSummaryRecord(
    val id: UUID,
    val type: String,
    val title: String,
    val lastEventAt: Instant,
    val lastMessageCiphertext: ByteArray?,
    val lastMessageEncryptionMetadata: String?,
    val unreadCount: Int,
)

data class ChatRecord(
    val id: UUID,
    val type: String,
    val title: String?,
    val createdAt: Instant,
    val updatedAt: Instant,
)

data class ChatDetailsRecord(
    val id: UUID,
    val type: String,
    val title: String,
    val createdAt: Instant,
    val updatedAt: Instant,
    val members: List<ChatMemberRecord>,
)

data class ChatMemberRecord(
    val userId: UUID,
    val login: String,
    val displayName: String,
    val role: String,
)
