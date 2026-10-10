package com.example.ui.components

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutomationDashboardSection(
    automationState: AutomationDashboardState,
    youTubeConnected: Boolean,
    youTubeChannelName: String,
    onConnectGoogleSheets: (String, String, String) -> Unit,
    onConnectYouTube: () -> Unit,
    onUploadMusicFiles: (List<Uri>) -> Unit,
    onSelectDefaultMusic: (String) -> Unit,
    onRefreshSheet: () -> Unit,
    onStartAutomation: () -> Unit,
    onPauseQueue: () -> Unit,
    onStopAutomation: () -> Unit,
    onRetryFailedJobs: () -> Unit,
    onAddRowToQueue: (SheetVideoRow) -> Unit,
    onUpdateSettings: (
        keepOriginalAudio: Boolean,
        originalVol: Float,
        musicVol: Float,
        fadeIn: Float,
        fadeOut: Float,
        privacy: YouTubePrivacy,
        defaultDesc: String
    ) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showConnectSheetDialog by remember { mutableStateOf(false) }
    var showUploadedVideosDialog by remember { mutableStateOf(false) }
    var showAddRowDialog by remember { mutableStateOf(false) }
    var showAutomationSettingsDialog by remember { mutableStateOf(false) }

    val multiAudioPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            onUploadMusicFiles(uris)
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("automation_dashboard_section"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. HERO STATUS & METRICS GRID
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AutoMode,
                                contentDescription = "Automation",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        Column {
                            Text(
                                text = "YouTube Automation Queue",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = automationState.estimatedQueueStatusText,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    IconButton(
                        onClick = { showAutomationSettingsDialog = true },
                        modifier = Modifier.testTag("automation_settings_btn")
                    ) {
                        Icon(Icons.Outlined.Tune, contentDescription = "Automation Settings")
                    }
                }

                // 5 Metric Pills: Total, Uploaded, Waiting, Processing, Failed
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MetricStatCard(
                        label = "Total",
                        value = automationState.totalVideosInQueue.toString(),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                    MetricStatCard(
                        label = "Uploaded",
                        value = automationState.uploadedCount.toString(),
                        color = Color(0xFF2E7D32),
                        modifier = Modifier.weight(1f)
                    )
                    MetricStatCard(
                        label = "Waiting",
                        value = automationState.waitingCount.toString(),
                        color = Color(0xFFEF6C00),
                        modifier = Modifier.weight(1f)
                    )
                    MetricStatCard(
                        label = "Active",
                        value = automationState.processingCount.toString(),
                        color = Color(0xFF1565C0),
                        modifier = Modifier.weight(1f)
                    )
                    MetricStatCard(
                        label = "Failed",
                        value = automationState.failedCount.toString(),
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Live Progress Section
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    shape = RoundedCornerShape(14.dp),
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
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = automationState.currentProcessingTitle
                                    ?.let { "Processing: $it" }
                                    ?: automationState.currentStepLabel,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Surface(
                                color = when (automationState.queueStatus) {
                                    QueueRunStatus.RUNNING -> Color(0xFFE8F5E9)
                                    QueueRunStatus.PAUSED -> Color(0xFFFFF3E0)
                                    else -> MaterialTheme.colorScheme.surfaceVariant
                                },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = automationState.queueStatus.name,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = when (automationState.queueStatus) {
                                        QueueRunStatus.RUNNING -> Color(0xFF2E7D32)
                                        QueueRunStatus.PAUSED -> Color(0xFFEF6C00)
                                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        // Video Processing Progress Bar
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Video & Music Muxing Progress", style = MaterialTheme.typography.labelSmall)
                            Text(
                                "${(automationState.videoProcessingProgress * 100).toInt()}%",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        LinearProgressIndicator(
                            progress = { automationState.videoProcessingProgress.coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = MaterialTheme.colorScheme.primary
                        )

                        // YouTube Upload Progress Bar
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("YouTube Data API v3 Upload Progress", style = MaterialTheme.typography.labelSmall)
                            Text(
                                "${(automationState.youtubeUploadProgress * 100).toInt()}%",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        LinearProgressIndicator(
                            progress = { automationState.youtubeUploadProgress.coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = Color(0xFFCC0000)
                        )

                        // Latest successful upload banner
                        if (automationState.latestSuccessfulUploadUrl != null) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp)
                                    .clickable {
                                        try {
                                            context.startActivity(
                                                Intent(Intent.ACTION_VIEW, Uri.parse(automationState.latestSuccessfulUploadUrl))
                                            )
                                        } catch (_: Exception) {}
                                    },
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        Icons.Filled.CheckCircle,
                                        contentDescription = null,
                                        tint = Color(0xFF2E7D32),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "Latest Upload: ${automationState.latestSuccessfulUploadTitle ?: "View on YouTube"}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF2E7D32)
                                    )
                                }
                                Text(
                                    text = "OPEN ↗",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                // 2. REQUIRED ACTION BUTTONS
                // Primary Automation Controls: START AUTOMATION, PAUSE QUEUE, STOP AUTOMATION
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onStartAutomation,
                        enabled = automationState.queueStatus != QueueRunStatus.RUNNING,
                        modifier = Modifier
                            .weight(1.3f)
                            .height(48.dp)
                            .testTag("start_automation_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF2E7D32),
                            contentColor = Color.White
                        )
                    ) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("START AUTOMATION", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = onPauseQueue,
                        enabled = automationState.queueStatus == QueueRunStatus.RUNNING,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("pause_queue_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Filled.Pause, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("PAUSE QUEUE", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = onStopAutomation,
                        enabled = automationState.queueStatus == QueueRunStatus.RUNNING || automationState.queueStatus == QueueRunStatus.PAUSED,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("stop_automation_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Filled.Stop, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("STOP", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                }

                // Connection & Utility Buttons Grid:
                // CONNECT GOOGLE SHEETS, CONNECT YOUTUBE, UPLOAD MUSIC, REFRESH SHEET, RETRY FAILED JOBS, VIEW UPLOADED VIDEOS
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledTonalButton(
                        onClick = { showConnectSheetDialog = true },
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .testTag("connect_google_sheets_button"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Filled.TableChart, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (automationState.isSheetsConnected) "SHEETS CONNECTED" else "CONNECT GOOGLE SHEETS",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }

                    FilledTonalButton(
                        onClick = onConnectYouTube,
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .testTag("connect_youtube_button"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Filled.PlayCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (youTubeConnected) "YT: ${youTubeChannelName.take(10)}" else "CONNECT YOUTUBE",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { multiAudioPicker.launch("audio/*") },
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("dashboard_upload_music_button"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Filled.LibraryMusic, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("UPLOAD MUSIC", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = onRefreshSheet,
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("refresh_sheet_button"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("REFRESH SHEET", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onRetryFailedJobs,
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("retry_failed_jobs_button"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Filled.Replay, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("RETRY FAILED JOBS", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = { showUploadedVideosDialog = true },
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("view_uploaded_videos_button"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Filled.VideoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("VIEW UPLOADED VIDEOS", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // 3. MUSIC LIBRARY CARD (MP3, WAV, AAC, M4A)
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Uploaded Music Library (MP3, WAV, AAC, M4A)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Select default track • Uploaded music is matched 1:1 with video",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Button(
                        onClick = { multiAudioPicker.launch("audio/*") },
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Filled.UploadFile, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Upload Music", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                }

                automationState.musicLibrary.forEach { track ->
                    val isDefault = track.id == automationState.defaultMusicTrackId || track.isDefault
                    Surface(
                        color = if (isDefault) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(
                            1.dp,
                            if (isDefault) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectDefaultMusic(track.id) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    Icons.Filled.Audiotrack,
                                    contentDescription = null,
                                    tint = if (isDefault) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Column {
                                    Text(
                                        text = track.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "Format: ${track.format} • Duration: ${track.formattedDuration} • Status: ${track.uploadStatus}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            if (isDefault) {
                                Surface(
                                    color = Color(0xFF2E7D32),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = "DEFAULT",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            } else {
                                TextButton(onClick = { onSelectDefaultMusic(track.id) }) {
                                    Text("Set Default", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4. GOOGLE SHEETS QUEUE TABLE (Columns A - J)
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Google Sheets Upload Queue (Cols A–J)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (automationState.spreadsheetId.isNotBlank())
                                "Sheet ID: ${automationState.spreadsheetId.take(22)}..."
                            else "Local / Demo Sheet Queue Active (Connect Google Sheet to sync)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    FilledTonalButton(
                        onClick = { showAddRowDialog = true },
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Row", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                }

                automationState.sheetError?.let { err ->
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = err,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                automationState.queueRows.forEach { row ->
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Row #${row.rowNumber} • ${row.videoTitle}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                StatusBadge(status = row.uploadStatus)
                            }
                            Text(
                                text = "A (Video URL): ${row.videoUrl}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "E (Music): ${row.musicSelection.ifBlank { "Default Library Track" }} • F (Vol): ${(row.musicVolume * 100).toInt()}%",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            if (row.youtubeVideoUrl.isNotBlank()) {
                                Text(
                                    text = "I (YouTube URL): ${row.youtubeVideoUrl}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF2E7D32),
                                    modifier = Modifier.clickable {
                                        try {
                                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(row.youtubeVideoUrl)))
                                        } catch (_: Exception) {}
                                    }
                                )
                            }
                            if (row.errorMessage.isNotBlank()) {
                                Text(
                                    text = "J (Error): ${row.errorMessage}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Connect Google Sheets Dialog
    if (showConnectSheetDialog) {
        var sheetInput by remember { mutableStateOf(automationState.spreadsheetId) }
        var tabInput by remember { mutableStateOf(automationState.sheetName) }
        var tokenInput by remember { mutableStateOf(automationState.googleAccessToken) }

        AlertDialog(
            onDismissRequest = { showConnectSheetDialog = false },
            title = { Text("Connect Google Sheets (Cols A–J)", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Paste your Google Spreadsheet URL or ID with columns:\nA: Video URL | B: Title | C: Description | D: Thumbnail | E: Music | F: Volume | G: Status (READY) | H: Schedule | I: YouTube URL | J: Error",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = sheetInput,
                        onValueChange = { sheetInput = it },
                        label = { Text("Spreadsheet URL or ID") },
                        placeholder = { Text("https://docs.google.com/spreadsheets/d/...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = tabInput,
                        onValueChange = { tabInput = it },
                        label = { Text("Sheet Tab Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = tokenInput,
                        onValueChange = { tokenInput = it },
                        label = { Text("Google OAuth Access Token (for write-back)") },
                        placeholder = { Text("ya29.a0...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onConnectGoogleSheets(sheetInput, tabInput.ifBlank { "Sheet1" }, tokenInput)
                        showConnectSheetDialog = false
                    }
                ) {
                    Text("Connect & Fetch Rows")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConnectSheetDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Add Row Dialog
    if (showAddRowDialog) {
        var videoUrl by remember { mutableStateOf("") }
        var videoTitle by remember { mutableStateOf("") }
        var videoDesc by remember { mutableStateOf(automationState.defaultDescription) }
        var musicName by remember {
            mutableStateOf(
                automationState.musicLibrary.firstOrNull { it.id == automationState.defaultMusicTrackId }?.name
                    ?: "Cyberpunk Anime Fight Beat.mp3"
            )
        }

        AlertDialog(
            onDismissRequest = { showAddRowDialog = false },
            title = { Text("Add Video Row (READY)", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = videoUrl,
                        onValueChange = { videoUrl = it },
                        label = { Text("Column A: Video URL") },
                        placeholder = { Text("https://.../video.mp4") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = videoTitle,
                        onValueChange = { videoTitle = it },
                        label = { Text("Column B: Video Title") },
                        placeholder = { Text("Insane Anime Fight Scene #shorts") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = musicName,
                        onValueChange = { musicName = it },
                        label = { Text("Column E: Music Selection") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = videoDesc,
                        onValueChange = { videoDesc = it },
                        label = { Text("Column C: Description") },
                        maxLines = 4,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (videoUrl.isNotBlank()) {
                            val nextRowNum = (automationState.queueRows.maxOfOrNull { it.rowNumber } ?: 1) + 1
                            onAddRowToQueue(
                                SheetVideoRow(
                                    rowNumber = nextRowNum,
                                    videoUrl = videoUrl.trim(),
                                    videoTitle = videoTitle.trim().ifBlank { "Anime Shorts Remix #$nextRowNum" },
                                    videoDescription = videoDesc.ifBlank { DEFAULT_REMIX_VIDEO_DESCRIPTION },
                                    musicSelection = musicName.trim(),
                                    musicVolume = automationState.defaultMusicVolume,
                                    uploadStatus = SheetJobStatus.READY
                                )
                            )
                            showAddRowDialog = false
                        }
                    }
                ) {
                    Text("Add READY Row")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddRowDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // View Uploaded Videos Dialog
    if (showUploadedVideosDialog) {
        val uploadedRows = automationState.queueRows.filter { it.uploadStatus == SheetJobStatus.UPLOADED }
        AlertDialog(
            onDismissRequest = { showUploadedVideosDialog = false },
            title = { Text("Uploaded YouTube Videos (${uploadedRows.size})", fontWeight = FontWeight.Bold) },
            text = {
                if (uploadedRows.isEmpty()) {
                    Text("No videos have finished uploading in this session yet. Press START AUTOMATION to process READY rows.")
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        uploadedRows.forEach { row ->
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (row.youtubeVideoUrl.isNotBlank()) {
                                            try {
                                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(row.youtubeVideoUrl)))
                                            } catch (_: Exception) {}
                                        }
                                    }
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(row.videoTitle, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        row.youtubeVideoUrl.ifBlank { "Uploaded to channel" },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF2E7D32)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showUploadedVideosDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // Automation Settings Dialog
    if (showAutomationSettingsDialog) {
        var keepOrig by remember { mutableStateOf(automationState.keepOriginalAudio) }
        var origVol by remember { mutableFloatStateOf(automationState.originalAudioVolume) }
        var musVol by remember { mutableFloatStateOf(automationState.defaultMusicVolume) }
        var fadeIn by remember { mutableFloatStateOf(automationState.fadeInSeconds) }
        var fadeOut by remember { mutableFloatStateOf(automationState.fadeOutSeconds) }
        var privacy by remember { mutableStateOf(automationState.defaultPrivacy) }
        var desc by remember { mutableStateOf(automationState.defaultDescription) }

        AlertDialog(
            onDismissRequest = { showAutomationSettingsDialog = false },
            title = { Text("Automation & Audio Settings", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Keep Original Video Audio", style = MaterialTheme.typography.bodyMedium)
                        Switch(checked = keepOrig, onCheckedChange = { keepOrig = it })
                    }
                    Text("Original Audio Volume: ${(origVol * 100).toInt()}%", style = MaterialTheme.typography.labelSmall)
                    Slider(value = origVol, onValueChange = { origVol = it }, valueRange = 0f..1.5f)

                    Text("Default Music Volume: ${(musVol * 100).toInt()}%", style = MaterialTheme.typography.labelSmall)
                    Slider(value = musVol, onValueChange = { musVol = it }, valueRange = 0f..1.5f)

                    Text("Fade-In (${String.format("%.1f", fadeIn)}s) & Fade-Out (${String.format("%.1f", fadeOut)}s)", style = MaterialTheme.typography.labelSmall)
                    Slider(value = fadeIn, onValueChange = { fadeIn = it }, valueRange = 0f..5f)

                    Text("Default YouTube Privacy:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        YouTubePrivacy.values().forEach { p ->
                            FilterChip(
                                selected = privacy == p,
                                onClick = { privacy = p },
                                label = { Text(p.displayName, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = desc,
                        onValueChange = { desc = it },
                        label = { Text("Default Remixed Video Description") },
                        maxLines = 5,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdateSettings(keepOrig, origVol, musVol, fadeIn, fadeOut, privacy, desc)
                        showAutomationSettingsDialog = false
                    }
                ) {
                    Text("Save Settings")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAutomationSettingsDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun MetricStatCard(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.3f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                color = color
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun StatusBadge(status: SheetJobStatus) {
    val (bg, fg) = when (status) {
        SheetJobStatus.READY -> Color(0xFFFFF3E0) to Color(0xFFEF6C00)
        SheetJobStatus.PROCESSING -> Color(0xFFE3F2FD) to Color(0xFF1565C0)
        SheetJobStatus.UPLOADED -> Color(0xFFE8F5E9) to Color(0xFF2E7D32)
        SheetJobStatus.FAILED -> Color(0xFFFFEBEE) to Color(0xFFC62828)
    }
    Surface(color = bg, shape = RoundedCornerShape(6.dp)) {
        Text(
            text = status.label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = fg,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        )
    }
}
