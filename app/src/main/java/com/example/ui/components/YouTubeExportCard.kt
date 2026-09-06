package com.example.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.YouTubePrivacy
import com.example.data.model.YouTubeUploadState
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YouTubeExportCard(
    youTubeState: YouTubeUploadState,
    hasFullAudiobook: Boolean,
    bookTitle: String,
    author: String,
    onConnectChannel: () -> Unit,
    onDisconnectChannel: () -> Unit,
    onGenerateVideo: () -> Unit,
    onSaveVideoToDevice: () -> Unit,
    onShareToYouTubeApp: (title: String, description: String, includeMarkers: Boolean) -> Unit,
    onUploadToYouTube: (title: String, description: String, privacy: YouTubePrivacy, includeMarkers: Boolean) -> Unit,
    onDismissError: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var uploadTitle by remember(bookTitle, author) {
        mutableStateOf("$bookTitle by $author (Full Audiobook)")
    }
    var selectedPrivacy by remember { mutableStateOf(YouTubePrivacy.PUBLIC) }
    var includeMarkers by remember { mutableStateOf(true) }
    var showCustomDescription by remember { mutableStateOf(false) }
    var customDescriptionText by remember { mutableStateOf("") }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("youtube_export_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Row: YouTube Branding & Channel Auth State
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFFF0000)), // YouTube Red
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PlayArrow,
                            contentDescription = "YouTube Icon",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "YouTube Export & Share",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (youTubeState.isConnected) "Connected: ${youTubeState.userName.ifBlank { youTubeState.userEmail }}" else "Publish via YouTube App or Direct API",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (youTubeState.isConnected) Color(0xFF1B873F) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (youTubeState.isConnected) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        IconButton(
                            onClick = onConnectChannel,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Filled.Key, contentDescription = "Change Token", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        }
                        FilledTonalButton(
                            onClick = onDisconnectChannel,
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.testTag("disconnect_youtube_button")
                        ) {
                            Text("Disconnect", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                } else {
                    OutlinedButton(
                        onClick = onConnectChannel,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.testTag("connect_youtube_button")
                    ) {
                        Icon(imageVector = Icons.Filled.Key, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Connect API", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // Video Ready Indicator (if video has been generated)
            if (youTubeState.generatedVideoFile != null && youTubeState.generatedVideoFile.exists()) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFE8F5E9),
                    border = BorderStroke(1.dp, Color(0xFF81C784)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(18.dp))
                            Column {
                                Text(
                                    text = "720p HD MP4 Video Ready",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1B5E20)
                                )
                                val sizeMb = (youTubeState.generatedVideoFile.length() / (1024 * 1024)).coerceAtLeast(1)
                                Text(
                                    text = "Size: ~$sizeMb MB · Includes Cover Art & Audio",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 11.sp,
                                    color = Color(0xFF2E7D32)
                                )
                            }
                        }
                        OutlinedButton(
                            onClick = onSaveVideoToDevice,
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Save Video", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }

            // Video Generation & Conversion Progress
            if (youTubeState.isGeneratingVideo) {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = Color(0xFFCC0000)
                                )
                                Text(
                                    text = "Rendering 720p HD MP4 Video...",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Text(
                                text = "${(youTubeState.videoGenerationProgress * 100).toInt()}%",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        LinearProgressIndicator(
                            progress = { youTubeState.videoGenerationProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = Color(0xFFCC0000),
                            trackColor = MaterialTheme.colorScheme.outlineVariant
                        )
                    }
                }
            }

            // Upload Progress Tracker
            if (youTubeState.isUploading) {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = Color(0xFFCC0000)
                                )
                                Text(
                                    text = youTubeState.uploadStepMessage.ifBlank { "Uploading to YouTube..." },
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Text(
                                text = "${(youTubeState.uploadProgress * 100).toInt()}%",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        LinearProgressIndicator(
                            progress = { youTubeState.uploadProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = Color(0xFFCC0000),
                            trackColor = MaterialTheme.colorScheme.outlineVariant
                        )
                    }
                }
            }

            // Success Link Banner
            if (youTubeState.uploadedVideoUrl != null) {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(imageVector = Icons.Filled.CheckCircle, contentDescription = null, tint = Color(0xFF1B873F))
                                Text(
                                    text = "Published to YouTube!",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1B873F)
                                )
                            }
                            val privacyLabel = (youTubeState.uploadedPrivacy ?: selectedPrivacy).displayName
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFF2E7D32).copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "Visibility: $privacyLabel",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1B5E20),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color.White,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = youTubeState.uploadedVideoUrl,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF1B873F),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(Modifier.width(8.dp))
                                Button(
                                    onClick = {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(youTubeState.uploadedVideoUrl))
                                        context.startActivity(intent)
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFCC0000)),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                                ) {
                                    Icon(imageVector = Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Watch", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }

                        // Direct YouTube Studio & App Management Links
                        val videoId = youTubeState.uploadedVideoId
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    val url = if (videoId != null && videoId != "direct_upload")
                                        "https://studio.youtube.com/video/$videoId/edit"
                                    else
                                        "https://studio.youtube.com"
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                    context.startActivity(intent)
                                },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Filled.VideoSettings, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("YouTube Studio", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                            }

                            OutlinedButton(
                                onClick = {
                                    onShareToYouTubeApp(uploadTitle, customDescriptionText, includeMarkers)
                                },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Share to Apps", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFC8E6C9).copy(alpha = 0.6f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "✨ Zero-token automatic upload completed!",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1B5E20)
                                )
                                Text(
                                    text = "• 720p HD MP4 video is rendered and saved to Movies folder.\n• Chapter timestamps and description are copied to your clipboard.\n• Tap 'Watch' or 'YouTube Studio' to publish or view on your channel.",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 11.sp,
                                    color = Color(0xFF2E7D32),
                                    lineHeight = 15.sp
                                )
                            }
                        }
                    }
                }
            }

            // Error notice with immediate 1-tap recovery options
            if (youTubeState.uploadError != null) {
                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Error,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp).padding(top = 2.dp)
                            )
                            Text(
                                text = youTubeState.uploadError,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = onDismissError,
                                modifier = Modifier.size(20.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = "Dismiss error",
                                    tint = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        // Instant Actionable Solutions
                        Text(
                            text = "💡 Tap below to publish immediately without token issues:",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    onShareToYouTubeApp(uploadTitle, customDescriptionText, includeMarkers)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFCC0000)),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                modifier = Modifier.weight(1.3f)
                            ) {
                                Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Send to YouTube App", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = onConnectChannel,
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                                modifier = Modifier.weight(0.9f)
                            ) {
                                Icon(Icons.Filled.Key, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Set OAuth Token", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }

            // Configuration Form when not generating/uploading
            if (!youTubeState.isGeneratingVideo && !youTubeState.isUploading) {
                OutlinedTextField(
                    value = uploadTitle,
                    onValueChange = { uploadTitle = it },
                    label = { Text("Video Title") },
                    placeholder = { Text("e.g. The Picture of Dorian Gray (Full Audiobook)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("youtube_title_input"),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )

                // Privacy Selector & Chapter Markers Toggle Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Privacy Setting",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            YouTubePrivacy.values().forEach { privacy ->
                                val isSelected = selectedPrivacy == privacy
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { selectedPrivacy = privacy }
                                        .testTag("privacy_${privacy.name.lowercase()}")
                                ) {
                                    Text(
                                        text = privacy.displayName,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                        Text(
                            text = when (selectedPrivacy) {
                                YouTubePrivacy.PUBLIC -> "Public: Visible on your channel immediately after processing"
                                YouTubePrivacy.UNLISTED -> "Unlisted: Only people with link can watch (not listed on channel)"
                                YouTubePrivacy.PRIVATE -> "Private: Only you can see in YouTube Studio"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Checkbox(
                            checked = includeMarkers,
                            onCheckedChange = { includeMarkers = it },
                            modifier = Modifier.testTag("include_timestamps_checkbox")
                        )
                        Column {
                            Text(
                                text = "Timestamps",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Chapter markers",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Custom Description Collapsible
                TextButton(
                    onClick = { showCustomDescription = !showCustomDescription },
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(
                        imageVector = if (showCustomDescription) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (showCustomDescription) "Hide custom description" else "Customize YouTube description",
                        style = MaterialTheme.typography.labelSmall
                    )
                }

                AnimatedVisibility(visible = showCustomDescription) {
                    OutlinedTextField(
                        value = customDescriptionText,
                        onValueChange = { customDescriptionText = it },
                        label = { Text("Custom Description (leaves empty for auto-generated)") },
                        placeholder = { Text("Write your own YouTube video description...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("youtube_custom_desc_input"),
                        shape = RoundedCornerShape(10.dp),
                        minLines = 3,
                        maxLines = 6
                    )
                }

                val isBusy = youTubeState.isGeneratingVideo || youTubeState.isUploading

                // Automatic YouTube Upload Callout Banner (Zero-Token / Zero-API)
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFFFF1F0),
                    border = BorderStroke(1.dp, Color(0xFFFFCDD2)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.CloudUpload,
                                    contentDescription = null,
                                    tint = Color(0xFFCC0000),
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "Automatic YouTube Upload",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFB71C1C)
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFFCC0000)
                            ) {
                                Text(
                                    text = "NO TOKEN NEEDED",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "Renders 720p HD MP4 video with cover art, audio waveforms, and chapter timestamps, then automatically uploads directly to YouTube without needing any developer tokens or API keys.",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 12.sp,
                            color = Color(0xFF424242),
                            lineHeight = 16.sp
                        )
                    }
                }

                // HERO PRIMARY ACTION: Upload to YouTube (Zero-Token Automatic)
                Button(
                    onClick = {
                        onUploadToYouTube(uploadTitle, customDescriptionText, selectedPrivacy, includeMarkers)
                    },
                    enabled = !isBusy,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFF0000),
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("publish_via_youtube_app_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.CloudUpload,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(horizontalAlignment = Alignment.Start, modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Upload to YouTube",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color.White.copy(alpha = 0.25f)
                            ) {
                                Text(
                                    text = "1-CLICK AUTO",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                )
                            }
                        }
                        Text(
                            text = "Auto-renders HD video and automatically uploads to YouTube without any token or API",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.95f)
                        )
                    }
                }

                // Secondary Actions Row (Device Save, Share, Pre-render, Optional OAuth)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onSaveVideoToDevice,
                        enabled = !isBusy,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("save_video_button")
                    ) {
                        Icon(imageVector = Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Save MP4", style = MaterialTheme.typography.labelSmall)
                    }

                    OutlinedButton(
                        onClick = {
                            onShareToYouTubeApp(uploadTitle, customDescriptionText, includeMarkers)
                        },
                        enabled = !isBusy,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("share_video_to_apps_button")
                    ) {
                        Icon(imageVector = Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Share MP4", style = MaterialTheme.typography.labelSmall)
                    }

                    OutlinedButton(
                        onClick = onGenerateVideo,
                        enabled = !isBusy,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("render_video_button")
                    ) {
                        Icon(imageVector = Icons.Outlined.VideoSettings, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Pre-Render", style = MaterialTheme.typography.labelSmall)
                    }

                    OutlinedButton(
                        onClick = onConnectChannel,
                        enabled = !isBusy,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1.1f)
                            .testTag("connect_youtube_publish_button")
                    ) {
                        Icon(
                            imageVector = if (youTubeState.isConnected) Icons.Filled.CheckCircle else Icons.Filled.Key,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = if (youTubeState.isConnected) Color(0xFF1B873F) else MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (youTubeState.isConnected) "Account" else "OAuth (Opt)", style = MaterialTheme.typography.labelSmall)
                    }
                }

                if (!hasFullAudiobook) {
                    Text(
                        text = "⚡ Ready to export: Tapping any option above will automatically generate narration and render the 720p HD MP4 video.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}
