package com.example.aichat.llm

import android.content.Context
import com.cactusml.compute.CactusEngine

/**
 * Singleton holder for the Cactus Compute engine.  Lazily instantiates
 * [CactusEngine] on first access and caches it for the lifetime of the process.
 */
object CactusEngineHolder {
    @Volatile
    private var engine: CactusEngine? = null

    /**
     * Ensure an engine instance exists.  If not, create one.  Thread safe.
     */
    fun ensure(context: Context): CactusEngine =
        engine ?: synchronized(this) {
            engine ?: CactusEngine(context).also { engine = it }
        }
}