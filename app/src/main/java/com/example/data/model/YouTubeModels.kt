package com.example.data.model

enum class YouTubePrivacy(val apiValue: String, val displayName: String) {
    PRIVATE("private", "Private"),
    UNLISTED("unlisted", "Unlisted"),
    PUBLIC("public", "Public")
}

data class YouTubeUploadState(
    val isConnected: Boolean = false,
    val userEmail: String = "",
    val userName: String = "",
    val userAvatarUrl: String? = null,
    val isGeneratingVideo: Boolean = false,
    val videoGenerationProgress: Float = 0f,
    val generatedVideoFile: java.io.File? = null,
    val isUploading: Boolean = false,
    val uploadProgress: Float = 0f,
    val uploadStepMessage: String = "",
    val uploadedVideoId: String? = null,
    val uploadedVideoUrl: String? = null,
    val uploadedPrivacy: YouTubePrivacy? = null,
    val uploadError: String? = null,
    val includeChapterMarkers: Boolean = true
)
