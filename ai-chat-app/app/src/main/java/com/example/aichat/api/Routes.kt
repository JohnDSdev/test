package com.example.aichat.api

import com.example.aichat.data.Message
import com.example.aichat.data.MessageRole
import com.example.aichat.llm.LlmSession
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import kotlinx.coroutines.flow.first
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Defines all API routes for the embedded server.  This function
 * conditionally registers endpoints based on the `normalEnabled` and
 * `weirdEnabled` flags.  Authentication is performed by validating the
 * Authorization header against the provided `localApiToken`.
 */
object Routes {
    fun configure(
        app: Application,
        session: LlmSession,
        localApiToken: String,
        normalEnabled: Boolean,
        weirdEnabled: Boolean
    ) {
        // Install JSON serialization support
        app.install(ContentNegotiation) {
            io.ktor.serialization.kotlinx.json.json()
        }
        app.routing {
            if (normalEnabled) {
                post("/v1/chat") {
                    // Authenticate
                    val authHeader = call.request.headers["Authorization"]
                    val expected = "Bearer $localApiToken"
                    if (authHeader != expected) {
                        call.respond(HttpStatusCode.Unauthorized)
                        return@post
                    }
                    val req = call.receive<ChatRequest>()
                    // In this simplified implementation we ignore tools and params.
                    val messages = req.messages
                    val responseMessage = session.stream(messages).first()
                    if (req.stream == true) {
                        // Streaming not yet implemented; return single message for now.
                        call.respondText(
                            "event: message\ndata: ${Json.encodeToString(Message.serializer(), responseMessage)}\n\n",
                            contentType = ContentType.parse("text/event-stream")
                        )
                    } else {
                        call.respond(responseMessage)
                    }
                }
                post("/v1/speech") {
                    // Example endpoint for speaking text via TextToSpeech; not implemented.
                    call.respond(HttpStatusCode.NotImplemented)
                }
            }
            if (weirdEnabled) {
                // Mirror API: WebSocket streaming events.  Stub implementation.
                get("/events") {
                    call.respond(HttpStatusCode.NotImplemented)
                }
            }
        }
    }

    @Serializable
    data class ChatRequest(
        val model: String? = null,
        val messages: List<Message> = emptyList(),
        val tools: List<ToolToggle>? = null,
        val stream: Boolean? = false,
        val params: ChatParams? = null
    )

    @Serializable
    data class ToolToggle(val name: String, val enabled: Boolean)

    @Serializable
    data class ChatParams(
        val temperature: Float? = null,
        @SerialName("top_p") val topP: Float? = null,
        @SerialName("top_k") val topK: Int? = null,
        @SerialName("max_tokens") val maxTokens: Int? = null,
        @SerialName("context_window") val contextWindow: Int? = null
    )
}