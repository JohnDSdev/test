package com.example.aichat.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.io.File

/**
 * Registry for discovering and managing locally downloaded LLM models.  All
 * models are stored under the app's files directory in a `models/` folder.
 */
class ModelRegistry(private val context: Context, private val settingsStore: SettingsStore) {
    data class LlmModel(val name: String, val file: File, val size: Long)

    private val _models = MutableStateFlow<List<LlmModel>>(emptyList())
    val models = _models.asStateFlow()

    /**
     * Refresh the list of available models by scanning the models folder.  This
     * should be invoked after downloads complete or when the app starts.
     */
    suspend fun refresh() {
        val modelsDir = File(context.filesDir, "models")
        if (!modelsDir.exists()) modelsDir.mkdirs()
        val files = modelsDir.listFiles { file -> file.extension.equals("gguf", ignoreCase = true) }
        val list = files?.map { file -> LlmModel(name = file.nameWithoutExtension, file = file, size = file.length()) } ?: emptyList()
        _models.update { list }
    }

    /**
     * Persist the selected model so that subsequent sessions will load it.  The
     * engine will be reloaded by [LlmSession] as needed.
     */
    suspend fun setActive(model: LlmModel) {
        settingsStore.setModelPath(model.file.absolutePath)
    }
}