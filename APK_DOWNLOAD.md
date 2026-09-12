# 📲 Latest APK Download

### **[Direct Download `app-debug.apk`](./APK_DOWNLOAD/app-debug.apk?raw=true)**

---

## 📦 Build Information

| Attribute | Details |
| :--- | :--- |
| **File Name** | `app-debug.apk` |
| **Location** | [`APK_DOWNLOAD/app-debug.apk`](./APK_DOWNLOAD/app-debug.apk) |
| **File Size** | ~22 MB |
| **SHA-256 Checksum** | `b161f6156b51bfcf3c0e6ec459e4a42ceb357dacb28dd66304f6a05d3a391ce8` |
| **Build Type** | Debug (Ready to install on Android 7.0+ / API 24–36) |
| **Target Architecture** | ARM64 / ARMv7 / x86_64 |

---

## 🚀 What's New in This Build

### 1. 🎵 YouTube Music URL Upload
- **Direct YouTube Music Extraction**: Enter or paste any YouTube or YouTube Music link (e.g. `https://www.youtube.com/watch?v=...`, `https://youtu.be/...`, or `https://music.youtube.com/watch?v=...`).
- **High-Fidelity Audio Stream Extraction**: Decodes the YouTube audio stream directly into pristine PCM audio using hardware `MediaCodec` and mixes it with your video.
- **One-Tap Presets & Fast Paste**: Quick sample chips for instant testing plus clipboard paste and clear controls.
- **Automatic Metadata Detection**: Displays the YouTube track title, channel/artist name, and duration directly in the remix audio card.

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
