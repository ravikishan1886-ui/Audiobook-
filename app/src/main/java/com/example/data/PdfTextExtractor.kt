package com.example.data

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.Inflater
import java.util.zip.InflaterInputStream

object PdfTextExtractor {
    private const val TAG = "PdfTextExtractor"

    data class ExtractedDocument(
        val text: String,
        val fileName: String,
        val isPdf: Boolean,
        val characterCount: Int
    )

    fun extract(context: Context, uri: Uri): Result<ExtractedDocument> {
        return try {
            val contentResolver = context.contentResolver
            var displayName = "Manuscript"

            try {
                contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (nameIndex >= 0) {
                            val name = cursor.getString(nameIndex)
                            if (!name.isNullOrBlank()) {
                                displayName = name
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Could not query display name: ${e.message}")
            }

            val rawBytes = contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: return Result.failure(IllegalArgumentException("Could not open file stream for selected document."))

            val isPdf = rawBytes.size >= 4 &&
                    rawBytes[0] == '%'.code.toByte() &&
                    rawBytes[1] == 'P'.code.toByte() &&
                    rawBytes[2] == 'D'.code.toByte() &&
                    rawBytes[3] == 'F'.code.toByte()

            val extractedText = if (isPdf) {
                extractFromPdf(rawBytes)
            } else {
                extractFromPlainText(rawBytes)
            }

            if (extractedText.isBlank()) {
                if (isPdf) {
                    Result.failure(
                        IllegalStateException(
                            "No selectable text found in this PDF. It may be a scanned image or encrypted. Please copy and paste the text directly into the manuscript editor."
                        )
                    )
                } else {
                    Result.failure(IllegalStateException("The uploaded document contains no readable text."))
                }
            } else {
                Result.success(
                    ExtractedDocument(
                        text = extractedText,
                        fileName = displayName,
                        isPdf = isPdf,
                        characterCount = extractedText.length
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting document text", e)
            Result.failure(e)
        }
    }

    private fun extractFromPlainText(bytes: ByteArray): String {
        return try {
            String(bytes, Charsets.UTF_8).trim()
        } catch (e: Exception) {
            String(bytes, Charsets.ISO_8859_1).trim()
        }
    }

    private fun extractFromPdf(bytes: ByteArray): String {
        val result = StringBuilder()
        val streamKeyword = "stream".toByteArray(Charsets.ISO_8859_1)
        val endstreamKeyword = "endstream".toByteArray(Charsets.ISO_8859_1)

        var searchIndex = 0
        while (searchIndex < bytes.size) {
            val streamStart = indexOf(bytes, streamKeyword, searchIndex)
            if (streamStart == -1) break

            // Skip "stream" and any immediate \r\n or \n
            var contentStart = streamStart + streamKeyword.size
            if (contentStart < bytes.size && bytes[contentStart] == '\r'.code.toByte()) contentStart++
            if (contentStart < bytes.size && bytes[contentStart] == '\n'.code.toByte()) contentStart++

            val streamEnd = indexOf(bytes, endstreamKeyword, contentStart)
            if (streamEnd == -1) break

            // Check dictionary preceding "stream" for /FlateDecode
            val dictHeaderStart = maxOf(0, streamStart - 400)
            val dictHeaderText = String(bytes, dictHeaderStart, streamStart - dictHeaderStart, Charsets.ISO_8859_1)
            val isFlateDecode = dictHeaderText.contains("/FlateDecode")

            val streamLength = streamEnd - contentStart
            if (streamLength > 0) {
                val streamBytes = bytes.copyOfRange(contentStart, streamEnd)
                val decompressedBytes = if (isFlateDecode) {
                    inflateBytes(streamBytes)
                } else {
                    streamBytes
                }

                if (decompressedBytes != null && decompressedBytes.isNotEmpty()) {
                    val streamText = extractTextFromStream(decompressedBytes)
                    if (streamText.isNotBlank()) {
                        if (result.isNotEmpty()) result.append("\n\n")
                        result.append(streamText)
                    }
                }
            }

            searchIndex = streamEnd + endstreamKeyword.size
        }

        // If stream-based parsing didn't find text, try parsing raw uncompressed text objects
        if (result.isBlank()) {
            val fallback = extractTextFromStream(bytes)
            if (fallback.isNotBlank()) {
                result.append(fallback)
            }
        }

        return result.toString().trim()
    }

    private fun inflateBytes(compressed: ByteArray): ByteArray? {
        // Try standard ZLIB wrapper
        try {
            val inflaterStream = InflaterInputStream(ByteArrayInputStream(compressed), Inflater(false))
            val out = ByteArrayOutputStream()
            val buf = ByteArray(4096)
            var n: Int
            while (inflaterStream.read(buf).also { n = it } > 0) {
                out.write(buf, 0, n)
            }
            return out.toByteArray()
        } catch (_: Exception) {}

        // Try raw deflate (no zlib header)
        try {
            val inflaterStream = InflaterInputStream(ByteArrayInputStream(compressed), Inflater(true))
            val out = ByteArrayOutputStream()
            val buf = ByteArray(4096)
            var n: Int
            while (inflaterStream.read(buf).also { n = it } > 0) {
                out.write(buf, 0, n)
            }
            return out.toByteArray()
        } catch (_: Exception) {}

        return null
    }

    private fun extractTextFromStream(bytes: ByteArray): String {
        val sb = StringBuilder()
        val text = String(bytes, Charsets.ISO_8859_1)

        // Find blocks between BT (Begin Text) and ET (End Text) or general Tj / TJ operators
        val btIndices = mutableListOf<Int>()
        var btPos = text.indexOf("BT")
        while (btPos != -1) {
            // Ensure BT is a standalone token
            val prevChar = if (btPos > 0) text[btPos - 1] else ' '
            val nextChar = if (btPos + 2 < text.length) text[btPos + 2] else ' '
            if (prevChar.isWhitespace() && nextChar.isWhitespace()) {
                btIndices.add(btPos)
            }
            btPos = text.indexOf("BT", btPos + 2)
        }

        if (btIndices.isNotEmpty()) {
            for (start in btIndices) {
                val end = text.indexOf("ET", start)
                val block = if (end != -1) text.substring(start, end) else text.substring(start, minOf(text.length, start + 2000))
                parsePdfTextBlock(block, sb)
            }
        } else {
            parsePdfTextBlock(text, sb)
        }

        return sb.toString().trim()
    }

    private fun parsePdfTextBlock(block: String, out: StringBuilder) {
        var i = 0
        val len = block.length

        while (i < len) {
            val char = block[i]
            if (char == '(') {
                // String literal: ( ... )
                val start = i + 1
                var depth = 1
                var escaped = false
                var j = start
                while (j < len && depth > 0) {
                    val c = block[j]
                    if (escaped) {
                        escaped = false
                    } else if (c == '\\') {
                        escaped = true
                    } else if (c == '(') {
                        depth++
                    } else if (c == ')') {
                        depth--
                    }
                    j++
                }
                val rawStr = block.substring(start, if (depth == 0) j - 1 else j)
                val decoded = decodePdfString(rawStr)
                if (decoded.isNotBlank()) {
                    out.append(decoded).append(" ")
                }
                i = j
            } else if (char == '[') {
                // Array of strings and kerning offsets: [(string) -10 (string)] TJ
                val start = i + 1
                val closeBracket = block.indexOf(']', start)
                if (closeBracket != -1) {
                    val arrayContent = block.substring(start, closeBracket)
                    parseArrayOfStrings(arrayContent, out)
                    i = closeBracket + 1
                } else {
                    i++
                }
            } else if (char == 'T' && i + 1 < len) {
                val next = block[i + 1]
                if (next == '*' || (next == 'd' || next == 'D')) {
                    // T*, Td, TD indicate newline or move to next line
                    out.append("\n")
                    i += 2
                } else {
                    i++
                }
            } else {
                i++
            }
        }
    }

    private fun parseArrayOfStrings(arrayContent: String, out: StringBuilder) {
        var k = 0
        val arrLen = arrayContent.length
        while (k < arrLen) {
            if (arrayContent[k] == '(') {
                val sStart = k + 1
                var depth = 1
                var escaped = false
                var m = sStart
                while (m < arrLen && depth > 0) {
                    val c = arrayContent[m]
                    if (escaped) {
                        escaped = false
                    } else if (c == '\\') {
                        escaped = true
                    } else if (c == '(') {
                        depth++
                    } else if (c == ')') {
                        depth--
                    }
                    m++
                }
                val raw = arrayContent.substring(sStart, if (depth == 0) m - 1 else m)
                val decoded = decodePdfString(raw)
                out.append(decoded)
                k = m
            } else {
                k++
            }
        }
        out.append(" ")
    }

    private fun decodePdfString(raw: String): String {
        val sb = StringBuilder()
        var i = 0
        val len = raw.length
        while (i < len) {
            val c = raw[i]
            if (c == '\\' && i + 1 < len) {
                val next = raw[i + 1]
                when (next) {
                    'n' -> { sb.append('\n'); i += 2 }
                    'r' -> { sb.append('\r'); i += 2 }
                    't' -> { sb.append('\t'); i += 2 }
                    'b' -> { sb.append('\b'); i += 2 }
                    'f' -> { sb.append('\u000C'); i += 2 }
                    '(' -> { sb.append('('); i += 2 }
                    ')' -> { sb.append(')'); i += 2 }
                    '\\' -> { sb.append('\\'); i += 2 }
                    in '0'..'7' -> {
                        // Octal character escape
                        var octalDigits = ""
                        var oi = i + 1
                        while (oi < len && oi < i + 4 && raw[oi] in '0'..'7') {
                            octalDigits += raw[oi]
                            oi++
                        }
                        val charCode = octalDigits.toIntOrNull(8) ?: 32
                        sb.append(charCode.toChar())
                        i = oi
                    }
                    else -> {
                        sb.append(next)
                        i += 2
                    }
                }
            } else {
                sb.append(c)
                i++
            }
        }
        return sb.toString()
    }

    private fun indexOf(source: ByteArray, target: ByteArray, fromIndex: Int = 0): Int {
        if (target.isEmpty() || source.size < target.size) return -1
        val max = source.size - target.size
        for (i in fromIndex..max) {
            var found = true
            for (j in target.indices) {
                if (source[i + j] != target[j]) {
                    found = false
                    break
                }
            }
            if (found) return i
        }
        return -1
    }
}
