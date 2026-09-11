package com.example.video

import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLDecoder
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.TimeUnit
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

data class MegaFileInfo(
    val fileId: String,
    val fileKey: String,
    val directUrl: String = "",
    val fileName: String = "video.mp4",
    val fileSizeBytes: Long = 0L
)

data class ResolvedVideoSource(
    val rawUrl: String,
    val cleanUrl: String,
    val isGoogleRedirect: Boolean,
    val isMega: Boolean,
    val isGoogleDrive: Boolean,
    val isDropbox: Boolean,
    val megaInfo: MegaFileInfo? = null,
    val displayBadge: String = "",
    val detectedTitle: String? = null
)

object VideoUrlUtils {
    private const val TAG = "VideoUrlUtils"

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    /**
     * Unwraps Google redirects, URL-encoded wrappers, and cleans leading/trailing whitespace.
     */
    fun unwrapAndCleanUrl(rawInput: String): String {
        var url = rawInput.trim()
        if (url.startsWith("\"") && url.endsWith("\"")) {
            url = url.substring(1, url.length - 1).trim()
        }
        if (url.startsWith("<") && url.endsWith(">")) {
            url = url.substring(1, url.length - 1).trim()
        }

        // Loop up to 3 times to unwrap nested redirects
        for (i in 0 until 3) {
            val unwrapped = tryUnwrapRedirect(url)
            if (unwrapped != null && unwrapped != url) {
                url = unwrapped
            } else {
                break
            }
        }

        return url
    }

    private fun tryUnwrapRedirect(url: String): String? {
        try {
            // Google Redirect: https://www.google.com/url?...&q=https%3A%2F%2F...
            if (url.contains("google.") && url.contains("/url")) {
                val targetQ = try {
                    val uri = Uri.parse(url)
                    uri.getQueryParameter("q") ?: uri.getQueryParameter("url")
                } catch (e: Exception) { null }
                    ?: """[?&](?:q|url)=([^&]+)""".toRegex().find(url)?.groupValues?.get(1)

                if (!targetQ.isNullOrBlank()) {
                    return decodeUrl(targetQ)
                }
            }

            // YouTube redirect: https://www.youtube.com/redirect?...&q=...
            if (url.contains("youtube.com/redirect")) {
                val uri = Uri.parse(url)
                val targetQ = uri.getQueryParameter("q")
                if (!targetQ.isNullOrBlank()) {
                    return decodeUrl(targetQ)
                }
            }

            // Facebook redirect: l.facebook.com/l.php?u=...
            if (url.contains("facebook.com/l.php") || url.contains("l.facebook.com")) {
                val uri = Uri.parse(url)
                val targetU = uri.getQueryParameter("u")
                if (!targetU.isNullOrBlank()) {
                    return decodeUrl(targetU)
                }
            }

            // If raw input starts with %3A or contains encoded protocol
            if (url.contains("%3A%2F%2F", ignoreCase = true) || url.contains("%2F%2F")) {
                return decodeUrl(url)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not unwrap redirect: ${e.message}")
        }
        return null
    }

    private fun decodeUrl(value: String): String {
        return try {
            URLDecoder.decode(value, "UTF-8")
        } catch (e: Exception) {
            value
        }
    }

    /**
     * Extracts MEGA file ID and Key from standard MEGA URLs.
     * Supports:
     * - https://mega.nz/file/{fileId}#{fileKey}
     * - https://mega.nz/#!{fileId}!{fileKey}
     * - https://mega.co.nz/file/{fileId}#{fileKey}
     * - https://mega.co.nz/#!{fileId}!{fileKey}
     */
    fun extractMegaFileInfo(url: String): MegaFileInfo? {
        val clean = unwrapAndCleanUrl(url)

        // Pattern 1: mega.nz/file/ID#KEY
        val pattern1 = """mega\.(?:nz|co\.nz)/file/([a-zA-Z0-9_-]+)(?:#|%23)([a-zA-Z0-9_-]+)""".toRegex(RegexOption.IGNORE_CASE)
        val match1 = pattern1.find(clean)
        if (match1 != null) {
            return MegaFileInfo(
                fileId = match1.groupValues[1],
                fileKey = match1.groupValues[2]
            )
        }

        // Pattern 2: mega.nz/#!ID!KEY
        val pattern2 = """mega\.(?:nz|co\.nz)/#(!|%21)([a-zA-Z0-9_-]+)(?:!|%21)([a-zA-Z0-9_-]+)""".toRegex(RegexOption.IGNORE_CASE)
        val match2 = pattern2.find(clean)
        if (match2 != null) {
            return MegaFileInfo(
                fileId = match2.groupValues[2],
                fileKey = match2.groupValues[3]
            )
        }

        return null
    }

    /**
     * Resolves the source details for display badges and engine routing.
     */
    fun resolveSource(rawUrl: String): ResolvedVideoSource {
        val clean = unwrapAndCleanUrl(rawUrl)
        val isGoogleRedirect = rawUrl.contains("google.") && rawUrl.contains("/url")
        val megaInfo = extractMegaFileInfo(clean)
        val isMega = megaInfo != null
        val isGoogleDrive = clean.contains("drive.google.com")
        val isDropbox = clean.contains("dropbox.com")

        val badge = when {
            isGoogleRedirect && isMega -> "✓ Google Redirect Unwrapped • MEGA Cloud"
            isMega -> "MEGA Cloud Storage (File: ${megaInfo?.fileId})"
            isGoogleDrive -> "Google Drive File"
            isDropbox -> "Dropbox Direct Video"
            clean.endsWith(".mp4", ignoreCase = true) -> "Direct MP4 Stream"
            else -> "Public Video Stream"
        }

        val detectedTitle = when {
            isMega -> "MEGA Video (${megaInfo?.fileId})"
            isGoogleDrive -> "Google Drive Video"
            else -> null
        }

        return ResolvedVideoSource(
            rawUrl = rawUrl,
            cleanUrl = clean,
            isGoogleRedirect = isGoogleRedirect,
            isMega = isMega,
            isGoogleDrive = isGoogleDrive,
            isDropbox = isDropbox,
            megaInfo = megaInfo,
            displayBadge = badge,
            detectedTitle = detectedTitle
        )
    }

    /**
     * Fetches metadata (real filename and file size) from MEGA API.
     */
    suspend fun fetchMegaMetadata(fileId: String, fileKey: String): Pair<String?, Long?> = withContext(Dispatchers.IO) {
        try {
            val jsonPayload = """[{"a":"g","g":1,"ssl":2,"p":"$fileId"}]"""
            val body = jsonPayload.toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url("https://g.api.mega.co.nz/cs")
                .post(body)
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) return@withContext Pair(null, null)

            val respBody = response.body?.string() ?: return@withContext Pair(null, null)
            val jsonArray = JSONArray(respBody)
            if (jsonArray.length() == 0) return@withContext Pair(null, null)

            // If MEGA returned an error code (e.g. [-9])
            if (jsonArray.opt(0) is Number) return@withContext Pair(null, null)

            val item = jsonArray.optJSONObject(0) ?: return@withContext Pair(null, null)
            val size = item.optLong("s", 0L)
            val atB64 = item.optString("at", "")

            var fileName: String? = null
            if (atB64.isNotBlank()) {
                val aesKey = deriveAesKey(fileKey)
                if (aesKey != null) {
                    fileName = decryptMegaAttributes(atB64, aesKey)
                }
            }

            Pair(fileName, if (size > 0) size else null)
        } catch (e: Exception) {
            Log.w(TAG, "Error fetching MEGA metadata: ${e.message}")
            Pair(null, null)
        }
    }

    /**
     * Derives 128-bit AES key and 128-bit IV from MEGA 32-byte Base64URL key.
     */
    fun deriveCryptoParams(fileKey: String): Pair<ByteArray, ByteArray>? {
        return try {
            val remainder = fileKey.length % 4
            val padded = if (remainder > 0) fileKey + "=".repeat(4 - remainder) else fileKey
            val rawBytes = decodeBase64Url(padded) ?: return null
            if (rawBytes.size < 32) return null

            val buffer = ByteBuffer.wrap(rawBytes).order(ByteOrder.BIG_ENDIAN)
            val k = IntArray(8) { buffer.int }

            val aesKeyBytes = ByteBuffer.allocate(16).order(ByteOrder.BIG_ENDIAN).apply {
                putInt(k[0] xor k[4])
                putInt(k[1] xor k[5])
                putInt(k[2] xor k[6])
                putInt(k[3] xor k[7])
            }.array()

            val ivBytes = ByteBuffer.allocate(16).order(ByteOrder.BIG_ENDIAN).apply {
                putInt(k[4])
                putInt(k[5])
                putInt(0)
                putInt(0)
            }.array()

            Pair(aesKeyBytes, ivBytes)
        } catch (e: Exception) {
            Log.e(TAG, "Error deriving MEGA crypto params", e)
            null
        }
    }

    private fun deriveAesKey(fileKey: String): ByteArray? {
        return deriveCryptoParams(fileKey)?.first
    }

    private fun decryptMegaAttributes(atB64: String, aesKeyBytes: ByteArray): String? {
        return try {
            val remainder = atB64.length % 4
            val padded = if (remainder > 0) atB64 + "=".repeat(4 - remainder) else atB64
            val rawBytes = decodeBase64Url(padded) ?: return null

            val cipher = Cipher.getInstance("AES/CBC/NoPadding")
            val secretKey = SecretKeySpec(aesKeyBytes, "AES")
            val zeroIv = IvParameterSpec(ByteArray(16))
            cipher.init(Cipher.DECRYPT_MODE, secretKey, zeroIv)
            val decrypted = cipher.doFinal(rawBytes)

            val str = String(decrypted, Charsets.UTF_8).trimEnd { it == '\u0000' || it.isWhitespace() }
            if (str.startsWith("MEGA")) {
                val jsonStr = str.substring(4)
                val json = JSONObject(jsonStr)
                if (json.has("n")) json.getString("n") else null
            } else null
        } catch (e: Exception) {
            Log.w(TAG, "Failed to decrypt MEGA attributes: ${e.message}")
            null
        }
    }

    private fun decodeBase64Url(input: String): ByteArray? {
        return try {
            android.util.Base64.decode(input, android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP)
        } catch (e: Exception) {
            try {
                java.util.Base64.getUrlDecoder().decode(input)
            } catch (e2: Exception) {
                null
            }
        }
    }
}
