package id.neotica.domain.model

import kotlinx.serialization.Serializable

/** Structured error body returned for every failure. */
@Serializable
data class ErrorResponse(
    val code: String,
    val message: String,
)