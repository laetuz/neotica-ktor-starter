package id.neotica.data

import id.neotica.domain.model.Task
import id.neotica.domain.repository.TaskRepository
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Thread-safe in-memory implementation of [TaskRepository].
 * This whole class is replaced by an Exposed/Postgres implementation when a
 * database is introduced — routes and services are untouched.
 */
class InMemoryTaskRepository : TaskRepository {
    private val store = ConcurrentHashMap<String, MutableMap<String, Task>>()

    override suspend fun findAll(userId: String): List<Task> =
        store[userId]?.values?.sortedBy { it.title } ?: emptyList()

    override suspend fun findById(userId: String, id: String): Task? =
        store[userId]?.get(id)

    override suspend fun create(userId: String, title: String, description: String?): Task {
        val task = Task(
            id = UUID.randomUUID().toString(),
            title = title,
            description = description,
            ownerId = userId,
        )
        store.computeIfAbsent(userId) { ConcurrentHashMap() }[task.id] = task
        return task
    }

    override suspend fun delete(userId: String, id: String): Boolean =
        store[userId]?.remove(id) != null
}