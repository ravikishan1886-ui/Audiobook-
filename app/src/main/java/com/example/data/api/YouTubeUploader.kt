package com.example.data.api

import android.content.Context
import android.util.Log
import com.example.data.model.Chapter
import com.example.data.model.YouTubePrivacy
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.IOException
import java.util.Locale
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class YouTubeUploadSnippet(
    val title: String,
    val description: String,
    val tags: List<String> = listOf("audiobook", "literature", "audio", "full audiobook", "ai narration"),
    val categoryId: String = "27" // Education
)

@JsonClass(generateAdapter = true)
data class YouTubeUploadStatus(
    val privacyStatus: String = "private",
    val selfDeclaredMadeForKids: Boolean = false
)

@JsonClass(generateAdapter = true)
data class YouTubeUploadMetadata(
    val snippet: YouTubeUploadSnippet,
    val status: YouTubeUploadStatus
)

@JsonClass(generateAdapter = true)
data class YouTubeUploadResponse(
    val id: String?,
    val snippet: YouTubeSnippetResponse?
)

@JsonClass(generateAdapter = true)
data class YouTubeSnippetResponse(
    val title: String?,
    val description: String?
)

@JsonClass(generateAdapter = true)
data class YouTubeChannelItem(
    val id: String?,
    val snippet: YouTubeChannelSnippet?
)

@JsonClass(generateAdapter = true)
data class YouTubeChannelSnippet(
    val title: String?,
    val description: String?,
    val customUrl: String?
)

@JsonClass(generateAdapter = true)
data class YouTubeChannelListResponse(
    val items: List<YouTubeChannelItem>?
)

class YouTubeUploader(private val context: Context) {
    private val TAG = "YouTubeUploader"

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val metadataAdapter = moshi.adapter(YouTubeUploadMetadata::class.java)
    private val responseAdapter = moshi.adapter(YouTubeUploadResponse::class.java)
    private val channelListAdapter = moshi.adapter(YouTubeChannelListResponse::class.java)

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(180, TimeUnit.SECONDS)
        .writeTimeout(180, TimeUnit.SECONDS)
        .build()

    // Access token passed dynamically from Google Sign-In / OAuth
    private var accessToken: String? = null

    companion object {
        fun sanitizeToken(raw: String?): String? {
            if (raw.isNullOrBlank()) return null
            var cleaned = raw.trim()
            if (cleaned.startsWith("Authorization:", ignoreCase = true)) {
                cleaned = cleaned.substringAfter("Authorization:").trim()
            }
            if (cleaned.startsWith("Bearer", ignoreCase = true)) {
                cleaned = cleaned.substringAfter("Bearer").trim()
            }
            cleaned = cleaned.removeSurrounding("\"").removeSurrounding("'").trim()
            return if (cleaned.isNotBlank()) cleaned else null
        }
    }

    fun setAccessToken(token: String?) {
        this.accessToken = sanitizeToken(token)
    }

    fun getAccessToken(): String? = accessToken

    fun hasAccessToken(): Boolean = !accessToken.isNullOrBlank()

    /**
     * Build rich description with timestamp chapters for YouTube
     */
    fun buildYouTubeDescription(
        bookTitle: String,
        author: String,
        voiceName: String,
        chapters: List<Chapter>,
        includeMarkers: Boolean
    ): String {
        val sb = StringBuilder()
        sb.append("📖 $bookTitle by $author (Full Audiobook)\n")
        sb.append("🎙️ Narrated by $voiceName with AI Audiobook Studio\n\n")
        sb.append("Enjoy this unabridged classic audiobook edition.\n\n")

        if (includeMarkers && chapters.isNotEmpty()) {
            sb.append("⏱️ CHAPTER TIMESTAMPS:\n")
            var cumulativeMs = 0L
            for ((index, chapter) in chapters.withIndex()) {
                val timestamp = formatTimestamp(cumulativeMs)
                sb.append("$timestamp - Chapter ${index + 1}: ${chapter.title}\n")
                // Calculate chapter audio duration or estimate from words
                val chapterDurationMs = if (chapter.audioDurationMs > 0) {
                    chapter.audioDurationMs
                } else {
                    val durationSec = (chapter.wordCount / 2.5).coerceAtLeast(15.0).toLong()
                    durationSec * 1000L
                }
                cumulativeMs += chapterDurationMs
            }
            sb.append("\n")
        }

        sb.append("--------------------------------------------------\n")
        sb.append("✨ Generated with AI Audiobook Studio & cvoice.ai Neural TTS\n")
        sb.append("🔔 Subscribe for more unabridged audiobooks and audio dramas!\n")
        return sb.toString()
    }

    private fun formatTimestamp(ms: Long): String {
        val totalSec = ms / 1000
        val hours = totalSec / 3600
        val minutes = (totalSec % 3600) / 60
        val seconds = totalSec % 60
        return if (hours > 0) {
            String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.US, "%02d:%02d", minutes, seconds)
        }
    }

    /**
     * Uploads the MP4 video file directly to YouTube using Resumable Upload protocol.
     */
    suspend fun uploadVideo(
        videoFile: File,
        title: String,
        description: String,
        privacy: YouTubePrivacy = YouTubePrivacy.PRIVATE,
        onProgress: (Float, String) -> Unit
    ): Result<Pair<String, String>> = withContext(Dispatchers.IO) {
        try {
            val token = accessToken
            if (token.isNullOrBlank()) {
                return@withContext Result.failure(IllegalStateException("No YouTube OAuth Access Token found. Please authenticate first."))
            }

            if (!videoFile.exists()) {
                return@withContext Result.failure(IllegalArgumentException("Video file not found at ${videoFile.absolutePath}"))
            }

            onProgress(0.05f, "Initiating YouTube upload session...")

            // Step 1: Initiate Resumable Upload Session
            val metadata = YouTubeUploadMetadata(
                snippet = YouTubeUploadSnippet(
                    title = title.take(100),
                    description = description.take(5000)
                ),
                status = YouTubeUploadStatus(
                    privacyStatus = privacy.apiValue,
                    selfDeclaredMadeForKids = false
                )
            )

            val jsonBody = metadataAdapter.toJson(metadata)
            val initRequest = Request.Builder()
                .url("https://www.googleapis.com/upload/youtube/v3/videos?uploadType=resumable&part=snippet,status")
                .addHeader("Authorization", "Bearer $token")
                .addHeader("Content-Type", "application/json; charset=UTF-8")
                .addHeader("X-Upload-Content-Length", videoFile.length().toString())
                .addHeader("X-Upload-Content-Type", "video/mp4")
                .post(jsonBody.toRequestBody("application/json".toMediaType()))
                .build()

            val initResponse = okHttpClient.newCall(initRequest).execute()
            if (!initResponse.isSuccessful) {
                val errBody = initResponse.body?.string() ?: ""
                Log.e(TAG, "Init upload failed: ${initResponse.code} - $errBody")
                val formattedErr = formatYouTubeError(initResponse.code, errBody, token)
                return@withContext Result.failure(IOException(formattedErr))
            }

            val uploadLocationUrl = initResponse.header("Location")
                ?: return@withContext Result.failure(IOException("YouTube did not return an upload session URL."))

            onProgress(0.2f, "Streaming video data to YouTube...")

            // Step 2: Upload Video File Content to session URL
            val fileLength = videoFile.length()
            val mediaType = "video/mp4".toMediaType()
            val fileRequestBody = videoFile.asRequestBody(mediaType)

            val uploadRequest = Request.Builder()
                .url(uploadLocationUrl)
                .addHeader("Authorization", "Bearer $token")
                .addHeader("Content-Type", "video/mp4")
                .put(fileRequestBody)
                .build()

            onProgress(0.6f, "Uploading media stream...")
            val uploadResponse = okHttpClient.newCall(uploadRequest).execute()
            val responseBody = uploadResponse.body?.string() ?: ""

            if (!uploadResponse.isSuccessful) {
                Log.e(TAG, "Upload chunk failed: ${uploadResponse.code} - $responseBody")
                val formattedErr = formatYouTubeError(uploadResponse.code, responseBody, token)
                return@withContext Result.failure(IOException(formattedErr))
            }

            onProgress(0.95f, "Finalizing upload and generating video link...")

            val parsedResponse = responseAdapter.fromJson(responseBody)
            val videoId = parsedResponse?.id ?: ""
            if (videoId.isBlank()) {
                return@withContext Result.failure(IOException("Video upload completed but video ID was not returned."))
            }

            val videoUrl = "https://youtu.be/$videoId"
            Log.i(TAG, "Upload successful: videoId=$videoId, url=$videoUrl")
            onProgress(1.0f, "Video published successfully!")

            Result.success(Pair(videoId, videoUrl))
        } catch (e: Exception) {
            Log.e(TAG, "Error uploading to YouTube", e)
            Result.failure(e)
        }
    }

    private fun formatYouTubeError(code: Int, rawBody: String, token: String?): String {
        var serverMsg: String? = null
        try {
            val jsonObject = org.json.JSONObject(rawBody)
            if (jsonObject.has("error")) {
                val errObj = jsonObject.getJSONObject("error")
                serverMsg = errObj.optString("message")
            }
        } catch (_: Exception) {}

        val tokenLen = token?.length ?: 0
        return when (code) {
            401 -> {
                if (tokenLen in 1..80) {
                    "OAuth Token Error (401 Unauthorized): The token appears truncated ($tokenLen chars, standard Google OAuth tokens are 150-250 chars) or expired. Google OAuth tokens expire after 60 minutes.\n\n💡 Reconnect with a valid token from OAuth Playground, or tap 'Send to YouTube App' to upload manually on your device."
                } else {
                    "OAuth Token Expired or Invalid (401 Unauthorized): Google OAuth tokens expire after 60 minutes.\n\n💡 Tap 'Set OAuth Token' to reconnect, or tap 'Send to YouTube App' to upload via your device."
                }
            }
            403 -> {
                if (serverMsg?.contains("quota", ignoreCase = true) == true) {
                    "YouTube Daily API Quota Exceeded (403): Google's free YouTube API quota limit reached for today.\n\n💡 Tap 'Send to YouTube App' or 'Save MP4' to upload directly via the YouTube app or YouTube Studio web."
                } else {
                    "YouTube Permission Denied (403): The Google account lacks 'youtube.upload' scope or channel verification is required.\n\n💡 Tap 'Send to YouTube App' or 'Save MP4' to upload directly without API restrictions."
                }
            }
            400 -> "Video Request Error (400): ${serverMsg ?: rawBody.take(120)}"
            else -> "YouTube API Error ($code): ${serverMsg ?: rawBody.take(120)}"
        }
    }

    /**
     * Confirms the token is valid and retrieves authorization info.
     * Checks Google's tokeninfo endpoint to verify token validity and YouTube scopes,
     * and attempts to fetch channel title if read scopes are present.
     */
    suspend fun fetchChannelInfo(): Result<String> = withContext(Dispatchers.IO) {
        try {
            val token = accessToken ?: return@withContext Result.failure(IllegalStateException("No access token provided"))

            // 1. Verify token status and scopes with Google OAuth tokeninfo endpoint
            val tokenInfoRequest = Request.Builder()
                .url("https://www.googleapis.com/oauth2/v3/tokeninfo?access_token=$token")
                .get()
                .build()

            val tokenInfoResponse = okHttpClient.newCall(tokenInfoRequest).execute()
            val tokenInfoBody = tokenInfoResponse.body?.string() ?: ""

            if (!tokenInfoResponse.isSuccessful) {
                if (tokenInfoResponse.code == 400 || tokenInfoBody.contains("invalid", ignoreCase = true)) {
                    return@withContext Result.failure(IOException("OAuth access token is invalid or has expired. Please generate a fresh token from OAuth Playground."))
                }
                return@withContext Result.failure(IOException("Token verification failed (HTTP ${tokenInfoResponse.code}): ${tokenInfoBody.take(100)}"))
            }

            // Verify that the token grants YouTube permissions
            val hasYouTubeScope = tokenInfoBody.contains("youtube", ignoreCase = true)
            if (!hasYouTubeScope) {
                return@withContext Result.failure(IOException("The token does not have YouTube scope. Please authorize 'https://www.googleapis.com/auth/youtube.upload' in OAuth Playground."))
            }

            // 2. Token is verified and valid! If user also granted read access, fetch their channel name
            try {
                val channelRequest = Request.Builder()
                    .url("https://www.googleapis.com/youtube/v3/channels?part=snippet&mine=true")
                    .addHeader("Authorization", "Bearer $token")
                    .get()
                    .build()

                val channelResponse = okHttpClient.newCall(channelRequest).execute()
                val channelBody = channelResponse.body?.string() ?: ""
                if (channelResponse.isSuccessful) {
                    val list = channelListAdapter.fromJson(channelBody)
                    val channelTitle = list?.items?.firstOrNull()?.snippet?.title
                    if (!channelTitle.isNullOrBlank()) {
                        return@withContext Result.success(channelTitle)
                    }
                }
            } catch (ignored: Exception) {
                // Ignore channel snippet errors since upload scope alone does not permit channels.list
            }

            // Upload scope is confirmed active
            Result.success("YouTube Account (Upload Authorized)")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
