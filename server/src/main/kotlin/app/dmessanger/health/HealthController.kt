package app.dmessanger.health

import app.dmessanger.config.DmessangerProperties
import org.springframework.boot.info.BuildProperties
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Instant

@RestController
@RequestMapping("/api/v1/health")
class HealthController(
    private val properties: DmessangerProperties,
    private val buildProperties: BuildProperties?,
) {
    @GetMapping
    fun health(): HealthResponse = HealthResponse(
        status = "UP",
        service = "dmessanger-backend",
        version = buildProperties?.version ?: "dev",
        timestamp = Instant.now().toString(),
        dependencies = HealthDependencies(
            s3Bucket = properties.s3.bucket,
            turnPublicUrl = properties.turn.publicUrl,
        ),
    )
}

data class HealthResponse(
    val status: String,
    val service: String,
    val version: String,
    val timestamp: String,
    val dependencies: HealthDependencies,
)

data class HealthDependencies(
    val s3Bucket: String,
    val turnPublicUrl: String,
)
