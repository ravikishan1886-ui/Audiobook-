package com.example.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.SampleMusicOption
import com.example.data.model.SampleVideoOption
import com.example.data.model.VideoMusicRemixState

val SAMPLE_VIDEOS = listOf(
    SampleVideoOption(
        title = "Nefer 4K CC (MEGA Cloud)",
        durationLabel = "04:15",
        url = "https://www.google.com/url?sa=E&q=https%3A%2F%2Fmega.nz%2Ffile%2FZJkW0RCK%23x9fu65rOm-h1xvlsP0p3Iw84kJ-JhPWK9macoWQGohs",
        description = "4K video from MEGA Cloud via Google redirect"
    ),
    SampleVideoOption(
        title = "Nature Landscapes (Creative Commons)",
        durationLabel = "05:32",
        url = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
        description = "Scenic cinematic footage with open rights"
    ),
    SampleVideoOption(
        title = "Big Buck Bunny Animation",
        durationLabel = "09:56",
        url = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
        description = "Open source 3D animated film"
    ),
    SampleVideoOption(
        title = "Tech Demo Footage",
        durationLabel = "00:15",
        url = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4",
        description = "Ultra-fast short video sample"
    )
)

val SAMPLE_MUSICS = listOf(
    SampleMusicOption(
        title = "Lofi Ambient Chillhop",
        durationLabel = "03:15",
        durationMs = 195000L,
        genre = "Lo-Fi / Beats"
    ),
    SampleMusicOption(
        title = "Inspiring Acoustic Melody",
        durationLabel = "02:40",
        durationMs = 160000L,
        genre = "Acoustic / Warm"
    ),
    SampleMusicOption(
        title = "Epic Cinematic Soundscape",
        durationLabel = "04:50",
        durationMs = 290000L,
        genre = "Cinematic / Strings"
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoRemixInputSection(
    remixState: VideoMusicRemixState,
    onVideoUrlChange: (String) -> Unit,
    onMusicSelected: (Uri, String) -> Unit,
    onSampleVideoSelected: (SampleVideoOption) -> Unit,
    onSampleMusicSelected: (SampleMusicOption) -> Unit,
    onRightsConfirmedChange: (Boolean) -> Unit,
    onNextToPreview: () -> Unit,
    modifier: Modifier = Modifier
) {
    val clipboardManager = LocalClipboardManager.current

    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val fileName = it.lastPathSegment?.substringAfterLast("/") ?: "custom_audio.mp3"
            onMusicSelected(it, fileName)
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("video_remix_input_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Movie,
                        contentDescription = "Video Remixer",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column {
                    Text(
                        text = "Video & Music Overlay",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Align soundtrack duration, render MP4 & upload to YouTube",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // 1. Video URL Input
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "1. Publicly Accessible Video URL",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                OutlinedTextField(
                    value = remixState.videoUrl,
                    onValueChange = onVideoUrlChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("video_url_input"),
                    placeholder = { Text("Paste video link, Google redirect, or MEGA.nz...") },
                    leadingIcon = {
                        Icon(Icons.Outlined.Link, contentDescription = "URL", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    },
                    trailingIcon = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (remixState.videoUrl.isNotBlank()) {
                                IconButton(onClick = { onVideoUrlChange("") }, modifier = Modifier.size(40.dp)) {
                                    Icon(Icons.Filled.Close, contentDescription = "Clear", modifier = Modifier.size(18.dp))
                                }
                            }
                            IconButton(
                                onClick = {
                                    val clip = clipboardManager.getText()
                                    if (!clip.isNullOrBlank()) {
                                        onVideoUrlChange(clip.text.trim())
                                    }
                                },
                                modifier = Modifier
                                    .size(40.dp)
                                    .testTag("paste_url_button")
                            ) {
                                Icon(Icons.Outlined.ContentPaste, contentDescription = "Paste", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                if (remixState.sourceBadge.isNotBlank()) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                Icons.Filled.CloudDone,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = remixState.sourceBadge,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // Quick sample video chips
                Text(
                    text = "Or choose a test video sample:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SAMPLE_VIDEOS.forEach { sample ->
                        val isSelected = remixState.videoUrl == sample.url
                        FilterChip(
                            selected = isSelected,
                            onClick = { onSampleVideoSelected(sample) },
                            label = { Text("${sample.title.take(18)}... (${sample.durationLabel})", style = MaterialTheme.typography.labelSmall) },
                            leadingIcon = if (isSelected) {
                                { Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                            } else null,
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }
            }

            // 2. Upload Music/Audio File
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "2. Upload Music / Audio File",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { audioPickerLauncher.launch("audio/*") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("upload_music_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
                    ) {
                        Icon(Icons.Filled.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (remixState.musicFileName.isNotBlank()) "Replace Audio" else "Choose Audio File",
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                if (remixState.musicFileName.isNotBlank()) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(Icons.Filled.Audiotrack, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                Column {
                                    Text(
                                        text = remixState.musicFileName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Duration: ${remixState.formattedMusicDuration}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Icon(Icons.Filled.CheckCircle, contentDescription = "Ready", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        }
                    }
                }

                // Sample music options
                Text(
                    text = "Or select a royalty-free music track:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SAMPLE_MUSICS.forEach { sample ->
                        val isSelected = remixState.musicFileName == sample.title
                        FilterChip(
                            selected = isSelected,
                            onClick = { onSampleMusicSelected(sample) },
                            label = { Text("${sample.title.take(15)} (${sample.durationLabel})", style = MaterialTheme.typography.labelSmall) },
                            leadingIcon = if (isSelected) {
                                { Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                            } else null,
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }
            }

            // 3. Permission and Legal Rights Checkbox
            Surface(
                color = if (remixState.hasRightsPermission) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        color = if (remixState.hasRightsPermission) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .clickable { onRightsConfirmedChange(!remixState.hasRightsPermission) }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Checkbox(
                        checked = remixState.hasRightsPermission,
                        onCheckedChange = onRightsConfirmedChange,
                        modifier = Modifier.testTag("rights_permission_checkbox")
                    )
                    Column {
                        Text(
                            text = "Rights & Permission Confirmation (Required)",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "I certify that I have full permission or rights to use both this video and music track. (Only process content you have permission to use)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Next / Proceed Button
            val canProceed = remixState.videoUrl.isNotBlank() &&
                    remixState.musicFileName.isNotBlank() &&
                    remixState.hasRightsPermission

            Button(
                onClick = onNextToPreview,
                enabled = canProceed,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("next_to_preview_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text(
                    text = "Next: Fullscreen Preview & Durations",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
            }

            if (!canProceed) {
                Text(
                    text = when {
                        remixState.videoUrl.isBlank() -> "• Enter or select a video URL above"
                        remixState.musicFileName.isBlank() -> "• Upload or select an audio file above"
                        !remixState.hasRightsPermission -> "• Please confirm legal permission checkbox above"
                        else -> ""
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
