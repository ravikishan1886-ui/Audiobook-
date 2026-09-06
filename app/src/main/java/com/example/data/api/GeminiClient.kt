package com.example.data.api

import android.util.Log
import com.example.BuildConfig
import com.example.data.model.Chapter
import com.example.data.model.ChapterStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import java.util.concurrent.TimeUnit

class GeminiClient(
    private var customApiKey: String? = null,
    private var customOpenAiKey: String? = null
) {
    private val TAG = "GeminiClient"
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    fun getEffectiveApiKey(): String {
        if (!customApiKey.isNullOrBlank()) {
            return customApiKey!!.trim()
        }
        return try {
            val buildKey = BuildConfig.GEMINI_API_KEY
            if (buildKey.isNullOrBlank() || buildKey == "MY_GEMINI_API_KEY") "" else buildKey.trim()
        } catch (e: Exception) {
            ""
        }
    }

    fun setApiKey(key: String) {
        customApiKey = key.trim()
    }

    fun setOpenAiKey(key: String) {
        customOpenAiKey = key.trim()
    }

    fun getEffectiveOpenAiKey(): String {
        if (!customOpenAiKey.isNullOrBlank()) {
            return customOpenAiKey!!.trim()
        }
        return try {
            val buildKey = BuildConfig.OPENAI_API_KEY
            if (buildKey.isNullOrBlank() || buildKey == "MY_OPENAI_API_KEY") "" else buildKey.trim()
        } catch (_: Exception) {
            ""
        }
    }

    suspend fun cleanAndStructureManuscript(
        bookTitle: String,
        author: String,
        rawText: String
    ): Result<List<Chapter>> = withContext(Dispatchers.IO) {
        val geminiKey = getEffectiveApiKey()
        val openAiKey = getEffectiveOpenAiKey()

        // If user set an OpenAI key as customApiKey (starts with sk-)
        if (geminiKey.startsWith("sk-") || (geminiKey.isBlank() && openAiKey.startsWith("sk-"))) {
            val keyToUse = if (geminiKey.startsWith("sk-")) geminiKey else openAiKey
            val openAiResult = cleanAndStructureWithOpenAi(bookTitle, author, rawText, keyToUse)
            if (openAiResult != null && openAiResult.isNotEmpty()) {
                return@withContext Result.success(openAiResult)
            }
        }

        if (geminiKey.isBlank()) {
            // If OpenAI key is available, use it
            if (openAiKey.isNotBlank()) {
                val openAiResult = cleanAndStructureWithOpenAi(bookTitle, author, rawText, openAiKey)
                if (openAiResult != null && openAiResult.isNotEmpty()) {
                    return@withContext Result.success(openAiResult)
                }
            }
            Log.w(TAG, "No LLM API keys configured, using smart local parser")
            val localChapters = splitLocally(rawText, bookTitle)
            return@withContext Result.success(localChapters)
        }

        try {
            val prompt = """
                You are a master audiobook editor and voice director.
                The user has provided a manuscript for an audiobook titled "$bookTitle" by $author.
                
                YOUR TASKS:
                1. Split the text into logical chapters or narrative sections (1 to 6 sections depending on length).
                2. For each chapter, clean and adapt the text specifically for natural audio narration:
                   - Preserve ALL of the original story, dialogue, characters, and wording.
                   - Expand spoken abbreviations (e.g., 'Dr.' to 'Doctor', 'St.' to 'Saint', 'Mr.' to 'Mister', 'Mrs.' to 'Missus', 'etc.' to 'et cetera', 'i.e.' to 'that is').
                   - Clean up OCR / text formatting artifacts, line wraps, page numbers, or footnotes.
                   - Enhance punctuation rhythm (e.g. em-dashes, commas, ellipses) so that text-to-speech audio sounds fluid and natural.
                
                Respond in VALID JSON format strictly adhering to this schema:
                {
                  "chapters": [
                    {
                      "title": "Chapter title or section heading",
                      "narrationText": "The refined, spoken-ready audiobook narration text for this chapter."
                    }
                  ]
                }
                
                Raw Manuscript:
                $rawText
            """.trimIndent()

            val requestJson = JSONObject().apply {
                val contents = JSONArray().apply {
                    val contentObj = JSONObject().apply {
                        val parts = JSONArray().apply {
                            put(JSONObject().apply { put("text", prompt) })
                        }
                        put("parts", parts)
                    }
                    put(contentObj)
                }
                put("contents", contents)

                val generationConfig = JSONObject().apply {
                    put("temperature", 0.2)
                    put("responseMimeType", "application/json")
                }
                put("generationConfig", generationConfig)
            }

            val requestBody = requestJson.toString().toRequestBody("application/json".toMediaType())
            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$geminiKey"

            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                Log.e(TAG, "Gemini API notice: ${response.code} - $responseBody")
                // Try OpenAI fallback if key available
                if (openAiKey.isNotBlank()) {
                    Log.i(TAG, "Falling back to OpenAI for structuring")
                    val openAiFallback = cleanAndStructureWithOpenAi(bookTitle, author, rawText, openAiKey)
                    if (openAiFallback != null && openAiFallback.isNotEmpty()) {
                        return@withContext Result.success(openAiFallback)
                    }
                }
                val fallbackChapters = splitLocally(rawText, bookTitle)
                return@withContext Result.success(fallbackChapters)
            }

            val jsonResponse = JSONObject(responseBody)
            val candidates = jsonResponse.optJSONArray("candidates")
            val firstCandidate = candidates?.optJSONObject(0)
            val content = firstCandidate?.optJSONObject("content")
            val parts = content?.optJSONArray("parts")
            val rawOutput = parts?.optJSONObject(0)?.optString("text") ?: ""

            val parsedJson = JSONObject(cleanJsonString(rawOutput))
            val chaptersArray = parsedJson.optJSONArray("chapters") ?: JSONArray()

            val chaptersList = parseChaptersJsonArray(chaptersArray)
            if (chaptersList.isEmpty()) {
                return@withContext Result.success(splitLocally(rawText, bookTitle))
            }

            Result.success(chaptersList)
        } catch (e: Exception) {
            Log.e(TAG, "Gemini call notice, attempting OpenAI or local fallback", e)
            if (openAiKey.isNotBlank()) {
                val openAiFallback = cleanAndStructureWithOpenAi(bookTitle, author, rawText, openAiKey)
                if (openAiFallback != null && openAiFallback.isNotEmpty()) {
                    return@withContext Result.success(openAiFallback)
                }
            }
            Result.success(splitLocally(rawText, bookTitle))
        }
    }

    private fun cleanAndStructureWithOpenAi(
        bookTitle: String,
        author: String,
        rawText: String,
        apiKey: String
    ): List<Chapter>? {
        return try {
            val systemPrompt = "You are a master audiobook editor and voice director. Split the manuscript into logical chapters or narrative sections, expanding spoken abbreviations and formatting text for fluid audio narration. Always respond in valid JSON adhering strictly to: {\"chapters\": [{\"title\": \"string\", \"narrationText\": \"string\"}]}"
            val userPrompt = "Audiobook: \"$bookTitle\" by $author.\n\nRaw Manuscript:\n$rawText"

            val requestJson = JSONObject().apply {
                put("model", "gpt-4o-mini")
                val messages = JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "system")
                        put("content", systemPrompt)
                    })
                    put(JSONObject().apply {
                        put("role", "user")
                        put("content", userPrompt)
                    })
                }
                put("messages", messages)
                put("response_format", JSONObject().apply { put("type", "json_object") })
                put("temperature", 0.2)
            }

            val requestBody = requestJson.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url("https://api.openai.com/v1/chat/completions")
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                Log.w(TAG, "OpenAI structuring call returned ${response.code}")
                return null
            }

            val respBody = response.body?.string() ?: return null
            val root = JSONObject(respBody)
            val choices = root.optJSONArray("choices") ?: return null
            val firstChoice = choices.optJSONObject(0) ?: return null
            val message = firstChoice.optJSONObject("message") ?: return null
            val contentStr = message.optString("content", "")

            val parsedJson = JSONObject(cleanJsonString(contentStr))
            val chaptersArray = parsedJson.optJSONArray("chapters") ?: JSONArray()
            val list = parseChaptersJsonArray(chaptersArray)
            if (list.isNotEmpty()) list else null
        } catch (e: Exception) {
            Log.w(TAG, "OpenAI structuring exception: ${e.message}")
            null
        }
    }

    private fun parseChaptersJsonArray(chaptersArray: JSONArray): List<Chapter> {
        val chaptersList = mutableListOf<Chapter>()
        for (i in 0 until chaptersArray.length()) {
            val chapObj = chaptersArray.getJSONObject(i)
            val title = chapObj.optString("title", "Chapter ${i + 1}")
            val narration = chapObj.optString("narrationText", "")
            val words = narration.split("\\s+".toRegex()).filter { it.isNotBlank() }.size
            val estDuration = (words / 2.5).toInt().coerceAtLeast(10) // ~150 words per min = 2.5 words/sec

            chaptersList.add(
                Chapter(
                    id = UUID.randomUUID().toString(),
                    index = i + 1,
                    title = title,
                    rawText = narration,
                    cleanedNarration = narration,
                    wordCount = words,
                    estimatedDurationSec = estDuration,
                    status = ChapterStatus.READY_FOR_AUDIO
                )
            )
        }
        return chaptersList
    }

    private fun cleanJsonString(raw: String): String {
        var trimmed = raw.trim()
        if (trimmed.startsWith("```json")) {
            trimmed = trimmed.removePrefix("```json")
        } else if (trimmed.startsWith("```")) {
            trimmed = trimmed.removePrefix("```")
        }
        if (trimmed.endsWith("```")) {
            trimmed = trimmed.removeSuffix("```")
        }
        return trimmed.trim()
    }

    fun splitLocally(text: String, bookTitle: String): List<Chapter> {
        val chapterRegex = Regex("(?i)(?:^|\\n\\n)(CHAPTER\\s+[0-9IVXLCDM]+(?:\\s*[:\\-–—]\\s*[^\\n]+)?|ACT\\s+[0-9IVXLCDM]+|SCENE\\s+[0-9IVXLCDM]+|PROLOGUE|EPILOGUE)")
        val matches = chapterRegex.findAll(text).toList()

        val chapters = mutableListOf<Chapter>()

        if (matches.isNotEmpty()) {
            for (i in matches.indices) {
                val start = matches[i].range.first
                val end = if (i + 1 < matches.size) matches[i + 1].range.first else text.length
                val sectionText = text.substring(start, end).trim()
                val heading = matches[i].value.trim().replace("\n", " ")
                val cleanHeading = heading.ifBlank { "Chapter ${i + 1}" }

                // Clean narration text
                val narration = cleanNarrationTextLocally(sectionText)
                val words = narration.split("\\s+".toRegex()).filter { it.isNotBlank() }.size
                val estDuration = (words / 2.5).toInt().coerceAtLeast(10)

                chapters.add(
                    Chapter(
                        id = UUID.randomUUID().toString(),
                        index = i + 1,
                        title = cleanHeading,
                        rawText = sectionText,
                        cleanedNarration = narration,
                        wordCount = words,
                        estimatedDurationSec = estDuration,
                        status = ChapterStatus.READY_FOR_AUDIO
                    )
                )
            }
        } else {
            // Split into 2-3 logical parts by paragraph length
            val paragraphs = text.split("\n\n").filter { it.isNotBlank() }
            if (paragraphs.size <= 2) {
                val words = text.split("\\s+".toRegex()).filter { it.isNotBlank() }.size
                chapters.add(
                    Chapter(
                        id = UUID.randomUUID().toString(),
                        index = 1,
                        title = "Complete Narration",
                        rawText = text,
                        cleanedNarration = cleanNarrationTextLocally(text),
                        wordCount = words,
                        estimatedDurationSec = (words / 2.5).toInt().coerceAtLeast(10),
                        status = ChapterStatus.READY_FOR_AUDIO
                    )
                )
            } else {
                val chunkSize = (paragraphs.size + 1) / 2
                val part1 = paragraphs.take(chunkSize).joinToString("\n\n")
                val part2 = paragraphs.drop(chunkSize).joinToString("\n\n")

                val words1 = part1.split("\\s+".toRegex()).filter { it.isNotBlank() }.size
                val words2 = part2.split("\\s+".toRegex()).filter { it.isNotBlank() }.size

                chapters.add(
                    Chapter(
                        id = UUID.randomUUID().toString(),
                        index = 1,
                        title = "Chapter 1: Opening",
                        rawText = part1,
                        cleanedNarration = cleanNarrationTextLocally(part1),
                        wordCount = words1,
                        estimatedDurationSec = (words1 / 2.5).toInt().coerceAtLeast(10),
                        status = ChapterStatus.READY_FOR_AUDIO
                    )
                )
                if (part2.isNotBlank()) {
                    chapters.add(
                        Chapter(
                            id = UUID.randomUUID().toString(),
                            index = 2,
                            title = "Chapter 2: Continuation",
                            rawText = part2,
                            cleanedNarration = cleanNarrationTextLocally(part2),
                            wordCount = words2,
                            estimatedDurationSec = (words2 / 2.5).toInt().coerceAtLeast(10),
                            status = ChapterStatus.READY_FOR_AUDIO
                        )
                    )
                }
            }
        }
        return chapters
    }

    private fun cleanNarrationTextLocally(text: String): String {
        return text
            .replace(Regex("\\bDr\\.\\s*"), "Doctor ")
            .replace(Regex("\\bMr\\.\\s*"), "Mister ")
            .replace(Regex("\\bMrs\\.\\s*"), "Missus ")
            .replace(Regex("\\bMs\\.\\s*"), "Miz ")
            .replace(Regex("\\betc\\.\\s*"), "et cetera ")
            .replace(Regex("\\bi\\.e\\.\\s*"), "that is ")
            .replace(Regex("\\be\\.g\\.\\s*"), "for example ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }
}
