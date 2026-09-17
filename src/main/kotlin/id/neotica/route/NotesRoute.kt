package id.neotica.route

import id.neotica.domain.model.CreateNoteRequest
import id.neotica.domain.model.NotFoundException
import id.neotica.domain.model.UpdateNoteRequest
import id.neotica.service.NoteService
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

/**
 * Public CRUD for notes.
 *
 *   GET    /notes        → list all notes
 *   POST   /notes        → create  { "text": "..." }   → 201
 *   PUT    /notes/{id}   → update  { "text": "..." }   → 200 / 404
 *   DELETE /notes/{id}   → remove                       → 204 / 404
 */
class NotesRoute(
    private val noteService: NoteService,
) {
    fun mount(route: Route) {
        route.route("/notes") {
            get {
                call.respond(noteService.list())
            }

            post {
                val body = call.receive<CreateNoteRequest>()
                val note = noteService.create(body.text)
                call.respond(HttpStatusCode.Created, note)
            }

            put("/{id}") {
                val id = call.parameters["id"]
                    ?: throw NotFoundException("Note not found")
                val body = call.receive<UpdateNoteRequest>()
                val note = noteService.update(id, body.text) ?: throw NotFoundException("Note not found")
                call.respond(note)
            }

            delete("/{id}") {
                val id = call.parameters["id"]
                    ?: throw NotFoundException("Note not found")
                if (!noteService.delete(id)) {
                    throw NotFoundException("Note not found")
                }
                call.respond(HttpStatusCode.NoContent)
            }
        }
    }
}