package com.example.aichat

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.aichat.ui.ChatScreen
import com.example.aichat.ui.SettingsScreen

/**
 * Root composable of the application.  Defines navigation graph and
 * applies the Material3 theme.
 */
@Composable
fun AIChatApp() {
    val navController = rememberNavController()
    MaterialTheme {
        Surface {
            NavHost(navController = navController, startDestination = "chat") {
                composable("chat") { ChatScreen(navController) }
                composable("settings") { SettingsScreen(navController) }
            }
        }
    }
}