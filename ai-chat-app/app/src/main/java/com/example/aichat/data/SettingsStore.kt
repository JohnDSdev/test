package com.example.aichat.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * A singleton holder for app settings persisted via DataStore.  This includes
 * non‑secret configuration such as API toggles, port number, model
 * hyperparameters and tool enablement flags.  Secrets (e.g. Ollama API key)
 * should be stored in [SecretsStore] instead.
 */
class SettingsStore private constructor(private val context: Context) {

    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

    // Preference keys
    // Keys are public so that callers such as SettingsScreen can reference
    // them when updating preferences via the update() function.
    object Keys {
        val normalApiEnabled = booleanPreferencesKey("normal_api_enabled")
        val weirdApiEnabled = booleanPreferencesKey("weird_api_enabled")
        val serverPort = intPreferencesKey("server_port")
        val localApiToken = stringPreferencesKey("local_api_token")
        val searchToolEnabled = booleanPreferencesKey("search_tool_enabled")
        val timeToolEnabled = booleanPreferencesKey("time_tool_enabled")
        val locationToolEnabled = booleanPreferencesKey("location_tool_enabled")
        val modelPath = stringPreferencesKey("model_path")
        val temperature = floatPreferencesKey("temperature")
        val topP = floatPreferencesKey("top_p")
        val topK = intPreferencesKey("top_k")
        val maxTokens = intPreferencesKey("max_tokens")
        val contextWindow = intPreferencesKey("context_window")
        val systemPrompt = stringPreferencesKey("system_prompt")
    }

    /**
     * An immutable snapshot of app settings.  Consumers should collect this via
     * [settingsFlow] rather than instantiating directly.
     */
    data class Settings(
        val normalApiEnabled: Boolean,
        val weirdApiEnabled: Boolean,
        val serverPort: Int,
        val localApiToken: String,
        val searchToolEnabled: Boolean,
        val timeToolEnabled: Boolean,
        val locationToolEnabled: Boolean,
        val modelPath: String?,
        val temperature: Float,
        val topP: Float,
        val topK: Int,
        val maxTokens: Int,
        val contextWindow: Int,
        val systemPrompt: String
    )

    /**
     * Flow emitting the current settings whenever they change.
     */
    val settingsFlow: Flow<Settings> = context.dataStore.data.map { prefs ->
        Settings(
            normalApiEnabled = prefs[Keys.normalApiEnabled] ?: true,
            weirdApiEnabled = prefs[Keys.weirdApiEnabled] ?: false,
            serverPort = prefs[Keys.serverPort] ?: 17890,
            localApiToken = prefs[Keys.localApiToken] ?: generateToken(),
            searchToolEnabled = prefs[Keys.searchToolEnabled] ?: false,
            timeToolEnabled = prefs[Keys.timeToolEnabled] ?: true,
            locationToolEnabled = prefs[Keys.locationToolEnabled] ?: false,
            modelPath = prefs[Keys.modelPath],
            temperature = prefs[Keys.temperature] ?: 0.7f,
            topP = prefs[Keys.topP] ?: 0.9f,
            topK = prefs[Keys.topK] ?: 40,
            maxTokens = prefs[Keys.maxTokens] ?: 512,
            contextWindow = prefs[Keys.contextWindow] ?: 8192,
            systemPrompt = prefs[Keys.systemPrompt] ?: "You are a helpful assistant."
        )
    }

    suspend fun update(update: suspend (Preferences.MutablePreferences) -> Unit) {
        context.dataStore.edit { prefs -> update(prefs) }
    }

    /**
     * Persist the absolute path of the active model.  Passing null clears the active model.
     */
    suspend fun setModelPath(path: String?) {
        update { prefs ->
            if (path == null) prefs.remove(Keys.modelPath) else prefs[Keys.modelPath] = path
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: SettingsStore? = null

        fun get(context: Context): SettingsStore =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: SettingsStore(context.applicationContext).also { INSTANCE = it }
            }

        /**
         * Generate a random token used for authenticating local API requests.
         * This implementation uses a simple pseudo‑random generator.  For
         * increased security consider using a cryptographically secure random.
         */
        fun generateToken(): String {
            val chars = (('a'..'z') + ('A'..'Z') + ('0'..'9')).toList()
            return (1..32).map { chars.random() }.joinToString("")
        }
    }
}