package app.dmessanger.auth

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import java.sql.ResultSet
import java.sql.Timestamp
import java.time.Instant
import java.util.UUID

@Repository
class SessionRepository(
    private val jdbc: NamedParameterJdbcTemplate,
) {
    fun create(userId: UUID, deviceId: UUID?, refreshTokenHash: String, expiresAt: Instant): SessionRecord = jdbc.query(
        """
        INSERT INTO sessions (user_id, device_id, refresh_token_hash, expires_at)
        VALUES (:userId, :deviceId, :refreshTokenHash, :expiresAt)
        RETURNING id, user_id, device_id, refresh_token_hash, expires_at, revoked_at, created_at
        """.trimIndent(),
        mapOf(
            "userId" to userId,
            "deviceId" to deviceId,
            "refreshTokenHash" to refreshTokenHash,
            "expiresAt" to Timestamp.from(expiresAt),
        ),
    ) { rs, _ -> rs.toSessionRecord() }.single()

    fun findActiveByRefreshTokenHash(refreshTokenHash: String, now: Instant): SessionRecord? = jdbc.query(
        """
        SELECT id, user_id, device_id, refresh_token_hash, expires_at, revoked_at, created_at
        FROM sessions
        WHERE refresh_token_hash = :refreshTokenHash
          AND revoked_at IS NULL
          AND expires_at > :now
        """.trimIndent(),
        mapOf(
            "refreshTokenHash" to refreshTokenHash,
            "now" to Timestamp.from(now),
        ),
    ) { rs, _ -> rs.toSessionRecord() }.singleOrNull()

    fun revoke(sessionId: UUID, revokedAt: Instant) {
        jdbc.update(
            """
            UPDATE sessions
            SET revoked_at = :revokedAt
            WHERE id = :sessionId AND revoked_at IS NULL
            """.trimIndent(),
            mapOf("sessionId" to sessionId, "revokedAt" to Timestamp.from(revokedAt)),
        )
    }

    fun revokeByRefreshTokenHash(refreshTokenHash: String, revokedAt: Instant) {
        jdbc.update(
            """
            UPDATE sessions
            SET revoked_at = :revokedAt
            WHERE refresh_token_hash = :refreshTokenHash AND revoked_at IS NULL
            """.trimIndent(),
            mapOf("refreshTokenHash" to refreshTokenHash, "revokedAt" to Timestamp.from(revokedAt)),
        )
    }

    private fun ResultSet.toSessionRecord(): SessionRecord = SessionRecord(
        id = getObject("id", UUID::class.java),
        userId = getObject("user_id", UUID::class.java),
        deviceId = getObject("device_id", UUID::class.java),
        refreshTokenHash = getString("refresh_token_hash"),
        expiresAt = getTimestamp("expires_at").toInstant(),
        revokedAt = getTimestamp("revoked_at")?.toInstant(),
        createdAt = getTimestamp("created_at").toInstant(),
    )
}

data class SessionRecord(
    val id: UUID,
    val userId: UUID,
    val deviceId: UUID?,
    val refreshTokenHash: String,
    val expiresAt: Instant,
    val revokedAt: Instant?,
    val createdAt: Instant,
)
