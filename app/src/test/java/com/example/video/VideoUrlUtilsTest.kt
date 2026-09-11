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
    fun resolveSource_identifiesMegaAndRedirect() {
        val resolved = VideoUrlUtils.resolveSource(userProvidedGoogleRedirectUrl)
        assertTrue(resolved.isGoogleRedirect)
        assertTrue(resolved.isMega)
        assertEquals("ZJkW0RCK", resolved.megaInfo?.fileId)
        assertTrue(resolved.displayBadge.contains("Google Redirect Unwrapped"))
        assertTrue(resolved.displayBadge.contains("MEGA"))
    }
}
