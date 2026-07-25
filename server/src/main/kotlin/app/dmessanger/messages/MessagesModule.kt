package app.dmessanger.messages

import app.dmessanger.auth.AuthService
import app.dmessanger.chats.ChatRepository
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size
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
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import java.sql.ResultSet
import java.sql.Timestamp
import java.time.Instant
import java.util.Base64
import java.util.UUID

@RestController
@RequestMapping("/api/v1")
class MessagesController(
    private val authService: AuthService,
    private val messageService: MessageService,
) {
    @GetMapping("/chats/{chatId}/messages")
    fun list(
        request: HttpServletRequest,
        @PathVariable chatId: UUID,
        @RequestParam(required = false) before: Instant?,
        @RequestParam(defaultValue = "50") @Positive limit: Int,
    ): List<MessageResponse> {
        val user = authService.currentUser(request.getHeader(HttpHeaders.AUTHORIZATION))
        return messageService.listMessages(user.id, chatId, before, limit)
    }

    @PostMapping("/chats/{chatId}/messages")
    fun send(
        request: HttpServletRequest,
        @PathVariable chatId: UUID,
        @Valid @RequestBody body: SendMessageRequest,
    ): MessageResponse {
        val user = authService.currentUser(request.getHeader(HttpHeaders.AUTHORIZATION))
        return messageService.sendMessage(user.id, chatId, body)
    }

    @PostMapping("/messages/{messageId}/delivered")
    fun delivered(request: HttpServletRequest, @PathVariable messageId: UUID): MessageStatusResponse {
        val user = authService.currentUser(request.getHeader(HttpHeaders.AUTHORIZATION))
        return messageService.markDelivered(user.id, messageId)
    }

    @PostMapping("/messages/{messageId}/read")
    fun read(request: HttpServletRequest, @PathVariable messageId: UUID): MessageStatusResponse {
        val user = authService.currentUser(request.getHeader(HttpHeaders.AUTHORIZATION))
        return messageService.markRead(user.id, messageId)
    }
}

@Service
class MessageService(
    private val chats: ChatRepository,
    private val messages: MessageRepository,
    private val objectMapper: ObjectMapper,
) {
    fun listMessages(userId: UUID, chatId: UUID, before: Instant?, limit: Int): List<MessageResponse> {
        ensureChatMember(chatId, userId)
        return messages.findByChat(chatId, userId, before, limit.coerceIn(1, 100)).map(::toResponse)
    }

    @Transactional
    fun sendMessage(userId: UUID, chatId: UUID, request: SendMessageRequest): MessageResponse {
        ensureChatMember(chatId, userId)
        val ciphertext = decodeCiphertext(request.ciphertext)
        val metadataJson = objectMapper.writeValueAsString(request.encryptionMetadata)
        return toResponse(
            messages.create(
                chatId = chatId,
                senderUserId = userId,
                clientMessageId = request.clientMessageId,
                ciphertext = ciphertext,
                encryptionMetadata = metadataJson,
            ),
        )
    }

    @Transactional
    fun markDelivered(userId: UUID, messageId: UUID): MessageStatusResponse {
        val updated = messages.markDelivered(messageId, userId, Instant.now())
        if (!updated) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "Message recipient not found")
        }
        return MessageStatusResponse(messageId = messageId, status = "DELIVERED")
    }

    @Transactional
    fun markRead(userId: UUID, messageId: UUID): MessageStatusResponse {
        val updated = messages.markRead(messageId, userId, Instant.now())
        if (!updated) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "Message recipient not found")
        }
        return MessageStatusResponse(messageId = messageId, status = "READ")
    }

    private fun ensureChatMember(chatId: UUID, userId: UUID) {
        if (!chats.isMember(chatId, userId)) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "Chat not found")
        }
    }

    private fun decodeCiphertext(value: String): ByteArray = try {
        Base64.getDecoder().decode(value)
    } catch (_: IllegalArgumentException) {
        throw ResponseStatusException(HttpStatus.BAD_REQUEST, "ciphertext must be base64 encoded")
    }

    private fun toResponse(record: MessageRecord): MessageResponse {
        val metadata = parseMetadata(record.encryptionMetadata)
        return MessageResponse(
            id = record.id,
            chatId = record.chatId,
            senderUserId = record.senderUserId,
            clientMessageId = record.clientMessageId,
            ciphertext = Base64.getEncoder().encodeToString(record.ciphertext),
            plaintext = decodeDevPlaintext(record.ciphertext, metadata),
            encryptionMetadata = metadata,
            createdAt = record.createdAt,
            status = record.currentUserStatus,
        )
    }

    private fun parseMetadata(metadata: String): Map<String, Any?> = try {
        objectMapper.readValue(metadata, object : TypeReference<Map<String, Any?>>() {})
    } catch (_: Exception) {
        emptyMap()
    }

    private fun decodeDevPlaintext(ciphertext: ByteArray, metadata: Map<String, Any?>): String? {
        if (metadata["mode"] != "dev-plaintext-base64") {
            return null
        }
        return ciphertext.toString(Charsets.UTF_8)
    }
}

@Repository
class MessageRepository(
    private val jdbc: NamedParameterJdbcTemplate,
) {
    fun findByChat(chatId: UUID, currentUserId: UUID, before: Instant?, limit: Int): List<MessageRecord> {
        val beforeFilter = if (before == null) "" else "AND m.created_at < :before"
        val params = mutableMapOf<String, Any>(
            "chatId" to chatId,
            "currentUserId" to currentUserId,
            "limit" to limit,
        )
        if (before != null) {
            params["before"] = Timestamp.from(before)
        }

        return jdbc.query(
            """
            SELECT
                m.id,
                m.chat_id,
                m.sender_user_id,
                m.sender_device_id,
                m.client_message_id,
                m.ciphertext,
                m.encryption_metadata::text AS encryption_metadata,
                m.created_at,
                COALESCE(mr.status::text, 'SENT') AS current_user_status
            FROM messages m
            LEFT JOIN message_recipients mr ON mr.message_id = m.id AND mr.user_id = :currentUserId
            WHERE m.chat_id = :chatId
              $beforeFilter
            ORDER BY m.created_at DESC
            LIMIT :limit
            """.trimIndent(),
            params,
        ) { rs, _ -> rs.toMessageRecord() }.asReversed()
    }

    fun create(
        chatId: UUID,
        senderUserId: UUID,
        clientMessageId: String,
        ciphertext: ByteArray,
        encryptionMetadata: String,
    ): MessageRecord {
        val insertedMessageId = jdbc.query(
            """
            INSERT INTO messages (chat_id, sender_user_id, client_message_id, ciphertext, encryption_metadata)
            VALUES (:chatId, :senderUserId, :clientMessageId, :ciphertext, CAST(:encryptionMetadata AS jsonb))
            ON CONFLICT (sender_user_id, client_message_id) DO NOTHING
            RETURNING id
            """.trimIndent(),
            mapOf(
                "chatId" to chatId,
                "senderUserId" to senderUserId,
                "clientMessageId" to clientMessageId,
                "ciphertext" to ciphertext,
                "encryptionMetadata" to encryptionMetadata,
            ),
        ) { rs, _ -> rs.getObject("id", UUID::class.java) }.singleOrNull()

        val messageId = insertedMessageId ?: jdbc.query(
            """
            SELECT id
            FROM messages
            WHERE sender_user_id = :senderUserId AND client_message_id = :clientMessageId
            """.trimIndent(),
            mapOf("senderUserId" to senderUserId, "clientMessageId" to clientMessageId),
        ) { rs, _ -> rs.getObject("id", UUID::class.java) }.single()

        jdbc.update(
            """
            INSERT INTO message_recipients (message_id, user_id, status, read_at)
            SELECT :messageId,
                   cm.user_id,
                   CASE WHEN cm.user_id = :senderUserId THEN 'READ'::message_delivery_status ELSE 'SENT'::message_delivery_status END,
                   CASE WHEN cm.user_id = :senderUserId THEN now() ELSE NULL END
            FROM chat_members cm
            WHERE cm.chat_id = :chatId AND cm.removed_at IS NULL
            ON CONFLICT (message_id, user_id) DO NOTHING
            """.trimIndent(),
            mapOf("messageId" to messageId, "senderUserId" to senderUserId, "chatId" to chatId),
        )

        jdbc.update(
            """
            UPDATE chats
            SET updated_at = now()
            WHERE id = :chatId
            """.trimIndent(),
            mapOf("chatId" to chatId),
        )

        return findById(messageId, senderUserId) ?: error("Created message was not found")
    }

    fun findById(messageId: UUID, currentUserId: UUID): MessageRecord? = jdbc.query(
        """
        SELECT
            m.id,
            m.chat_id,
            m.sender_user_id,
            m.sender_device_id,
            m.client_message_id,
            m.ciphertext,
            m.encryption_metadata::text AS encryption_metadata,
            m.created_at,
            COALESCE(mr.status::text, 'SENT') AS current_user_status
        FROM messages m
        LEFT JOIN message_recipients mr ON mr.message_id = m.id AND mr.user_id = :currentUserId
        WHERE m.id = :messageId
        """.trimIndent(),
        mapOf("messageId" to messageId, "currentUserId" to currentUserId),
    ) { rs, _ -> rs.toMessageRecord() }.singleOrNull()


    fun markDelivered(messageId: UUID, userId: UUID, deliveredAt: Instant): Boolean = jdbc.update(
        """
        UPDATE message_recipients
        SET status = CASE WHEN status = 'READ' THEN status ELSE 'DELIVERED'::message_delivery_status END,
            delivered_at = COALESCE(delivered_at, :deliveredAt)
        WHERE message_id = :messageId AND user_id = :userId
        """.trimIndent(),
        mapOf(
            "messageId" to messageId,
            "userId" to userId,
            "deliveredAt" to Timestamp.from(deliveredAt),
        ),
    ) > 0

    fun markRead(messageId: UUID, userId: UUID, readAt: Instant): Boolean = jdbc.update(
        """
        UPDATE message_recipients
        SET status = 'READ',
            delivered_at = COALESCE(delivered_at, :readAt),
            read_at = COALESCE(read_at, :readAt)
        WHERE message_id = :messageId AND user_id = :userId
        """.trimIndent(),
        mapOf(
            "messageId" to messageId,
            "userId" to userId,
            "readAt" to Timestamp.from(readAt),
        ),
    ) > 0

    private fun ResultSet.toMessageRecord(): MessageRecord = MessageRecord(
        id = getObject("id", UUID::class.java),
        chatId = getObject("chat_id", UUID::class.java),
        senderUserId = getObject("sender_user_id", UUID::class.java),
        senderDeviceId = getObject("sender_device_id", UUID::class.java),
        clientMessageId = getString("client_message_id"),
        ciphertext = getBytes("ciphertext"),
        encryptionMetadata = getString("encryption_metadata"),
        createdAt = getTimestamp("created_at").toInstant(),
        currentUserStatus = getString("current_user_status"),
    )
}

data class SendMessageRequest(
    @field:NotBlank
    @field:Size(max = 120)
    val clientMessageId: String,
    @field:NotBlank
    @field:Size(max = 65536)
    val ciphertext: String,
    val encryptionMetadata: Map<String, Any?> = emptyMap(),
)

data class MessageResponse(
    val id: UUID,
    val chatId: UUID,
    val senderUserId: UUID,
    val clientMessageId: String,
    val ciphertext: String,
    val plaintext: String?,
    val encryptionMetadata: Map<String, Any?>,
    val createdAt: Instant,
    val status: String,
)

data class MessageStatusResponse(
    val messageId: UUID,
    val status: String,
)

data class MessageRecord(
    val id: UUID,
    val chatId: UUID,
    val senderUserId: UUID,
    val senderDeviceId: UUID?,
    val clientMessageId: String,
    val ciphertext: ByteArray,
    val encryptionMetadata: String,
    val createdAt: Instant,
    val currentUserStatus: String,
)
