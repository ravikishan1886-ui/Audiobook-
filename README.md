# AI Audiobook & Video Remix Studio

An Android application built with Jetpack Compose, Kotlin Coroutines, and Material Design 3.

[![Download APK](https://img.shields.io/badge/Download-APK%20(v1.0)-success?style=for-the-badge&logo=android)](./APK_DOWNLOAD/app-debug.apk?raw=true)

---

## 📲 Direct APK Download

The latest pre-compiled debug APK is available directly in this repository:

👉 **[Download Latest APK (`APK_DOWNLOAD/app-debug.apk`)](./APK_DOWNLOAD/app-debug.apk?raw=true)**

See **[`APK_DOWNLOAD.md`](./APK_DOWNLOAD.md)** for full release notes, SHA-256 checksums, and installation steps.

---

## ✨ Features

- **Video & Music Remixer**:
  - **Mega.nz & Google Redirect Support**: Paste any Google redirect wrapper or MEGA.nz link (e.g. `https://www.google.com/url?...&q=https://mega.nz/file/...#...`).
  - **On-The-Fly Decryption**: Automatic client-side AES-128-CTR decryption of MEGA cloud streams.
  - **Google Drive & Dropbox Support**: Direct link resolution.
  - **Timeline Synchronization**: Matches soundtrack duration with video duration using loop and fade logic.
  - **YouTube Direct Upload**: Integrated upload pipeline with privacy settings and metadata customization.
- **AI Audiobook Generator**:
  - AI script generation powered by Gemini API.
  - Expressive character speech synthesis with customizable voice profiles.
  - Multi-track background ambiance and chapter management.
  - Offline local audio caching.
- **Mobile-Optimized Interface**:
  - Material Design 3 adaptive theme with dynamic color support.
  - Ergonomic touch targets (48dp+) and scaled icons tailored for mobile devices.

---

## 🛠️ Building from Source

```bash
# Clone the repository
git clone <repo-url>
cd <repo-folder>

# Build the debug APK
gradle :app:assembleDebug
```

The output APK will be located at `app/build/outputs/apk/debug/app-debug.apk`.
