package com.example.video

import android.content.ClipData
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.media.*
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
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder

object VideoGenerator {
    private const val TAG = "VideoGenerator"
    private const val VIDEO_WIDTH = 1280
    private const val VIDEO_HEIGHT = 720
    private const val FRAME_RATE = 2 // 2 fps is optimal for static background audiobook video
    private const val BIT_RATE = 1_000_000 // 1 Mbps
    private const val I_FRAME_INTERVAL = 1

    private fun createSafeVideoEncoder(): MediaCodec {
        return try {
            val codecList = MediaCodecList(MediaCodecList.REGULAR_CODECS)
            val videoFormatPrototype = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, VIDEO_WIDTH, VIDEO_HEIGHT)
            val videoEncoderName = codecList.findEncoderForFormat(videoFormatPrototype)
            if (videoEncoderName != null) {
                MediaCodec.createByCodecName(videoEncoderName)
            } else {
                MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
            }
        } catch (_: Exception) {
            MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
        }
    }

    private fun createSafeAudioEncoder(audioFormat: MediaFormat): MediaCodec {
        return try {
            val codecList = MediaCodecList(MediaCodecList.REGULAR_CODECS)
            val audioEncoderName = codecList.findEncoderForFormat(audioFormat)
            if (audioEncoderName != null) {
                MediaCodec.createByCodecName(audioEncoderName)
            } else {
                MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)
            }
        } catch (_: Exception) {
            MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)
        }
    }

    @Suppress("DEPRECATION")
    private fun selectColorFormat(codecInfo: MediaCodecInfo, mimeType: String): Pair<Int, Boolean> {
        return try {
            val capabilities = codecInfo.getCapabilitiesForType(mimeType)
            val formats = capabilities.colorFormats
            when {
                formats.contains(MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar) ->
                    Pair(MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar, true)
                formats.contains(MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar) ->
                    Pair(MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar, false)
                formats.contains(MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible) ->
                    Pair(MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible, true)
                else -> {
                    val fallback = formats.firstOrNull() ?: MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar
                    Pair(fallback, true)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Color format query notice: ${e.message}")
            Pair(MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar, true)
        }
    }

    private fun convertBitmapToYuv420(bitmap: Bitmap, width: Int, height: Int, isNv12: Boolean): ByteArray {
        val ySize = width * height
        val uvSize = (width * height) / 4
        val yuv = ByteArray(ySize + uvSize * 2)

        val argb = IntArray(width * height)
        bitmap.getPixels(argb, 0, width, 0, 0, width, height)

        var yIndex = 0
        var uIndex = ySize
        var vIndex = ySize + uvSize
        var uvIndex = ySize

        for (j in 0 until height) {
            for (i in 0 until width) {
                val pixel = argb[j * width + i]
                val r = (pixel shr 16) and 0xff
                val g = (pixel shr 8) and 0xff
                val b = pixel and 0xff

                // ITU-R BT.601 formula
                val y = ((66 * r + 129 * g + 25 * b + 128) shr 8) + 16
                yuv[yIndex++] = y.coerceIn(0, 255).toByte()

                if (j % 2 == 0 && i % 2 == 0) {
                    val u = ((-38 * r - 74 * g + 112 * b + 128) shr 8) + 128
                    val v = ((112 * r - 94 * g - 18 * b + 128) shr 8) + 128
                    if (isNv12) {
                        yuv[uvIndex++] = u.coerceIn(0, 255).toByte()
                        yuv[uvIndex++] = v.coerceIn(0, 255).toByte()
                    } else {
                        yuv[uIndex++] = u.coerceIn(0, 255).toByte()
                        yuv[vIndex++] = v.coerceIn(0, 255).toByte()
                    }
                }
            }
        }
        return yuv
    }

    /**
     * Generates a branded cover image bitmap for the audiobook video.
     * If a custom thumbnail file is provided, renders that image into the cover showcase card.
     */
    fun createCoverBitmap(
        bookTitle: String,
        author: String,
        voiceName: String,
        chaptersCount: Int,
        customThumbnailFile: File? = null
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(VIDEO_WIDTH, VIDEO_HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val customThumbBitmap = if (customThumbnailFile != null && customThumbnailFile.exists()) {
            try {
                BitmapFactory.decodeFile(customThumbnailFile.absolutePath)
            } catch (e: Exception) {
                Log.w(TAG, "Could not decode custom thumbnail: ${e.message}")
                null
            }
        } else null

        // Background Gradient: Deep rich literary aesthetic
        val bgPaint = Paint().apply {
            shader = LinearGradient(
                0f, 0f, VIDEO_WIDTH.toFloat(), VIDEO_HEIGHT.toFloat(),
                intArrayOf(
                    Color.rgb(20, 24, 33),   // #141821 deep obsidian
                    Color.rgb(38, 28, 44),   // #261C2C plum slate
                    Color.rgb(15, 17, 26)    // #0F111A midnight
                ),
                floatArrayOf(0f, 0.5f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, VIDEO_WIDTH.toFloat(), VIDEO_HEIGHT.toFloat(), bgPaint)

        // If custom thumbnail is present, draw a subtle blurred ambient glow in background
        if (customThumbBitmap != null) {
            val ambientPaint = Paint(Paint.FILTER_BITMAP_FLAG).apply {
                alpha = 45
            }
            val src = Rect(0, 0, customThumbBitmap.width, customThumbBitmap.height)
            val dst = Rect(0, 0, VIDEO_WIDTH, VIDEO_HEIGHT)
            canvas.drawBitmap(customThumbBitmap, src, dst, ambientPaint)
        }

        // Decorative subtle grid / accent circles
        val circlePaint = Paint().apply {
            color = Color.argb(35, 230, 175, 46) // warm amber glow
            isAntiAlias = true
            maskFilter = BlurMaskFilter(120f, BlurMaskFilter.Blur.NORMAL)
        }
        canvas.drawCircle(VIDEO_WIDTH * 0.8f, VIDEO_HEIGHT * 0.25f, 260f, circlePaint)
        canvas.drawCircle(VIDEO_WIDTH * 0.2f, VIDEO_HEIGHT * 0.85f, 220f, circlePaint)

        // Left Book Cover Card Mockup
        val bookCardLeft = 90f
        val bookCardTop = 80f
        val bookCardRight = 450f
        val bookCardBottom = 640f
        val bookCardRect = RectF(bookCardLeft, bookCardTop, bookCardRight, bookCardBottom)

        // Book Cover Shadow
        val shadowPaint = Paint().apply {
            color = Color.argb(120, 0, 0, 0)
            maskFilter = BlurMaskFilter(24f, BlurMaskFilter.Blur.NORMAL)
        }
        canvas.drawRoundRect(RectF(bookCardLeft + 10f, bookCardTop + 15f, bookCardRight + 15f, bookCardBottom + 15f), 24f, 24f, shadowPaint)

        // Book Cover / Custom Thumbnail Rendering
        if (customThumbBitmap != null) {
            val clipPath = Path().apply {
                addRoundRect(bookCardRect, 24f, 24f, Path.Direction.CW)
            }
            canvas.save()
            canvas.clipPath(clipPath)
            val srcRect = Rect(0, 0, customThumbBitmap.width, customThumbBitmap.height)
            canvas.drawBitmap(customThumbBitmap, srcRect, bookCardRect, Paint(Paint.FILTER_BITMAP_FLAG))
            canvas.restore()

            // Custom cover gold border accent
            val goldBorder = Paint().apply {
                color = Color.rgb(245, 197, 66)
                strokeWidth = 3f
                style = Paint.Style.STROKE
                isAntiAlias = true
            }
            canvas.drawRoundRect(bookCardRect, 24f, 24f, goldBorder)
        } else {
            val bookPaint = Paint().apply {
                shader = LinearGradient(
                    bookCardLeft, bookCardTop, bookCardRight, bookCardBottom,
                    intArrayOf(Color.rgb(180, 83, 9), Color.rgb(120, 53, 15), Color.rgb(67, 20, 7)),
                    null,
                    Shader.TileMode.CLAMP
                )
                isAntiAlias = true
            }
            canvas.drawRoundRect(bookCardRect, 24f, 24f, bookPaint)

            // Book Spine Highlight
            val spinePaint = Paint().apply {
                color = Color.argb(70, 255, 255, 255)
                strokeWidth = 6f
                style = Paint.Style.STROKE
                isAntiAlias = true
            }
            canvas.drawLine(bookCardLeft + 24f, bookCardTop + 20f, bookCardLeft + 24f, bookCardBottom - 20f, spinePaint)

            // Book Gold Border Accent
            val goldBorder = Paint().apply {
                color = Color.rgb(245, 197, 66)
                strokeWidth = 2f
                style = Paint.Style.STROKE
                isAntiAlias = true
            }
            canvas.drawRoundRect(RectF(bookCardLeft + 16f, bookCardTop + 16f, bookCardRight - 16f, bookCardBottom - 16f), 16f, 16f, goldBorder)

            // Book Cover Inner Texts
            val bookTitlePaint = Paint().apply {
                color = Color.WHITE
                textSize = 28f
                typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
                isAntiAlias = true
                textAlign = Paint.Align.CENTER
            }

            // Wrap title inside cover
            val coverTitleLines = wrapText(bookTitle, bookTitlePaint, (bookCardRight - bookCardLeft - 50f))
            var coverTextY = bookCardTop + 180f
            for (line in coverTitleLines.take(3)) {
                canvas.drawText(line, (bookCardLeft + bookCardRight) / 2f, coverTextY, bookTitlePaint)
                coverTextY += 36f
            }

            val bookAuthorPaint = Paint().apply {
                color = Color.rgb(245, 197, 66)
                textSize = 20f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("by $author", (bookCardLeft + bookCardRight) / 2f, bookCardBottom - 60f, bookAuthorPaint)
        }

        // Top Badge: Complete Audiobook
        val badgeBgPaint = Paint().apply {
            color = Color.argb(180, 245, 197, 66)
            isAntiAlias = true
        }
        val badgeRect = RectF(500f, 85f, 740f, 125f)
        canvas.drawRoundRect(badgeRect, 12f, 12f, badgeBgPaint)

        val badgeTextPaint = Paint().apply {
            color = Color.rgb(20, 24, 33)
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("OFFICIAL AUDIOBOOK", 620f, 112f, badgeTextPaint)

        // Right side Main Title
        val mainTitlePaint = Paint().apply {
            color = Color.WHITE
            textSize = 48f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
            isAntiAlias = true
        }

        val rightTitleLines = wrapText(bookTitle, mainTitlePaint, 680f)
        var rightTextY = 200f
        for (line in rightTitleLines.take(2)) {
            canvas.drawText(line, 500f, rightTextY, mainTitlePaint)
            rightTextY += 58f
        }

        // Author Name Subtitle
        val mainAuthorPaint = Paint().apply {
            color = Color.rgb(220, 220, 220)
            textSize = 28f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
        }
        canvas.drawText("Written by $author", 500f, rightTextY + 10f, mainAuthorPaint)

        // Divider
        val divPaint = Paint().apply {
            color = Color.argb(100, 255, 255, 255)
            strokeWidth = 1.5f
        }
        canvas.drawLine(500f, rightTextY + 40f, 1180f, rightTextY + 40f, divPaint)

        // Voice & Features Info Box
        val infoY = rightTextY + 90f
        val infoPaint = Paint().apply {
            color = Color.rgb(245, 197, 66)
            textSize = 22f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        canvas.drawText("• Narrated by $voiceName (Neural Voice)", 500f, infoY, infoPaint)

        val subInfoPaint = Paint().apply {
            color = Color.rgb(180, 190, 205)
            textSize = 20f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
        }
        canvas.drawText("• Unabridged Edition · $chaptersCount Chapter(s)", 500f, infoY + 38f, subInfoPaint)
        canvas.drawText("• High Fidelity Master Narration · Studio Audio", 500f, infoY + 76f, subInfoPaint)

        // Bottom Watermark / Visualizer branding
        val footerPaint = Paint().apply {
            color = Color.argb(140, 255, 255, 255)
            textSize = 16f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
        }
        canvas.drawText("Generated with AI Audiobook Studio · YouTube High Definition", 500f, VIDEO_HEIGHT - 60f, footerPaint)

        return bitmap
    }

    private fun wrapText(text: String, paint: Paint, maxWidth: Float): List<String> {
        val words = text.split(" ")
        val lines = mutableListOf<String>()
        var currentLine = StringBuilder()

        for (word in words) {
            val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
            val width = paint.measureText(testLine)
            if (width > maxWidth && currentLine.isNotEmpty()) {
                lines.add(currentLine.toString())
                currentLine = StringBuilder(word)
            } else {
                currentLine = StringBuilder(testLine)
            }
        }
        if (currentLine.isNotEmpty()) {
            lines.add(currentLine.toString())
        }
        return lines
    }

    /**
     * Converts a full audiobook WAV file + cover bitmap into an MP4 video file.
     * Uses Android MediaCodec and MediaMuxer hardware accelerated pipeline.
     */
    suspend fun convertAudioToMp4(
        context: Context,
        audioWavFile: File,
        bookTitle: String,
        author: String,
        voiceName: String,
        chaptersCount: Int,
        customThumbnailFile: File? = null,
        onProgress: (Float) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            if (!audioWavFile.exists()) {
                return@withContext Result.failure(IllegalArgumentException("Audio file does not exist"))
            }

            val sanitizedTitle = bookTitle.replace("[^a-zA-Z0-9.-]".toRegex(), "_").ifBlank { "Audiobook" }
            val outputDir = File(context.cacheDir, "youtube_videos")
            if (!outputDir.exists()) outputDir.mkdirs()
            val outputFile = File(outputDir, "${sanitizedTitle}_YouTube.mp4")
            if (outputFile.exists()) outputFile.delete()

            // Calculate duration of audio file from WAV header
            var sampleRate = 22050
            var channels = 1
            var bitsPerSample = 16
            var pcmDataBytes = (audioWavFile.length() - 44).coerceAtLeast(0L)

            FileInputStream(audioWavFile).use { input ->
                val header = ByteArray(44)
                input.read(header)
                val buffer = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN)
                buffer.position(22)
                channels = buffer.short.toInt().coerceAtLeast(1)
                sampleRate = buffer.int.coerceAtLeast(8000)
                buffer.position(34)
                bitsPerSample = buffer.short.toInt().coerceAtLeast(16)
            }

            val bytesPerSecond = sampleRate * channels * (bitsPerSample / 8)
            val durationUs = (pcmDataBytes * 1_000_000L / bytesPerSecond.coerceAtLeast(1)).coerceAtLeast(1_000_000L)
            val totalFrames = ((durationUs / 1_000_000.0) * FRAME_RATE).toLong().coerceAtLeast(1L)
            val frameDurationUs = 1_000_000L / FRAME_RATE

            Log.i(TAG, "Encoding MP4 video: durationUs=$durationUs, frames=$totalFrames, audioSize=$pcmDataBytes")

            var videoEncoder: MediaCodec? = null
            var audioEncoder: MediaCodec? = null
            var muxer: MediaMuxer? = null
            var coverBitmap: Bitmap? = null
            var pcmInputStream: FileInputStream? = null
            var muxerStarted = false

            try {
                // 1. Generate Cover Bitmap & YUV frame
                coverBitmap = createCoverBitmap(bookTitle, author, voiceName, chaptersCount, customThumbnailFile)

                // 2. Set up Video Encoder (AVC / H.264)
                val vEncoder = createSafeVideoEncoder()
                videoEncoder = vEncoder

                val (colorFormat, isNv12) = selectColorFormat(vEncoder.codecInfo, MediaFormat.MIMETYPE_VIDEO_AVC)
                val yuvFrame = convertBitmapToYuv420(coverBitmap, VIDEO_WIDTH, VIDEO_HEIGHT, isNv12)

                val videoFormat = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, VIDEO_WIDTH, VIDEO_HEIGHT).apply {
                    setInteger(MediaFormat.KEY_COLOR_FORMAT, colorFormat)
                    setInteger(MediaFormat.KEY_BIT_RATE, BIT_RATE)
                    setInteger(MediaFormat.KEY_FRAME_RATE, FRAME_RATE)
                    setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, I_FRAME_INTERVAL)
                    setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, VIDEO_WIDTH * VIDEO_HEIGHT * 2)
                }
                vEncoder.configure(videoFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
                vEncoder.start()

                // 3. Set up Audio Encoder (AAC)
                val validSampleRates = setOf(8000, 11025, 12000, 16000, 22050, 24000, 32000, 44100, 48000)
                val aacSampleRate = if (validSampleRates.contains(sampleRate)) sampleRate else 22050
                val aacBitrate = 64_000 // Standard reliable bitrate for mono speech
                val audioFormat = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, aacSampleRate, channels).apply {
                    setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
                    setInteger(MediaFormat.KEY_BIT_RATE, aacBitrate)
                    setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 16384)
                }

                val aEncoder = createSafeAudioEncoder(audioFormat)
                audioEncoder = aEncoder

                aEncoder.configure(audioFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
                aEncoder.start()

                // 4. Set up MediaMuxer with synchronized sample queue
                val mMuxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
                muxer = mMuxer
                var videoTrackIndex = -1
                var audioTrackIndex = -1

                class EncodedSample(
                    val isVideo: Boolean,
                    val data: ByteArray,
                    val ptsUs: Long,
                    val flags: Int
                )
                val sampleQueue = mutableListOf<EncodedSample>()

                fun flushSamples(drainAll: Boolean = false) {
                    if (!muxerStarted) return
                    sampleQueue.sortBy { it.ptsUs }

                    val thresholdUs = if (drainAll) {
                        Long.MAX_VALUE
                    } else {
                        val latestV = sampleQueue.lastOrNull { it.isVideo }?.ptsUs
                        val latestA = sampleQueue.lastOrNull { !it.isVideo }?.ptsUs
                        if (latestV != null && latestA != null) {
                            minOf(latestV, latestA)
                        } else {
                            return
                        }
                    }

                    val iter = sampleQueue.iterator()
                    while (iter.hasNext()) {
                        val item = iter.next()
                        if (item.ptsUs <= thresholdUs) {
                            val track = if (item.isVideo) videoTrackIndex else audioTrackIndex
                            val buf = ByteBuffer.wrap(item.data)
                            val info = MediaCodec.BufferInfo().apply {
                                set(0, item.data.size, item.ptsUs, item.flags)
                            }
                            try {
                                mMuxer.writeSampleData(track, buf, info)
                            } catch (e: Exception) {
                                Log.w(TAG, "Sample write notice: ${e.message}")
                            }
                            iter.remove()
                        } else {
                            break
                        }
                    }
                }

                fun tryStartMuxer() {
                    if (!muxerStarted && videoTrackIndex >= 0 && audioTrackIndex >= 0) {
                        try {
                            mMuxer.start()
                            muxerStarted = true
                            flushSamples(drainAll = false)
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to start MediaMuxer", e)
                        }
                    }
                }

                // 5. Interleaved feed & drain loop
                val pcmStream = FileInputStream(audioWavFile)
                pcmInputStream = pcmStream
                pcmStream.skip(44) // Skip WAV header

                val pcmChunk = ByteArray(2048) // 1024 16-bit mono samples
                val bytesPerSample = (channels * (bitsPerSample / 8)).coerceAtLeast(1)
                var audioBytesQueued = 0L
                var audioEosQueued = false
                var audioDone = false

                var videoFramesQueued = 0L
                var videoEosQueued = false
                var videoDone = false

                val vBufferInfo = MediaCodec.BufferInfo()
                val aBufferInfo = MediaCodec.BufferInfo()

                var loopIdleIterations = 0
                while ((!videoDone || !audioDone) && loopIdleIterations < 1500) {
                    var didWork = false

                    // Calculate upcoming presentation timestamps for lockstep feeding
                    val nextVideoPtsUs = videoFramesQueued * frameDurationUs
                    val nextAudioPtsUs = (audioBytesQueued * 1_000_000L) / (sampleRate.toLong() * bytesPerSample.toLong()).coerceAtLeast(1L)

                    // 1. Feed Video Frame if it's video's turn
                    if (!videoEosQueued && (nextVideoPtsUs <= nextAudioPtsUs || audioEosQueued)) {
                        val inIndex = vEncoder.dequeueInputBuffer(2000)
                        if (inIndex >= 0) {
                            val inBuf = vEncoder.getInputBuffer(inIndex)
                            if (inBuf != null) {
                                inBuf.clear()
                                if (videoFramesQueued < totalFrames) {
                                    val bytesToPut = minOf(yuvFrame.size, inBuf.remaining())
                                    inBuf.put(yuvFrame, 0, bytesToPut)
                                    val pts = videoFramesQueued * frameDurationUs
                                    vEncoder.queueInputBuffer(inIndex, 0, bytesToPut, pts, 0)
                                    videoFramesQueued++
                                } else {
                                    val pts = videoFramesQueued * frameDurationUs
                                    vEncoder.queueInputBuffer(inIndex, 0, 0, pts, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                                    videoEosQueued = true
                                }
                                didWork = true
                            }
                        }
                    }

                    // 2. Feed Audio Chunk if it's audio's turn
                    if (!audioEosQueued && (nextAudioPtsUs <= nextVideoPtsUs || videoEosQueued)) {
                        val inIndex = aEncoder.dequeueInputBuffer(2000)
                        if (inIndex >= 0) {
                            val inBuf = aEncoder.getInputBuffer(inIndex)
                            if (inBuf != null) {
                                inBuf.clear()
                                val bytesRead = pcmStream.read(pcmChunk)
                                if (bytesRead <= 0) {
                                    val pts = audioBytesQueued * 1_000_000L / (sampleRate * bytesPerSample).coerceAtLeast(1)
                                    aEncoder.queueInputBuffer(inIndex, 0, 0, pts, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                                    audioEosQueued = true
                                } else {
                                    inBuf.put(pcmChunk, 0, bytesRead)
                                    val pts = audioBytesQueued * 1_000_000L / (sampleRate * bytesPerSample).coerceAtLeast(1)
                                    aEncoder.queueInputBuffer(inIndex, 0, bytesRead, pts, 0)
                                    audioBytesQueued += bytesRead
                                }
                                didWork = true
                            }
                        }
                    }

                    // 3. Drain Video Output
                    var vOutIndex = vEncoder.dequeueOutputBuffer(vBufferInfo, 2000)
                    while (vOutIndex >= 0 || vOutIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                        didWork = true
                        if (vOutIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                            if (videoTrackIndex < 0) {
                                videoTrackIndex = mMuxer.addTrack(vEncoder.outputFormat)
                                tryStartMuxer()
                            }
                        } else {
                            val outBuf = vEncoder.getOutputBuffer(vOutIndex)
                            if (outBuf != null && (vBufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) == 0 && vBufferInfo.size > 0) {
                                val bytes = ByteArray(vBufferInfo.size)
                                outBuf.position(vBufferInfo.offset)
                                outBuf.get(bytes)
                                sampleQueue.add(EncodedSample(isVideo = true, data = bytes, ptsUs = vBufferInfo.presentationTimeUs, flags = vBufferInfo.flags))
                                flushSamples(drainAll = false)
                            }
                            if ((vBufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                                videoDone = true
                            }
                            vEncoder.releaseOutputBuffer(vOutIndex, false)
                        }
                        vOutIndex = vEncoder.dequeueOutputBuffer(vBufferInfo, 0)
                    }

                    // 4. Drain Audio Output
                    var aOutIndex = aEncoder.dequeueOutputBuffer(aBufferInfo, 2000)
                    while (aOutIndex >= 0 || aOutIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                        didWork = true
                        if (aOutIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                            if (audioTrackIndex < 0) {
                                audioTrackIndex = mMuxer.addTrack(aEncoder.outputFormat)
                                tryStartMuxer()
                            }
                        } else {
                            val outBuf = aEncoder.getOutputBuffer(aOutIndex)
                            if (outBuf != null && (aBufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) == 0 && aBufferInfo.size > 0) {
                                val bytes = ByteArray(aBufferInfo.size)
                                outBuf.position(aBufferInfo.offset)
                                outBuf.get(bytes)
                                sampleQueue.add(EncodedSample(isVideo = false, data = bytes, ptsUs = aBufferInfo.presentationTimeUs, flags = aBufferInfo.flags))
                                flushSamples(drainAll = false)
                            }
                            if ((aBufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                                audioDone = true
                            }
                            aEncoder.releaseOutputBuffer(aOutIndex, false)
                        }
                        aOutIndex = aEncoder.dequeueOutputBuffer(aBufferInfo, 0)
                    }

                    if (didWork) {
                        loopIdleIterations = 0
                    } else {
                        loopIdleIterations++
                    }

                    val vidProg = (videoFramesQueued.toFloat() / totalFrames.toFloat()).coerceIn(0f, 1f)
                    val audProg = (audioBytesQueued.toFloat() / pcmDataBytes.coerceAtLeast(1L).toFloat()).coerceIn(0f, 1f)
                    onProgress((vidProg * 0.4f + audProg * 0.6f).coerceIn(0f, 0.99f))
                }

                // Final flush of any pending samples
                if (muxerStarted) {
                    flushSamples(drainAll = true)
                }
            } finally {
                try { pcmInputStream?.close() } catch (_: Exception) {}
                if (muxerStarted) {
                    try { muxer?.stop() } catch (e: Exception) { Log.w(TAG, "Muxer stop notice: ${e.message}") }
                }
                try { muxer?.release() } catch (_: Exception) {}
                try { videoEncoder?.stop() } catch (_: Exception) {}
                try { videoEncoder?.release() } catch (_: Exception) {}
                try { audioEncoder?.stop() } catch (_: Exception) {}
                try { audioEncoder?.release() } catch (_: Exception) {}
                if (coverBitmap?.isRecycled == false) {
                    try { coverBitmap?.recycle() } catch (_: Exception) {}
                }
            }

            if (outputFile.exists() && outputFile.length() > 500) {
                onProgress(1.0f)
                Log.i(TAG, "Video creation complete: ${outputFile.absolutePath} (${outputFile.length()} bytes)")
                Result.success(outputFile)
            } else {
                Result.failure(IllegalStateException("Generated video file is missing or invalid."))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to convert audio to MP4", e)
            Result.failure(e)
        }
    }

    /**
     * Saves the rendered MP4 video file to the device's Movies/Audiobooks media collection.
     */
    suspend fun saveVideoToMovies(
        context: Context,
        videoFile: File,
        displayName: String,
        title: String
    ): Result<Uri> = withContext(Dispatchers.IO) {
        try {
            val contentResolver = context.contentResolver
            val safeName = displayName.replace("[^a-zA-Z0-9._-]".toRegex(), "_")
            val finalFileName = if (safeName.endsWith(".mp4", ignoreCase = true)) safeName else "$safeName.mp4"

            val contentValues = ContentValues().apply {
                put(MediaStore.Video.Media.DISPLAY_NAME, finalFileName)
                put(MediaStore.Video.Media.TITLE, title)
                put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/Audiobooks")
                    put(MediaStore.Video.Media.IS_PENDING, 1)
                }
            }

            val collectionUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            } else {
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            }

            val itemUri = contentResolver.insert(collectionUri, contentValues)
                ?: return@withContext Result.failure(IllegalStateException("Failed to create MediaStore Video entry"))

            contentResolver.openOutputStream(itemUri).use { outStream ->
                if (outStream == null) return@withContext Result.failure(IllegalStateException("Cannot open output stream for video"))
                FileInputStream(videoFile).use { inStream ->
                    inStream.copyTo(outStream)
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.Video.Media.IS_PENDING, 0)
                contentResolver.update(itemUri, contentValues, null, null)
            }

            Log.i(TAG, "Saved video to Movies: $itemUri")
            Result.success(itemUri)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save video to Movies", e)
            Result.failure(e)
        }
    }

    /**
     * Shares the MP4 video directly with the YouTube App or system chooser,
     * pre-filling the title, description, and chapter markers.
     */
    fun shareVideoFile(
        context: Context,
        videoFile: File,
        title: String,
        description: String,
        targetYouTubeApp: Boolean = true
    ): Result<Unit> {
        return try {
            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                videoFile
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "video/mp4"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                putExtra(Intent.EXTRA_SUBJECT, title)
                putExtra(Intent.EXTRA_TITLE, title)
                putExtra(Intent.EXTRA_TEXT, description)
                clipData = ClipData.newRawUri("Video", contentUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            if (targetYouTubeApp) {
                val ytIntent = Intent(shareIntent).apply {
                    setPackage("com.google.android.youtube")
                }
                try {
                    context.startActivity(ytIntent)
                    Result.success(Unit)
                } catch (e: Exception) {
                    Log.w(TAG, "YouTube app not installed directly, falling back to system chooser", e)
                    val chooser = Intent.createChooser(shareIntent, "Publish Video: $title").apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(chooser)
                    Result.success(Unit)
                }
            } else {
                val chooser = Intent.createChooser(shareIntent, "Share Video: $title").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(chooser)
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to share video file", e)
            Result.failure(e)
        }
    }

    /**
     * Directly triggers YouTube upload without requiring any developer token or API key.
     * 1. Copies title and description to clipboard for instant access.
     * 2. Checks if YouTube app is installed. If installed, launches YouTube's upload activity directly.
     * 3. If YouTube app is not installed (e.g. streaming emulator or AOSP), opens official YouTube Web Upload (https://www.youtube.com/upload) in the browser.
     */
    fun launchDirectYouTubeUpload(
        context: Context,
        videoFile: File,
        title: String,
        description: String
    ): Result<String> {
        return try {
            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                videoFile
            )

            // Copy metadata to clipboard as convenient backup
            try {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
                val clip = ClipData.newPlainText("YouTube Description", description)
                clipboard?.setPrimaryClip(clip)
            } catch (e: Exception) {
                Log.w(TAG, "Could not copy description to clipboard", e)
            }

            // Check if YouTube app is installed
            val pm = context.packageManager
            val ytInstalled = try {
                pm.getPackageInfo("com.google.android.youtube", 0) != null
            } catch (_: Exception) {
                false
            }

            if (ytInstalled) {
                val ytIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "video/mp4"
                    setPackage("com.google.android.youtube")
                    putExtra(Intent.EXTRA_STREAM, contentUri)
                    putExtra(Intent.EXTRA_SUBJECT, title)
                    putExtra(Intent.EXTRA_TITLE, title)
                    putExtra(Intent.EXTRA_TEXT, description)
                    clipData = ClipData.newRawUri("Audiobook Video", contentUri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(ytIntent)
                Result.success("https://www.youtube.com/upload")
            } else {
                // Open YouTube Web Upload Studio in browser
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/upload")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(browserIntent)
                Result.success("https://www.youtube.com/upload")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch YouTube upload", e)
            Result.failure(e)
        }
    }
}
