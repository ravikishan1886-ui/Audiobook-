package com.example.data.api

import com.example.data.model.DEFAULT_REMIX_VIDEO_DESCRIPTION
import com.example.data.model.SheetJobStatus
import com.example.data.model.SheetVideoRow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

class GoogleSheetsClient {
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .writeTimeout(25, TimeUnit.SECONDS)
        .build()

    fun extractSpreadsheetId(input: String): String {
        val trimmed = input.trim()
        val regex = Regex("/spreadsheets/d/([a-zA-Z0-9-_]+)")
        val match = regex.find(trimmed)
        if (match != null) {
            return match.groupValues[1]
        }
        return trimmed
    }

    suspend fun fetchRows(
        spreadsheetId: String,
        sheetName: String = "Sheet1",
        accessToken: String = "",
        apiKey: String = ""
    ): Result<List<SheetVideoRow>> = withContext(Dispatchers.IO) {
        try {
            val cleanId = extractSpreadsheetId(spreadsheetId)
            if (cleanId.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("Spreadsheet ID is empty"))
            }

            // First try official Google Sheets API v4 if token or key is available
            val encodedRange = URLEncoder.encode("$sheetName!A1:J200", "UTF-8")
            val apiUrl = buildString {
                append("https://sheets.googleapis.com/v4/spreadsheets/$cleanId/values/$encodedRange")
                if (apiKey.isNotBlank() && accessToken.isBlank()) {
                    append("?key=$apiKey")
                }
            }

            val reqBuilder = Request.Builder().url(apiUrl).get()
            if (accessToken.isNotBlank()) {
                reqBuilder.addHeader("Authorization", "Bearer $accessToken")
            }

            val response = httpClient.newCall(reqBuilder.build()).execute()
            val bodyStr = response.body?.string() ?: ""

            if (response.isSuccessful && bodyStr.isNotBlank()) {
                val json = JSONObject(bodyStr)
                val values = json.optJSONArray("values") ?: JSONArray()
                val parsed = parseValuesArray(values)
                return@withContext Result.success(parsed)
            }

            // Fallback: Public Google Sheet CSV export for read access when shared via link
            val csvUrl = "https://docs.google.com/spreadsheets/d/$cleanId/gviz/tq?tqx=out:csv&sheet=${URLEncoder.encode(sheetName, "UTF-8")}"
            val csvResp = httpClient.newCall(Request.Builder().url(csvUrl).get().build()).execute()
            val csvBody = csvResp.body?.string() ?: ""
            if (csvResp.isSuccessful && csvBody.isNotBlank() && !csvBody.trimStart().startsWith("<!DOCTYPE", ignoreCase = true)) {
                val rows = parseCsvRows(csvBody)
                return@withContext Result.success(rows)
            }

            val errMessage = try {
                JSONObject(bodyStr).optJSONObject("error")?.optString("message")
                    ?: "HTTP ${response.code}: Ensure spreadsheet is shared or Google OAuth token is connected."
            } catch (_: Exception) {
                "HTTP ${response.code}: Unable to read Google Sheet. Verify Spreadsheet ID and permissions."
            }
            Result.failure(Exception(errMessage))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateRowStatus(
        spreadsheetId: String,
        sheetName: String = "Sheet1",
        rowNumber: Int,
        status: SheetJobStatus,
        youtubeUrl: String = "",
        errorMessage: String = "",
        accessToken: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val cleanId = extractSpreadsheetId(spreadsheetId)
            if (cleanId.isBlank() || accessToken.isBlank()) {
                return@withContext Result.failure(IllegalStateException("Google OAuth access token required to update sheet cells"))
            }

            // Update G (Status), I (YouTube URL), J (Error Message) via batchUpdate values
            val url = "https://sheets.googleapis.com/v4/spreadsheets/$cleanId/values:batchUpdate"
            val dataArray = JSONArray().apply {
                put(JSONObject().apply {
                    put("range", "$sheetName!G$rowNumber")
                    put("values", JSONArray().put(JSONArray().put(status.label)))
                })
                if (youtubeUrl.isNotBlank()) {
                    put(JSONObject().apply {
                        put("range", "$sheetName!I$rowNumber")
                        put("values", JSONArray().put(JSONArray().put(youtubeUrl)))
                    })
                }
                put(JSONObject().apply {
                    put("range", "$sheetName!J$rowNumber")
                    put("values", JSONArray().put(JSONArray().put(errorMessage)))
                })
            }

            val payload = JSONObject().apply {
                put("valueInputOption", "USER_ENTERED")
                put("data", dataArray)
            }

            val req = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $accessToken")
                .addHeader("Content-Type", "application/json")
                .post(payload.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val resp = httpClient.newCall(req).execute()
            if (resp.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Sheets update failed: HTTP ${resp.code}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun appendRow(
        spreadsheetId: String,
        sheetName: String = "Sheet1",
        row: SheetVideoRow,
        accessToken: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val cleanId = extractSpreadsheetId(spreadsheetId)
            if (cleanId.isBlank() || accessToken.isBlank()) {
                return@withContext Result.failure(IllegalStateException("OAuth token required to append to Google Sheet"))
            }

            val encodedRange = URLEncoder.encode("$sheetName!A:J", "UTF-8")
            val url = "https://sheets.googleapis.com/v4/spreadsheets/$cleanId/values/$encodedRange:append?valueInputOption=USER_ENTERED"

            val rowValues = JSONArray().apply {
                put(row.videoUrl)
                put(row.videoTitle)
                put(row.videoDescription)
                put(row.thumbnailUrl)
                put(row.musicSelection)
                put(row.musicVolume.toString())
                put(row.uploadStatus.label)
                put(row.scheduledUploadTime)
                put(row.youtubeVideoUrl)
                put(row.errorMessage)
            }
            val payload = JSONObject().apply {
                put("values", JSONArray().put(rowValues))
            }

            val req = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $accessToken")
                .addHeader("Content-Type", "application/json")
                .post(payload.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val resp = httpClient.newCall(req).execute()
            if (resp.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Append failed: HTTP ${resp.code}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseValuesArray(values: JSONArray): List<SheetVideoRow> {
        val result = mutableListOf<SheetVideoRow>()
        for (i in 0 until values.length()) {
            val rowArr = values.optJSONArray(i) ?: continue
            val colA = rowArr.optString(0, "").trim()
            val colB = rowArr.optString(1, "").trim()
            if (i == 0 && (colA.equals("Video URL", ignoreCase = true) || colB.equals("Video Title", ignoreCase = true))) {
                continue // skip header row
            }
            if (colA.isBlank() && colB.isBlank()) continue

            val desc = rowArr.optString(2, "").trim().ifBlank { DEFAULT_REMIX_VIDEO_DESCRIPTION }
            val thumb = rowArr.optString(3, "").trim()
            val music = rowArr.optString(4, "").trim()
            val volStr = rowArr.optString(5, "1.0").trim().removeSuffix("%")
            val volRaw = volStr.toFloatOrNull() ?: 1.0f
            val normalizedVol = if (volRaw > 2.0f) (volRaw / 100f).coerceIn(0f, 2f) else volRaw.coerceIn(0f, 2f)
            val statusStr = rowArr.optString(6, "READY").trim()
            val scheduled = rowArr.optString(7, "").trim()
            val ytUrl = rowArr.optString(8, "").trim()
            val err = rowArr.optString(9, "").trim()

            result.add(
                SheetVideoRow(
                    rowNumber = i + 1,
                    videoUrl = colA,
                    videoTitle = colB.ifBlank { "Remixed Shorts Video #${i + 1}" },
                    videoDescription = desc,
                    thumbnailUrl = thumb,
                    musicSelection = music,
                    musicVolume = normalizedVol,
                    uploadStatus = SheetJobStatus.fromString(statusStr),
                    scheduledUploadTime = scheduled,
                    youtubeVideoUrl = ytUrl,
                    errorMessage = err
                )
            )
        }
        return result
    }

    private fun parseCsvRows(csvContent: String): List<SheetVideoRow> {
        val lines = csvContent.lines().filter { it.isNotBlank() }
        val result = mutableListOf<SheetVideoRow>()
        lines.forEachIndexed { index, line ->
            val cols = parseCsvLine(line)
            val colA = cols.getOrNull(0)?.trim() ?: ""
            val colB = cols.getOrNull(1)?.trim() ?: ""
            if (index == 0 && (colA.equals("Video URL", ignoreCase = true) || colB.equals("Video Title", ignoreCase = true))) {
                return@forEachIndexed
            }
            if (colA.isBlank() && colB.isBlank()) return@forEachIndexed

            val desc = cols.getOrNull(2)?.trim()?.ifBlank { DEFAULT_REMIX_VIDEO_DESCRIPTION } ?: DEFAULT_REMIX_VIDEO_DESCRIPTION
            val thumb = cols.getOrNull(3)?.trim() ?: ""
            val music = cols.getOrNull(4)?.trim() ?: ""
            val volStr = (cols.getOrNull(5)?.trim() ?: "1.0").removeSuffix("%")
            val volRaw = volStr.toFloatOrNull() ?: 1.0f
            val normalizedVol = if (volRaw > 2.0f) (volRaw / 100f).coerceIn(0f, 2f) else volRaw.coerceIn(0f, 2f)
            val statusStr = cols.getOrNull(6)?.trim() ?: "READY"
            val scheduled = cols.getOrNull(7)?.trim() ?: ""
            val ytUrl = cols.getOrNull(8)?.trim() ?: ""
            val err = cols.getOrNull(9)?.trim() ?: ""

            result.add(
                SheetVideoRow(
                    rowNumber = index + 1,
                    videoUrl = colA,
                    videoTitle = colB.ifBlank { "Remixed Shorts Video #${index + 1}" },
                    videoDescription = desc,
                    thumbnailUrl = thumb,
                    musicSelection = music,
                    musicVolume = normalizedVol,
                    uploadStatus = SheetJobStatus.fromString(statusStr),
                    scheduledUploadTime = scheduled,
                    youtubeVideoUrl = ytUrl,
                    errorMessage = err
                )
            )
        }
        return result
    }

    private fun parseCsvLine(line: String): List<String> {
        val cols = mutableListOf<String>()
        val sb = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            if (c == '"') {
                if (inQuotes && i + 1 < line.length && line[i + 1] == '"') {
                    sb.append('"')
                    i++
                } else {
                    inQuotes = !inQuotes
                }
            } else if (c == ',' && !inQuotes) {
                cols.add(sb.toString())
                sb.clear()
            } else {
                sb.append(c)
            }
            i++
        }
        cols.add(sb.toString())
        return cols
    }
}
