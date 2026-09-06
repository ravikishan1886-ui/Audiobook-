package com.example.data.api

import android.content.Context
import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import com.example.data.model.AudioEngineType
import com.example.data.model.VoiceProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.TimeUnit

class CVoiceApiException(message: String, val statusCode: Int? = null) : Exception(message)

class CVoiceClient(
    private val context: Context,
    private var customApiKey: String? = null,
    private var customApiBaseUrl: String? = null
) {
    private val TAG = "CVoiceClient"
    private val client = OkHttpClient.Builder()
        .connectTimeout(45, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(45, TimeUnit.SECONDS)
        .build()

    companion object {
        const val CVOICE_TTS_ENDPOINT = "https://cvoice.ai/api/tts"
        const val OPENAI_TTS_ENDPOINT = "https://api.openai.com/v1/audio/speech"

        fun normalizeEndpoint(url: String?, apiKey: String? = null): String {
            val trimmed = url?.trim() ?: ""
            val key = apiKey?.trim() ?: ""
            if (key.startsWith("sk-") && (trimmed.isBlank() || trimmed.contains("cvoice.ai"))) {
                return OPENAI_TTS_ENDPOINT
            }
            if (trimmed.isBlank() || trimmed.contains("api.cvoice.ai")) {
                return CVOICE_TTS_ENDPOINT
            }
            return trimmed
        }
    }

    fun getEffectiveApiKey(): String {
        if (!customApiKey.isNullOrBlank()) {
            return customApiKey!!.trim()
        }
        // First check BuildConfig OPENAI_API_KEY
        try {
            val openAiBuild = BuildConfig.OPENAI_API_KEY
            if (!openAiBuild.isNullOrBlank() && openAiBuild != "MY_OPENAI_API_KEY") {
                return openAiBuild.trim()
            }
        } catch (_: Exception) {}

        // Then check CVOICE_API_KEY
        return try {
            val buildKey = BuildConfig.CVOICE_API_KEY
            if (buildKey.isNullOrBlank() || buildKey == "MY_CVOICE_API_KEY") "" else buildKey.trim()
        } catch (e: Exception) {
            ""
        }
    }

    fun setApiKey(key: String) {
        customApiKey = key.trim()
    }

    fun setBaseUrl(url: String) {
        customApiBaseUrl = url.trim()
    }

    fun getEffectiveBaseUrl(): String {
        return normalizeEndpoint(customApiBaseUrl, getEffectiveApiKey())
    }

    private fun isUsingOpenAi(key: String, url: String): Boolean {
        return key.startsWith("sk-") ||
                url.contains("api.openai.com", ignoreCase = true) ||
                url.contains("openai", ignoreCase = true)
    }

    suspend fun testConnection(apiKey: String, baseUrl: String): Result<String> = withContext(Dispatchers.IO) {
        val key = apiKey.trim().ifBlank { getEffectiveApiKey() }
        if (key.isBlank() || key == "MY_CVOICE_API_KEY" || key == "MY_OPENAI_API_KEY") {
            return@withContext Result.failure(
                CVoiceApiException("API key is not configured. Please enter your API key to test.")
            )
        }

        val targetUrl = normalizeEndpoint(baseUrl, key)
        val isOpenAi = isUsingOpenAi(key, targetUrl)

        try {
            val testJson = if (isOpenAi) {
                JSONObject().apply {
                    put("model", "tts-1")
                    put("voice", "alloy")
                    put("input", "Voice synthesis engine connection test.")
                    put("response_format", "wav")
                }
            } else {
                JSONObject().apply {
                    put("voice_id", "en_narrator_01")
                    put("text", "Audiobook engine connection test.")
                    put("audio_format", "wav")
                }
            }
            val requestBody = testJson.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url(targetUrl)
                .addHeader("Authorization", "Bearer $key")
                .addHeader("X-API-Key", key)
                .addHeader("x-api-key", key)
                .addHeader("Content-Type", "application/json")
                .addHeader("User-Agent", "AudiobookStudio/1.0")
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            val engineName = if (isOpenAi) "OpenAI Neural TTS" else "cvoice.ai Voice Engine"
            if (response.isSuccessful) {
                val contentType = response.body?.contentType()?.toString() ?: ""
                Result.success("$engineName connection verified! (HTTP ${response.code} $contentType)")
            } else {
                val errorBody = response.body?.string()?.take(250) ?: ""
                Result.failure(CVoiceApiException("$engineName returned HTTP ${response.code}: $errorBody"))
            }
        } catch (e: Exception) {
            val msg = e.localizedMessage ?: e.message ?: "Network error"
            Log.w(TAG, "Voice engine testConnection warning: $msg")
            Result.failure(CVoiceApiException("Connection notice: $msg"))
        }
    }

    suspend fun generateSpeech(
        chapterId: String,
        text: String,
        voice: VoiceProfile,
        speedMultiplier: Float = 1.0f
    ): Result<Pair<File, AudioEngineType>> = withContext(Dispatchers.IO) {
        val apiKey = getEffectiveApiKey()
        val rawBaseUrl = getEffectiveBaseUrl()
        val baseUrl = normalizeEndpoint(rawBaseUrl, apiKey)
        val isOpenAi = isUsingOpenAi(apiKey, baseUrl)

        if (apiKey.isBlank() || apiKey == "MY_CVOICE_API_KEY" || apiKey == "MY_OPENAI_API_KEY") {
            val noticeMsg = "Voice engine API key not configured. Using high-fidelity Studio Voice Engine."
            Log.d(TAG, noticeMsg)
            return@withContext Result.failure(CVoiceApiException(noticeMsg, 401))
        }

        try {
            val outputDir = File(context.cacheDir, "audiobook_chapters")
            if (!outputDir.exists()) outputDir.mkdirs()
            val outputFile = File(outputDir, "chap_${chapterId}.wav")

            if (isOpenAi) {
                generateSpeechWithOpenAi(
                    apiKey = apiKey,
                    endpointUrl = baseUrl,
                    text = text,
                    voice = voice,
                    speedMultiplier = speedMultiplier,
                    outputFile = outputFile
                )
            } else {
                generateSpeechWithCVoice(
                    apiKey = apiKey,
                    endpointUrl = baseUrl,
                    text = text,
                    voice = voice,
                    speedMultiplier = speedMultiplier,
                    outputFile = outputFile
                )
            }
        } catch (e: Exception) {
            val engineName = if (isOpenAi) "OpenAI TTS" else "cvoice.ai"
            val noticeMsg = "$engineName unavailable (${e.localizedMessage ?: e.message})."
            Log.w(TAG, noticeMsg)
            Result.failure(CVoiceApiException(noticeMsg))
        }
    }

    private fun generateSpeechWithOpenAi(
        apiKey: String,
        endpointUrl: String,
        text: String,
        voice: VoiceProfile,
        speedMultiplier: Float,
        outputFile: File
    ): Result<Pair<File, AudioEngineType>> {
        // Map voice profile to OpenAI voice: alloy, echo, fable, onyx, nova, shimmer
        val openAiVoiceName = when (voice.id) {
            "ballad_nyc_cabbie" -> "echo"
            "cvoice_oliver_uk" -> "onyx"
            "cvoice_clara_us" -> "nova"
            "cvoice_marcus_deep" -> "onyx"
            "cvoice_evelyn_soft" -> "fable"
            "cvoice_arthur_vintage" -> "echo"
            "cvoice_aria_crisp" -> "alloy"
            else -> when (voice.openAiVoice?.lowercase()) {
                "alloy", "echo", "fable", "onyx", "nova", "shimmer", "ash", "coral", "sage" -> voice.openAiVoice.lowercase()
                else -> if (voice.gender.equals("Female", ignoreCase = true)) "nova" else "onyx"
            }
        }

        val speed = (voice.speed * speedMultiplier).coerceIn(0.25f, 4.0f)
        val chunks = splitTextForTts(text, maxChars = 3500)
        Log.i(TAG, "Synthesizing with OpenAI ($openAiVoiceName, ${chunks.size} chunk(s)): endpoint=$endpointUrl")

        val audioByteChunks = mutableListOf<ByteArray>()

        for ((index, chunk) in chunks.withIndex()) {
            val requestJson = JSONObject().apply {
                put("model", "tts-1")
                put("voice", openAiVoiceName)
                put("input", chunk)
                put("response_format", "wav")
                put("speed", speed)
            }

            val requestBody = requestJson.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url(endpointUrl)
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .addHeader("User-Agent", "AudiobookStudio/1.0")
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                val errorBody = response.body?.string()?.take(250) ?: ""
                val errorMsg = "OpenAI TTS returned HTTP ${response.code}: $errorBody"
                Log.w(TAG, errorMsg)
                return Result.failure(CVoiceApiException(errorMsg, response.code))
            }

            val rawBytes = response.body?.bytes()
                ?: return Result.failure(CVoiceApiException("Empty response from OpenAI TTS", response.code))

            if (rawBytes.size < 44) {
                return Result.failure(CVoiceApiException("Incomplete WAV audio from OpenAI", response.code))
            }

            audioByteChunks.add(rawBytes)
        }

        if (audioByteChunks.isEmpty()) {
            return Result.failure(CVoiceApiException("No audio received from OpenAI"))
        }

        // Combine WAV audio chunks
        combineWavByteChunks(audioByteChunks, outputFile)
        return Result.success(Pair(outputFile, AudioEngineType.OPENAI_TTS))
    }

    private fun generateSpeechWithCVoice(
        apiKey: String,
        endpointUrl: String,
        text: String,
        voice: VoiceProfile,
        speedMultiplier: Float,
        outputFile: File
    ): Result<Pair<File, AudioEngineType>> {
        val requestJson = JSONObject().apply {
            put("voice_id", voice.id)
            put("voice_name", voice.name)
            put("text", text)
            put("speed", (voice.speed * speedMultiplier).coerceIn(0.5f, 2.0f))
            put("pitch", voice.pitch)
            put("audio_format", "wav")
            if (!voice.instructions.isNullOrBlank()) {
                put("instructions", voice.instructions)
            }
        }

        val requestBody = requestJson.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url(endpointUrl)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("X-API-Key", apiKey)
            .addHeader("x-api-key", apiKey)
            .addHeader("Content-Type", "application/json")
            .addHeader("User-Agent", "AudiobookStudio/1.0")
            .post(requestBody)
            .build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) {
            val errorBody = response.body?.string()?.take(250) ?: ""
            val errorMsg = "cvoice.ai returned HTTP ${response.code}: $errorBody"
            Log.w(TAG, errorMsg)
            return Result.failure(CVoiceApiException(errorMsg, response.code))
        }

        val responseBody = response.body
            ?: return Result.failure(CVoiceApiException("Empty response from cvoice.ai", response.code))

        val contentType = responseBody.contentType()?.toString() ?: ""
        val rawBytes = responseBody.bytes()

        val audioBytes: ByteArray = if (contentType.contains("json") || (rawBytes.isNotEmpty() && rawBytes[0] == '{'.code.toByte())) {
            val json = try { JSONObject(String(rawBytes)) } catch (e: Exception) { null }
            val audioUrl = json?.let {
                it.optString("audio_url").ifBlank {
                    it.optString("url").ifBlank {
                        it.optString("output_file_url").ifBlank {
                            it.optString("download_url")
                        }
                    }
                }
            }
            val base64Data = json?.let {
                it.optString("audio_base64").ifBlank {
                    it.optString("audio").ifBlank {
                        it.optString("base64")
                    }
                }
            }

            if (!audioUrl.isNullOrBlank()) {
                val downloadReq = Request.Builder().url(audioUrl).build()
                val downloadResp = client.newCall(downloadReq).execute()
                downloadResp.body?.bytes() ?: rawBytes
            } else if (!base64Data.isNullOrBlank()) {
                Base64.decode(base64Data, Base64.DEFAULT)
            } else {
                rawBytes
            }
        } else {
            rawBytes
        }

        if (audioBytes.isEmpty()) {
            return Result.failure(CVoiceApiException("cvoice.ai returned 0 audio bytes", response.code))
        }

        FileOutputStream(outputFile).use { it.write(audioBytes) }
        return Result.success(Pair(outputFile, AudioEngineType.CVOICE_AI))
    }

    /**
     * Splits long text into chunks under maxChars without cutting words or sentences.
     */
    private fun splitTextForTts(text: String, maxChars: Int = 3500): List<String> {
        if (text.length <= maxChars) return listOf(text)

        val chunks = mutableListOf<String>()
        val paragraphs = text.split("\n\n")
        var currentChunk = StringBuilder()

        for (para in paragraphs) {
            if (currentChunk.length + para.length + 2 <= maxChars) {
                if (currentChunk.isNotEmpty()) currentChunk.append("\n\n")
                currentChunk.append(para)
            } else {
                if (currentChunk.isNotEmpty()) {
                    chunks.add(currentChunk.toString().trim())
                    currentChunk = StringBuilder()
                }

                if (para.length <= maxChars) {
                    currentChunk.append(para)
                } else {
                    // Split sentences within long paragraph
                    val sentences = para.split(Regex("(?<=[.!?])\\s+"))
                    for (sentence in sentences) {
                        if (currentChunk.length + sentence.length + 1 <= maxChars) {
                            if (currentChunk.isNotEmpty()) currentChunk.append(" ")
                            currentChunk.append(sentence)
                        } else {
                            if (currentChunk.isNotEmpty()) {
                                chunks.add(currentChunk.toString().trim())
                                currentChunk = StringBuilder()
                            }
                            if (sentence.length <= maxChars) {
                                currentChunk.append(sentence)
                            } else {
                                // Hard word split
                                val words = sentence.split(" ")
                                for (word in words) {
                                    if (currentChunk.length + word.length + 1 <= maxChars) {
                                        if (currentChunk.isNotEmpty()) currentChunk.append(" ")
                                        currentChunk.append(word)
                                    } else {
                                        chunks.add(currentChunk.toString().trim())
                                        currentChunk = StringBuilder(word)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (currentChunk.isNotEmpty()) {
            chunks.add(currentChunk.toString().trim())
        }

        return chunks.filter { it.isNotBlank() }
    }

    /**
     * Merges multiple WAV byte arrays into a single valid WAV file by keeping the first header
     * and writing PCM payloads, then fixing the total data length in the header.
     */
    private fun combineWavByteChunks(chunks: List<ByteArray>, outputFile: File) {
        if (chunks.size == 1) {
            FileOutputStream(outputFile).use { it.write(chunks[0]) }
            return
        }

        var totalPcmBytes = 0L
        FileOutputStream(outputFile).use { out ->
            // Write first chunk completely (header + pcm)
            out.write(chunks[0])
            totalPcmBytes += (chunks[0].size - 44).coerceAtLeast(0)

            // Write subsequent chunks without 44-byte header
            for (i in 1 until chunks.size) {
                val chunk = chunks[i]
                if (chunk.size > 44) {
                    val pcmBytes = chunk.size - 44
                    out.write(chunk, 44, pcmBytes)
                    totalPcmBytes += pcmBytes
                }
            }
        }

        // Update total data length in header of outputFile
        try {
            RandomAccessFile(outputFile, "rw").use { raf ->
                // Total file length - 8 at position 4
                raf.seek(4)
                val totalDataLen = (totalPcmBytes + 36).toInt()
                raf.write(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(totalDataLen).array())

                // Subchunk2Size at position 40
                raf.seek(40)
                raf.write(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(totalPcmBytes.toInt()).array())
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not update WAV header sizes: ${e.message}")
        }
    }
}
