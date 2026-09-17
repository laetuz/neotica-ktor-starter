package id.neotica.data.repository

import id.neotica.domain.model.Note
import id.neotica.domain.repository.NoteRepository
import java.time.Clock
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Thread-safe in-memory notes store. Process-local — restarts lose data.
 * Replace with an Exposed/Postgres implementation behind the same interface
 * when persistence lands (see AGENTS.md "Database design (Exposed)").
 */
class InMemoryNoteRepository(
    private val clock: Clock = Clock.systemUTC(),
) : NoteRepository {
    private val store = ConcurrentHashMap<String, Note>()

    override suspend fun findAll(): List<Note> =
        store.values.sortedBy { it.createdAt }

    override suspend fun findById(id: String): Note? =
        store[id]

    override suspend fun create(text: String): Note {
        val note = Note(
            id = UUID.randomUUID().toString(),
            text = text,
            createdAt = clock.millis(),
        )
        store[note.id] = note
        return note
    }

    override suspend fun update(id: String, text: String): Note? {
        val existing = store[id] ?: return null
        val updated = existing.copy(text = text)
        store[id] = updated
        return updated
    }

    override suspend fun delete(id: String): Boolean =
        store.remove(id) != null
}