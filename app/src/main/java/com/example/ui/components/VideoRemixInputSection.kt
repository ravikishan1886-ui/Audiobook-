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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import coil.compose.AsyncImage
import com.example.data.model.SampleMusicOption
import com.example.data.model.SampleVideoOption
import com.example.data.model.VideoMusicRemixState
import com.example.data.model.YouTubeSourceInfo
import com.example.video.YouTubeAudioExtractor

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

data class SampleYouTubeMusicOption(
    val title: String,
    val artist: String,
    val url: String,
    val durationLabel: String
)

val SAMPLE_YOUTUBE_MUSICS = listOf(
    SampleYouTubeMusicOption(
        title = "Paijo (YouTube Shorts Audio)",
        artist = "Zaskia Gotik - Topic",
        url = "https://youtube.com/source/KheSUT2stiM/shorts",
        durationLabel = "00:58"
    ),
    SampleYouTubeMusicOption(
        title = "Dark Iruma (Dancin Krono Remix)",
        artist = "Kiki Baskerville / Krono",
        url = "https://youtu.be/FLKvBcLv-AY?si=ZVaWzq5bfVcexDWk",
        durationLabel = "03:18"
    ),
    SampleYouTubeMusicOption(
        title = "BTTH Xiao Yan (Shorts)",
        artist = "Google donghua",
        url = "https://youtube.com/shorts/mN0EiTdNmHs",
        durationLabel = "00:45"
    ),
    SampleYouTubeMusicOption(
        title = "Never Gonna Give You Up",
        artist = "Rick Astley",
        url = "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
        durationLabel = "03:33"
    ),
    SampleYouTubeMusicOption(
        title = "Lofi Hip Hop Chill Beats",
        artist = "Lofi Girl",
        url = "https://www.youtube.com/watch?v=jfKfPfyJRdk",
        durationLabel = "02:45"
    ),
    SampleYouTubeMusicOption(
        title = "Retro Synthwave Sunset",
        artist = "Synthwave",
        url = "https://www.youtube.com/watch?v=4xDzrJKXOOY",
        durationLabel = "03:12"
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
    onUseThisMusic: () -> Unit = { onFetchYouTubeMusic(null) },
    onMakeVideoMusicSame: () -> Unit = {},
    onKeepOriginalVoiceChange: (Boolean) -> Unit = {},
    onOriginalVoiceVolumeChange: (Float) -> Unit = {},
    onMusicVolumeChange: (Float) -> Unit = {},
    onLoopMusicChange: (Boolean) -> Unit = {},
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
                        text = "Video & Music Remixer",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Pure Video & Music Remix • No AI narration or voice synthesis",
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
                        text = "Direct video remixing: The app fetches the video from your URL, loops/trims your music track to match the video, and renders the remixed MP4 with zero audiobook narration.",
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

            // 2. Upload Music / YouTube Music URL
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "2. Upload Music or YouTube Music URL",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Surface(
                        color = Color(0xFFFF0000).copy(alpha = 0.12f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Filled.PlayCircle,
                                contentDescription = null,
                                tint = Color(0xFFFF0000),
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "YouTube Music Supported",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFF0000)
                            )
                        }
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
                                        text = "Keep Original Voice / Dialogue",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (keepVoice) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = if (keepVoice) "Preserves dialogue and mixes with selected music"
                                               else "Source audio removed (Selected music only)",
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
                                        text = "Original Voice Volume",
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

                            // Selected Music Volume Slider
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Selected Music Volume",
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
                        } else {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        Icons.Filled.VolumeOff,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "Original audio from source video is completely removed. Only the selected music will play.",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                        // Loop Music If Shorter Option
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
                                    color = if (remixState.loopMusicIfShorter) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Filled.Repeat,
                                            contentDescription = null,
                                            tint = if (remixState.loopMusicIfShorter) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                Column {
                                    Text(
                                        text = "Loop Music If Shorter Than Video",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (remixState.loopMusicIfShorter) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = if (remixState.loopMusicIfShorter) "Music repeats smoothly if shorter than target video"
                                               else "Music plays once without looping (stops when track ends)",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Switch(
                                checked = remixState.loopMusicIfShorter,
                                onCheckedChange = onLoopMusicChange,
                                modifier = Modifier.testTag("switch_loop_music")
                            )
                        }
                    }
                }

                // YouTube Music URL Input Card
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Filled.MusicVideo,
                                contentDescription = null,
                                tint = Color(0xFFFF0000),
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Paste YouTube Video / Shorts or Public Music URL:",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        OutlinedTextField(
                            value = remixState.youtubeMusicUrl,
                            onValueChange = onYouTubeMusicUrlChange,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("youtube_music_url_input"),
                            placeholder = {
                                Text(
                                    "e.g. https://youtu.be/... or direct audio link (.mp3, .wav, .m4a)",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 12.sp
                                )
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            trailingIcon = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (remixState.youtubeMusicUrl.isNotBlank()) {
                                        IconButton(
                                            onClick = { onYouTubeMusicUrlChange("") },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(Icons.Filled.Clear, contentDescription = "Clear", modifier = Modifier.size(18.dp))
                                        }
                                    }
                                    IconButton(
                                        onClick = {
                                            clipboardManager.getText()?.text?.let { clipText ->
                                                if (clipText.isNotBlank()) onYouTubeMusicUrlChange(clipText.trim())
                                            }
                                        },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(Icons.Filled.ContentPaste, contentDescription = "Paste", modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        )

                        // Live URL Detection & Validation
                        val urlTrimmed = remixState.youtubeMusicUrl.trim()
                        val detectedVideoId: String? = remember(urlTrimmed) {
                            YouTubeAudioExtractor.extractYouTubeVideoId(urlTrimmed)
                        }
                        val isShortsSource = remember(urlTrimmed) {
                            YouTubeAudioExtractor.isShortsSourceUrl(urlTrimmed)
                        }
                        val isDirectPublicAudio = remember(urlTrimmed, detectedVideoId) {
                            detectedVideoId == null && (urlTrimmed.startsWith("http://", ignoreCase = true) || urlTrimmed.startsWith("https://", ignoreCase = true))
                        }
                        val hasUsableAudio = (remixState.localMusicFile != null && remixState.localMusicFile.exists() && remixState.localMusicFile.length() > 44) || (remixState.musicFileUri != null)

                        if (detectedVideoId != null) {
                            val resolved = remixState.resolvedYouTubeSource
                            val musicTitle = resolved?.title ?: remixState.youtubeMusicTitle ?: if (remixState.isResolvingYouTubeSource) "Resolving music track..." else "YouTube Track ($detectedVideoId)"
                            val musicAuthor = resolved?.author ?: remixState.youtubeMusicAuthor ?: if (remixState.isResolvingYouTubeSource) "Resolving artist..." else "YouTube Artist / Channel"
                            val thumbUrl = resolved?.thumbnailUrl ?: remixState.youtubeMusicThumbnailUrl ?: "https://i.ytimg.com/vi/$detectedVideoId/hqdefault.jpg"
                            val sourceRefUrl = resolved?.sourceUrl ?: if (isShortsSource) "https://youtube.com/source/$detectedVideoId/shorts" else "https://youtu.be/$detectedVideoId"
                            val isMusicActive = hasUsableAudio && (remixState.youtubeMusicTitle == musicTitle || remixState.musicFileName.contains(musicTitle, ignoreCase = true))

                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                border = BorderStroke(1.dp, if (isMusicActive) Color(0xFF2E7D32) else MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("resolved_youtube_source_card")
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    // URL Parser detection tag
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
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
                                                text = if (isShortsSource) "Detected /source/{ID}/shorts" else "YouTube Audio Source",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF1B5E20)
                                            )
                                        }
                                        Surface(
                                            color = Color(0xFFE8F5E9),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = "Extract: $detectedVideoId",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF2E7D32),
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }

                                    // Resolved source/music information
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // 🖼️ Thumbnail
                                        Box(
                                            modifier = Modifier
                                                .size(width = 96.dp, height = 72.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(Color.Black),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            AsyncImage(
                                                model = thumbUrl,
                                                contentDescription = "YouTube Music Thumbnail",
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .size(24.dp)
                                                    .clip(CircleShape)
                                                    .background(Color.Black.copy(alpha = 0.65f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    Icons.Filled.MusicNote,
                                                    contentDescription = null,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        }

                                        // Source Information Details
                                        Column(
                                            modifier = Modifier.weight(1f),
                                            verticalArrangement = Arrangement.spacedBy(3.dp)
                                        ) {
                                            // 🎵 Music title
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Text("🎵", fontSize = 13.sp)
                                                Text(
                                                    text = musicTitle,
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 2,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }

                                            // 👤 Artist/channel
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Text("👤", fontSize = 12.sp)
                                                Text(
                                                    text = musicAuthor,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }

                                            // 🔗 YouTube music/source reference
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Text("🔗", fontSize = 11.sp)
                                                Text(
                                                    text = sourceRefUrl.removePrefix("https://"),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    textDecoration = TextDecoration.Underline,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }

                                            if (remixState.isResolvingYouTubeSource) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                    modifier = Modifier.padding(top = 2.dp)
                                                ) {
                                                    CircularProgressIndicator(modifier = Modifier.size(10.dp), strokeWidth = 1.5.dp)
                                                    Text(
                                                        text = "Resolving YouTube metadata...",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.outline,
                                                        fontSize = 10.sp
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    // Action: User selects "Use this music"
                                    Button(
                                        onClick = { onUseThisMusic() },
                                        enabled = !remixState.isFetchingYouTubeMusic,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("use_this_music_button"),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isMusicActive) Color(0xFF2E7D32) else MaterialTheme.colorScheme.primary,
                                            contentColor = Color.White
                                        )
                                    ) {
                                        if (remixState.isFetchingYouTubeMusic) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(16.dp),
                                                color = Color.White,
                                                strokeWidth = 2.dp
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Loading & Preparing Audio...", fontWeight = FontWeight.Bold)
                                        } else if (isMusicActive) {
                                            Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("✓ Audio Ready & Selected", fontWeight = FontWeight.Bold)
                                        } else {
                                            Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Extract & Use YouTube Music", fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        } else if (isDirectPublicAudio) {
                            Surface(
                                color = Color(0xFFE3F2FD),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        Icons.Filled.CheckCircle,
                                        contentDescription = null,
                                        tint = Color(0xFF1976D2),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Column {
                                        Text(
                                            text = "Public Music URL Accepted ✓",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF0D47A1)
                                        )
                                        Text(
                                            text = "Direct web audio stream (Ready to download & remix)",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFF1976D2),
                                            fontSize = 10.sp
                                        )
                                    }
                                }
                            }
                        }

                        // Sample YouTube Music Quick Presets
                        Text(
                            text = "Quick Sample YouTube Music Links:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            SAMPLE_YOUTUBE_MUSICS.forEach { sampleYt ->
                                val isSelected = remixState.youtubeMusicUrl == sampleYt.url
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        onYouTubeMusicUrlChange(sampleYt.url)
                                        onFetchYouTubeMusic(sampleYt.url)
                                    },
                                    label = {
                                        Text(
                                            "▶ ${sampleYt.title} (${sampleYt.durationLabel})",
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    },
                                    shape = RoundedCornerShape(8.dp)
                                )
                            }
                        }

                        // Fetch Public Music Button (shown for direct audio streams when no YouTube card is active)
                        if (detectedVideoId == null) {
                            Button(
                                onClick = { onFetchYouTubeMusic(null) },
                                enabled = remixState.youtubeMusicUrl.isNotBlank() && !remixState.isFetchingYouTubeMusic,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("fetch_youtube_music_button"),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFCC0000),
                                    contentColor = Color.White
                                )
                            ) {
                                if (remixState.isFetchingYouTubeMusic) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        color = Color.White,
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Downloading Music...", fontWeight = FontWeight.Medium)
                                } else {
                                    Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Fetch & Use Music Track", fontWeight = FontWeight.Medium)
                                }
                            }
                        }

                        // YouTube Music Error display if any (hidden once audio is ready)
                        if (remixState.youtubeMusicError != null && !hasUsableAudio) {
                            Surface(
                                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        Icons.Filled.ErrorOutline,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = remixState.youtubeMusicError ?: "",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }
                        }
                    }
                }

                // Divider OR
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    HorizontalDivider(modifier = Modifier.weight(1f))
                    Text(
                        text = "OR CHOOSE OTHER AUDIO",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    HorizontalDivider(modifier = Modifier.weight(1f))
                }

                // Choose local audio file button
                Button(
                    onClick = { audioPickerLauncher.launch("audio/*") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("upload_music_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                ) {
                    Icon(Icons.Filled.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (remixState.musicFileName.isNotBlank() && !remixState.musicFileName.contains("YouTube")) "Replace Local Audio File" else "Upload Local Audio File (MP3, WAV, AAC)",
                        fontWeight = FontWeight.Medium
                    )
                }

                // Active Audio Status Card
                if (remixState.musicFileName.isNotBlank()) {
                    Surface(
                        color = if (remixState.musicFileName.contains("YouTube")) Color(0xFFFF0000).copy(alpha = 0.08f)
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(10.dp),
                        border = if (remixState.musicFileName.contains("YouTube")) BorderStroke(1.dp, Color(0xFFFF0000).copy(alpha = 0.3f)) else null,
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
                                Icon(
                                    if (remixState.musicFileName.contains("YouTube")) Icons.Filled.PlayCircle else Icons.Filled.Audiotrack,
                                    contentDescription = null,
                                    tint = if (remixState.musicFileName.contains("YouTube")) Color(0xFFFF0000) else MaterialTheme.colorScheme.primary,
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
                                        text = "Duration: ${remixState.formattedMusicDuration}" +
                                                (if (remixState.youtubeMusicAuthor != null) " • Channel: ${remixState.youtubeMusicAuthor}" else ""),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Icon(
                                Icons.Filled.CheckCircle,
                                contentDescription = "Ready",
                                tint = if (remixState.musicFileName.contains("YouTube")) Color(0xFFFF0000) else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // Sample music options
                Text(
                    text = "Or choose a replacement music soundtrack:",
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

            // 4. Pre-Export Validation Display
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("pre_export_validation_card")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Pre-Export Configuration",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Music source:",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = if (remixState.musicFileName.isNotBlank()) remixState.musicFileName.take(30) else "[No music selected]",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (remixState.musicFileName.isNotBlank()) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error,
                            modifier = Modifier.testTag("validation_music_source")
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Original audio:",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = if (remixState.keepOriginalVoice) "ON" else "OFF",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (remixState.keepOriginalVoice) MaterialTheme.colorScheme.primary else Color(0xFF2E7D32),
                            modifier = Modifier.testTag("validation_original_audio")
                        )
                    }
                    if (remixState.loopMusicIfShorter) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Loop music:",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "ON (repeats if shorter)",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
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
                // Primary: Direct Remix (Fetch video from URL + Add Music + Render Remixed MP4)
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
                        text = "Remix Video with Music (No Narration)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Secondary: Preview Durations & Video Settings
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
