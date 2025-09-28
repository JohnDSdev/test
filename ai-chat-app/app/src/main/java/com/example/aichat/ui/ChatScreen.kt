package com.example.aichat.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.aichat.data.Message
import com.example.aichat.data.MessageRole
import com.example.aichat.data.SecretsStore
import com.example.aichat.data.SettingsStore
import com.example.aichat.llm.LlmSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Chat UI allowing the user to converse with the local model.  Messages are
 * displayed chronologically, with the input field at the bottom.  The top bar
 * includes a settings icon to navigate to the settings screen.
 */
@Composable
fun ChatScreen(navController: NavHostController) {
    val context = LocalContext.current
    val settingsStore = remember { SettingsStore.get(context) }
    val secretsStore = remember { SecretsStore.get(context) }
    val session = remember { LlmSession(context, settingsStore, secretsStore) }
    val messages = remember { mutableStateListOf<Message>() }
    val input = remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "AI Chat") },
                actions = {
                    IconButton(onClick = { navController.navigate("settings") }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier
            .fillMaxSize()
            .padding(padding)) {
            LazyColumn(modifier = Modifier.weight(1f), reverseLayout = false) {
                items(messages) { msg ->
                    val prefix = when (msg.role) {
                        MessageRole.USER -> "You: "
                        MessageRole.ASSISTANT -> "Assistant: "
                        MessageRole.TOOL -> "Tool (${msg.name ?: "?"}): "
                        MessageRole.SYSTEM -> "System: "
                    }
                    Text(text = prefix + msg.content, modifier = Modifier.padding(8.dp))
                }
            }
            Divider()
            Row(modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)) {
                TextField(
                    value = input.value,
                    onValueChange = { input.value = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Enter your message...") }
                )
                IconButton(
                    onClick = {
                        val text = input.value.trim()
                        if (text.isNotEmpty()) {
                            input.value = ""
                            val userMsg = Message(MessageRole.USER, text)
                            messages.add(userMsg)
                            // Launch generation asynchronously
                            coroutineScope.launch(Dispatchers.IO) {
                                session.stream(messages.toList()).collect { assistantMsg ->
                                    // Remove any existing assistant stub at the end
                                    if (messages.isNotEmpty() && messages.last().role == MessageRole.ASSISTANT) {
                                        messages.removeAt(messages.size - 1)
                                    }
                                    messages.add(assistantMsg)
                                }
                            }
                        }
                    }
                ) {
                    Icon(Icons.Default.Send, contentDescription = "Send")
                }
            }
        }
    }
}