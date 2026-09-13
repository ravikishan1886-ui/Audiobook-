package com.example.data.model

import android.net.Uri
import java.io.File
import java.util.Locale

enum class RemixViewMode {
    INPUT,
    PREVIEW,
    PROCESSING
}

enum class PipelineStepStatus {
    PENDING,
    IN_PROGRESS,
    COMPLETED,
    FAILED
}

enum class RemixPipelineStep(val stepNumber: Int, val label: String) {
    VIDEO_RECEIVED(1, "Video received"),
    MUSIC_UPLOADED(2, "Music uploaded"),
    VIDEO_ANALYZED(3, "Video analyzed"),
    MIXING_AUDIO(4, "Mixing audio"),
    RENDERING_VIDEO(5, "Rendering final video"),
    PREPARING_YOUTUBE(6, "Preparing YouTube upload"),
    UPLOADING_YOUTUBE(7, "Uploading to YouTube"),
    COMPLETE(8, "Complete")
}

data class SampleVideoOption(
    val title: String,
    val durationLabel: String,
    val url: String,
    val description: String
)

data class SampleMusicOption(
    val title: String,
    val durationLabel: String,
    val durationMs: Long,
    val genre: String
)

data class YouTubeSourceInfo(
    val videoId: String,
    val title: String,
    val author: String,
    val thumbnailUrl: String,
    val sourceUrl: String,
    val isShortsSource: Boolean = false
)

data class VideoMusicRemixState(
    val currentView: RemixViewMode = RemixViewMode.INPUT,
    val videoUrl: String = "",
    val musicFileName: String = "Original Video Audio (Keep Same Music)",
    val musicFileUri: Uri? = null,
    val localMusicFile: File? = null,
    val youtubeMusicUrl: String = "",
    val isFetchingYouTubeMusic: Boolean = false,
    val youtubeMusicError: String? = null,
    val youtubeMusicTitle: String? = null,
    val youtubeMusicAuthor: String? = null,
    val youtubeMusicThumbnailUrl: String? = null,
    val isResolvingYouTubeSource: Boolean = false,
    val resolvedYouTubeSource: YouTubeSourceInfo? = null,
    val downloadedVideoFile: File? = null,
    val renderedMp4File: File? = null,

    // Durations in milliseconds
    val originalDurationMs: Long = 332000L, // Default 05:32 demo
    val musicDurationMs: Long = 195000L,    // Default 03:15 demo
    val finalDurationMs: Long = 332000L,    // Matched to original duration: 05:32

    val hasRightsPermission: Boolean = true,
    val videoTitle: String = "Remixed Video with Soundtrack",
    val videoDescription: String = "Video remixed with custom background music overlay and duration alignment.",
    val privacyStatus: YouTubePrivacy = YouTubePrivacy.PRIVATE,
    val sourceBadge: String = "",
    val detectedFileName: String? = null,
    val fileSizeBytes: Long? = null,

    // Processing Pipeline
    val isProcessing: Boolean = false,
    val isComplete: Boolean = false,
    val currentStep: RemixPipelineStep = RemixPipelineStep.VIDEO_RECEIVED,
    val stepStatuses: Map<RemixPipelineStep, PipelineStepStatus> = defaultStepStatuses(),
    val progressPercent: Float = 0.0f,
    val statusMessage: String = "Ready to process",
    val errorMessage: String? = null,

    // YouTube Upload Result
    val youtubeVideoId: String? = null,
    val youtubeVideoUrl: String? = null,
    val isSavedToGallery: Boolean = false
) {
    val isExactSameMusic: Boolean
        get() = musicFileName.contains("Original", ignoreCase = true) ||
                musicFileName.contains("Same", ignoreCase = true) ||
                (youtubeMusicUrl.isBlank() && musicFileUri == null && localMusicFile == null)

    val formattedOriginalDuration: String
        get() = formatDuration(originalDurationMs)

    val formattedMusicDuration: String
        get() = if (isExactSameMusic) formatDuration(originalDurationMs) else formatDuration(musicDurationMs)

    val formattedFinalDuration: String
        get() = formatDuration(finalDurationMs)

    companion object {
        fun defaultStepStatuses(): Map<RemixPipelineStep, PipelineStepStatus> {
            return RemixPipelineStep.values().associateWith { PipelineStepStatus.PENDING }
        }

        fun formatDuration(durationMs: Long): String {
            if (durationMs <= 0) return "00:00"
            val totalSeconds = durationMs / 1000
            val hours = totalSeconds / 3600
            val minutes = (totalSeconds % 3600) / 60
            val seconds = totalSeconds % 60
            return if (hours > 0) {
                String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
            } else {
                String.format(Locale.US, "%02d:%02d", minutes, seconds)
            }
        }
    }
}
