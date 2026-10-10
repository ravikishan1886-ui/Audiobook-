package com.example.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
        title = "Hiromi Higuruma 4K CC (MEGA)",
        durationLabel = "04:30",
        url = "https://mega.nz/file/5AFgkbCK#Ej-0k6UUgvXq5_bJifzk4bxPk4HLwElFXrVhiYyvwp4",
        description = "Hiromi Higuruma 4K CC video from MEGA Cloud"
    ),
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
        title = "Phonk Anime Fight Beat",
        durationLabel = "02:18",
        durationMs = 138000L,
        genre = "Phonk / Drift Bass"
    ),
    SampleMusicOption(
        title = "Upbeat Electronic Dance",
        durationLabel = "02:50",
        durationMs = 170000L,
        genre = "EDM / Electro"
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
    ),
    SampleMusicOption(
        title = "Energetic Pop Beat",
        durationLabel = "03:05",
        durationMs = 185000L,
        genre = "Pop / Groovy"
    ),
    SampleMusicOption(
        title = "Deep Emotional Piano",
        durationLabel = "02:30",
        durationMs = 150000L,
        genre = "Piano / Ballad"
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
    onDirectRemixNow: () -> Unit = {},
    onYouTubeMusicUrlChange: (String) -> Unit = {},
    onFetchYouTubeMusic: (String?) -> Unit = {},
    onUseThisMusic: () -> Unit = {},
    onMakeVideoMusicSame: () -> Unit = {},
    onKeepOriginalVoiceChange: (Boolean) -> Unit = {},
    onOriginalVoiceVolumeChange: (Float) -> Unit = {},
    onMusicVolumeChange: (Float) -> Unit = {},
    onLoopMusicChange: (Boolean) -> Unit = {},
    onDescriptionChange: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val clipboardManager = LocalClipboardManager.current

    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val fileName = it.lastPathSegment?.substringAfterLast("/") ?: "uploaded_music.mp3"
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
                        text = "Video & Music Remixer",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Uploaded Music Exact Sync • Anime/Donghua Shorts Ready",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Pure Remix Mode Banner
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Uploaded music is kept 100% identical (exact original pitch, fidelity, and duration synced to video) with default Anime/Donghua Shorts description.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
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

            // 2. Upload Music Button ONLY (Music URL option removed per user request)
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "2. Upload Music (Exact Same Audio Sync)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Surface(
                        color = Color(0xFF2E7D32).copy(alpha = 0.12f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Filled.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF2E7D32),
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "Uploaded Music Must Be Same",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2E7D32)
                            )
                        }
                    }
                }

                // Upload Music Button (Primary)
                Button(
                    onClick = { audioPickerLauncher.launch("audio/*") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("upload_music_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Icon(Icons.Filled.UploadFile, contentDescription = "Upload Music", modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (remixState.musicFileUri != null) "Change Uploaded Music (${remixState.musicFileName.take(22)})"
                               else "UPLOAD MUSIC (MP3, WAV, AAC, M4A)",
                        fontWeight = FontWeight.Bold
                    )
                }

                // Active Audio Status Card
                if (remixState.musicFileName.isNotBlank()) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Color(0xFF2E7D32).copy(alpha = 0.4f)),
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
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    Icons.Filled.Audiotrack,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                                Column {
                                    Text(
                                        text = remixState.musicFileName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Exact Same Music • Synced to Video (${remixState.formattedFinalDuration})",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF2E7D32),
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                            Icon(
                                Icons.Filled.CheckCircle,
                                contentDescription = "Ready",
                                tint = Color(0xFF2E7D32),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // Built-in music library presets
                Text(
                    text = "Or select from Music Library:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SAMPLE_MUSICS.forEach { sample ->
                        val isSelected = remixState.musicFileName == sample.title
                        val labelText = "${sample.title.take(22)} (${sample.durationLabel})"
                        FilterChip(
                            selected = isSelected,
                            onClick = { onSampleMusicSelected(sample) },
                            label = { Text(labelText, style = MaterialTheme.typography.labelSmall) },
                            leadingIcon = if (isSelected) {
                                { Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                            } else null,
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }

                // Original Voice Option & Audio Mixing Card
                val keepVoice = remixState.keepOriginalVoice
                Surface(
                    color = if (keepVoice) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(
                        1.5.dp,
                        if (keepVoice) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("card_original_voice_control")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = if (keepVoice) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = if (keepVoice) Icons.Filled.Mic else Icons.Filled.MicOff,
                                            contentDescription = null,
                                            tint = if (keepVoice) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                Column {
                                    Text(
                                        text = "Keep Original Video Audio",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (keepVoice) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = if (keepVoice) "Mixes original video audio with uploaded music"
                                        else "Mutes original audio (Uploaded music plays identically)",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Switch(
                                checked = keepVoice,
                                onCheckedChange = onKeepOriginalVoiceChange,
                                modifier = Modifier.testTag("switch_keep_original_voice")
                            )
                        }

                        if (keepVoice) {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                            // Original Voice Volume Slider
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Original Video Audio Volume",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = "${(remixState.originalVoiceVolume * 100).toInt()}%",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Slider(
                                    value = remixState.originalVoiceVolume,
                                    onValueChange = onOriginalVoiceVolumeChange,
                                    valueRange = 0.0f..2.0f,
                                    modifier = Modifier.testTag("slider_original_voice_volume")
                                )
                            }
                        }

                        // Selected Music Volume Slider
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Uploaded Music Volume",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "${(remixState.musicVolume * 100).toInt()}%",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Slider(
                                value = remixState.musicVolume,
                                onValueChange = onMusicVolumeChange,
                                valueRange = 0.0f..2.0f,
                                modifier = Modifier.testTag("slider_music_volume")
                            )
                        }
                    }
                }
            }

            // 3. Default Remixed Video Description (Pre-populated with Anime/Donghua Shorts template)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "3. Default Remixed Video Description",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                OutlinedTextField(
                    value = remixState.videoDescription,
                    onValueChange = onDescriptionChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("remix_description_input"),
                    minLines = 5,
                    maxLines = 8,
                    shape = RoundedCornerShape(12.dp)
                )
            }

            // 4. Permission and Legal Rights Checkbox
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
                            text = "I certify that I own or am authorized to use both this video and uploaded music track.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Action Buttons
            val canProceed = remixState.videoUrl.isNotBlank() &&
                    remixState.musicFileName.isNotBlank() &&
                    remixState.hasRightsPermission

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onDirectRemixNow,
                    enabled = canProceed,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("direct_remix_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Icon(Icons.Filled.MovieFilter, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Remix Video with Uploaded Music",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                OutlinedButton(
                    onClick = onNextToPreview,
                    enabled = canProceed,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("next_to_preview_button"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Filled.Visibility, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Fullscreen Preview & Settings",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
