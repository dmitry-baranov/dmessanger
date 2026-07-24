package app.dmessanger.auth

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import java.sql.ResultSet
import java.time.Instant
import java.util.UUID

@Repository
class UserCredentialRepository(
    private val jdbc: NamedParameterJdbcTemplate,
) {
    fun findByUserId(userId: UUID): UserCredentialRecord? = jdbc.query(
        """
        SELECT user_id, password_hash, password_algorithm, updated_at
        FROM user_credentials
        WHERE user_id = :userId
        """.trimIndent(),
        mapOf("userId" to userId),
    ) { rs, _ -> rs.toUserCredentialRecord() }.singleOrNull()

    fun upsert(userId: UUID, passwordHash: String) {
        jdbc.update(
            """
            INSERT INTO user_credentials (user_id, password_hash, password_algorithm, updated_at)
            VALUES (:userId, :passwordHash, 'argon2id', now())
            ON CONFLICT (user_id) DO UPDATE SET
                password_hash = excluded.password_hash,
                password_algorithm = excluded.password_algorithm,
                updated_at = now()
            """.trimIndent(),
            mapOf(
                "userId" to userId,
                "passwordHash" to passwordHash,
            ),
        )
    }

    private fun ResultSet.toUserCredentialRecord(): UserCredentialRecord = UserCredentialRecord(
        userId = getObject("user_id", UUID::class.java),
        passwordHash = getString("password_hash"),
        passwordAlgorithm = getString("password_algorithm"),
        updatedAt = getTimestamp("updated_at").toInstant(),
    )
}

data class UserCredentialRecord(
    val userId: UUID,
    val passwordHash: String,
    val passwordAlgorithm: String,
    val updatedAt: Instant,
)
