package id.neotica.domain.repository

import id.neotica.domain.model.Note

/**
 * Note storage contract. The current implementation lives in `data/`
 * (`InMemoryNoteRepository`) and can be swapped for an Exposed/Postgres
 * implementation without touching routes or services.
 */
interface NoteRepository {
    suspend fun findAll(): List<Note>
    suspend fun findById(id: String): Note?
    suspend fun create(text: String): Note
    suspend fun update(id: String, text: String): Note?
    suspend fun delete(id: String): Boolean
}