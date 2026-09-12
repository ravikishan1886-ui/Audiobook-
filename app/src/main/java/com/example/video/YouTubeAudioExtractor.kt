package com.example.video

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit

data class YouTubeVideoInfo(
    val videoId: String,
    val title: String,
    val author: String,
    val durationSeconds: Long,
    val streamUrl: String?,
    val mimeType: String?,
    val itag: Int
)

data class YouTubeAudioResult(
    val videoId: String,
    val title: String,
    val author: String,
    val durationMs: Long,
    val pcmWavFile: File
)

object YouTubeAudioExtractor {
    private const val TAG = "YouTubeAudioExtractor"

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    /**
     * Extracts YouTube Video ID from any standard YouTube URL format:
     * - https://www.youtube.com/watch?v=VIDEO_ID
     * - https://youtu.be/VIDEO_ID
     * - https://music.youtube.com/watch?v=VIDEO_ID
     * - https://www.youtube.com/shorts/VIDEO_ID
     * - https://m.youtube.com/watch?v=VIDEO_ID
     * - Google redirects pointing to YouTube
     */
    fun extractYouTubeVideoId(rawUrl: String): String? {
        val clean = VideoUrlUtils.unwrapAndCleanUrl(rawUrl).trim()
        val patterns = listOf(
            """(?:youtube\.com\/(?:[^\/]+\/.+\/|(?:v|e(?:mbed)?)\/|.*[?&]v=)|youtu\.be\/|youtube\.com\/shorts\/)([a-zA-Z0-9_-]{11})""".toRegex(RegexOption.IGNORE_CASE),
            """music\.youtube\.com\/watch\?.*v=([a-zA-Z0-9_-]{11})""".toRegex(RegexOption.IGNORE_CASE)
        )
        for (p in patterns) {
            val m = p.find(clean)
            if (m != null) return m.groupValues[1]
        }
        return null
    }

    /**
     * Resolves metadata and direct audio stream URL from YouTube.
     * Uses Android VR / Embedded player client which serves direct unthrottled streaming URLs.
     */
    suspend fun resolveYouTubeAudio(videoId: String): Result<YouTubeVideoInfo> = withContext(Dispatchers.IO) {
        try {
            // Tier 1: YouTube Innertube ANDROID_VR Client
            val innertubeUrl = "https://www.youtube.com/youtubei/v1/player"
            val payload = JSONObject().apply {
                put("videoId", videoId)
                put("context", JSONObject().apply {
                    put("client", JSONObject().apply {
                        put("clientName", "ANDROID_VR")
                        put("clientVersion", "1.56.21")
                        put("hl", "en")
                        put("gl", "US")
                    })
                })
            }

            val request = Request.Builder()
                .url(innertubeUrl)
                .addHeader("Content-Type", "application/json")
                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .post(payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
                .build()

            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val jsonStr = response.body?.string() ?: ""
                val root = JSONObject(jsonStr)

                val playability = root.optJSONObject("playabilityStatus")
                val status = playability?.optString("status") ?: ""

                val videoDetails = root.optJSONObject("videoDetails")
                val title = videoDetails?.optString("title")?.takeIf { it.isNotBlank() } ?: "YouTube Music"
                val author = videoDetails?.optString("author") ?: "YouTube"
                val durationSec = videoDetails?.optLong("lengthSeconds", 0L) ?: 0L

                val streamingData = root.optJSONObject("streamingData")
                if (streamingData != null) {
                    val adaptiveFormats = streamingData.optJSONArray("adaptiveFormats")
                    if (adaptiveFormats != null) {
                        var bestAudioUrl: String? = null
                        var bestMime: String? = null
                        var bestItag = 0
                        var bestBitrate = 0

                        for (i in 0 until adaptiveFormats.length()) {
                            val fmt = adaptiveFormats.getJSONObject(i)
                            val mime = fmt.optString("mimeType", "")
                            if (mime.startsWith("audio/")) {
                                val url = fmt.optString("url")
                                val bitrate = fmt.optInt("bitrate", 0)
                                val itag = fmt.optInt("itag", 0)

                                if (url.isNotBlank()) {
                                    // Prefer AAC itag 140 or highest bitrate audio
                                    val isAac = mime.contains("audio/mp4")
                                    if (bestAudioUrl == null || (isAac && bestItag != 140) || bitrate > bestBitrate) {
                                        bestAudioUrl = url
                                        bestMime = mime
                                        bestItag = itag
                                        bestBitrate = bitrate
                                    }
                                }
                            }
                        }

                        if (bestAudioUrl != null) {
                            return@withContext Result.success(
                                YouTubeVideoInfo(
                                    videoId = videoId,
                                    title = title,
                                    author = author,
                                    durationSeconds = durationSec,
                                    streamUrl = bestAudioUrl,
                                    mimeType = bestMime,
                                    itag = bestItag
                                )
                            )
                        }
                    }
                }

                if (status.equals("UNPLAYABLE", ignoreCase = true) || status.equals("ERROR", ignoreCase = true)) {
                    val reason = playability?.optString("reason") ?: "Video is not playable"
                    Log.w(TAG, "Innertube returned unplayable: $reason")
                }
            }

            // Tier 2: Fallback to public Invidious instances
            val invidiousInstances = listOf(
                "https://inv.nadeko.net/api/v1/videos/$videoId",
                "https://invidious.privacydev.net/api/v1/videos/$videoId",
                "https://vid.puffyan.us/api/v1/videos/$videoId"
            )

            for (invUrl in invidiousInstances) {
                try {
                    val invReq = Request.Builder()
                        .url(invUrl)
                        .addHeader("User-Agent", "Mozilla/5.0")
                        .build()
                    val invRes = httpClient.newCall(invReq).execute()
                    if (invRes.isSuccessful) {
                        val body = invRes.body?.string() ?: ""
                        val invJson = JSONObject(body)
                        val invTitle = invJson.optString("title", "YouTube Music")
                        val invAuthor = invJson.optString("author", "YouTube")
                        val invDuration = invJson.optLong("lengthSeconds", 0L)
                        val adaptive = invJson.optJSONArray("adaptiveFormats")
                        if (adaptive != null) {
                            for (j in 0 until adaptive.length()) {
                                val f = adaptive.getJSONObject(j)
                                val mime = f.optString("type", "")
                                val directUrl = f.optString("url")
                                if (mime.startsWith("audio/") && directUrl.isNotBlank()) {
                                    return@withContext Result.success(
                                        YouTubeVideoInfo(
                                            videoId = videoId,
                                            title = invTitle,
                                            author = invAuthor,
                                            durationSeconds = invDuration,
                                            streamUrl = directUrl,
                                            mimeType = mime,
                                            itag = f.optInt("itag", 140)
                                        )
                                    )
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Invidious fallback failed for $invUrl: ${e.message}")
                }
            }

            Result.failure(IOException("Could not resolve audio stream for YouTube video $videoId. Please ensure the video is public and available."))
        } catch (e: Exception) {
            Log.e(TAG, "Error resolving YouTube audio: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Downloads YouTube audio stream and decodes it to a pristine PCM WAV file.
     */
    suspend fun fetchAndDecodeYouTubeAudio(
        context: Context,
        youtubeUrl: String,
        onProgress: (Float, String) -> Unit
    ): Result<YouTubeAudioResult> = withContext(Dispatchers.IO) {
        try {
            val videoId = extractYouTubeVideoId(youtubeUrl)
                ?: return@withContext Result.failure(IllegalArgumentException("Invalid YouTube URL. Please enter a valid YouTube or YouTube Music link."))

            onProgress(0.1f, "Connecting to YouTube...")

            val infoResult = resolveYouTubeAudio(videoId)
            val info = infoResult.getOrThrow()

            val streamUrl = info.streamUrl
                ?: return@withContext Result.failure(IOException("Audio stream URL not found for '${info.title}'"))

            onProgress(0.25f, "Downloading YouTube audio: ${info.title.take(30)}...")

            val remixDir = File(context.cacheDir, "video_remix").apply { if (!exists()) mkdirs() }
            val extension = if (info.mimeType?.contains("webm") == true) "webm" else "m4a"
            val rawAudioFile = File(remixDir, "yt_raw_${videoId}_${System.currentTimeMillis()}.$extension")

            val dlRequest = Request.Builder()
                .url(streamUrl)
                .addHeader("User-Agent", "Mozilla/5.0")
                .build()

            val dlResponse = httpClient.newCall(dlRequest).execute()
            if (!dlResponse.isSuccessful) {
                return@withContext Result.failure(IOException("Failed downloading YouTube audio stream: HTTP ${dlResponse.code}"))
            }

            val body = dlResponse.body
                ?: return@withContext Result.failure(IOException("Empty audio response body from YouTube"))

            val contentLength = body.contentLength()
            var downloadedBytes = 0L

            body.byteStream().use { input ->
                FileOutputStream(rawAudioFile).use { output ->
                    val buffer = ByteArray(8192)
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        output.write(buffer, 0, read)
                        downloadedBytes += read
                        if (contentLength > 0) {
                            val dlFraction = (downloadedBytes.toFloat() / contentLength.toFloat()).coerceIn(0f, 1f)
                            onProgress(0.25f + dlFraction * 0.45f, "Downloading music: ${(dlFraction * 100).toInt()}%")
                        }
                    }
                    output.flush()
                }
            }

            onProgress(0.75f, "Decoding audio track to PCM WAV...")

            // Decode downloaded audio directly into PCM WAV via hardware MediaCodec
            val pcmWav = File(remixDir, "yt_audio_${videoId}_${System.currentTimeMillis()}.wav")
            val decoded = VideoMusicRemixerEngine.decodeAudioToPcmWav(rawAudioFile, pcmWav)

            try { rawAudioFile.delete() } catch (_: Exception) {}

            if (!decoded || !pcmWav.exists() || pcmWav.length() <= 44) {
                return@withContext Result.failure(IOException("Failed to decode YouTube audio stream into PCM WAV"))
            }

            onProgress(1.0f, "YouTube audio loaded: ${info.title.take(28)}")

            Result.success(
                YouTubeAudioResult(
                    videoId = videoId,
                    title = info.title,
                    author = info.author,
                    durationMs = (info.durationSeconds * 1000L).coerceAtLeast(1000L),
                    pcmWavFile = pcmWav
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch and decode YouTube audio", e)
            Result.failure(e)
        }
    }
}
