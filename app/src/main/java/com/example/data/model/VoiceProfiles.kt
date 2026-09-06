package com.example.data.model

object VoiceProfiles {
    val availableVoices = listOf(
        VoiceProfile(
            id = "ballad_nyc_cabbie",
            name = "Ballad (NYC Cabbie)",
            gender = "Male",
            accent = "New York City",
            style = "Gruff, Fast-Talking & Quick-Witted",
            description = "Gruff, fast-talking New York cabbie with dropped 'r's, clipped cadence, dry humor, and no-nonsense efficiency.",
            pitch = 0.94f,
            speed = 1.15f,
            isCvoiceOfficial = true,
            openAiVoice = "ballad",
            instructions = "Voice: Gruff, fast-talking, and a little worn-out, like a New York cabbie who's seen it all but still keeps things moving.\n\nTone: Slightly exasperated but still functional, with a mix of sarcasm and no-nonsense efficiency.\n\nDialect: Strong New York accent, with dropped \"r\"s, sharp consonants, and classic phrases like whaddaya and lemme guess.\n\nPronunciation: Quick and clipped, with a rhythm that mimics the natural hustle of a busy city conversation.\n\nFeatures: Uses informal, straight-to-the-point language, throws in some dry humor, and keeps the energy just on the edge of impatience but still helpful."
        ),
        VoiceProfile(
            id = "cvoice_oliver_uk",
            name = "Oliver Vance",
            gender = "Male",
            accent = "British Received Pronunciation",
            style = "Warm, Classical & Literary",
            description = "Rich, measured baritone ideal for historical fiction, classics, and philosophical treatises.",
            pitch = 1.0f,
            speed = 1.0f
        ),
        VoiceProfile(
            id = "cvoice_clara_us",
            name = "Clara Sterling",
            gender = "Female",
            accent = "American (Mid-Atlantic)",
            style = "Expressive, Vivid & Dramatic",
            description = "Crisp, dynamic narrator capturing dialogue subtleties, emotional arcs, and modern fiction.",
            pitch = 1.05f,
            speed = 1.0f
        ),
        VoiceProfile(
            id = "cvoice_marcus_deep",
            name = "Marcus Drake",
            gender = "Male",
            accent = "American Deep",
            style = "Authoritative, Cinematic & Gripping",
            description = "Deep resonant timber perfect for thrillers, sci-fi, strategy, and documentary non-fiction.",
            pitch = 0.92f,
            speed = 0.95f
        ),
        VoiceProfile(
            id = "cvoice_evelyn_soft",
            name = "Evelyn Reed",
            gender = "Female",
            accent = "British Soft",
            style = "Gentle, Intimate & Poetic",
            description = "Soothing and melodic tone suited for memoirs, poetry, fantasy, and meditative prose.",
            pitch = 1.0f,
            speed = 0.92f
        ),
        VoiceProfile(
            id = "cvoice_arthur_vintage",
            name = "Arthur Pendelton",
            gender = "Male",
            accent = "Vintage Oxford",
            style = "Distinguished & Sophisticated",
            description = "The classic BBC/Victorian storyteller for detective mysteries and period drama.",
            pitch = 0.98f,
            speed = 1.02f
        ),
        VoiceProfile(
            id = "cvoice_aria_crisp",
            name = "Aria Nova",
            gender = "Female",
            accent = "Standard American",
            style = "Modern, Crisp & Engaging",
            description = "Vibrant, highly articulate cadence great for biographies, self-help, and educational texts.",
            pitch = 1.02f,
            speed = 1.05f
        )
    )
}
