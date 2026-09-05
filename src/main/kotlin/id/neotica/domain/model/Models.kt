package id.neotica.domain.model

import kotlinx.serialization.Serializable

enum class Role { USER, ADMIN }

@Serializable
data class Task(
    val id: String,
    val title: String,
    val description: String? = null,
    val completed: Boolean = false,
    val ownerId: String,
)

@Serializable
data class CreateTaskRequest(
    val title: String,
    val description: String? = null,
)

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