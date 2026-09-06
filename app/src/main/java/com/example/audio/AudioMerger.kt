package com.example.audio

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.core.content.FileProvider
import com.example.data.model.Chapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder

object AudioMerger {
    private const val TAG = "AudioMerger"

    suspend fun combineChaptersIntoAudiobook(
        context: Context,
        bookTitle: String,
        author: String,
        chapters: List<Chapter>
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val validChapterFiles = chapters
                .mapNotNull { it.audioFilePath }
                .map { File(it) }
                .filter { it.exists() && it.length() > 44 }

            if (validChapterFiles.isEmpty()) {
                return@withContext Result.failure(IllegalStateException("No valid chapter audio files found to merge."))
            }

            val sanitizedTitle = bookTitle.replace("[^a-zA-Z0-9.-]".toRegex(), "_").ifBlank { "Audiobook" }
            val outputDir = File(context.cacheDir, "completed_audiobooks")
            if (!outputDir.exists()) outputDir.mkdirs()
            val combinedFile = File(outputDir, "${sanitizedTitle}_FullAudiobook.wav")

            // Read the first file's header to determine format
            var sampleRate = 22050
            var channels = 1
            var bitsPerSample = 16

            val firstFile = validChapterFiles.first()
            FileInputStream(firstFile).use { input ->
                val header = ByteArray(44)
                input.read(header)
                val buffer = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN)
                buffer.position(22)
                channels = buffer.short.toInt()
                sampleRate = buffer.int
                buffer.position(34)
                bitsPerSample = buffer.short.toInt()
            }

            // Write combined raw PCM data
            val tempPcmFile = File(outputDir, "temp_raw.pcm")
            var totalPcmBytes = 0L

            FileOutputStream(tempPcmFile).use { pcmOut ->
                for (chapFile in validChapterFiles) {
                    FileInputStream(chapFile).use { chapIn ->
                        // Skip 44-byte WAV header
                        chapIn.skip(44)
                        val buffer = ByteArray(8192)
                        var bytesRead: Int
                        while (chapIn.read(buffer).also { bytesRead = it } != -1) {
                            pcmOut.write(buffer, 0, bytesRead)
                            totalPcmBytes += bytesRead
                        }
                    }
                    // Add 0.5s silence gap between chapters
                    val silenceSamples = (sampleRate * 0.5 * channels * (bitsPerSample / 8)).toInt()
                    val silenceBytes = ByteArray(silenceSamples)
                    pcmOut.write(silenceBytes)
                    totalPcmBytes += silenceBytes.size
                }
            }

            // Write final WAV with correct headers
            FileOutputStream(combinedFile).use { out ->
                val totalAudioLen = totalPcmBytes
                val totalDataLen = totalAudioLen + 36
                val byteRate = (sampleRate * channels * bitsPerSample / 8).toLong()

                val header = ByteArray(44)
                val buffer = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN)

                // RIFF
                buffer.put("RIFF".toByteArray())
                buffer.putInt(totalDataLen.toInt())
                buffer.put("WAVE".toByteArray())

                // fmt
                buffer.put("fmt ".toByteArray())
                buffer.putInt(16)
                buffer.putShort(1.toShort())
                buffer.putShort(channels.toShort())
                buffer.putInt(sampleRate)
                buffer.putInt(byteRate.toInt())
                buffer.putShort((channels * bitsPerSample / 8).toShort())
                buffer.putShort(bitsPerSample.toShort())

                // data
                buffer.put("data".toByteArray())
                buffer.putInt(totalAudioLen.toInt())

                out.write(header)

                // Copy PCM data
                FileInputStream(tempPcmFile).use { pcmIn ->
                    val buf = ByteArray(8192)
                    var r: Int
                    while (pcmIn.read(buf).also { r = it } != -1) {
                        out.write(buf, 0, r)
                    }
                }
            }

            tempPcmFile.delete()
            Result.success(combinedFile)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to merge chapters into audiobook", e)
            Result.failure(e)
        }
    }

    suspend fun saveAudioToDownloads(
        context: Context,
        audioFile: File,
        displayName: String,
        title: String,
        artist: String
    ): Result<Uri> = withContext(Dispatchers.IO) {
        try {
            val contentResolver = context.contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.Audio.Media.DISPLAY_NAME, displayName)
                put(MediaStore.Audio.Media.TITLE, title)
                put(MediaStore.Audio.Media.ARTIST, artist)
                put(MediaStore.Audio.Media.MIME_TYPE, "audio/wav")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Audio.Media.RELATIVE_PATH, Environment.DIRECTORY_MUSIC + "/Audiobooks")
                    put(MediaStore.Audio.Media.IS_PENDING, 1)
                }
            }

            val collectionUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            } else {
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            }

            val itemUri = contentResolver.insert(collectionUri, contentValues)
                ?: return@withContext Result.failure(IllegalStateException("Failed to create MediaStore entry"))

            contentResolver.openOutputStream(itemUri).use { outStream ->
                if (outStream == null) return@withContext Result.failure(IllegalStateException("Cannot open output stream"))
                FileInputStream(audioFile).use { inStream ->
                    inStream.copyTo(outStream)
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.Audio.Media.IS_PENDING, 0)
                contentResolver.update(itemUri, contentValues, null, null)
            }

            Result.success(itemUri)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save audio to downloads", e)
            Result.failure(e)
        }
    }

    fun shareAudioFile(context: Context, audioFile: File, title: String) {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                audioFile
            )
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "audio/wav"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, title)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share Audiobook: $title"))
        } catch (e: Exception) {
            Log.e(TAG, "Share failed, trying direct intent", e)
        }
    }
}
