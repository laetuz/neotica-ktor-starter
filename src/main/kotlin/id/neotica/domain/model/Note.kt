package id.neotica.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Note(
    val id: String,
    val text: String,
    val createdAt: Long,
)

@Serializable
data class CreateNoteRequest(
    val text: String,
)

@Serializable
data class UpdateNoteRequest(
    val text: String,
)