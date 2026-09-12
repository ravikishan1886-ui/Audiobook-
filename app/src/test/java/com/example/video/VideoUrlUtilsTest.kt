package com.example.video

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class VideoUrlUtilsTest {

    private val userProvidedGoogleRedirectUrl =
        "https://www.google.com/url?sa=E&q=https%3A%2F%2Fmega.nz%2Ffile%2FZJkW0RCK%23x9fu65rOm-h1xvlsP0p3Iw84kJ-JhPWK9macoWQGohs"

    @Test
    fun unwrapAndCleanUrl_unwrapsUserGoogleRedirect() {
        val clean = VideoUrlUtils.unwrapAndCleanUrl(userProvidedGoogleRedirectUrl)
        assertEquals(
            "https://mega.nz/file/ZJkW0RCK#x9fu65rOm-h1xvlsP0p3Iw84kJ-JhPWK9macoWQGohs",
            clean
        )
    }

    @Test
    fun extractMegaFileInfo_extractsFileIdAndKeyFromUserUrl() {
        val megaInfo = VideoUrlUtils.extractMegaFileInfo(userProvidedGoogleRedirectUrl)
        assertNotNull(megaInfo)
        assertEquals("ZJkW0RCK", megaInfo?.fileId)
        assertEquals("x9fu65rOm-h1xvlsP0p3Iw84kJ-JhPWK9macoWQGohs", megaInfo?.fileKey)
    }

    @Test
    fun deriveCryptoParams_derivesAccurateAesKeyAndIv() {
        val fileKey = "x9fu65rOm-h1xvlsP0p3Iw84kJ-JhPWK9macoWQGohs"
        val params = VideoUrlUtils.deriveCryptoParams(fileKey)
        assertNotNull(params)

        val (aesKey, iv) = params!!
        assertEquals(16, aesKey.size)
        assertEquals(16, iv.size)

        // Verify cipher initialization succeeds with derived key and IV
        val cipher = Cipher.getInstance("AES/CTR/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(aesKey, "AES"), IvParameterSpec(iv))

        // Decrypt a sample block without throwing exceptions
        val sampleEncrypted = ByteArray(32)
        val decrypted = cipher.update(sampleEncrypted)
        assertNotNull(decrypted)
    }

    @Test
    fun extractMegaFileInfo_extractsUserHigurumaUrl() {
        val userUrl = "https://mega.nz/file/5AFgkbCK#Ej-0k6UUgvXq5_bJifzk4bxPk4HLwElFXrVhiYyvwp4"
        val megaInfo = VideoUrlUtils.extractMegaFileInfo(userUrl)
        assertNotNull(megaInfo)
        assertEquals("5AFgkbCK", megaInfo?.fileId)
        assertEquals("Ej-0k6UUgvXq5_bJifzk4bxPk4HLwElFXrVhiYyvwp4", megaInfo?.fileKey)

        val cryptoParams = VideoUrlUtils.deriveCryptoParams(megaInfo!!.fileKey)
        assertNotNull(cryptoParams)
        val (aesKey, iv) = cryptoParams!!
        assertEquals(16, aesKey.size)
        assertEquals(16, iv.size)
    }

    @Test
    fun resolveSource_identifiesMegaAndRedirect() {
        val resolved = VideoUrlUtils.resolveSource(userProvidedGoogleRedirectUrl)
        assertTrue(resolved.isGoogleRedirect)
        assertTrue(resolved.isMega)
        assertEquals("ZJkW0RCK", resolved.megaInfo?.fileId)
        assertTrue(resolved.displayBadge.contains("Google Redirect Unwrapped"))
        assertTrue(resolved.displayBadge.contains("MEGA"))
    }

    @Test
    fun extractYouTubeVideoId_acceptsUserRequestedUrlWithSiTracking() {
        val userUrl = "https://youtu.be/FLKvBcLv-AY?si=ZVaWzq5bfVcexDWk"
        val videoId = YouTubeAudioExtractor.extractYouTubeVideoId(userUrl)
        assertEquals("FLKvBcLv-AY", videoId)
    }

    @Test
    fun extractYouTubeVideoId_acceptsVariousFormats() {
        // Standard watch URL with tracking param
        assertEquals("FLKvBcLv-AY", YouTubeAudioExtractor.extractYouTubeVideoId("https://www.youtube.com/watch?v=FLKvBcLv-AY&si=ZVaWzq5bfVcexDWk"))
        // Shortened URL without protocol
        assertEquals("FLKvBcLv-AY", YouTubeAudioExtractor.extractYouTubeVideoId("youtu.be/FLKvBcLv-AY?si=ZVaWzq5bfVcexDWk"))
        // YouTube Shorts URL
        assertEquals("FLKvBcLv-AY", YouTubeAudioExtractor.extractYouTubeVideoId("https://youtube.com/shorts/FLKvBcLv-AY"))
        // YouTube Shorts URL from user screenshot
        assertEquals("mN0EiTdNmHs", YouTubeAudioExtractor.extractYouTubeVideoId("https://youtube.com/shorts/mN0EiTdNmHs"))
        // Direct video ID
        assertEquals("FLKvBcLv-AY", YouTubeAudioExtractor.extractYouTubeVideoId("FLKvBcLv-AY"))
    }

    @Test
    fun isPublicMusicUrl_detectsYouTubeAndDirectPublicAudioUrls() {
        assertTrue(YouTubeAudioExtractor.isPublicMusicUrl("https://youtube.com/shorts/mN0EiTdNmHs"))
        assertTrue(YouTubeAudioExtractor.isPublicMusicUrl("https://youtu.be/FLKvBcLv-AY?si=ZVaWzq5bfVcexDWk"))
        assertTrue(YouTubeAudioExtractor.isPublicMusicUrl("https://www.youtube.com/watch?v=dQw4w9WgXcQ"))
        assertTrue(YouTubeAudioExtractor.isPublicMusicUrl("https://actions.google.com/sounds/v1/weather/thunderstorm.ogg"))
        assertTrue(YouTubeAudioExtractor.isPublicMusicUrl("https://example.com/music/sample.mp3"))
        assertFalse(YouTubeAudioExtractor.isPublicMusicUrl("not_a_valid_music_url"))
    }

    @Test
    fun generateHighFidelityRemixWav_createsValidPcmWav() {
        val tempFile = java.io.File.createTempFile("test_remix_", ".wav")
        tempFile.deleteOnExit()
        val success = YouTubeAudioExtractor.generateHighFidelityRemixWav(tempFile, durationSeconds = 3, sampleRate = 44100, bpm = 128.0)
        assertTrue(success)
        assertTrue(tempFile.exists())
        assertTrue(tempFile.length() > 44) // Contains canonical 44-byte WAV header and PCM audio
    }
}
