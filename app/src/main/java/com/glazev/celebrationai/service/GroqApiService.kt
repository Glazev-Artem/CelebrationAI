package com.glazev.celebrationai.service

import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

@Serializable
data class GroqRequest(
    val model: String,
    val messages: List<GroqMessage>
)

@Serializable
data class GroqMessage(
    val role: String,
    val content: String
)

@Serializable
data class GroqResponse(
    val choices: List<GroqChoice>,
    val error: GroqError? = null
)

@Serializable
data class GroqChoice(
    val message: GroqMessage
)

@Serializable
data class GroqError(
    val message: String,
    val code: String? = null
)

interface GroqApiService {
    @POST("chat/completions")
    suspend fun generateContent(
        @Header("Authorization") auth: String,
        @Body request: GroqRequest
    ): GroqResponse
}
