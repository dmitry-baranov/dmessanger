package app.dmessanger.auth

import app.dmessanger.config.DmessangerProperties
import app.dmessanger.users.CurrentUserResponse
import app.dmessanger.users.UserRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.time.Clock
import java.time.Instant

@Service
class AuthService(
    private val users: UserRepository,
    private val credentials: UserCredentialRepository,
    private val sessions: SessionRepository,
    private val passwordHasher: PasswordHasher,
    private val jwtTokenService: JwtTokenService,
    private val refreshTokenService: RefreshTokenService,
    private val properties: DmessangerProperties,
    private val clock: Clock,
) {
    @Transactional
    fun login(request: LoginRequest): AuthResponse {
        val user = users.findByLogin(request.login)
            ?: throw invalidCredentials()

        if (!user.enabled) {
            throw invalidCredentials()
        }

        val credential = credentials.findByUserId(user.id)
            ?: throw invalidCredentials()

        if (!passwordHasher.verify(credential.passwordHash, request.password)) {
            throw invalidCredentials()
        }

        val refreshToken = refreshTokenService.generate()
        val now = Instant.now(clock)
        sessions.create(
            userId = user.id,
            deviceId = null,
            refreshTokenHash = refreshTokenService.hash(refreshToken),
            expiresAt = now.plus(properties.security.refreshTokenTtlDays, java.time.temporal.ChronoUnit.DAYS),
        )

        val accessToken = jwtTokenService.createAccessToken(user, now)
        return AuthResponse(
            accessToken = accessToken,
            refreshToken = refreshToken,
            expiresInSeconds = properties.security.accessTokenTtl.seconds,
            user = CurrentUserResponse.from(user),
        )
    }

    @Transactional
    fun refresh(request: RefreshRequest): AuthResponse {
        val tokenHash = refreshTokenService.hash(request.refreshToken)
        val session = sessions.findActiveByRefreshTokenHash(tokenHash, Instant.now(clock))
            ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token")

        val user = users.findById(session.userId)
            ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token")

        if (!user.enabled) {
            throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "User is disabled")
        }

        sessions.revoke(session.id, Instant.now(clock))

        val refreshToken = refreshTokenService.generate()
        val now = Instant.now(clock)
        sessions.create(
            userId = user.id,
            deviceId = session.deviceId,
            refreshTokenHash = refreshTokenService.hash(refreshToken),
            expiresAt = now.plus(properties.security.refreshTokenTtlDays, java.time.temporal.ChronoUnit.DAYS),
        )

        return AuthResponse(
            accessToken = jwtTokenService.createAccessToken(user, now),
            refreshToken = refreshToken,
            expiresInSeconds = properties.security.accessTokenTtl.seconds,
            user = CurrentUserResponse.from(user),
        )
    }

    @Transactional
    fun logout(request: LogoutRequest): LogoutResponse {
        sessions.revokeByRefreshTokenHash(refreshTokenService.hash(request.refreshToken), Instant.now(clock))
        return LogoutResponse(success = true)
    }

    fun currentUser(authorizationHeader: String?): CurrentUserResponse {
        val userId = jwtTokenService.parseBearerAccessToken(authorizationHeader)
        val user = users.findById(userId)
            ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found")

        if (!user.enabled) {
            throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "User is disabled")
        }

        return CurrentUserResponse.from(user)
    }

    private fun invalidCredentials(): ResponseStatusException =
        ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid login or password")
}
