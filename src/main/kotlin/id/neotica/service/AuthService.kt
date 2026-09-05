package id.neotica.service

import id.neotica.domain.model.Role
import id.neotica.domain.model.TokenData

/**
 * Stand-in for a real user store. Returns a fixed token on any valid login.
 * Swap for a user repository + password hasher (BCrypt) when real auth lands.
 */
class AuthService(
    private val tokenService: TokenService,
) {
    fun login(username: String): TokenData {
        val role = if (username == "admin") Role.ADMIN else Role.USER
        return TokenData(userId = username, role = role)
    }
}