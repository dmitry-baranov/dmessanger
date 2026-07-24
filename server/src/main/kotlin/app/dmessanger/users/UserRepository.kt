package app.dmessanger.users

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import java.sql.ResultSet
import java.time.Instant
import java.util.UUID

@Repository
class UserRepository(
    private val jdbc: NamedParameterJdbcTemplate,
) {
    fun findByLogin(login: String): UserRecord? = jdbc.query(
        """
        SELECT id, login, display_name, enabled, created_at, updated_at
        FROM users
        WHERE login = :login
        """.trimIndent(),
        mapOf("login" to login),
    ) { rs, _ -> rs.toUserRecord() }.singleOrNull()

    fun findById(id: UUID): UserRecord? = jdbc.query(
        """
        SELECT id, login, display_name, enabled, created_at, updated_at
        FROM users
        WHERE id = :id
        """.trimIndent(),
        mapOf("id" to id),
    ) { rs, _ -> rs.toUserRecord() }.singleOrNull()

    fun upsertSeedUser(login: String, displayName: String): UserRecord = jdbc.query(
        """
        INSERT INTO users (login, display_name, enabled)
        VALUES (:login, :displayName, true)
        ON CONFLICT (login) DO UPDATE SET
            display_name = excluded.display_name,
            enabled = true,
            updated_at = now()
        RETURNING id, login, display_name, enabled, created_at, updated_at
        """.trimIndent(),
        mapOf("login" to login, "displayName" to displayName),
    ) { rs, _ -> rs.toUserRecord() }.single()

    private fun ResultSet.toUserRecord(): UserRecord = UserRecord(
        id = getObject("id", UUID::class.java),
        login = getString("login"),
        displayName = getString("display_name"),
        enabled = getBoolean("enabled"),
        createdAt = getTimestamp("created_at").toInstant(),
        updatedAt = getTimestamp("updated_at").toInstant(),
    )
}

data class UserRecord(
    val id: UUID,
    val login: String,
    val displayName: String,
    val enabled: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant,
)

data class CurrentUserResponse(
    val id: UUID,
    val login: String,
    val displayName: String,
) {
    companion object {
        fun from(user: UserRecord): CurrentUserResponse = CurrentUserResponse(
            id = user.id,
            login = user.login,
            displayName = user.displayName,
        )
    }
}
