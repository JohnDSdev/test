package com.example.aichat.api

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.aichat.R
import com.example.aichat.data.SecretsStore
import com.example.aichat.data.SettingsStore
import com.example.aichat.llm.LlmSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Foreground service responsible for hosting the Ktor server.  The service
 * observes app settings and restarts the server when toggles such as API
 * enablement or port change.  A persistent notification informs the user that
 * the APIs are running and provides the phone's IP/port.
 */
class ServerService : Service() {
    private val coroutineScope = CoroutineScope(Dispatchers.Default)
    private var server: KtorServer? = null
    private var settingsJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val settingsStore = SettingsStore.get(this)
        val secretsStore = SecretsStore.get(this)
        // Cancel any previous settings observation job
        settingsJob?.cancel()
        settingsJob = coroutineScope.launch {
            settingsStore.settingsFlow.collectLatest { settings ->
                // On each settings change restart the server with new config
                server?.stop()
                if (settings.normalApiEnabled || settings.weirdApiEnabled) {
                    val session = LlmSession(this@ServerService, settingsStore, secretsStore)
                    val newServer = KtorServer { session }
                    newServer.start(
                        port = settings.serverPort,
                        localApiToken = settings.localApiToken,
                        normalEnabled = settings.normalApiEnabled,
                        weirdEnabled = settings.weirdApiEnabled
                    )
                    server = newServer
                    // Update notification text with current IP/port
                    val notif = buildNotification(settings.serverPort)
                    startForeground(NOTIF_ID, notif)
                } else {
                    // Stop foreground if APIs disabled
                    stopForeground(STOP_FOREGROUND_DETACH)
                    server = null
                }
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        settingsJob?.cancel()
        server?.stop()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun buildNotification(port: Int): Notification {
        val ip = "0.0.0.0" // In a real implementation, determine local LAN IP.
        val text = "AI Chat APIs running on http://$ip:$port"
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(text)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notification_channel_description)
            }
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }

    companion object {
        private const val CHANNEL_ID = "ai_chat_api"
        private const val NOTIF_ID = 1
    }
}