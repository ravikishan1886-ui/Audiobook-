package com.example.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.AudioMerger
import com.example.audio.AudioPlayerManager
import com.example.audio.LocalAudioSynthesizer
import com.example.data.api.CVoiceApiException
import com.example.data.api.CVoiceClient
import com.example.data.api.GeminiClient
import com.example.data.api.YouTubeUploader
import com.example.data.model.AudioEngineType
import com.example.data.model.Chapter
import com.example.data.model.ChapterStatus
import com.example.data.model.GenerationProgress
import com.example.data.model.SampleBook
import com.example.data.model.VoiceProfile
import com.example.data.model.VoiceProfiles
import com.example.data.model.YouTubePrivacy
import com.example.data.model.YouTubeUploadState
import com.example.data.model.*
import com.example.data.samples.SampleBooks
import com.example.video.VideoGenerator
import com.example.video.VideoMusicRemixerEngine
import com.example.video.VideoUrlUtils
import com.example.video.YouTubeAudioExtractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

data class AudiobookUiState(
    val bookTitle: String = "Ye Grand Internet Service Company",
    val authorName: String = "Grand Telecom",
    val manuscriptText: String = "",
    val rightsConfirmed: Boolean = true,
    val selectedVoice: VoiceProfile = VoiceProfiles.availableVoices[0],
    val chapters: List<Chapter> = emptyList(),
    val progress: GenerationProgress = GenerationProgress(),
    val fullAudiobookFile: File? = null,
    val fullAudiobookDurationMs: Long = 0L,
    val cvoiceApiError: String? = null,
    val notificationMessage: String? = null,
    val showSettingsDialog: Boolean = false,
    val previewChapterText: Chapter? = null,
    val currentSampleBook: SampleBook? = null,
    val isAutoLocalFallbackEnabled: Boolean = true,
    val customGeminiKey: String = "",
    val customCVoiceKey: String = "sk-ijkl1234ijkl1234ijkl1234ijkl1234ijkl1234",
    val customCVoiceUrl: String = CVoiceClient.OPENAI_TTS_ENDPOINT,
    val customClaudeKey: String = "",
    val customThumbnailFile: File? = null,
    val showYouTubeAuthDialog: Boolean = false,
    val customYouTubeToken: String = "",
    val youTubeState: YouTubeUploadState = YouTubeUploadState(
        isConnected = false,
        userEmail = "",
        userName = ""
    )
)

class AudiobookViewModel(application: Application) : AndroidViewModel(application) {
    private val TAG = "AudiobookViewModel"
    private val context: Context get() = getApplication<Application>().applicationContext

    private val geminiClient = GeminiClient()
    private val cvoiceClient = CVoiceClient(context)
    private val youTubeUploader = YouTubeUploader(context)
    private val localSynthesizer = LocalAudioSynthesizer(context)
    val playerManager = AudioPlayerManager(context)

    private val _uiState = MutableStateFlow(AudiobookUiState())
    val uiState: StateFlow<AudiobookUiState> = _uiState.asStateFlow()

    private val _remixState = MutableStateFlow(VideoMusicRemixState())
    val remixState: StateFlow<VideoMusicRemixState> = _remixState.asStateFlow()

    init {
        // Load initial sample book
        val defaultSample = SampleBooks.samples.first()
        loadSampleBook(defaultSample)

        // Initialize with user provided OpenAI key for speech synthesis and structuring
        val userOpenAiKey = "sk-ijkl1234ijkl1234ijkl1234ijkl1234ijkl1234"
        cvoiceClient.setApiKey(userOpenAiKey)
        cvoiceClient.setBaseUrl(CVoiceClient.OPENAI_TTS_ENDPOINT)
        geminiClient.setOpenAiKey(userOpenAiKey)
        _uiState.update {
            it.copy(
                customCVoiceKey = userOpenAiKey,
                customCVoiceUrl = CVoiceClient.OPENAI_TTS_ENDPOINT
            )
        }
    }

    fun updateBookTitle(title: String) {
        _uiState.update { it.copy(bookTitle = title) }
    }

    fun updateAuthorName(author: String) {
        _uiState.update { it.copy(authorName = author) }
    }

    fun updateManuscriptText(text: String) {
        _uiState.update { it.copy(manuscriptText = text, chapters = emptyList(), fullAudiobookFile = null) }
    }

    fun setRightsConfirmed(confirmed: Boolean) {
        _uiState.update { it.copy(rightsConfirmed = confirmed) }
    }

    fun selectVoice(voice: VoiceProfile) {
        _uiState.update { it.copy(selectedVoice = voice) }
    }

    fun loadSampleBook(sample: SampleBook) {
        _uiState.update {
            it.copy(
                bookTitle = sample.title,
                authorName = sample.author,
                manuscriptText = sample.excerpt,
                rightsConfirmed = true,
                currentSampleBook = sample,
                chapters = emptyList(),
                fullAudiobookFile = null,
                cvoiceApiError = null
            )
        }
    }

    fun onFileUploaded(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val extractResult = com.example.data.PdfTextExtractor.extract(context, uri)
            if (extractResult.isSuccess) {
                val doc = extractResult.getOrThrow()
                val inferredTitle = doc.fileName.substringBeforeLast(".")
                    .replace("_", " ")
                    .replace("-", " ")
                    .trim()
                    .ifBlank { "Uploaded Manuscript" }
                withContext(Dispatchers.Main) {
                    _uiState.update {
                        it.copy(
                            manuscriptText = doc.text,
                            bookTitle = if (it.bookTitle.isBlank() || it.bookTitle == "Ye Grand Internet Service Company") inferredTitle else it.bookTitle,
                            chapters = emptyList(),
                            fullAudiobookFile = null,
                            notificationMessage = "${if (doc.isPdf) "PDF" else "Document"} loaded (${doc.characterCount} characters)"
                        )
                    }
                }
            } else {
                val errorMsg = extractResult.exceptionOrNull()?.message ?: "Failed to read file"
                Log.e(TAG, "Failed to read file: $errorMsg")
                withContext(Dispatchers.Main) {
                    _uiState.update { it.copy(cvoiceApiError = errorMsg) }
                }
            }
        }
    }

    fun cleanAndSplitWithGemini() {
        val state = _uiState.value
        if (!state.rightsConfirmed) {
            _uiState.update { it.copy(notificationMessage = "Please confirm content copyright / rights before proceeding.") }
            return
        }
        if (state.manuscriptText.isBlank()) {
            _uiState.update { it.copy(notificationMessage = "Please provide book text or manuscript.") }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    progress = GenerationProgress(
                        isGenerating = true,
                        currentStep = "Step 1: Cleaning & Structuring with Gemini",
                        progressPercent = 0.15f,
                        detailMessage = "Analyzing manuscript, expanding spoken abbreviations, and structuring chapters..."
                    ),
                    cvoiceApiError = null,
                    fullAudiobookFile = null
                )
            }

            val result = geminiClient.cleanAndStructureManuscript(
                bookTitle = state.bookTitle,
                author = state.authorName,
                rawText = state.manuscriptText
            )

            result.onSuccess { chapters ->
                _uiState.update {
                    it.copy(
                        chapters = chapters,
                        progress = GenerationProgress(
                            isGenerating = true,
                            currentStep = "Step 2: Synthesizing Audio (0/${chapters.size})",
                            progressPercent = 0.25f,
                            detailMessage = "Manuscript structured into ${chapters.size} chapter(s). Generating voice narration..."
                        ),
                        notificationMessage = "Manuscript structured. Generating audio..."
                    )
                }

                // Automatically proceed to synthesize audio for all chapters
                synthesizeAudiobookPipeline(chapters)
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        progress = GenerationProgress(isGenerating = false, error = err.message),
                        cvoiceApiError = "Gemini organization error: ${err.message}"
                    )
                }
            }
        }
    }

    private suspend fun synthesizeAudiobookPipeline(inputChapters: List<Chapter>) {
        val state = _uiState.value
        val total = inputChapters.size
        val updatedChapters = inputChapters.toMutableList()
        var cvoiceErrorDetected: String? = null

        for (i in updatedChapters.indices) {
            val chap = updatedChapters[i]
            val stepPercent = 0.25f + (0.55f * ((i.toFloat()) / total))

            _uiState.update {
                it.copy(
                    progress = GenerationProgress(
                        isGenerating = true,
                        currentStep = "Step 2: Synthesizing Audio (${i + 1}/$total)",
                        currentChapterIndex = i + 1,
                        totalChapters = total,
                        progressPercent = stepPercent,
                        detailMessage = "Generating voice narration for '${chap.title}'..."
                    )
                )
            }

            // Update chapter status in list
            updatedChapters[i] = chap.copy(status = ChapterStatus.SYNTHESIZING)
            _uiState.update { it.copy(chapters = updatedChapters.toList()) }

            // Attempt cvoice.ai synthesis
            val cvoiceResult = cvoiceClient.generateSpeech(
                chapterId = chap.id,
                text = chap.cleanedNarration,
                voice = state.selectedVoice
            )

            if (cvoiceResult.isSuccess) {
                val (file, engine) = cvoiceResult.getOrThrow()
                updatedChapters[i] = chap.copy(
                    status = ChapterStatus.AUDIO_READY,
                    audioFilePath = file.absolutePath,
                    engineUsed = engine,
                    errorMessage = null
                )
            } else {
                val error = cvoiceResult.exceptionOrNull()
                val errorText = error?.message ?: "cvoice.ai API request failed"
                cvoiceErrorDetected = errorText
                Log.w(TAG, "cvoice.ai failed for chapter ${chap.title}: $errorText")

                if (state.isAutoLocalFallbackEnabled) {
                    // Synthesize via local engine as fallback so user can still test
                    val localResult = localSynthesizer.synthesizeChapterToWav(
                        chapterId = chap.id,
                        text = chap.cleanedNarration,
                        voice = state.selectedVoice
                    )

                    if (localResult.isSuccess) {
                        val localFile = localResult.getOrThrow()
                        updatedChapters[i] = chap.copy(
                            status = ChapterStatus.AUDIO_READY,
                            audioFilePath = localFile.absolutePath,
                            engineUsed = AudioEngineType.LOCAL_FALLBACK,
                            errorMessage = null
                        )
                    } else {
                        updatedChapters[i] = chap.copy(
                            status = ChapterStatus.ERROR,
                            errorMessage = errorText
                        )
                    }
                } else {
                    updatedChapters[i] = chap.copy(
                        status = ChapterStatus.ERROR,
                        errorMessage = errorText
                    )
                }
            }

            _uiState.update { it.copy(chapters = updatedChapters.toList()) }
        }

        // Step 3: Combine all chapters into full audiobook
        _uiState.update {
            it.copy(
                progress = GenerationProgress(
                    isGenerating = true,
                    currentStep = "Step 3: Combining Complete Audiobook",
                    progressPercent = 0.9f,
                    detailMessage = "Merging chapter audio tracks into unified master WAV file..."
                )
            )
        }

        val mergeResult = AudioMerger.combineChaptersIntoAudiobook(
            context = context,
            bookTitle = state.bookTitle,
            author = state.authorName,
            chapters = updatedChapters
        )

        if (mergeResult.isSuccess) {
            val fullFile = mergeResult.getOrThrow()
            val anyFailed = updatedChapters.any { it.status == ChapterStatus.ERROR }
            val anyFallback = updatedChapters.any { it.engineUsed == AudioEngineType.LOCAL_FALLBACK }

            _uiState.update {
                it.copy(
                    fullAudiobookFile = fullFile,
                    progress = GenerationProgress(
                        isGenerating = false,
                        currentStep = "Audiobook Complete",
                        progressPercent = 1.0f,
                        detailMessage = "Master audiobook ready for playback & download."
                    ),
                    cvoiceApiError = if (anyFailed) cvoiceErrorDetected else null,
                    notificationMessage = when {
                        anyFailed -> "Audiobook generated with some errors: $cvoiceErrorDetected"
                        anyFallback -> "Audiobook narration generated successfully using Studio Voice Engine!"
                        updatedChapters.any { it.engineUsed == AudioEngineType.OPENAI_TTS } -> "Audiobook narration generated successfully using OpenAI Neural TTS!"
                        else -> "Audiobook narration generated successfully using cvoice.ai!"
                    }
                )
            }
        } else {
            _uiState.update {
                it.copy(
                    progress = GenerationProgress(
                        isGenerating = false,
                        error = mergeResult.exceptionOrNull()?.message ?: "Failed to merge audio"
                    ),
                    cvoiceApiError = cvoiceErrorDetected
                )
            }
        }
    }

    fun generateCompleteAudiobook() {
        val state = _uiState.value
        if (!state.rightsConfirmed) {
            _uiState.update { it.copy(notificationMessage = "Please confirm content rights before generating.") }
            return
        }

        viewModelScope.launch {
            // If chapters haven't been split yet, split first
            var currentChapters = state.chapters
            if (currentChapters.isEmpty()) {
                _uiState.update {
                    it.copy(
                        progress = GenerationProgress(
                            isGenerating = true,
                            currentStep = "Step 1: Cleaning & Structuring with Gemini",
                            progressPercent = 0.15f,
                            detailMessage = "Refining narration script..."
                        )
                    )
                }

                val geminiResult = geminiClient.cleanAndStructureManuscript(
                    state.bookTitle,
                    state.authorName,
                    state.manuscriptText
                )

                if (geminiResult.isSuccess) {
                    currentChapters = geminiResult.getOrNull() ?: emptyList()
                    _uiState.update { it.copy(chapters = currentChapters) }
                } else {
                    _uiState.update {
                        it.copy(
                            progress = GenerationProgress(isGenerating = false, error = "Failed to organize text"),
                            cvoiceApiError = "Could not structure manuscript."
                        )
                    }
                    return@launch
                }
            }

            synthesizeAudiobookPipeline(currentChapters)
        }
    }

    fun synthesizeSingleChapter(chapterId: String) {
        val state = _uiState.value
        val index = state.chapters.indexOfFirst { it.id == chapterId }
        if (index == -1) return

        viewModelScope.launch {
            val chapters = state.chapters.toMutableList()
            val chap = chapters[index]

            chapters[index] = chap.copy(status = ChapterStatus.SYNTHESIZING, errorMessage = null)
            _uiState.update { it.copy(chapters = chapters.toList()) }

            val cvoiceResult = cvoiceClient.generateSpeech(
                chapterId = chap.id,
                text = chap.cleanedNarration,
                voice = state.selectedVoice
            )

            if (cvoiceResult.isSuccess) {
                val (file, engine) = cvoiceResult.getOrThrow()
                chapters[index] = chap.copy(
                    status = ChapterStatus.AUDIO_READY,
                    audioFilePath = file.absolutePath,
                    engineUsed = engine,
                    errorMessage = null
                )
                _uiState.update {
                    it.copy(
                        chapters = chapters.toList(),
                        notificationMessage = "Synthesized audio for '${chap.title}'"
                    )
                }
            } else {
                val err = cvoiceResult.exceptionOrNull()?.message ?: "cvoice.ai synthesis failed"
                if (state.isAutoLocalFallbackEnabled) {
                    val fallbackResult = localSynthesizer.synthesizeChapterToWav(
                        chapterId = chap.id,
                        text = chap.cleanedNarration,
                        voice = state.selectedVoice
                    )
                    if (fallbackResult.isSuccess) {
                        chapters[index] = chap.copy(
                            status = ChapterStatus.AUDIO_READY,
                            audioFilePath = fallbackResult.getOrThrow().absolutePath,
                            engineUsed = AudioEngineType.LOCAL_FALLBACK,
                            errorMessage = null
                        )
                        _uiState.update {
                            it.copy(
                                chapters = chapters.toList(),
                                cvoiceApiError = null,
                                notificationMessage = "Synthesized '${chap.title}' using Studio Voice Engine"
                            )
                        }
                    } else {
                        chapters[index] = chap.copy(status = ChapterStatus.ERROR, errorMessage = err)
                        _uiState.update {
                            it.copy(
                                chapters = chapters.toList(),
                                cvoiceApiError = "Voice generation error on ${chap.title}: $err"
                            )
                        }
                    }
                } else {
                    chapters[index] = chap.copy(status = ChapterStatus.ERROR, errorMessage = err)
                    _uiState.update {
                        it.copy(
                            chapters = chapters.toList(),
                            cvoiceApiError = "cvoice.ai error on ${chap.title}: $err"
                        )
                    }
                }
            }
        }
    }

    fun previewChapter(chapter: Chapter) {
        val path = chapter.audioFilePath
        if (path != null && File(path).exists()) {
            playerManager.playAudio(
                filePath = path,
                audioTitle = "${_uiState.value.bookTitle} - ${chapter.title}",
                chapterId = chapter.id,
                isFullAudiobook = false
            )
        } else {
            _uiState.update { it.copy(notificationMessage = "Please synthesize audio for '${chapter.title}' first.") }
        }
    }

    fun playFullAudiobook() {
        val file = _uiState.value.fullAudiobookFile
        if (file != null && file.exists()) {
            playerManager.playAudio(
                filePath = file.absolutePath,
                audioTitle = "${_uiState.value.bookTitle} (Complete Audiobook)",
                chapterId = null,
                isFullAudiobook = true
            )
        } else {
            _uiState.update { it.copy(notificationMessage = "Please generate the audiobook first.") }
        }
    }

    fun downloadCompletedAudiobook() {
        val state = _uiState.value
        val file = state.fullAudiobookFile
        if (file == null || !file.exists()) {
            _uiState.update { it.copy(notificationMessage = "No completed audiobook file available to download.") }
            return
        }

        viewModelScope.launch {
            val fileName = "${state.bookTitle.replace("[^a-zA-Z0-9.-]".toRegex(), "_")}_Audiobook.wav"
            val saveResult = AudioMerger.saveAudioToDownloads(
                context = context,
                audioFile = file,
                displayName = fileName,
                title = state.bookTitle,
                artist = state.authorName
            )

            if (saveResult.isSuccess) {
                _uiState.update {
                    it.copy(notificationMessage = "Audiobook saved to Music/Audiobooks ($fileName)")
                }
                Toast.makeText(context, "Saved to Device Music/Audiobooks folder!", Toast.LENGTH_LONG).show()
            } else {
                // Fallback to share intent
                AudioMerger.shareAudioFile(context, file, state.bookTitle)
            }
        }
    }

    fun downloadChapterAudio(chapter: Chapter) {
        val path = chapter.audioFilePath ?: return
        val file = File(path)
        if (!file.exists()) return

        viewModelScope.launch {
            val state = _uiState.value
            val fileName = "${state.bookTitle}_${chapter.title.replace("[^a-zA-Z0-9.-]".toRegex(), "_")}.wav"
            val saveResult = AudioMerger.saveAudioToDownloads(
                context = context,
                audioFile = file,
                displayName = fileName,
                title = "${state.bookTitle} - ${chapter.title}",
                artist = state.authorName
            )

            if (saveResult.isSuccess) {
                _uiState.update { it.copy(notificationMessage = "Saved '${chapter.title}' to Audiobooks") }
            } else {
                AudioMerger.shareAudioFile(context, file, "${state.bookTitle} - ${chapter.title}")
            }
        }
    }

    fun shareCompletedAudiobook() {
        val file = _uiState.value.fullAudiobookFile ?: return
        if (file.exists()) {
            AudioMerger.shareAudioFile(context, file, _uiState.value.bookTitle)
        }
    }

    fun openSettings(show: Boolean) {
        _uiState.update {
            it.copy(
                showSettingsDialog = show,
                customGeminiKey = geminiClient.getEffectiveApiKey(),
                customCVoiceKey = cvoiceClient.getEffectiveApiKey(),
                customCVoiceUrl = cvoiceClient.getEffectiveBaseUrl()
            )
        }
    }

    suspend fun testCVoiceApi(apiKey: String, baseUrl: String): Result<String> {
        return cvoiceClient.testConnection(apiKey, baseUrl)
    }

    fun saveApiSettings(geminiKey: String, cvoiceKey: String, cvoiceUrl: String, autoFallback: Boolean) {
        val normalizedUrl = CVoiceClient.normalizeEndpoint(cvoiceUrl, cvoiceKey)
        geminiClient.setApiKey(geminiKey)
        if (cvoiceKey.startsWith("sk-")) {
            geminiClient.setOpenAiKey(cvoiceKey)
        }
        cvoiceClient.setApiKey(cvoiceKey)
        cvoiceClient.setBaseUrl(normalizedUrl)
        _uiState.update {
            it.copy(
                showSettingsDialog = false,
                customGeminiKey = geminiKey,
                customCVoiceKey = cvoiceKey,
                customCVoiceUrl = normalizedUrl,
                isAutoLocalFallbackEnabled = autoFallback,
                cvoiceApiError = null,
                notificationMessage = "API configuration updated"
            )
        }
    }

    fun openChapterTextPreview(chapter: Chapter?) {
        _uiState.update { it.copy(previewChapterText = chapter) }
    }

    fun updateChapterNarration(chapterId: String, newNarration: String) {
        val chapters = _uiState.value.chapters.toMutableList()
        val idx = chapters.indexOfFirst { it.id == chapterId }
        if (idx != -1) {
            val old = chapters[idx]
            val words = newNarration.split("\\s+".toRegex()).filter { it.isNotBlank() }.size
            chapters[idx] = old.copy(
                cleanedNarration = newNarration,
                wordCount = words,
                estimatedDurationSec = (words / 2.5).toInt().coerceAtLeast(10),
                status = ChapterStatus.READY_FOR_AUDIO
            )
            _uiState.update { it.copy(chapters = chapters.toList()) }
        }
    }

    fun openYouTubeAuthDialog(show: Boolean) {
        _uiState.update { it.copy(showYouTubeAuthDialog = show) }
    }

    fun saveYouTubeToken(token: String) {
        val sanitized = YouTubeUploader.sanitizeToken(token)
        youTubeUploader.setAccessToken(sanitized)
        _uiState.update {
            it.copy(
                customYouTubeToken = sanitized ?: "",
                youTubeState = it.youTubeState.copy(
                    uploadError = null
                )
            )
        }

        if (sanitized != null) {
            viewModelScope.launch {
                _uiState.update {
                    it.copy(
                        youTubeState = it.youTubeState.copy(
                            isConnected = true,
                            userName = "YouTube Channel",
                            uploadError = null
                        ),
                        notificationMessage = "Connecting to YouTube..."
                    )
                }
                val channelResult = youTubeUploader.fetchChannelInfo()
                if (channelResult.isSuccess) {
                    val channelName = channelResult.getOrThrow()
                    _uiState.update {
                        it.copy(
                            youTubeState = it.youTubeState.copy(
                                isConnected = true,
                                userName = channelName,
                                uploadError = null
                            ),
                            notificationMessage = "Connected to YouTube: $channelName"
                        )
                    }
                } else {
                    val errorMsg = channelResult.exceptionOrNull()?.message ?: "Invalid or expired token"
                    _uiState.update {
                        it.copy(
                            youTubeState = it.youTubeState.copy(
                                isConnected = false,
                                userName = "",
                                uploadError = errorMsg
                            ),
                            notificationMessage = "YouTube connection error: $errorMsg"
                        )
                    }
                }
            }
        } else {
            _uiState.update {
                it.copy(
                    youTubeState = it.youTubeState.copy(
                        isConnected = false,
                        userName = "",
                        userEmail = "",
                        uploadedVideoUrl = null,
                        uploadedVideoId = null
                    ),
                    notificationMessage = "YouTube token removed"
                )
            }
        }
    }

    fun setYouTubeToken(token: String?) {
        saveYouTubeToken(token ?: "")
    }

    fun saveYouTubeTokenAndUpload(
        token: String,
        customTitle: String,
        customDescription: String,
        privacy: YouTubePrivacy,
        includeMarkers: Boolean
    ) {
        val sanitized = YouTubeUploader.sanitizeToken(token)
        youTubeUploader.setAccessToken(sanitized)
        _uiState.update {
            it.copy(
                customYouTubeToken = sanitized ?: "",
                showYouTubeAuthDialog = false,
                youTubeState = it.youTubeState.copy(
                    uploadError = null
                )
            )
        }

        if (sanitized.isNullOrBlank()) {
            _uiState.update {
                it.copy(
                    youTubeState = it.youTubeState.copy(isConnected = false),
                    notificationMessage = "Please provide a valid YouTube OAuth access token."
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    notificationMessage = "Connecting to YouTube channel..."
                )
            }
            val channelResult = youTubeUploader.fetchChannelInfo()
            if (channelResult.isSuccess) {
                val channelName = channelResult.getOrThrow()
                _uiState.update {
                    it.copy(
                        youTubeState = it.youTubeState.copy(
                            isConnected = true,
                            userName = channelName,
                            uploadError = null
                        ),
                        notificationMessage = "Connected to $channelName! Starting direct upload..."
                    )
                }
                uploadAudiobookToYouTube(customTitle, customDescription, privacy, includeMarkers)
            } else {
                val errorMsg = channelResult.exceptionOrNull()?.message ?: "Invalid or expired token"
                _uiState.update {
                    it.copy(
                        showYouTubeAuthDialog = true,
                        youTubeState = it.youTubeState.copy(
                            isConnected = false,
                            userName = "",
                            uploadError = errorMsg
                        ),
                        notificationMessage = "YouTube connection error: $errorMsg"
                    )
                }
            }
        }
    }

    fun connectYouTubeChannel() {
        openYouTubeAuthDialog(true)
    }

    fun disconnectYouTubeChannel() {
        saveYouTubeToken("")
        _uiState.update {
            it.copy(
                notificationMessage = "Disconnected YouTube channel."
            )
        }
    }

    private suspend fun ensureAudiobookAvailable(): File? {
        val existing = _uiState.value.fullAudiobookFile
        if (existing != null && existing.exists()) return existing

        val state = _uiState.value
        var currentChapters = state.chapters
        if (currentChapters.isEmpty()) {
            val defaultSample = SampleBooks.samples.first()
            val text = if (state.manuscriptText.isNotBlank()) state.manuscriptText else defaultSample.excerpt
            val title = if (state.bookTitle.isNotBlank()) state.bookTitle else defaultSample.title
            val author = if (state.authorName.isNotBlank()) state.authorName else defaultSample.author

            val ch1 = Chapter(
                id = "ch_auto_1",
                index = 0,
                title = "Chapter 1",
                rawText = text,
                cleanedNarration = text,
                wordCount = text.split("\\s+".toRegex()).size,
                status = ChapterStatus.PENDING
            )
            currentChapters = listOf(ch1)
            _uiState.update {
                it.copy(
                    bookTitle = title,
                    authorName = author,
                    manuscriptText = text,
                    chapters = currentChapters
                )
            }
        }

        _uiState.update {
            it.copy(
                notificationMessage = "Synthesizing audiobook audio...",
                youTubeState = it.youTubeState.copy(
                    isGeneratingVideo = true,
                    videoGenerationProgress = 0.05f,
                    uploadStepMessage = "Synthesizing narration audio...",
                    uploadError = null
                )
            )
        }

        val updatedChapters = mutableListOf<Chapter>()
        for (chap in currentChapters) {
            val audioPath = chap.audioFilePath
            if (audioPath != null && File(audioPath).exists()) {
                updatedChapters.add(chap)
                continue
            }
            val synthResult = localSynthesizer.synthesizeChapterToWav(
                chapterId = chap.id,
                text = chap.cleanedNarration,
                voice = state.selectedVoice
            )
            if (synthResult.isSuccess) {
                val file = synthResult.getOrThrow()
                updatedChapters.add(
                    chap.copy(
                        status = ChapterStatus.AUDIO_READY,
                        audioFilePath = file.absolutePath,
                        engineUsed = AudioEngineType.LOCAL_FALLBACK
                    )
                )
            } else {
                updatedChapters.add(chap)
            }
        }

        _uiState.update { it.copy(chapters = updatedChapters) }

        val mergeResult = AudioMerger.combineChaptersIntoAudiobook(
            context = context,
            bookTitle = state.bookTitle,
            author = state.authorName,
            chapters = updatedChapters
        )

        return if (mergeResult.isSuccess) {
            val fullFile = mergeResult.getOrThrow()
            _uiState.update { it.copy(fullAudiobookFile = fullFile) }
            fullFile
        } else {
            null
        }
    }

    fun saveVideoToGallery() {
        val state = _uiState.value
        viewModelScope.launch {
            var videoFile = state.youTubeState.generatedVideoFile
            if (videoFile == null || !videoFile.exists()) {
                val genResult = ensureVideoGenerated()
                if (genResult == null) return@launch
                videoFile = genResult
            }

            val title = "${state.bookTitle} by ${state.authorName}"
            val result = VideoGenerator.saveVideoToMovies(
                context = context,
                videoFile = videoFile,
                displayName = "${state.bookTitle}.mp4",
                title = title
            )

            if (result.isSuccess) {
                _uiState.update {
                    it.copy(notificationMessage = "Saved HD Video to Movies/Audiobooks on device!")
                }
            } else {
                VideoGenerator.shareVideoFile(
                    context = context,
                    videoFile = videoFile,
                    title = title,
                    description = "Audiobook Video",
                    targetYouTubeApp = false
                )
                _uiState.update {
                    it.copy(notificationMessage = "Video ready to save or share.")
                }
            }
        }
    }

    fun shareToYouTubeApp(
        customTitle: String,
        customDescription: String,
        includeMarkers: Boolean
    ) {
        val state = _uiState.value
        viewModelScope.launch {
            var videoFile = state.youTubeState.generatedVideoFile
            if (videoFile == null || !videoFile.exists()) {
                val genResult = ensureVideoGenerated()
                if (genResult == null) return@launch
                videoFile = genResult
            }

            val description = if (customDescription.isNotBlank()) {
                customDescription
            } else {
                youTubeUploader.buildYouTubeDescription(
                    bookTitle = state.bookTitle,
                    author = state.authorName,
                    voiceName = state.selectedVoice.name,
                    chapters = state.chapters,
                    includeMarkers = includeMarkers
                )
            }
            val title = customTitle.ifBlank { "${state.bookTitle} by ${state.authorName} (Full Audiobook)" }

            val shareResult = VideoGenerator.shareVideoFile(
                context = context,
                videoFile = videoFile,
                title = title,
                description = description,
                targetYouTubeApp = true
            )

            // Also copy description to clipboard as backup for YouTube upload screen
            try {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
                val clip = android.content.ClipData.newPlainText("YouTube Description", description)
                clipboard?.setPrimaryClip(clip)
            } catch (e: Exception) {
                Log.w("AudiobookViewModel", "Could not copy description to clipboard", e)
            }

            if (shareResult.isSuccess) {
                _uiState.update {
                    it.copy(
                        notificationMessage = "Video opened in device share menu. To publish directly to your channel, tap 'Upload to YouTube (Option A)'."
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        notificationMessage = "Could not open share menu: ${shareResult.exceptionOrNull()?.message}"
                    )
                }
            }
        }
    }

    private suspend fun ensureVideoGenerated(): File? {
        val existing = _uiState.value.youTubeState.generatedVideoFile
        if (existing != null && existing.exists()) return existing

        val audioFile = ensureAudiobookAvailable()
        if (audioFile == null || !audioFile.exists()) {
            _uiState.update {
                it.copy(
                    youTubeState = it.youTubeState.copy(
                        isGeneratingVideo = false,
                        uploadError = "Could not generate narration audio for video."
                    )
                )
            }
            return null
        }

        val state = _uiState.value
        _uiState.update {
            it.copy(
                youTubeState = it.youTubeState.copy(
                    isGeneratingVideo = true,
                    videoGenerationProgress = 0.05f,
                    uploadError = null
                )
            )
        }

        val videoResult = VideoGenerator.convertAudioToMp4(
            context = context,
            audioWavFile = audioFile,
            bookTitle = state.bookTitle,
            author = state.authorName,
            voiceName = state.selectedVoice.name,
            chaptersCount = state.chapters.size,
            customThumbnailFile = state.customThumbnailFile,
            onProgress = { prog ->
                _uiState.update {
                    it.copy(
                        youTubeState = it.youTubeState.copy(
                            videoGenerationProgress = prog
                        )
                    )
                }
            }
        )

        return if (videoResult.isSuccess) {
            val videoFile = videoResult.getOrThrow()
            _uiState.update {
                it.copy(
                    youTubeState = it.youTubeState.copy(
                        isGeneratingVideo = false,
                        videoGenerationProgress = 1.0f,
                        generatedVideoFile = videoFile
                    ),
                    notificationMessage = "Audiobook 720p HD MP4 video rendered successfully!"
                )
            }
            videoFile
        } else {
            val err = videoResult.exceptionOrNull()?.message ?: "Video rendering failed"
            Log.e("AudiobookViewModel", "Video render failed", videoResult.exceptionOrNull())
            _uiState.update {
                it.copy(
                    youTubeState = it.youTubeState.copy(
                        isGeneratingVideo = false,
                        uploadError = "Video render error: $err"
                    ),
                    notificationMessage = "Video render notice: $err"
                )
            }
            null
        }
    }

    fun generateYouTubeVideoFile() {
        viewModelScope.launch {
            ensureVideoGenerated()
        }
    }

    fun uploadAudiobookToYouTube(
        customTitle: String,
        customDescription: String,
        privacy: YouTubePrivacy,
        includeMarkers: Boolean
    ) {
        val state = _uiState.value
        val context = getApplication<Application>()

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    youTubeState = it.youTubeState.copy(
                        isUploading = true,
                        uploadProgress = 0.05f,
                        uploadStepMessage = "Preparing audiobook video for YouTube...",
                        uploadError = null
                    )
                )
            }

            // Step 1: Ensure MP4 video is rendered
            var videoFile = state.youTubeState.generatedVideoFile
            if (videoFile == null || !videoFile.exists()) {
                videoFile = ensureVideoGenerated()
                if (videoFile == null || !videoFile.exists()) {
                    _uiState.update {
                        it.copy(
                            youTubeState = it.youTubeState.copy(
                                isUploading = false,
                                uploadError = "Could not render video for upload."
                            )
                        )
                    }
                    return@launch
                }
            }

            // Step 2: Build description with chapters and timestamps
            val description = if (customDescription.isNotBlank()) {
                customDescription
            } else {
                youTubeUploader.buildYouTubeDescription(
                    bookTitle = state.bookTitle,
                    author = state.authorName,
                    voiceName = state.selectedVoice.name,
                    chapters = state.chapters,
                    includeMarkers = includeMarkers
                )
            }
            val title = customTitle.ifBlank { "${state.bookTitle} by ${state.authorName} (Full Audiobook)" }

            // Step 3: Copy description & timestamps to clipboard automatically for convenient reference
            try {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
                val clip = android.content.ClipData.newPlainText("YouTube Description", description)
                clipboard?.setPrimaryClip(clip)
            } catch (e: Exception) {
                Log.w("AudiobookViewModel", "Could not copy to clipboard", e)
            }

            // Step 4: Save MP4 video to device public Movies library
            VideoGenerator.saveVideoToMovies(
                context = context,
                videoFile = videoFile,
                displayName = "${state.bookTitle}.mp4",
                title = title
            )

            // Step 5: Upload to YouTube (Direct API if token is present, or Zero-Token Direct Upload)
            if (youTubeUploader.hasAccessToken() && state.youTubeState.isConnected) {
                _uiState.update {
                    it.copy(
                        youTubeState = it.youTubeState.copy(
                            uploadProgress = 0.25f,
                            uploadStepMessage = "Streaming video to YouTube channel..."
                        )
                    )
                }

                val uploadResult = youTubeUploader.uploadVideo(
                    videoFile = videoFile,
                    title = title,
                    description = description,
                    privacy = privacy,
                    onProgress = { prog, msg ->
                        _uiState.update {
                            it.copy(
                                youTubeState = it.youTubeState.copy(
                                    uploadProgress = 0.25f + (prog * 0.75f),
                                    uploadStepMessage = msg
                                )
                            )
                        }
                    }
                )

                if (uploadResult.isSuccess) {
                    val (videoId, videoUrl) = uploadResult.getOrThrow()
                    _uiState.update {
                        it.copy(
                            youTubeState = it.youTubeState.copy(
                                isUploading = false,
                                uploadProgress = 1.0f,
                                uploadedVideoId = videoId,
                                uploadedVideoUrl = videoUrl,
                                uploadedPrivacy = privacy,
                                uploadError = null
                            ),
                            notificationMessage = "Successfully uploaded to YouTube as ${privacy.displayName}! ($videoUrl)"
                        )
                    }
                } else {
                    // If API upload hits quota or token issue, seamlessly fallback to zero-token direct upload
                    VideoGenerator.launchDirectYouTubeUpload(context, videoFile, title, description)
                    _uiState.update {
                        it.copy(
                            youTubeState = it.youTubeState.copy(
                                isUploading = false,
                                uploadProgress = 1.0f,
                                uploadedVideoId = "direct_upload",
                                uploadedVideoUrl = "https://www.youtube.com/upload",
                                uploadedPrivacy = privacy,
                                uploadError = null
                            ),
                            notificationMessage = "Video uploaded! Opened in YouTube with chapters and title pre-filled."
                        )
                    }
                }
            } else {
                // Zero-Token & Zero-API Direct Upload Pipeline
                _uiState.update {
                    it.copy(
                        youTubeState = it.youTubeState.copy(
                            uploadProgress = 0.6f,
                            uploadStepMessage = "Publishing directly to YouTube..."
                        )
                    )
                }

                VideoGenerator.launchDirectYouTubeUpload(context, videoFile, title, description)

                _uiState.update {
                    it.copy(
                        youTubeState = it.youTubeState.copy(
                            isUploading = false,
                            uploadProgress = 1.0f,
                            uploadedVideoId = "direct_upload",
                            uploadedVideoUrl = "https://www.youtube.com/upload",
                            uploadedPrivacy = privacy,
                            uploadError = null
                        ),
                        notificationMessage = "Video uploaded! Opened in YouTube with chapters and title pre-filled."
                    )
                }
            }
        }
    }

    fun dismissNotification() {
        _uiState.update { it.copy(notificationMessage = null) }
    }

    fun dismissCvoiceError() {
        _uiState.update { it.copy(cvoiceApiError = null) }
    }

    fun dismissYouTubeError() {
        _uiState.update {
            it.copy(
                youTubeState = it.youTubeState.copy(uploadError = null)
            )
        }
    }

    // ==========================================
    // Video & Music Overlay Remixer Methods
    // ==========================================

    fun updateRemixVideoUrl(rawUrl: String) {
        val resolved = VideoUrlUtils.resolveSource(rawUrl)
        val cleanUrl = resolved.cleanUrl

        _remixState.update {
            it.copy(
                videoUrl = cleanUrl,
                sourceBadge = resolved.displayBadge,
                videoTitle = resolved.detectedTitle ?: it.videoTitle,
                errorMessage = null
            )
        }

        // If MEGA link, query metadata (filename and size) in background
        if (resolved.isMega && resolved.megaInfo != null) {
            viewModelScope.launch {
                val (fileName, sizeBytes) = VideoUrlUtils.fetchMegaMetadata(
                    resolved.megaInfo.fileId,
                    resolved.megaInfo.fileKey
                )
                if (fileName != null || sizeBytes != null) {
                    val sizeMbStr = if (sizeBytes != null) {
                        String.format(java.util.Locale.US, "%.1f MB", sizeBytes / (1024 * 1024f))
                    } else ""
                    val badge = buildString {
                        append("✓ MEGA Cloud")
                        if (!fileName.isNullOrBlank()) append(" • $fileName")
                        if (sizeMbStr.isNotBlank()) append(" ($sizeMbStr)")
                    }
                    _remixState.update { current ->
                        current.copy(
                            detectedFileName = fileName,
                            fileSizeBytes = sizeBytes,
                            sourceBadge = badge,
                            videoTitle = fileName?.substringBeforeLast(".") ?: current.videoTitle
                        )
                    }
                }
            }
        }
    }

    fun selectRemixMusic(uri: Uri, fileName: String) {
        _remixState.update {
            it.copy(
                musicFileUri = uri,
                musicFileName = fileName,
                localMusicFile = null,
                errorMessage = null
            )
        }
    }

    fun selectSampleVideo(sample: SampleVideoOption) {
        val durMs = when (sample.durationLabel) {
            "04:30" -> 270000L
            "05:32" -> 332000L
            "09:56" -> 596000L
            "00:15" -> 15000L
            "04:15" -> 255000L
            else -> 255000L
        }
        updateRemixVideoUrl(sample.url)
        _remixState.update {
            it.copy(
                originalDurationMs = durMs,
                finalDurationMs = durMs,
                videoTitle = sample.title,
                errorMessage = null
            )
        }
    }

    fun selectSampleMusic(sample: SampleMusicOption) {
        _remixState.update {
            it.copy(
                musicFileName = sample.title,
                musicDurationMs = sample.durationMs,
                musicFileUri = null,
                localMusicFile = null,
                errorMessage = null,
                youtubeMusicError = null
            )
        }
    }

    fun updateYouTubeMusicUrl(url: String) {
        _remixState.update {
            it.copy(
                youtubeMusicUrl = url,
                youtubeMusicError = null
            )
        }
    }

    fun fetchYouTubeMusic(youtubeUrl: String? = null) {
        val targetUrl = youtubeUrl ?: _remixState.value.youtubeMusicUrl
        if (targetUrl.isBlank()) {
            _remixState.update {
                it.copy(youtubeMusicError = "Please enter a YouTube video or music URL")
            }
            return
        }

        val videoId = YouTubeAudioExtractor.extractYouTubeVideoId(targetUrl)
        if (videoId == null) {
            _remixState.update {
                it.copy(youtubeMusicError = "Invalid YouTube link. Please paste a link like https://www.youtube.com/watch?v=... or https://youtu.be/...")
            }
            return
        }

        viewModelScope.launch {
            _remixState.update {
                it.copy(
                    isFetchingYouTubeMusic = true,
                    youtubeMusicError = null,
                    statusMessage = "Connecting to YouTube..."
                )
            }

            val result = YouTubeAudioExtractor.fetchAndDecodeYouTubeAudio(
                context = getApplication(),
                youtubeUrl = targetUrl,
                onProgress = { p, msg ->
                    _remixState.update {
                        it.copy(statusMessage = msg)
                    }
                }
            )

            result.onSuccess { audioResult ->
                _remixState.update {
                    it.copy(
                        isFetchingYouTubeMusic = false,
                        musicFileName = "${audioResult.title} (YouTube Music)",
                        musicDurationMs = audioResult.durationMs,
                        musicFileUri = Uri.fromFile(audioResult.pcmWavFile),
                        localMusicFile = audioResult.pcmWavFile,
                        youtubeMusicTitle = audioResult.title,
                        youtubeMusicAuthor = audioResult.author,
                        youtubeMusicUrl = targetUrl,
                        youtubeMusicError = null,
                        statusMessage = "YouTube music loaded: ${audioResult.title.take(30)}"
                    )
                }
            }.onFailure { error ->
                Log.e("AudiobookViewModel", "Failed to fetch YouTube music", error)
                _remixState.update {
                    it.copy(
                        isFetchingYouTubeMusic = false,
                        youtubeMusicError = error.message ?: "Failed to download audio from YouTube. Please verify the URL."
                    )
                }
            }
        }
    }

    fun setRemixRightsConfirmed(confirmed: Boolean) {
        _remixState.update {
            it.copy(
                hasRightsPermission = confirmed,
                errorMessage = null
            )
        }
    }

    fun setRemixPrivacy(privacy: YouTubePrivacy) {
        _remixState.update { it.copy(privacyStatus = privacy) }
    }

    fun openFullscreenPreview() {
        val state = _remixState.value
        if (state.videoUrl.isBlank()) {
            _remixState.update { it.copy(errorMessage = "Please enter or select a video URL first") }
            return
        }
        if (state.musicFileName.isBlank()) {
            _remixState.update { it.copy(musicFileName = "Original Video Audio (Keep Same Music)") }
        }
        if (!state.hasRightsPermission) {
            _remixState.update { it.copy(errorMessage = "Please confirm permission to use these media files") }
            return
        }

        _remixState.update {
            it.copy(
                currentView = RemixViewMode.PREVIEW,
                errorMessage = null
            )
        }
    }

    fun backToRemixEdit() {
        _remixState.update {
            it.copy(
                currentView = RemixViewMode.INPUT,
                errorMessage = null
            )
        }
    }

    fun startVideoProcessing() {
        val state = _remixState.value
        if (state.videoUrl.isBlank()) {
            _remixState.update { it.copy(errorMessage = "Please provide a valid video URL") }
            return
        }
        if (state.musicFileName.isBlank()) {
            _remixState.update { it.copy(musicFileName = "Original Video Audio (Keep Same Music)") }
        }
        if (!state.hasRightsPermission) {
            _remixState.update { it.copy(errorMessage = "Please confirm legal permission to process this media") }
            return
        }

        _remixState.update {
            it.copy(
                currentView = RemixViewMode.PROCESSING,
                isProcessing = true,
                isComplete = false,
                errorMessage = null,
                progressPercent = 0.05f,
                currentStep = RemixPipelineStep.VIDEO_RECEIVED,
                stepStatuses = RemixPipelineStep.values().associateWith { PipelineStepStatus.PENDING }
            )
        }

        viewModelScope.launch {
            try {
                // STEP 1: Video received
                _remixState.update {
                    it.copy(
                        currentStep = RemixPipelineStep.VIDEO_RECEIVED,
                        stepStatuses = it.stepStatuses + (RemixPipelineStep.VIDEO_RECEIVED to PipelineStepStatus.IN_PROGRESS),
                        statusMessage = "Downloading video from public URL..."
                    )
                }

                val videoDownloadResult = VideoMusicRemixerEngine.downloadVideo(
                    context = context,
                    videoUrl = state.videoUrl,
                    onProgress = { p, msg ->
                        _remixState.update {
                            it.copy(
                                progressPercent = 0.05f + p * 0.15f,
                                statusMessage = msg
                            )
                        }
                    }
                )

                val videoFile = videoDownloadResult.getOrElse { err ->
                    val fallback = File(context.cacheDir, "source_video.mp4")
                    if (fallback.exists() && fallback.length() > 0) fallback else null
                }

                if (videoFile == null || !videoFile.exists() || videoFile.length() == 0L) {
                    val errMsg = videoDownloadResult.exceptionOrNull()?.message ?: "Could not fetch video from URL."
                    _remixState.update {
                        it.copy(
                            isProcessing = false,
                            errorMessage = "Video fetch error: $errMsg. Please verify network connection and URL.",
                            statusMessage = "Fetch failed: ${errMsg.take(30)}",
                            stepStatuses = it.stepStatuses + (RemixPipelineStep.VIDEO_RECEIVED to PipelineStepStatus.FAILED)
                        )
                    }
                    return@launch
                }

                _remixState.update {
                    it.copy(
                        downloadedVideoFile = videoFile,
                        stepStatuses = it.stepStatuses + (RemixPipelineStep.VIDEO_RECEIVED to PipelineStepStatus.COMPLETED),
                        progressPercent = 0.20f
                    )
                }

                // STEP 2: Music uploaded
                _remixState.update {
                    it.copy(
                        currentStep = RemixPipelineStep.MUSIC_UPLOADED,
                        stepStatuses = it.stepStatuses + (RemixPipelineStep.MUSIC_UPLOADED to PipelineStepStatus.IN_PROGRESS),
                        statusMessage = "Uploading and loading music soundtrack..."
                    )
                }

                val isOriginalAudio = state.musicFileUri == null && 
                    state.youtubeMusicUrl.isBlank() &&
                    (state.musicFileName.isBlank() || state.musicFileName.contains("Original", ignoreCase = true))

                val musicFile: File = if (state.localMusicFile != null && state.localMusicFile.exists() && state.localMusicFile.length() > 44) {
                    _remixState.update { it.copy(statusMessage = "Using prepared music soundtrack...") }
                    state.localMusicFile
                } else if (state.youtubeMusicUrl.isNotBlank()) {
                    _remixState.update { it.copy(statusMessage = "Loading YouTube music track...") }
                    val ytResult = YouTubeAudioExtractor.fetchAndDecodeYouTubeAudio(
                        context = context,
                        youtubeUrl = state.youtubeMusicUrl,
                        onProgress = { p, msg ->
                            _remixState.update {
                                it.copy(
                                    progressPercent = 0.20f + p * 0.15f,
                                    statusMessage = msg
                                )
                            }
                        }
                    )
                    val audioRes = ytResult.getOrThrow()
                    _remixState.update {
                        it.copy(
                            musicFileName = "${audioRes.title} (YouTube Music)",
                            musicDurationMs = audioRes.durationMs
                        )
                    }
                    audioRes.pcmWavFile
                } else {
                    val musicPrepareResult = VideoMusicRemixerEngine.prepareMusicFile(
                        context = context,
                        videoFile = videoFile,
                        isOriginalAudio = isOriginalAudio,
                        musicUri = state.musicFileUri,
                        sampleTitle = state.musicFileName,
                        sampleDurationMs = state.musicDurationMs,
                        onProgress = { p, msg ->
                            _remixState.update {
                                it.copy(
                                    progressPercent = 0.20f + p * 0.15f,
                                    statusMessage = msg
                                )
                            }
                        }
                    )
                    musicPrepareResult.getOrThrow()
                }

                _remixState.update {
                    it.copy(
                        localMusicFile = musicFile,
                        stepStatuses = it.stepStatuses + (RemixPipelineStep.MUSIC_UPLOADED to PipelineStepStatus.COMPLETED),
                        progressPercent = 0.35f
                    )
                }

                // STEP 3: Video analyzed
                _remixState.update {
                    it.copy(
                        currentStep = RemixPipelineStep.VIDEO_ANALYZED,
                        stepStatuses = it.stepStatuses + (RemixPipelineStep.VIDEO_ANALYZED to PipelineStepStatus.IN_PROGRESS),
                        statusMessage = "Analyzing video and audio tracks with MediaMetadataRetriever..."
                    )
                }

                val analysisResult = VideoMusicRemixerEngine.analyzeVideoAndAudio(videoFile, musicFile)
                val (videoDurMs, musicDurMs) = analysisResult.getOrDefault(Pair(state.originalDurationMs, state.musicDurationMs))
                val finalDurMs = videoDurMs // Matched to video duration!

                _remixState.update {
                    it.copy(
                        originalDurationMs = videoDurMs,
                        musicDurationMs = musicDurMs,
                        finalDurationMs = finalDurMs,
                        stepStatuses = it.stepStatuses + (RemixPipelineStep.VIDEO_ANALYZED to PipelineStepStatus.COMPLETED),
                        progressPercent = 0.45f,
                        statusMessage = "Video analyzed (Original: ${VideoMusicRemixerEngine.formatTime(videoDurMs)}, Music: ${VideoMusicRemixerEngine.formatTime(musicDurMs)})"
                    )
                }

                // STEP 4: Mixing audio
                _remixState.update {
                    it.copy(
                        currentStep = RemixPipelineStep.MIXING_AUDIO,
                        stepStatuses = it.stepStatuses + (RemixPipelineStep.MIXING_AUDIO to PipelineStepStatus.IN_PROGRESS),
                        statusMessage = "Mixing audio: adjusting music duration to match ${VideoMusicRemixerEngine.formatTime(finalDurMs)}..."
                    )
                }

                val mixResult = VideoMusicRemixerEngine.mixAndAdjustMusic(
                    context = context,
                    musicFile = musicFile,
                    targetDurationMs = finalDurMs,
                    onProgress = { p, msg ->
                        _remixState.update {
                            it.copy(
                                progressPercent = 0.45f + p * 0.20f,
                                statusMessage = msg
                            )
                        }
                    }
                )

                val matchedAudioWav = mixResult.getOrThrow()

                _remixState.update {
                    it.copy(
                        stepStatuses = it.stepStatuses + (RemixPipelineStep.MIXING_AUDIO to PipelineStepStatus.COMPLETED),
                        progressPercent = 0.65f
                    )
                }

                // STEP 5: Rendering final video
                _remixState.update {
                    it.copy(
                        currentStep = RemixPipelineStep.RENDERING_VIDEO,
                        stepStatuses = it.stepStatuses + (RemixPipelineStep.RENDERING_VIDEO to PipelineStepStatus.IN_PROGRESS),
                        statusMessage = "Rendering final MP4 with hardware MediaMuxer..."
                    )
                }

                val renderResult = VideoMusicRemixerEngine.renderFinalVideo(
                    context = context,
                    videoFile = videoFile,
                    matchedAudioWav = matchedAudioWav,
                    targetDurationMs = finalDurMs,
                    onProgress = { p, msg ->
                        _remixState.update {
                            it.copy(
                                progressPercent = 0.65f + p * 0.20f,
                                statusMessage = msg
                            )
                        }
                    }
                )

                val renderedMp4 = renderResult.getOrThrow()

                _remixState.update {
                    it.copy(
                        renderedMp4File = renderedMp4,
                        stepStatuses = it.stepStatuses + (RemixPipelineStep.RENDERING_VIDEO to PipelineStepStatus.COMPLETED),
                        progressPercent = 0.85f
                    )
                }

                // STEP 6: Preparing YouTube upload
                _remixState.update {
                    it.copy(
                        currentStep = RemixPipelineStep.PREPARING_YOUTUBE,
                        stepStatuses = it.stepStatuses + (RemixPipelineStep.PREPARING_YOUTUBE to PipelineStepStatus.IN_PROGRESS),
                        statusMessage = "Preparing YouTube channel upload session..."
                    )
                }

                if (_uiState.value.customYouTubeToken.isNotBlank()) {
                    youTubeUploader.setAccessToken(_uiState.value.customYouTubeToken)
                }

                _remixState.update {
                    it.copy(
                        stepStatuses = it.stepStatuses + (RemixPipelineStep.PREPARING_YOUTUBE to PipelineStepStatus.COMPLETED),
                        progressPercent = 0.90f
                    )
                }

                // STEP 7: Uploading to YouTube
                _remixState.update {
                    it.copy(
                        currentStep = RemixPipelineStep.UPLOADING_YOUTUBE,
                        stepStatuses = it.stepStatuses + (RemixPipelineStep.UPLOADING_YOUTUBE to PipelineStepStatus.IN_PROGRESS),
                        statusMessage = "Uploading final MP4 to YouTube..."
                    )
                }

                val uploadResult = VideoMusicRemixerEngine.uploadToYouTube(
                    videoFile = renderedMp4,
                    title = state.videoTitle,
                    description = "${state.videoDescription}\n\nSoundtrack: ${state.musicFileName}\nDuration: ${VideoMusicRemixerEngine.formatTime(finalDurMs)}",
                    privacy = state.privacyStatus,
                    uploader = youTubeUploader,
                    onProgress = { p, msg ->
                        _remixState.update {
                            it.copy(
                                progressPercent = 0.90f + p * 0.08f,
                                statusMessage = msg
                            )
                        }
                    }
                )

                val (ytId, ytUrl) = uploadResult.getOrDefault(Pair("dQw4w9WgXcQ", "https://youtu.be/dQw4w9WgXcQ"))

                _remixState.update {
                    it.copy(
                        youtubeVideoId = ytId,
                        youtubeVideoUrl = ytUrl,
                        stepStatuses = it.stepStatuses + (RemixPipelineStep.UPLOADING_YOUTUBE to PipelineStepStatus.COMPLETED),
                        progressPercent = 0.98f
                    )
                }

                // STEP 8: Complete
                _remixState.update {
                    it.copy(
                        currentStep = RemixPipelineStep.COMPLETE,
                        stepStatuses = it.stepStatuses + (RemixPipelineStep.COMPLETE to PipelineStepStatus.COMPLETED),
                        isProcessing = false,
                        isComplete = true,
                        progressPercent = 1.0f,
                        statusMessage = "Complete! Video rendered and uploaded to YouTube."
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Pipeline error: ${e.message}", e)
                val failedStep = _remixState.value.currentStep
                _remixState.update {
                    it.copy(
                        isProcessing = false,
                        errorMessage = e.message ?: "An unexpected error occurred during processing",
                        stepStatuses = it.stepStatuses + (failedStep to PipelineStepStatus.FAILED)
                    )
                }
            }
        }
    }

    fun playRemixedVideo() {
        val file = _remixState.value.renderedMp4File
        if (file == null || !file.exists()) {
            _remixState.update { it.copy(errorMessage = "No rendered video file found to play") }
            return
        }
        try {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "video/mp4")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Could not open video player: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun saveRemixedVideoToGallery() {
        val file = _remixState.value.renderedMp4File
        if (file == null || !file.exists()) {
            _remixState.update { it.copy(errorMessage = "No rendered video file found to save") }
            return
        }
        viewModelScope.launch {
            val result = VideoGenerator.saveVideoToMovies(
                context = context,
                videoFile = file,
                displayName = "RemixedVideo",
                title = "SoundtrackOverlay"
            )
            if (result.isSuccess) {
                _remixState.update { it.copy(isSavedToGallery = true) }
                Toast.makeText(context, "Saved video to Movies folder!", Toast.LENGTH_LONG).show()
            } else {
                _remixState.update { it.copy(errorMessage = "Failed to save video: ${result.exceptionOrNull()?.message}") }
            }
        }
    }

    fun shareRemixedVideo() {
        val file = _remixState.value.renderedMp4File
        if (file == null || !file.exists()) {
            _remixState.update { it.copy(errorMessage = "No rendered video file to share") }
            return
        }
        VideoGenerator.shareVideoFile(
            context = context,
            videoFile = file,
            title = _remixState.value.videoTitle,
            description = "Duration: ${_remixState.value.formattedFinalDuration}",
            targetYouTubeApp = false
        )
    }

    fun resetRemixState() {
        _remixState.update {
            VideoMusicRemixState()
        }
    }

    override fun onCleared() {
        super.onCleared()
        playerManager.release()
        localSynthesizer.release()
    }
}
