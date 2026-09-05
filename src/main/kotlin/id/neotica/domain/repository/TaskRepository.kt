package id.neotica.domain.repository

import id.neotica.domain.model.Task

/**
 * Data-access contract lives in `domain`.
 * Implementations (in `data/`) can swap storage without touching routes or
 * services — e.g. the in-memory store here → an Exposed/Postgres impl later.
 */
interface TaskRepository {
    suspend fun findAll(userId: String): List<Task>
    suspend fun findById(userId: String, id: String): Task?
    suspend fun create(userId: String, title: String, description: String?): Task
    suspend fun delete(userId: String, id: String): Boolean
}