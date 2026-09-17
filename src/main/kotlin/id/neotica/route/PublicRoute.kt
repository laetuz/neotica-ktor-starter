package id.neotica.route

import id.neotica.domain.model.LoginRequest
import id.neotica.domain.model.LoginResponse
import id.neotica.service.AuthService
import id.neotica.service.TokenService
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

/**
 * Auth example. Mounted ONLY when `AUTH_ENABLED=true` (see ./env config).
 *
 * The login snippet is kept so newcomers can read how a token is minted and
 * flip the flag to activate it. Greeting endpoint `GET /hello` does NOT require
 * auth — this route exists as the optional demo.
 */
class PublicRoute(
    private val authService: AuthService,
    private val tokenService: TokenService,
) {
    fun mount(route: Route) {
        route.post("/auth/login") {
            val body = call.receive<LoginRequest>()
            val principal = authService.login(body.username)
            val token = tokenService.issue(principal.userId, principal.role)
            call.respond(LoginResponse(token = token, role = principal.role))
        }
    }
}