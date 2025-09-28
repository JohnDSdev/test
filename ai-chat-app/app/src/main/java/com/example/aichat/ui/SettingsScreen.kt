package com.example.aichat.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.aichat.R
import com.example.aichat.data.ModelRegistry
import com.example.aichat.data.SecretsStore
import com.example.aichat.data.SettingsStore
import kotlinx.coroutines.launch

/**
 * Settings UI allowing users to toggle APIs, configure ports and LLM params,
 * manage API keys and choose models.  All changes persist via DataStore and
 * EncryptedSharedPreferences.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavHostController) {
    val context = LocalContext.current
    val settingsStore = remember { SettingsStore.get(context) }
    val secretsStore = remember { SecretsStore.get(context) }
    val coroutineScope = rememberCoroutineScope()
    val settings by settingsStore.settingsFlow.collectAsState(
        initial = SettingsStore.Settings(
            normalApiEnabled = true,
            weirdApiEnabled = false,
            serverPort = 17890,
            localApiToken = SettingsStore.generateToken(),
            searchToolEnabled = false,
            timeToolEnabled = true,
            locationToolEnabled = false,
            modelPath = null,
            temperature = 0.7f,
            topP = 0.9f,
            topK = 40,
            maxTokens = 512,
            contextWindow = 8192,
            systemPrompt = "You are a helpful assistant."
        )
    )
    val clipboard = LocalClipboardManager.current
    var apiKey by remember { mutableStateOf(secretsStore.getOllamaApiKey() ?: "") }

    // Model registry for listing local models
    val modelRegistry = remember { ModelRegistry(context, settingsStore) }
    val models by modelRegistry.models.collectAsState()
    LaunchedEffect(Unit) { modelRegistry.refresh() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding)) {
            item {
                ListItem(
                    headlineContent = { Text("Normal API") },
                    trailingContent = {
                        Switch(
                            checked = settings.normalApiEnabled,
                            onCheckedChange = { enabled ->
                                coroutineScope.launch {
                                    settingsStore.update { prefs ->
                                        prefs[SettingsStore.Keys.normalApiEnabled] = enabled
                                    }
                                }
                            }
                        )
                    },
                    supportingContent = { Text("Enable the OpenAI‑like API over LAN") }
                )
            }
            item {
                ListItem(
                    headlineContent = { Text("Weird API") },
                    trailingContent = {
                        Switch(
                            checked = settings.weirdApiEnabled,
                            onCheckedChange = { enabled ->
                                coroutineScope.launch {
                                    settingsStore.update { prefs ->
                                        prefs[SettingsStore.Keys.weirdApiEnabled] = enabled
                                    }
                                }
                            }
                        )
                    },
                    supportingContent = { Text("Enable the mirror events API over WebSocket") }
                )
            }
            item {
                ListItem(
                    headlineContent = { Text("Server Port") },
                    supportingContent = {
                        var portText by remember { mutableStateOf(settings.serverPort.toString()) }
                        TextField(
                            value = portText,
                            onValueChange = { value ->
                                portText = value.filter { it.isDigit() }
                                val p = portText.toIntOrNull()
                                if (p != null) {
                                    coroutineScope.launch {
                                        settingsStore.update { prefs ->
                                            prefs[SettingsStore.Keys.serverPort] = p
                                        }
                                    }
                                }
                            },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                )
            }
            item {
                ListItem(
                    headlineContent = { Text("Local API Token") },
                    supportingContent = { Text(settings.localApiToken) },
                    trailingContent = {
                        IconButton(onClick = {
                            clipboard.setText(androidx.compose.ui.text.AnnotatedString(settings.localApiToken))
                        }) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy")
                        }
                    }
                )
            }
            item {
                ListItem(
                    headlineContent = { Text("Enable Web Search Tool") },
                    trailingContent = {
                        Switch(
                            checked = settings.searchToolEnabled,
                            onCheckedChange = { enabled ->
                                coroutineScope.launch {
                                    settingsStore.update { prefs ->
                                        prefs[SettingsStore.Keys.searchToolEnabled] = enabled
                                    }
                                }
                            }
                        )
                    }
                )
            }
            item {
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Text(text = "Ollama Search API Key", style = MaterialTheme.typography.bodyMedium)
                    TextField(
                        value = apiKey,
                        onValueChange = { apiKey = it },
                        placeholder = { Text("Enter API key") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        Button(onClick = {
                            secretsStore.setOllamaApiKey(apiKey.ifBlank { null })
                        }) {
                            Text("Save")
                        }
                    }
                }
            }
            // LLM Parameters
            item {
                Text(
                    text = "LLM Parameters",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
                )
            }
            // Temperature
            item {
                ListItem(
                    headlineContent = { Text("Temperature: ${settings.temperature}") },
                    supportingContent = {
                        Slider(
                            value = settings.temperature,
                            onValueChange = { value ->
                                coroutineScope.launch {
                                    settingsStore.update { prefs ->
                                        prefs[SettingsStore.Keys.temperature] = value
                                    }
                                }
                            },
                            valueRange = 0f..1f,
                            steps = 10
                        )
                    }
                )
            }
            item {
                ListItem(
                    headlineContent = { Text("Top‑p: ${settings.topP}") },
                    supportingContent = {
                        Slider(
                            value = settings.topP,
                            onValueChange = { value ->
                                coroutineScope.launch {
                                    settingsStore.update { prefs ->
                                        prefs[SettingsStore.Keys.topP] = value
                                    }
                                }
                            },
                            valueRange = 0f..1f,
                            steps = 10
                        )
                    }
                )
            }
            item {
                ListItem(
                    headlineContent = { Text("Top‑k: ${settings.topK}") },
                    supportingContent = {
                        Slider(
                            value = settings.topK.toFloat(),
                            onValueChange = { value ->
                                coroutineScope.launch {
                                    settingsStore.update { prefs ->
                                        prefs[SettingsStore.Keys.topK] = value.toInt()
                                    }
                                }
                            },
                            valueRange = 1f..100f,
                            steps = 99
                        )
                    }
                )
            }
            item {
                ListItem(
                    headlineContent = { Text("Max Tokens: ${settings.maxTokens}") },
                    supportingContent = {
                        Slider(
                            value = settings.maxTokens.toFloat(),
                            onValueChange = { value ->
                                coroutineScope.launch {
                                    settingsStore.update { prefs ->
                                        prefs[SettingsStore.Keys.maxTokens] = value.toInt()
                                    }
                                }
                            },
                            valueRange = 32f..4096f,
                            steps = 32
                        )
                    }
                )
            }
            // Tools toggles
            item {
                Text(
                    text = "Tools",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
                )
            }
            item {
                ListItem(
                    headlineContent = { Text("Time Tool") },
                    trailingContent = {
                        Switch(
                            checked = settings.timeToolEnabled,
                            onCheckedChange = { enabled ->
                                coroutineScope.launch {
                                    settingsStore.update { prefs ->
                                        prefs[SettingsStore.Keys.timeToolEnabled] = enabled
                                    }
                                }
                            }
                        )
                    }
                )
            }
            item {
                ListItem(
                    headlineContent = { Text("Location Tool") },
                    trailingContent = {
                        Switch(
                            checked = settings.locationToolEnabled,
                            onCheckedChange = { enabled ->
                                coroutineScope.launch {
                                    settingsStore.update { prefs ->
                                        prefs[SettingsStore.Keys.locationToolEnabled] = enabled
                                    }
                                }
                            }
                        )
                    }
                )
            }
            // Model picker
            item {
                Text(
                    text = "Models",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
                )
            }
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    val expanded = remember { mutableStateOf(false) }
                    OutlinedButton(onClick = { expanded.value = !expanded.value }) {
                        Text(settings.modelPath?.substringAfterLast('/') ?: "Select Model")
                    }
                    DropdownMenu(expanded = expanded.value, onDismissRequest = { expanded.value = false }) {
                        models.forEach { model ->
                            DropdownMenuItem(onClick = {
                                expanded.value = false
                                coroutineScope.launch { modelRegistry.setActive(model) }
                            }, text = { Text(model.name) })
                        }
                    }
                    Button(onClick = { coroutineScope.launch { modelRegistry.refresh() } }, modifier = Modifier.padding(top = 8.dp)) {
                        Text("Refresh Models")
                    }
                }
            }
            item { Spacer(Modifier.height(32.dp)) }
        }
    }
}