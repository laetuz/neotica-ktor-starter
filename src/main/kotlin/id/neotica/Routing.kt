package id.neotica

import id.neotica.domain.model.ErrorResponse
import id.neotica.domain.model.ForbiddenException
import id.neotica.domain.model.NotFoundException
import id.neotica.domain.model.ValidationException
import id.neotica.route.HelloRoute
import id.neotica.route.NotesRoute
import id.neotica.route.PublicRoute
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.get

/**
 * Installs the unified error contract (typed exceptions → structured JSON) and
 * mounts every route group. `/auth/login` is mounted only when auth is enabled.
 */
object Routing {

    fun configure(app: Application) {
        val helloRoute: HelloRoute = app.get()
        val notesRoute: NotesRoute = app.get()

        app.install(StatusPages) {
            status(HttpStatusCode.NotFound) { call, _ ->
                call.respond(HttpStatusCode.NotFound, ErrorResponse("not_found", "Resource not found"))
            }
            exception<ValidationException> { call, e ->
                call.respond(HttpStatusCode.BadRequest, ErrorResponse("invalid_request", e.message ?: "Invalid request"))
            }
            exception<NotFoundException> { call, e ->
                call.respond(HttpStatusCode.NotFound, ErrorResponse("not_found", e.message ?: "Not found"))
            }
            exception<ForbiddenException> { call, e ->
                call.respond(HttpStatusCode.Forbidden, ErrorResponse("forbidden", e.message ?: "Forbidden"))
            }
            exception<Throwable> { call, e ->
                // Never leak the internal exception message to clients.
                call.application.log.error("Unhandled exception", e)
                call.respond(HttpStatusCode.InternalServerError, ErrorResponse("internal_error", "Something went wrong"))
            }
        }

       app.routing {
            helloRoute.mount(this)
            notesRoute.mount(this)

            if (AppConfig.authEnabled) {
                val publicRoute: PublicRoute = app.get()
                publicRoute.mount(this)
            }
        }
    }
}