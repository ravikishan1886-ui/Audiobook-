package com.example.data.model

enum class ChapterStatus {
    PENDING,
    CLEANING_TEXT,
    READY_FOR_AUDIO,
    SYNTHESIZING,
    AUDIO_READY,
    ERROR
}

enum class AudioEngineType(val label: String) {
    OPENAI_TTS("OpenAI Neural TTS"),
    CVOICE_AI("cvoice.ai Voice"),
    LOCAL_FALLBACK("Studio Voice Engine")
}

data class VoiceProfile(
    val id: String,
    val name: String,
    val gender: String,
    val accent: String,
    val style: String,
    val description: String,
    val pitch: Float = 1.0f,
    val speed: Float = 1.0f,
    val isCvoiceOfficial: Boolean = true,
    val openAiVoice: String? = null,
    val instructions: String? = null
)

data class Chapter(
    val id: String,
    val index: Int,
    val title: String,
    val rawText: String,
    var cleanedNarration: String,
    val wordCount: Int = 0,
    val estimatedDurationSec: Int = 0,
    var audioFilePath: String? = null,
    var audioDurationMs: Long = 0L,
    var status: ChapterStatus = ChapterStatus.PENDING,
    var engineUsed: AudioEngineType? = null,
    var errorMessage: String? = null
)

data class SampleBook(
    val title: String,
    val author: String,
    val genre: String,
    val excerpt: String
)

data class GenerationProgress(
    val isGenerating: Boolean = false,
    val currentStep: String = "",
    val currentChapterIndex: Int = 0,
    val totalChapters: Int = 0,
    val progressPercent: Float = 0f,
    val detailMessage: String = "",
    val error: String? = null
)

data class PlayerState(
    val isPlaying: Boolean = false,
    val currentChapterId: String? = null,
    val currentPositionMs: Long = 0L,
    val totalDurationMs: Long = 0L,
    val playbackSpeed: Float = 1.0f,
    val isFullAudiobookMode: Boolean = false,
    val audioTitle: String = ""
)
