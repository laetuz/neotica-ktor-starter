package id.neotica.domain.model

/** Authenticated identity, populated from the JWT claims. */
data class TokenPrincipal(
    val userId: String,
    val role: Role,
)

/** Typed exceptions mapped to HTTP responses by StatusPages. */
class ValidationException(message: String) : RuntimeException(message)
class NotFoundException(message: String) : RuntimeException(message)
class ForbiddenException(message: String) : RuntimeException(message)