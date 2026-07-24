package app.dmessanger.auth

import app.dmessanger.users.CurrentUserResponse
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import org.springframework.http.HttpHeaders
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1")
class AuthController(
    private val authService: AuthService,
) {
    @PostMapping("/auth/login")
    fun login(@Valid @RequestBody request: LoginRequest): AuthResponse = authService.login(request)

    @PostMapping("/auth/refresh")
    fun refresh(@Valid @RequestBody request: RefreshRequest): AuthResponse = authService.refresh(request)

    @PostMapping("/auth/logout")
    fun logout(@Valid @RequestBody request: LogoutRequest): LogoutResponse = authService.logout(request)

    @GetMapping("/me")
    fun me(request: HttpServletRequest): CurrentUserResponse = authService.currentUser(
        authorizationHeader = request.getHeader(HttpHeaders.AUTHORIZATION),
    )
}

data class LoginRequest(
    @field:NotBlank val login: String,
    @field:NotBlank val password: String,
)

data class RefreshRequest(
    @field:NotBlank val refreshToken: String,
)

data class LogoutRequest(
    @field:NotBlank val refreshToken: String,
)

data class AuthResponse(
    val accessToken: String,
    val refreshToken: String,
    val tokenType: String = "Bearer",
    val expiresInSeconds: Long,
    val user: CurrentUserResponse,
)

data class LogoutResponse(
    val success: Boolean,
)
