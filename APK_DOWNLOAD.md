# 📲 Latest APK Download

### ⬇️ **[Direct Download `app-debug.apk`](./APK_DOWNLOAD/app-debug.apk?raw=true)**
- **Alternative Root Link**: [`./app-debug.apk`](./app-debug.apk?raw=true)
- **Web Server Endpoint**: `/api/download-apk` or `/download-apk`

---

## 📦 Build Information

| Attribute | Details |
| :--- | :--- |
| **File Name** | `app-debug.apk` |
| **Locations** | [`APK_DOWNLOAD/app-debug.apk`](./APK_DOWNLOAD/app-debug.apk) & [`app-debug.apk`](./app-debug.apk) & [`public/app-debug.apk`](./public/app-debug.apk) |
| **File Size** | ~23.24 MB (24,367,746 bytes) |
| **SHA-256 Checksum** | `4eb7afed08ea85d3f8db802f74beabe0d6f631a349ad305ac190b66db8e7ea4c` |
| **Build Type** | Debug (Ready to install on Android 7.0+ / API 24–36) |
| **Target Architecture** | ARM64 / ARMv7 / x86 / x86_64 |

---

## 🚀 What's Included in This Updated Build

### 1. 🤖 AI-Powered YouTube Video Automation & Google Sheets Queue
- **10-Column Google Sheets Integration (`A`–`J`)**:
  - `A`: Video URL
  - `B`: Video Title
  - `C`: Video Description
  - `D`: Thumbnail URL (optional)
  - `E`: Music Selection
  - `F`: Music Volume
  - `G`: Upload Status (`READY`, `PROCESSING`, `UPLOADED`, `FAILED`)
  - `H`: Scheduled Upload Time
  - `I`: YouTube Video URL
  - `J`: Error Message
- **Automated Sequential Queue Engine**:
  - Reads `READY` rows, updates status to `PROCESSING`, downloads video, mixes uploaded music with FFmpeg/MediaMuxer, uploads to YouTube Data API v3, and writes back `UPLOADED` + the resulting YouTube URL to Column `I` (or `FAILED` + reason to Column `J`).
  - Prevents duplicate uploads unless explicitly retried via **RETRY FAILED JOBS**.
- **Complete Mobile Dashboard Controls**:
  - `CONNECT GOOGLE SHEETS`, `CONNECT YOUTUBE`, `UPLOAD MUSIC`, `REFRESH SHEET`, `START AUTOMATION`, `PAUSE QUEUE`, `STOP AUTOMATION`, `RETRY FAILED JOBS`, and `VIEW UPLOADED VIDEOS`.

### 2. 🎵 Upload Music Button Only & 100% Identical Uploaded Music Sync
- **Music URL Input Removed**: Removed external music URL input boxes so music selection is streamlined exclusively to the **UPLOAD MUSIC** button and your persistent Music Library (`MP3`, `WAV`, `AAC`, `M4A`).
- **Uploaded Music Must Be Same**: Preserves the uploaded music track's exact original audio fidelity, sample rate, and pitch while synchronizing duration 1:1 with the target video.
- **Default Anime/Donghua Shorts Description**:
  ```text
  🔥 Anime lovers, this moment was INSANE! ⚡
  Watch till the end and tell me your favorite scene! 👀🔥

  🎬 Anime/Donghua Shorts | Hindi/Urdu Explanation
  💬 Comment your thoughts below!
  ❤️ Like • 🔔 Subscribe • Share

  #anime #animeedit #animeshorts #donghua #donghuashorts #animeclips #shorts #viralshorts #hindianime #animefans
  ```

---

## 📲 Installation Instructions

1. **Download the APK**: Click on [`APK_DOWNLOAD/app-debug.apk`](./APK_DOWNLOAD/app-debug.apk?raw=true) or tap the **Download APK** button in the top navigation bar of the web app.
2. **Allow Installation from Unknown Sources**:
   - If prompted by your Android browser or file manager, tap **Settings** and enable **"Allow from this source"**.
3. **Install & Open**: Tap **Install**, then tap **Open** to launch the app.
