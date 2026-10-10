package com.example.data.model

import android.net.Uri
import java.io.File

enum class SheetJobStatus(val label: String) {
    READY("READY"),
    PROCESSING("PROCESSING"),
    UPLOADED("UPLOADED"),
    FAILED("FAILED");

    companion object {
        fun fromString(raw: String): SheetJobStatus {
            return when (raw.trim().uppercase()) {
                "PROCESSING" -> PROCESSING
                "UPLOADED", "COMPLETED", "DONE" -> UPLOADED
                "FAILED", "ERROR" -> FAILED
                else -> READY
            }
        }
    }
}

data class SheetVideoRow(
    val rowNumber: Int,
    val videoUrl: String,                     // Column A
    val videoTitle: String,                   // Column B
    val videoDescription: String,             // Column C
    val thumbnailUrl: String = "",            // Column D (optional)
    val musicSelection: String = "",          // Column E
    val musicVolume: Float = 1.0f,            // Column F
    val uploadStatus: SheetJobStatus = SheetJobStatus.READY, // Column G
    val scheduledUploadTime: String = "",     // Column H
    val youtubeVideoUrl: String = "",         // Column I
    val errorMessage: String = ""             // Column J
)

data class UploadedMusicTrack(
    val id: String,
    val name: String,
    val durationMs: Long,
    val formattedDuration: String,
    val format: String,
    val uploadStatus: String = "UPLOADED",
    val localFilePath: String? = null,
    val fileUri: Uri? = null,
    val isDefault: Boolean = false
)

enum class QueueRunStatus {
    IDLE,
    RUNNING,
    PAUSED,
    STOPPED
}

data class AutomationDashboardState(
    // Google Sheets Connection
    val isSheetsConnected: Boolean = false,
    val spreadsheetId: String = "",
    val sheetName: String = "Sheet1",
    val sheetUrl: String = "",
    val googleAccountEmail: String = "",
    val googleAccessToken: String = "",
    val isFetchingSheet: Boolean = false,
    val sheetError: String? = null,

    // Queue Rows from Google Sheets
    val queueRows: List<SheetVideoRow> = defaultInitialQueueRows(),

    // Music Library (MP3, WAV, AAC, M4A)
    val musicLibrary: List<UploadedMusicTrack> = defaultMusicLibrary(),
    val defaultMusicTrackId: String = "track_cyberpunk_anime",

    // Automation Engine & Progress
    val queueStatus: QueueRunStatus = QueueRunStatus.IDLE,
    val currentProcessingRowNumber: Int? = null,
    val currentProcessingTitle: String? = null,
    val currentStepLabel: String = "Idle • Ready to start automation",
    val videoProcessingProgress: Float = 0f,
    val youtubeUploadProgress: Float = 0f,
    val latestSuccessfulUploadTitle: String? = null,
    val latestSuccessfulUploadUrl: String? = null,
    val automationLogs: List<String> = listOf("System ready. Connect Google Sheets & YouTube or press START AUTOMATION."),

    // Settings
    val keepOriginalAudio: Boolean = true,
    val originalAudioVolume: Float = 0.75f,
    val defaultMusicVolume: Float = 0.85f,
    val fadeInSeconds: Float = 1.5f,
    val fadeOutSeconds: Float = 2.0f,
    val defaultPrivacy: YouTubePrivacy = YouTubePrivacy.PUBLIC,
    val defaultDescription: String = DEFAULT_REMIX_VIDEO_DESCRIPTION,
    val deleteTempFilesAfterUpload: Boolean = true,
    val notificationsEnabled: Boolean = true
) {
    val totalVideosInQueue: Int get() = queueRows.size
    val uploadedCount: Int get() = queueRows.count { it.uploadStatus == SheetJobStatus.UPLOADED }
    val waitingCount: Int get() = queueRows.count { it.uploadStatus == SheetJobStatus.READY }
    val processingCount: Int get() = queueRows.count { it.uploadStatus == SheetJobStatus.PROCESSING }
    val failedCount: Int get() = queueRows.count { it.uploadStatus == SheetJobStatus.FAILED }

    val estimatedQueueStatusText: String
        get() = when {
            queueStatus == QueueRunStatus.RUNNING && currentProcessingTitle != null ->
                "Processing row #$currentProcessingRowNumber ($waitingCount waiting in queue)"
            queueStatus == QueueRunStatus.PAUSED ->
                "Queue paused ($waitingCount READY rows remaining)"
            waitingCount == 0 && uploadedCount > 0 ->
                "All queued videos processed ($uploadedCount uploaded)"
            waitingCount > 0 ->
                "$waitingCount READY video(s) queued for automation"
            else ->
                "Queue empty — add rows to Google Sheet or refresh"
        }

    companion object {
        fun defaultMusicLibrary(): List<UploadedMusicTrack> = listOf(
            UploadedMusicTrack(
                id = "track_cyberpunk_anime",
                name = "Cyberpunk Anime Fight Beat.mp3",
                durationMs = 195000L,
                formattedDuration = "03:15",
                format = "MP3",
                uploadStatus = "UPLOADED",
                isDefault = true
            ),
            UploadedMusicTrack(
                id = "track_phonk_drift",
                name = "Phonk Donghua Battle Ost.wav",
                durationMs = 138000L,
                formattedDuration = "02:18",
                format = "WAV",
                uploadStatus = "UPLOADED",
                isDefault = false
            ),
            UploadedMusicTrack(
                id = "track_lofi_chillhop",
                name = "Lofi Ambient Chillhop.m4a",
                durationMs = 170000L,
                formattedDuration = "02:50",
                format = "M4A",
                uploadStatus = "UPLOADED",
                isDefault = false
            )
        )

        fun defaultInitialQueueRows(): List<SheetVideoRow> = listOf(
            SheetVideoRow(
                rowNumber = 2,
                videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4",
                videoTitle = "Hiromi Higuruma Domain Expansion Edit #shorts",
                videoDescription = DEFAULT_REMIX_VIDEO_DESCRIPTION,
                thumbnailUrl = "",
                musicSelection = "Cyberpunk Anime Fight Beat.mp3",
                musicVolume = 0.9f,
                uploadStatus = SheetJobStatus.READY,
                scheduledUploadTime = "",
                youtubeVideoUrl = "",
                errorMessage = ""
            ),
            SheetVideoRow(
                rowNumber = 3,
                videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
                videoTitle = "Xiao Yan BTTH Final Battle 4K Explained #donghua",
                videoDescription = DEFAULT_REMIX_VIDEO_DESCRIPTION,
                thumbnailUrl = "",
                musicSelection = "Phonk Donghua Battle Ost.wav",
                musicVolume = 0.85f,
                uploadStatus = SheetJobStatus.READY,
                scheduledUploadTime = "",
                youtubeVideoUrl = "",
                errorMessage = ""
            )
        )
    }
}
