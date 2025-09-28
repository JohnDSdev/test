package com.example.aichat.llm.tools

import android.util.Log
import com.example.aichat.data.SecretsStore
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.contentnegotiation.json
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Tool that performs a web search via the Ollama Search API.  The base URL
 * should be set to your Ollama Search endpoint.  If the API key is missing
 * or the feature is disabled, [search] returns a tool error message.
 */
class SearchTool(private val secretsStore: SecretsStore, private val baseUrl: String = DEFAULT_BASE) {
    companion object {
        const val NAME = "search"
        private const val DEFAULT_BASE = "https://search.ollama.ai"
    }

    private val client = HttpClient {
        install(ContentNegotiation) {
            json()
        }
    }

    @Serializable
    data class WebSearchRequest(
        val query: String,
        @SerialName("max_results") val maxResults: Int
    )

    @Serializable
    data class SearchResult(
        val title: String,
        val url: String
    )

    @Serializable
    data class WebSearchResponse(
        val results: List<SearchResult>
    )

    /**
     * Perform a web search using the Ollama Search API and return a list of
     * results.  When the API key is missing, an empty list is returned.
     */
    suspend fun search(query: String, maxResults: Int = 5): List<SearchResult> {
        val key = secretsStore.getOllamaApiKey() ?: return emptyList()
        return try {
            val resp: WebSearchResponse = client.post("$baseUrl/api/web_search") {
                contentType(ContentType.Application.Json)
                header("Authorization", "Bearer $key")
                setBody(WebSearchRequest(query, maxResults))
            }.body()
            resp.results
        } catch (e: Exception) {
            Log.e("SearchTool", "Search failed", e)
            emptyList()
        }
    }
}