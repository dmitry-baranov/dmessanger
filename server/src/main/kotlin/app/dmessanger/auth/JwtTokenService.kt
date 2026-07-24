package app.dmessanger.auth

import app.dmessanger.config.DmessangerProperties
import app.dmessanger.users.UserRecord
import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import java.time.Instant
import java.util.Date
import java.util.UUID

@Service
class JwtTokenService(
    properties: DmessangerProperties,
) {
    private val accessTokenTtl = properties.security.accessTokenTtl
    private val algorithm = Algorithm.HMAC256(properties.security.accessTokenSecret)
    private val verifier: JWTVerifier = JWT.require(algorithm)
        .withIssuer(ISSUER)
        .withClaim("type", ACCESS_TOKEN_TYPE)
        .build()

    fun createAccessToken(user: UserRecord, issuedAt: Instant): String = JWT.create()
        .withIssuer(ISSUER)
        .withSubject(user.id.toString())
        .withClaim("type", ACCESS_TOKEN_TYPE)
        .withClaim("login", user.login)
        .withIssuedAt(Date.from(issuedAt))
        .withExpiresAt(Date.from(issuedAt.plus(accessTokenTtl)))
        .sign(algorithm)

    fun parseBearerAccessToken(authorizationHeader: String?): UUID {
        if (authorizationHeader == null || !authorizationHeader.startsWith(BEARER_PREFIX)) {
            throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing bearer token")
        }

        val token = authorizationHeader.removePrefix(BEARER_PREFIX).trim()
        if (token.isBlank()) {
            throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing bearer token")
        }

        return try {
            UUID.fromString(verifier.verify(token).subject)
        } catch (_: Exception) {
            throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid bearer token")
        }
    }

    companion object {
        private const val ISSUER = "dmessanger"
        private const val ACCESS_TOKEN_TYPE = "access"
        private const val BEARER_PREFIX = "Bearer "
    }
}
