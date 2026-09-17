package id.neotica.route

import id.neotica.domain.model.GreetingResponse
import id.neotica.service.GreetingService
import io.ktor.server.response.*
import io.ktor.server.routing.*

/**
 * The generic, newcomer-friendly endpoint. Always mounted.
 *
 *   GET /hello           → {"message":"Hello, World!"}
 *   GET /hello?name=Ktor → {"message":"Hello, Ktor!"}
 */
class HelloRoute(
    private val greetingService: GreetingService,
) {
    fun mount(route: Route) {
        route.get("/hello") {
            val name = call.request.queryParameters["name"]
            call.respond(GreetingResponse(greetingService.greet(name)))
        }
    }
}