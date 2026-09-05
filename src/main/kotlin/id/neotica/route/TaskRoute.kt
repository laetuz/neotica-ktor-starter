package id.neotica.route

import id.neotica.domain.model.CreateTaskRequest
import id.neotica.domain.model.NotFoundException
import id.neotica.domain.model.Role
import id.neotica.domain.model.ValidationException
import id.neotica.service.TaskService
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

/**
 * Protected resource. Wrapped in `authenticate("auth-jwt")` so every handler
 * requires a valid token; `requireAnyRole(...)` gates admin-only operations.
 */
class TaskRoute(
    private val taskService: TaskService,
) {
    fun mount(route: Route) {
        route.authenticate("auth-jwt") {
            route("/tasks") {
                get {
                    val principal = call.principalOrThrow()
                    val tasks = taskService.list(principal.userId)
                    call.respond(tasks)
                }

                post {
                    val principal = call.principalOrThrow()
                    val body = call.receive<CreateTaskRequest>()

                    val errors = mutableListOf<String>()
                    if (body.title.isBlank()) {
                        errors += "title must not be blank"
                    }
                    if (errors.isNotEmpty()) {
                        throw ValidationException(errors.joinToString("; "))
                    }

                    val task = taskService.create(principal.userId, body.title.trim(), body.description)
                    call.respond(HttpStatusCode.Created, task)
                }

                delete("/{id}") {
                    val principal = call.requireAnyRole(Role.ADMIN)
                    val id = call.parameters["id"] ?: throw ValidationException("Missing id")
                    val deleted = taskService.delete(principal.userId, id)
                    if (!deleted) {
                        throw NotFoundException("Task not found")
                    }
                    call.respond(HttpStatusCode.NoContent)
                }
            }
        }
    }
}