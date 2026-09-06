package com.example

import com.example.data.model.VoiceProfiles
import com.example.data.samples.SampleBooks
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests verifying default voice configuration and samples.
 */
class ExampleUnitTest {
    @Test
    fun defaultVoice_isBalladNycCabbie() {
        val defaultVoice = VoiceProfiles.availableVoices.first()
        assertEquals("ballad_nyc_cabbie", defaultVoice.id)
        assertEquals("Ballad (NYC Cabbie)", defaultVoice.name)
        assertEquals("ballad", defaultVoice.openAiVoice)
        assertTrue(defaultVoice.instructions?.contains("New York cabbie") == true)
        assertTrue(defaultVoice.instructions?.contains("dropped \"r\"s") == true)
    }

    @Test
    fun defaultSample_isYeGrandInternetCompany() {
        val defaultSample = SampleBooks.samples.first()
        assertEquals("Ye Grand Internet Service Company", defaultSample.title)
        assertTrue(defaultSample.excerpt.contains("Ye Grand Internet Service Company"))
    }
}
