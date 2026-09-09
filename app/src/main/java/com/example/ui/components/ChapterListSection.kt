package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AudioEngineType
import com.example.data.model.Chapter
import com.example.data.model.ChapterStatus

@Composable
fun ChapterListSection(
    chapters: List<Chapter>,
    currentlyPlayingChapterId: String?,
    isPlaying: Boolean,
    isGeneratingAudio: Boolean,
    onSynthesizeAllClick: () -> Unit,
    onSynthesizeSingleChapter: (String) -> Unit,
    onPreviewChapter: (Chapter) -> Unit,
    onViewScript: (Chapter) -> Unit,
    onDownloadChapter: (Chapter) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("chapter_list_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Audiotrack,
                            contentDescription = "Chapters",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Audiobook Chapters (${chapters.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Review, synthesize voice, and preview sections",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (chapters.isNotEmpty()) {
                    val readyCount = chapters.count { it.status == ChapterStatus.AUDIO_READY }
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (readyCount == chapters.size) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface
                    ) {
                        Text(
                            text = "$readyCount/${chapters.size} Audio Ready",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (readyCount == chapters.size) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            if (chapters.isEmpty()) {
                // Empty state
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.FormatListNumbered,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(40.dp)
                        )
                        Text(
                            text = "No Chapters Organized Yet",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Click 'Clean & Split Chapters with Gemini' above to process your manuscript into structured narration sections.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
            } else {
                // List of Chapters
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    chapters.forEach { chapter ->
                        val isThisPlaying = currentlyPlayingChapterId == chapter.id && isPlaying
                        ChapterItemCard(
                            chapter = chapter,
                            isCurrentlyPlaying = isThisPlaying,
                            onPreviewClick = { onPreviewChapter(chapter) },
                            onSynthesizeClick = { onSynthesizeSingleChapter(chapter.id) },
                            onViewScriptClick = { onViewScript(chapter) },
                            onDownloadClick = { onDownloadChapter(chapter) }
                        )
                    }
                }

                // CTA: Generate All Audio Button
                Button(
                    onClick = onSynthesizeAllClick,
                    enabled = !isGeneratingAudio,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("generate_audiobook_button")
                ) {
                    if (isGeneratingAudio) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Synthesizing Narration & Combining...")
                    } else {
                        Icon(
                            imageVector = Icons.Filled.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Synthesize & Combine Full Audiobook", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun ChapterItemCard(
    chapter: Chapter,
    isCurrentlyPlaying: Boolean,
    onPreviewClick: () -> Unit,
    onSynthesizeClick: () -> Unit,
    onViewScriptClick: () -> Unit,
    onDownloadClick: () -> Unit
) {
    val isReady = chapter.status == ChapterStatus.AUDIO_READY
    val isSynthesizing = chapter.status == ChapterStatus.SYNTHESIZING

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isCurrentlyPlaying) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface,
        border = if (isCurrentlyPlaying) {
            androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
        } else {
            androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
        },
        modifier = Modifier
            .fillMaxWidth()
            .testTag("chapter_item_${chapter.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Title and index
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(
                                if (isReady) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${chapter.index}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isReady) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Column {
                        Text(
                            text = chapter.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${chapter.wordCount} words · ~${chapter.estimatedDurationSec}s",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Status Badge & Engine
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (chapter.engineUsed != null) {
                        val badgeColor = when (chapter.engineUsed!!) {
                            AudioEngineType.OPENAI_TTS -> MaterialTheme.colorScheme.tertiaryContainer
                            AudioEngineType.CVOICE_AI -> MaterialTheme.colorScheme.primaryContainer
                            AudioEngineType.LOCAL_FALLBACK -> MaterialTheme.colorScheme.secondaryContainer
                        }
                        val textColor = when (chapter.engineUsed!!) {
                            AudioEngineType.OPENAI_TTS -> MaterialTheme.colorScheme.onTertiaryContainer
                            AudioEngineType.CVOICE_AI -> MaterialTheme.colorScheme.onPrimaryContainer
                            AudioEngineType.LOCAL_FALLBACK -> MaterialTheme.colorScheme.onSecondaryContainer
                        }
                        val label = when (chapter.engineUsed!!) {
                            AudioEngineType.OPENAI_TTS -> "OpenAI TTS"
                            AudioEngineType.CVOICE_AI -> "cvoice.ai"
                            AudioEngineType.LOCAL_FALLBACK -> "Studio TTS"
                        }
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = badgeColor
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = textColor,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    when (chapter.status) {
                        ChapterStatus.SYNTHESIZING -> {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        }
                        ChapterStatus.AUDIO_READY -> {
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = "Audio ready",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        ChapterStatus.ERROR -> {
                            Icon(
                                imageVector = Icons.Filled.Error,
                                contentDescription = "Error",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        else -> {
                            Icon(
                                imageVector = Icons.Outlined.HourglassEmpty,
                                contentDescription = "Pending",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // Excerpt preview
            Text(
                text = chapter.cleanedNarration.take(120) + if (chapter.cleanedNarration.length > 120) "..." else "",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            // Action Buttons Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: Script view
                TextButton(
                    onClick = onViewScriptClick,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier.testTag("view_script_${chapter.id}")
                ) {
                    Icon(imageVector = Icons.Outlined.Description, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Read / Edit Script", style = MaterialTheme.typography.labelSmall)
                }

                // Right: Audio Actions
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (isReady) {
                        FilledTonalIconButton(
                            onClick = onPreviewClick,
                            modifier = Modifier
                                .size(40.dp)
                                .testTag("preview_chapter_${chapter.id}")
                        ) {
                            Icon(
                                imageVector = if (isCurrentlyPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                contentDescription = "Preview chapter",
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        IconButton(
                            onClick = onDownloadClick,
                            modifier = Modifier
                                .size(40.dp)
                                .testTag("download_chapter_${chapter.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Download,
                                contentDescription = "Download chapter",
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        FilledTonalButton(
                            onClick = onSynthesizeClick,
                            enabled = !isSynthesizing,
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("synthesize_chapter_${chapter.id}")
                        ) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Synthesize", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }

            // Error note if any
            if (chapter.errorMessage != null) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = chapter.errorMessage ?: "",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}
