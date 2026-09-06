package com.example.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.api.YouTubeUploader

@Composable
fun YouTubeAuthDialog(
    initialToken: String,
    isConnected: Boolean,
    channelName: String,
    onSaveToken: (String) -> Unit,
    onSaveTokenAndUpload: ((String) -> Unit)? = null,
    onDismiss: () -> Unit,
    onUploadViaAppInstead: () -> Unit
) {
    val context = LocalContext.current
    var tokenInput by remember(initialToken) { mutableStateOf(initialToken) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 640.dp)
                .padding(8.dp)
                .testTag("youtube_auth_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header Row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color(0xFFFF0000), shape = RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CloudUpload,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Connect YouTube Channel",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (isConnected) "Connected: $channelName" else "Required for direct automated upload",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isConnected) Color(0xFF1B873F) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Info banner explaining direct upload
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Direct Cloud Publishing",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Text(
                            text = "Automated YouTube uploads stream 720p HD MP4 video directly to your channel with chapters and timestamps using the official YouTube Data API v3.",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                // 4-step quick guide
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "How to get your OAuth Token (30 seconds):",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "1. Click the button below to open Google OAuth Playground.\n2. Scroll to 'YouTube Data API v3' and check 'https://www.googleapis.com/auth/youtube.upload'.\n3. Click 'Authorize APIs' and sign in with your Google account.\n4. Click 'Exchange authorization code for tokens', copy the Access token, and paste it below.",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp,
                            lineHeight = 15.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(2.dp))
                        OutlinedButton(
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://developers.google.com/oauthplayground"))
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                context.startActivity(intent)
                            },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.primary
                            ),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Open Google OAuth Playground", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Text(
                    text = "OAuth 2.0 Access Token",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                OutlinedTextField(
                    value = tokenInput,
                    onValueChange = { tokenInput = it },
                    label = { Text("Paste Token (starts with ya29...)") },
                    placeholder = { Text("ya29.a0AdMD...") },
                    minLines = 2,
                    maxLines = 4,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("youtube_token_input")
                )

                val cleanedToken = YouTubeUploader.sanitizeToken(tokenInput)
                if (tokenInput.isNotBlank()) {
                    if (tokenInput.contains("Authorization:", ignoreCase = true) || tokenInput.contains("Bearer", ignoreCase = true)) {
                        Text(
                            text = "✓ 'Authorization: Bearer' prefix detected and will be auto-trimmed.",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF1B873F),
                            fontSize = 11.sp
                        )
                    }

                    val len = cleanedToken?.length ?: 0
                    if (len in 1..80) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Warning,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Token appears truncated ($len characters). Standard Google OAuth access tokens are usually 150–250+ characters long. In OAuth Playground, make sure to select and copy the complete text of the Access Token field.",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    } else if (len > 80) {
                        Text(
                            text = "✓ Valid token length ($len chars)",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF1B873F),
                            fontSize = 11.sp
                        )
                    }
                }

                // Action Buttons
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (onSaveTokenAndUpload != null && tokenInput.isNotBlank()) {
                        Button(
                            onClick = {
                                onSaveTokenAndUpload(tokenInput)
                                onDismiss()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF0000)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("save_and_upload_button")
                        ) {
                            Icon(Icons.Filled.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Save Token & Upload Now", fontWeight = FontWeight.Bold)
                        }
                    }

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
                            onClick = {
                                onSaveToken(tokenInput)
                                onDismiss()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (onSaveTokenAndUpload != null && tokenInput.isNotBlank())
                                    MaterialTheme.colorScheme.secondary
                                else
                                    Color(0xFFCC0000)
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(if (tokenInput.isNotBlank()) "Save Token Only" else "Clear Token")
                        }
                    }
                }
            }
        }
    }
}
