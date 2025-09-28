package com.example.aichat.llm

import android.content.Context
import com.example.aichat.data.Message
import com.example.aichat.data.MessageRole
import com.example.aichat.data.SecretsStore
import com.example.aichat.data.SettingsStore
import com.example.aichat.llm.tools.LocationTool
import com.example.aichat.llm.tools.SearchTool
import com.example.aichat.llm.tools.TimeTool
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * High‑level orchestrator for LLM interactions.  It wraps the Cactus Engine
 * and coordinates tool invocation.  At present this implementation is a
 * placeholder that returns a canned response while the full integration is
 * under development.
 */
class LlmSession(
    private val context: Context,
    private val settingsStore: SettingsStore,
    private val secretsStore: SecretsStore
) {
    private val engine by lazy { CactusEngineHolder.ensure(context) }

    /**
     * Stream tokens representing the assistant's reply given the conversation
     * history and enabled tools.  This stub simply emits a fixed message.
     *
     * In a complete implementation this would call into CactusEngine.stream()
     * with the provided messages, tools and sampling parameters, then use
     * [SearchTool], [TimeTool] and [LocationTool] when the model requests them.
     */
    fun stream(messages: List<Message>): Flow<Message> = flow {
        // Simulate latency.
        delay(500)
        val reply = "This is a stub response from the local model."
        emit(Message(role = MessageRole.ASSISTANT, content = reply))
    }
}