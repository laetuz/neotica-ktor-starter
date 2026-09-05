package id.neotica.service

import id.neotica.domain.model.Task

/**
 * Business logic — coordinates the repository. Routes stay thin and only talk
 * to a service, never directly to storage.
 */
class TaskService(
    private val repository: id.neotica.domain.repository.TaskRepository,
) {
    suspend fun list(userId: String): List<Task> = repository.findAll(userId)

    suspend fun create(userId: String, title: String, description: String?): Task =
        repository.create(userId, title, description)

    suspend fun delete(userId: String, id: String): Boolean = repository.delete(userId, id)
}