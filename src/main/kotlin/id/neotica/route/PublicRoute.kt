package id.neotica.route

import id.neotica.domain.model.LoginRequest
import id.neotica.domain.model.LoginResponse
import id.neotica.service.AuthService
import id.neotica.service.TokenService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

class PublicRoute(
    private val authService: AuthService,
    private val tokenService: TokenService,
) {
    fun mount(route: Route) {
        route.get("/healthz") {
            call.respond(mapOf("status" to "ok"))
        }

        route.get("/test") {
            call.respond(HttpStatusCode.OK, "ini example response")
        }

        route.post("/auth/login") {
            val body = call.receive<LoginRequest>()
            val principal = authService.login(body.username)
            val token = tokenService.issue(principal.userId, principal.role)
            call.respond(LoginResponse(token = token, role = principal.role))
        }
    }
}