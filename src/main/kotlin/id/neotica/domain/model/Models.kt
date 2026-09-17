package id.neotica.domain.model

import kotlinx.serialization.Serializable

enum class Role { USER, ADMIN }

@Serializable
data class LoginRequest(
    val username: String,
    val password: String,
)

@Serializable
data class LoginResponse(
    val token: String,
    val role: Role,
)