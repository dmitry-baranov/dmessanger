package app.dmessanger.config

import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.validation.annotation.Validated
import java.time.Duration

@Validated
@ConfigurationProperties(prefix = "dmessanger")
data class DmessangerProperties(
    @field:Valid val security: Security = Security(),
    @field:Valid val seed: Seed = Seed(),
    @field:Valid val s3: S3 = S3(),
    @field:Valid val turn: Turn = Turn(),
) {
    data class Security(
        @field:NotBlank val accessTokenSecret: String = "dev-only-change-me-access-secret",
        val accessTokenTtl: Duration = Duration.ofMinutes(15),
        @field:Positive val refreshTokenTtlDays: Long = 30,
    )

    data class Seed(
        val enabled: Boolean = true,
        @field:Valid val users: List<SeedUser> = listOf(
            SeedUser(login = "alice", displayName = "Alice", password = "password"),
            SeedUser(login = "bob", displayName = "Bob", password = "password"),
        ),
    )

    data class SeedUser(
        @field:NotBlank val login: String = "",
        @field:NotBlank val displayName: String = "",
        @field:NotBlank val password: String = "",
    )

    data class S3(
        @field:NotBlank val endpoint: String = "http://localhost:9000",
        @field:NotBlank val bucket: String = "dmessanger-attachments",
        @field:NotBlank val accessKey: String = "dmessanger",
        @field:NotBlank val secretKey: String = "dmessanger-secret",
    )

    data class Turn(
        @field:NotBlank val publicUrl: String = "turn:localhost:3478",
    )
}
