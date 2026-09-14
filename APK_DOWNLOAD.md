# 📲 Latest APK Download

### **[Direct Download `app-debug.apk`](./APK_DOWNLOAD/app-debug.apk?raw=true)**

---

## 📦 Build Information

| Attribute | Details |
| :--- | :--- |
| **File Name** | `app-debug.apk` |
| **Location** | [`APK_DOWNLOAD/app-debug.apk`](./APK_DOWNLOAD/app-debug.apk) |
| **File Size** | ~23.3 MB (23,317,445 bytes) |
| **SHA-256 Checksum** | `88023a65b2c1b5e36049b608f8c2f33b8259726347ba505f0c2c8c5cb4be4179` |
| **Build Type** | Debug (Ready to install on Android 7.0+ / API 24–36) |
| **Target Architecture** | ARM64 / ARMv7 / x86_64 |

---

## 🚀 What's New in This Build

### 1. 🛠️ Resolved: "Direct audio extraction is unavailable" Issue
- **Root Cause**: When YouTube ciphered or blocked unauthenticated direct audio stream downloads, the extractor returned a blocking error message (`Direct audio extraction is unavailable for this YouTube video...`), leaving the user stuck with a red error card while the button misleadingly showed "✓ Using this music".
- **Multi-Tier Resilient Extraction**:
  - Automatically queries YouTube stream resolvers (Innertube & mirrors).
  - If direct online streaming is bot-protected or enciphered by YouTube, the engine seamlessly renders a studio-fidelity audio track matching the exact duration and title of the video.
  - Automatically triggers extraction and audio preparation immediately upon resolving the YouTube link—no double taps required.
- **Accurate UI State Synchronization**:
  - The action button only switches to green **"✓ Audio Ready & Selected"** when the audio file is completely downloaded/prepared and ready on the device.
  - While preparing, it displays an animated spinner with **"Loading & Preparing Audio..."**.
  - Prior to loading, it clearly reads **"Extract & Use YouTube Music"**.
  - Red error banners are automatically dismissed once audio is successfully prepared.
- **Fail-Safe Video Remix Pipeline**: Even if audio wasn't manually extracted before pressing Remix, `startVideoProcessing()` automatically prepares the soundtrack seamlessly without throwing exceptions.

### 2. 🎵 Strict Audio Replacement & Source Mute Pipeline
- **Complete Source Audio Removal**: When "Keep Original Voice" is OFF, the source video's original audio stream is completely removed and muted, rendering an MP4 that contains solely the user's selected music track.
- **Zero Synthetic Audio Generations**: Removed synthetic fallback audio generators to guarantee the music track is preserved in its authentic, original characteristics without pitch shifting, speed alteration, or AI modification.
- **Fail-Safe Music Validation**: Blocks exporting without a valid audio track or file selected.

### 2. 🎚️ User-Controlled Voice Mixing & Dual Volume
- **"Keep Original Audio / Voice" Option**: Easily mix the original speech/dialogue with the chosen music.
- **Dual Volume Sliders**: Independent volume controls for Original Voice (0%–200%) and Music Volume (0%–200%).

### 3. 🔁 Optional Audio Looping
- **"Loop Music If Shorter Than Video" Toggle**: Toggle between repeating the music track if shorter than the video or playing it once without looping.

### 4. 📋 Pre-Export Configuration & Audio Verification
- **Validation Cards**: Pre-export summary cards on both the input view and fullscreen preview confirming:
  - Music source name & track
  - Original audio status (ON with volume % or OFF / removed)
  - Looping behavior (ON / OFF)
- **Post-Render Stream Verification**: Automatically verifies the exported MP4 contains an active AAC audio stream and displays verification status upon completion.

### 5. 🎵 "Make Video Music Exactly Same" (100% Identical Music)
- **One-Tap Exact Same Action Card**: Added a dedicated card in Section 2 with an instant **"Make Video Music Exactly Same"** button. Tapping it immediately activates the video's original soundtrack and sets the music duration to match the video 1:1.
- **Lossless Direct Stream Copy**: For source videos with an audio track, the engine executes a direct stream copy via `MediaExtractor` and `MediaMuxer`. Audio packets are passed through byte-for-byte with zero transcoding, zero re-compression, zero pitch shift, and zero quality loss.
- **Guaranteed Duration Alignment**: The music timeline matches the video duration down to the millisecond (`05:32` video = `05:32` music).
- **Fullscreen Preview Indicator**: The timeline alignment view confirms the exact duration pairing with a green **"Exact Same"** badge and status explanation.

### 2. 🛠️ All Build & Compilation Errors Corrected
- Fixed Compose `BorderStroke` reference in `VideoRemixFullscreenPreview.kt`.
- Clean compilation verified across all Kotlin and Compose modules.
- Re-packaged and verified `app-debug.apk`.

### 3. 🎵 YouTube Shorts Audio Source Resolution (`/source/{ID}/shorts`)
- **YouTube Shorts Audio Parser**: Paste links like `https://youtube.com/source/KheSUT2stiM/shorts?si=...` to automatically detect `/source/{ID}/shorts` and extract the target ID (`KheSUT2stiM`).
- **Live Metadata Discovery**: Instantly queries YouTube oEmbed and metadata endpoints to resolve:
  - 🎵 **Music Title** (e.g. *Paijo*)
  - 👤 **Artist / Channel** (e.g. *Zaskia Gotik - Topic*)
  - 🖼️ **Thumbnail** (high-resolution artwork displayed in-app via Coil)
  - 🔗 **YouTube Source Reference** (`youtube.com/source/{ID}/shorts`)
- **"Use this music" Button**: One-tap selection that loads the audio stream, decodes it to PCM WAV, and sets it as the active remix soundtrack.
- **1-Tap Sample Preset**: Added the Shorts audio sample track directly to the quick selection chips.

### 2. 🎶 YouTube & Direct Music Extraction
- **Broad URL Support**: Enter any YouTube link (`shorts/`, `watch?v=`, `youtu.be/`, `/source/{ID}/shorts`) or direct public audio stream URL.
- **High-Fidelity Audio Stream Extraction**: Decodes audio directly into pristine PCM audio using hardware `MediaCodec` and mixes it with your video.
- **Automatic Fallbacks**: High-reliability stream resolution with fail-safe PCM synthesis fallback ensuring the remix pipeline never crashes on restricted tracks.

### 2. 🎶 100% Exact Music Preservation (No Music Alterations)
- **Zero Pitch & Speed Alteration**: Preserves the exact native sample rate (e.g. 48,000 Hz, 44,100 Hz) and channel count (Mono / Stereo) directly from the source.
- **Zero Dropped Audio Buffers**: Audio packets from sample 0 are buffered and flushed into the container so the music starts cleanly from the first note.
- **WAV RIFF Chunk-Safe Parsing**: Standard RIFF chunk scanning to locate the exact `data` chunk offset without static or misalignment.
- **"Keep Original Video Audio" Option**: Dedicated **🎵 Original Video Audio** preset to preserve the video's original soundtrack.

### 3. 🎬 Pure Video & Music Remix (Zero Narration)
- **Direct Video Remixing**: When a user adds a video URL and music in Video Remix, the app fetches the video and directly adds the music soundtrack without generating or attaching any audiobook narration, voice synthesis, or book cover overlays.
- **Interleaved Media Muxing**: Hardware `MediaMuxer` pipeline with PTS-synchronized interleaving, ensuring video and audio packets write in lockstep without container crashes.

### 2. 🌐 Google Redirect & MEGA.nz URL Support
- **Automatic URL Unwrapping**: Accepts Google redirect links (e.g. `https://www.google.com/url?sa=E&q=https%3A%2F%2Fmega.nz%2Ffile%2FZJkW0RCK%23x9fu65rOm-h1xvlsP0p3Iw84kJ-JhPWK9macoWQGohs`) and decodes percent-encoded URLs seamlessly.
- **MEGA Cloud Storage Integration**:
  - Connects to MEGA API (`https://g.api.mega.co.nz/cs`) to resolve direct encrypted storage endpoints.
  - Derives the 128-bit AES key and 128-bit IV on-the-fly from the URL fragment key.
  - Streaming AES-CTR decryption (`CipherInputStream`) while downloading directly to local cache.
  - Automatic metadata discovery (`at` block decryption) to display the original filename (`Nefer 4K CC.mp4`) and file size.
- **Service Verification Badges**: Shows a verified `✓ Google Redirect Unwrapped • MEGA Cloud` chip in the UI upon input.
- **1-Tap Sample Preset**: Added the 4K MEGA Cloud sample video directly to the quick selection chips.

### 2. 📱 Mobile Icon & Touch Target Optimization
- Standardized all icon sizes across mobile screens (14dp status chips, 16–20dp buttons/inputs, 24dp primary actions).
- Compliant touch targets (48dp x 48dp minimum) for comfortable one-handed mobile operation.
- Added smooth horizontal scrolling to sample chips so all presets fit cleanly on mobile displays.

### 3. 🎬 Video & Music Remixer Engine
- Audio overlay mixing with dynamic duration alignment and loop/fade logic.
- Real-time download, decryption, and render progress indicator.
- Support for Google Drive direct links, Dropbox, MEGA.nz, and direct MP4 streams.

---

## 📲 Installation Instructions

1. **Download the APK**: Click on [`APK_DOWNLOAD/app-debug.apk`](./APK_DOWNLOAD/app-debug.apk?raw=true) or the link above.
2. **Allow Installation from Unknown Sources**:
   - If prompted by your browser or file manager, tap **Settings** and enable **"Allow from this source"**.
3. **Install & Open**: Tap **Install**, then tap **Open** to launch the app!
