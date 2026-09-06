package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.launch

@Composable
fun ApiKeySettingsDialog(
    currentGeminiKey: String,
    currentCVoiceKey: String,
    currentCVoiceUrl: String,
    currentAutoFallback: Boolean,
    onTestCVoice: suspend (apiKey: String, baseUrl: String) -> Result<String>,
    onSave: (geminiKey: String, cvoiceKey: String, cvoiceUrl: String, autoFallback: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var geminiKey by remember { mutableStateOf(currentGeminiKey) }
    var cvoiceKey by remember { mutableStateOf(currentCVoiceKey) }
    val initialUrl = remember(currentCVoiceUrl) {
        if (currentCVoiceUrl.isBlank() || currentCVoiceUrl.contains("api.cvoice.ai")) {
            "https://cvoice.ai/api/tts"
        } else {
            currentCVoiceUrl
        }
    }
    var cvoiceUrl by remember { mutableStateOf(initialUrl) }
    var autoFallback by remember { mutableStateOf(currentAutoFallback) }

    var isTestingConnection by remember { mutableStateOf(false) }
    var testResultMessage by remember { mutableStateOf<String?>(null) }
    var isTestSuccess by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 580.dp)
                .padding(16.dp)
                .testTag("api_settings_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Settings,
                        contentDescription = "Settings",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Column {
                        Text(
                            text = "API & Engine Configuration",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Server credentials & cvoice.ai access",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Security Note
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Security,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Credentials are bound securely from environment / BuildConfig. You may also supply an API key directly here for testing.",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Gemini Key
                OutlinedTextField(
                    value = geminiKey,
                    onValueChange = { geminiKey = it },
                    label = { Text("Gemini AI API Key") },
                    placeholder = { Text("AI Studio injected or custom key") },
                    leadingIcon = {
                        Icon(Icons.Outlined.Psychology, contentDescription = null, modifier = Modifier.size(18.dp))
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("gemini_key_input")
                )

                // Engine Presets Quick-Select
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Quick Engine Select:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val isOpenAiActive = cvoiceUrl.contains("openai.com") || cvoiceKey.startsWith("sk-")
                        FilterChip(
                            selected = isOpenAiActive,
                            onClick = {
                                cvoiceUrl = "https://api.openai.com/v1/audio/speech"
                                if (!cvoiceKey.startsWith("sk-")) {
                                    cvoiceKey = "sk-ijkl1234ijkl1234ijkl1234ijkl1234ijkl1234"
                                }
                            },
                            label = { Text("OpenAI Neural TTS", fontSize = 12.sp) },
                            leadingIcon = if (isOpenAiActive) {
                                { Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                            } else null,
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = !isOpenAiActive,
                            onClick = {
                                cvoiceUrl = "https://cvoice.ai/api/tts"
                                if (cvoiceKey.startsWith("sk-")) {
                                    cvoiceKey = ""
                                }
                            },
                            label = { Text("cvoice.ai Engine", fontSize = 12.sp) },
                            leadingIcon = if (!isOpenAiActive) {
                                { Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                            } else null,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // cvoice.ai / OpenAI Key
                OutlinedTextField(
                    value = cvoiceKey,
                    onValueChange = { 
                        cvoiceKey = it
                        if (it.startsWith("sk-") && !cvoiceUrl.contains("openai")) {
                            cvoiceUrl = "https://api.openai.com/v1/audio/speech"
                        }
                    },
                    label = { 
                        Text(if (cvoiceKey.startsWith("sk-") || cvoiceUrl.contains("openai")) "OpenAI API Key (Speech & Fallback LLM)" else "TTS Engine Key (cvoice.ai or OpenAI)")
                    },
                    placeholder = { Text("sk-ijkl1234... or cvai_...") },
                    leadingIcon = {
                        Icon(Icons.Outlined.RecordVoiceOver, contentDescription = null, modifier = Modifier.size(18.dp))
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("cvoice_key_input")
                )

                // cvoice.ai / OpenAI Endpoint
                OutlinedTextField(
                    value = cvoiceUrl,
                    onValueChange = { cvoiceUrl = it },
                    label = { Text("TTS Endpoint URL") },
                    placeholder = { Text("https://api.openai.com/v1/audio/speech or https://cvoice.ai/api/tts") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                // Test Connection Button & Status
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            isTestingConnection = true
                            testResultMessage = null
                            coroutineScope.launch {
                                val result = onTestCVoice(cvoiceKey, cvoiceUrl)
                                isTestingConnection = false
                                if (result.isSuccess) {
                                    isTestSuccess = true
                                    testResultMessage = result.getOrThrow()
                                } else {
                                    isTestSuccess = false
                                    testResultMessage = result.exceptionOrNull()?.message ?: "Connection test failed."
                                }
                            }
                        },
                        enabled = !isTestingConnection,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("test_cvoice_connection_button")
                    ) {
                        if (isTestingConnection) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Verifying Voice Engine API...", fontSize = 13.sp)
                        } else {
                            Icon(Icons.Filled.Sensors, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Test Voice Engine Connection", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    if (testResultMessage != null) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isTestSuccess) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = if (isTestSuccess) Icons.Filled.CheckCircle else Icons.Filled.ErrorOutline,
                                    contentDescription = null,
                                    tint = if (isTestSuccess) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = testResultMessage ?: "",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 12.sp,
                                    color = if (isTestSuccess) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                }

                // Fallback switch
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Local Fallback Engine",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Use local TTS / synthesizer when cvoice.ai is offline, with clear badge indicator.",
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = autoFallback,
                            onCheckedChange = { autoFallback = it }
                        )
                    }
                }

                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onSave(geminiKey, cvoiceKey, cvoiceUrl, autoFallback) },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("save_settings_button")
                    ) {
                        Text("Save Configuration")
                    }
                }
            }
        }
    }
}
