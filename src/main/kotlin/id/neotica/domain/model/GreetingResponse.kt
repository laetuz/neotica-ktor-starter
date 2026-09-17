package id.neotica.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class GreetingResponse(
    val message: String,
)