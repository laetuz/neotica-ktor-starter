package id.neotica.service

import id.neotica.domain.model.Note
import id.neotica.domain.model.ValidationException

/**
 * Thin business layer for notes. Routes call this service only — never the
 * repository directly.
 */
class NoteService(
    private val repository: id.neotica.domain.repository.NoteRepository,
) {
    suspend fun list(): List<Note> = repository.findAll()

    suspend fun create(rawText: String): Note {
        val text = requireValidText(rawText)
        return repository.create(text)
    }

    suspend fun update(id: String, rawText: String): Note? {
        val text = requireValidText(rawText)
        return repository.update(id, text)
    }

    suspend fun delete(id: String): Boolean = repository.delete(id)

    private fun requireValidText(raw: String): String {
        val text = raw.trim()
        if (text.isEmpty()) {
            throw ValidationException("text must not be blank")
        }
        return text
    }
}