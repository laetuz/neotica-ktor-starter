package id.neotica.route

import id.neotica.domain.model.Role
import id.neotica.domain.model.ForbiddenException
import id.neotica.domain.model.ValidationException
import id.neotica.domain.model.TokenPrincipal
import io.ktor.server.application.*
import io.ktor.server.auth.*

/** Read the authenticated identity, failing the request if absent. */
suspend fun ApplicationCall.principalOrThrow(): TokenPrincipal =
    principal<TokenPrincipal>() ?: throw ValidationException("Authentication required")

/** Enforce a set of allowed roles for the current principal. */
suspend fun ApplicationCall.requireAnyRole(vararg allowed: Role): TokenPrincipal {
    val principal = principalOrThrow()
    if (allowed.isNotEmpty() && principal.role !in allowed) {
        throw ForbiddenException("Insufficient permissions")
    }
    return principal
}