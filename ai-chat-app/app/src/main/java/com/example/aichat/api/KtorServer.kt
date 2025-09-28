package com.example.aichat.api

import android.util.Log
import com.example.aichat.llm.LlmSession
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.cio.EngineMain
import io.ktor.server.cio.CIO
import io.ktor.server.engine.ApplicationEngine
import io.ktor.server.engine.embeddedServer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Encapsulates an embedded Ktor server running on a configurable port.  It
 * exposes Normal and Weird APIs based on the flags provided at startup.  The
 * server must be started and stopped from a background thread (e.g. within
 * a Service).
 */
class KtorServer(private val sessionProvider: () -> LlmSession) {
    private var engine: ApplicationEngine? = null

    suspend fun start(
        port: Int,
        localApiToken: String,
        normalEnabled: Boolean,
        weirdEnabled: Boolean
    ) = withContext(Dispatchers.IO) {
        stop() // ensure previous server is stopped
        try {
            engine = embeddedServer(CIO, port = port, host = "0.0.0.0") {
                // Install necessary plugins here (e.g. call logging)
                install(io.ktor.server.plugins.callloging.CallLogging)
                // Configure routes based on flags
                Routes.configure(this, sessionProvider(), localApiToken, normalEnabled, weirdEnabled)
            }.start(false)
            Log.i("KtorServer", "Server started on port $port")
        } catch (e: Exception) {
            Log.e("KtorServer", "Failed to start server", e)
        }
    }

    fun stop() {
        engine?.stop(1000, 2000)
        engine = null
    }
}