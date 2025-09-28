package com.example.aichat.data

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Persist secrets such as API keys in encrypted storage.  This store wraps
 * EncryptedSharedPreferences to protect sensitive data at rest.  When the
 * encryption key is generated, it is stored in the Android keystore.
 */
class SecretsStore private constructor(private val context: Context) {

    private val prefs by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "secrets",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    /**
     * Retrieve the stored Ollama API key, or null if none has been set.
     */
    fun getOllamaApiKey(): String? = prefs.getString(KEY_OLLAMA_API_KEY, null)

    /**
     * Persist the provided Ollama API key.  Passing null removes the key.
     */
    fun setOllamaApiKey(key: String?) {
        prefs.edit().apply {
            if (key == null) remove(KEY_OLLAMA_API_KEY) else putString(KEY_OLLAMA_API_KEY, key)
        }.apply()
    }

    companion object {
        private const val KEY_OLLAMA_API_KEY = "ollama_api_key"

        @Volatile
        private var INSTANCE: SecretsStore? = null

        fun get(context: Context): SecretsStore =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: SecretsStore(context.applicationContext).also { INSTANCE = it }
            }
    }
}