import express from 'express';
import cors from 'cors';
import multer from 'multer';
import path from 'path';
import fs from 'fs';
import { fileURLToPath } from 'url';
import { spawn, execFile } from 'child_process';
import dotenv from 'dotenv';

dotenv.config();

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

const app = express();
// The container nginx reverse proxies from port 8080 to internal port 3000
const PORT = process.env.APP_PORT || 3000;

// Directories
const UPLOADS_DIR = path.join(__dirname, 'uploads');
const EXPORTS_DIR = path.join(__dirname, 'exports');

if (!fs.existsSync(UPLOADS_DIR)) fs.mkdirSync(UPLOADS_DIR, { recursive: true });
if (!fs.existsSync(EXPORTS_DIR)) fs.mkdirSync(EXPORTS_DIR, { recursive: true });

// Middleware
app.use(cors());
app.use(express.json());
app.use(express.urlencoded({ extended: true }));

// Serve exports
app.use('/exports', express.static(EXPORTS_DIR));

// In-memory active jobs
const remixJobs = new Map();

// Allowed MIME types & extensions
const ALLOWED_VIDEO_EXTS = ['.mp4', '.mov', '.webm'];
const ALLOWED_AUDIO_EXTS = ['.mp3', '.wav', '.m4a'];

// Multer Storage Configuration
const storage = multer.diskStorage({
  destination: (_req, _file, cb) => cb(null, UPLOADS_DIR),
  filename: (_req, file, cb) => {
    const ext = path.extname(file.originalname).toLowerCase();
    const uniqueName = `${Date.now()}-${Math.random().toString(36).substring(2, 8)}${ext}`;
    cb(null, uniqueName);
  }
});

const upload = multer({
  storage,
  limits: {
    fileSize: 200 * 1024 * 1024 // 200MB max
  },
  fileFilter: (_req, file, cb) => {
    const ext = path.extname(file.originalname).toLowerCase();
    if (file.fieldname === 'video') {
      if (ALLOWED_VIDEO_EXTS.includes(ext)) {
        return cb(null, true);
      }
      return cb(new Error('Invalid video format. Only MP4, MOV, and WebM files are allowed.'));
    }
    if (file.fieldname === 'music') {
      if (ALLOWED_AUDIO_EXTS.includes(ext)) {
        return cb(null, true);
      }
      return cb(new Error('Invalid audio format. Only MP3, WAV, and M4A files are allowed.'));
    }
    cb(new Error('Unexpected upload field'));
  }
});

/**
 * YouTube URL Parser
 * Supports:
 * - https://www.youtube.com/watch?v=VIDEO_ID
 * - https://youtu.be/VIDEO_ID
 * - https://www.youtube.com/shorts/VIDEO_ID
 */
function extractYouTubeVideoId(url) {
  if (!url || typeof url !== 'string') return null;
  const trimmed = url.trim();

  // youtube.com/watch?v=VIDEO_ID
  const watchMatch = trimmed.match(/(?:https?:\/\/)?(?:www\.)?youtube\.com\/watch\?(?:.*&)?v=([a-zA-Z0-9_-]{11})/i);
  if (watchMatch) return watchMatch[1];

  // youtu.be/VIDEO_ID
  const shortMatch = trimmed.match(/(?:https?:\/\/)?youtu\.be\/([a-zA-Z0-9_-]{11})/i);
  if (shortMatch) return shortMatch[1];

  // youtube.com/shorts/VIDEO_ID
  const shortsMatch = trimmed.match(/(?:https?:\/\/)?(?:www\.)?youtube\.com\/shorts\/([a-zA-Z0-9_-]{11})/i);
  if (shortsMatch) return shortsMatch[1];

  // Direct 11-char ID
  if (/^[a-zA-Z0-9_-]{11}$/.test(trimmed)) {
    return trimmed;
  }

  return null;
}

/**
 * YouTube Data API v3 Metadata Endpoint
 * Credentials kept securely on server side
 */
app.get('/api/youtube-metadata', async (req, res) => {
  try {
    const { url, videoId: rawId } = req.query;
    const videoId = rawId || extractYouTubeVideoId(url);

    if (!videoId) {
      return res.status(400).json({
        error: 'Invalid YouTube URL. Please provide a valid URL (e.g., youtube.com/watch?v=..., youtu.be/..., or youtube.com/shorts/...)'
      });
    }

    const apiKey = process.env.YOUTUBE_API_KEY || process.env.GEMINI_API_KEY;

    // 1. If YouTube API Key is available, query YouTube Data API v3
    if (apiKey) {
      try {
        const apiUrl = `https://www.googleapis.com/youtube/v3/videos?part=snippet,contentDetails&id=${videoId}&key=${apiKey}`;
        const ytResponse = await fetch(apiUrl);
        if (ytResponse.ok) {
          const data = await ytResponse.json();
          if (data.items && data.items.length > 0) {
            const item = data.items[0];
            const snippet = item.snippet;
            const thumbnail =
              snippet.thumbnails?.maxres?.url ||
              snippet.thumbnails?.standard?.url ||
              snippet.thumbnails?.high?.url ||
              snippet.thumbnails?.medium?.url ||
              `https://i.ytimg.com/vi/${videoId}/hqdefault.jpg`;

            return res.json({
              videoId,
              title: snippet.title || 'Untitled Video',
              channelTitle: snippet.channelTitle || 'Unknown Channel',
              thumbnail
            });
          }
        }
      } catch (apiErr) {
        console.warn('YouTube Data API query failed, falling back to public metadata lookup:', apiErr.message);
      }
    }

    // 2. Fallback: Public oEmbed lookup (requires no API key, standard YouTube API endpoint)
    try {
      const oembedUrl = `https://www.youtube.com/oembed?url=https://www.youtube.com/watch?v=${videoId}&format=json`;
      const oembedRes = await fetch(oembedUrl, {
        headers: { 'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64)' }
      });
      if (oembedRes.ok) {
        const oembedData = await oembedRes.json();
        return res.json({
          videoId,
          title: oembedData.title || `YouTube Video (${videoId})`,
          channelTitle: oembedData.author_name || 'YouTube Creator',
          thumbnail: oembedData.thumbnail_url || `https://i.ytimg.com/vi/${videoId}/hqdefault.jpg`
        });
      }
    } catch (oembedErr) {
      console.warn('oEmbed lookup failed:', oembedErr.message);
    }

    // 3. Guaranteed metadata fallback with official YouTube HQ thumbnail
    return res.json({
      videoId,
      title: `YouTube Video (${videoId})`,
      channelTitle: 'YouTube Video',
      thumbnail: `https://i.ytimg.com/vi/${videoId}/hqdefault.jpg`
    });
  } catch (error) {
    console.error('Error fetching YouTube metadata:', error);
    res.status(500).json({ error: 'Internal error retrieving YouTube video metadata' });
  }
});

/**
 * Probe media file duration and streams using ffprobe
 */
function probeMedia(filePath) {
  return new Promise((resolve, reject) => {
    execFile(
      'ffprobe',
      [
        '-v', 'error',
        '-show_entries', 'format=duration:stream=codec_type,codec_name,width,height,r_frame_rate',
        '-of', 'json',
        filePath
      ],
      (err, stdout, _stderr) => {
        if (err) return reject(err);
        try {
          const info = JSON.parse(stdout);
          const duration = parseFloat(info.format?.duration || 0);
          const hasAudio = info.streams?.some(s => s.codec_type === 'audio') || false;
          const hasVideo = info.streams?.some(s => s.codec_type === 'video') || false;
          const videoStream = info.streams?.find(s => s.codec_type === 'video');
          resolve({
            duration,
            hasAudio,
            hasVideo,
            width: videoStream?.width,
            height: videoStream?.height,
            fps: videoStream?.r_frame_rate
          });
        } catch (parseErr) {
          reject(parseErr);
        }
      }
    );
  });
}

/**
 * Remix Video Endpoint
 */
app.post(
  '/api/remix',
  upload.fields([
    { name: 'video', maxCount: 1 },
    { name: 'music', maxCount: 1 }
  ]),
  async (req, res) => {
    const videoFile = req.files?.['video']?.[0];
    const musicFile = req.files?.['music']?.[0];

    if (!videoFile) {
      return res.status(400).json({ error: 'Please upload a video file (MP4, MOV, WebM)' });
    }
    if (!musicFile) {
      return res.status(400).json({ error: 'Please upload a licensed music file (MP3, WAV, M4A)' });
    }

    const jobId = `remix-${Date.now()}-${Math.random().toString(36).substring(2, 7)}`;
    const outputFilename = `${jobId}.mp4`;
    const outputPath = path.join(EXPORTS_DIR, outputFilename);

    // Parse options
    const musicVolume = Math.min(Math.max(parseFloat(req.body.musicVolume ?? 1.0), 0.0), 2.0);
    const videoVolume = Math.min(Math.max(parseFloat(req.body.videoVolume ?? 1.0), 0.0), 2.0);
    const replaceOriginalAudio = req.body.replaceOriginalAudio === 'true' || req.body.replaceOriginalAudio === true;
    const startTime = Math.max(parseFloat(req.body.startTime ?? 0), 0);
    const fadeIn = Math.max(parseFloat(req.body.fadeIn ?? 0), 0);
    const fadeOut = Math.max(parseFloat(req.body.fadeOut ?? 0), 0);
    const musicDurationMode = req.body.musicDurationMode || 'full_video';
    const customDuration = Math.max(parseFloat(req.body.customDuration ?? 0), 0);
    const loopMusic = req.body.loopMusic === 'true' || req.body.loopMusic === true;

    // Track job status
    remixJobs.set(jobId, {
      status: 'processing',
      step: 'Processing media files...',
      progress: 25,
      downloadUrl: null,
      error: null
    });

    try {
      // Step 1: Probe video & music
      const [videoProbe, musicProbe] = await Promise.all([
        probeMedia(videoFile.path),
        probeMedia(musicFile.path)
      ]);

      const videoDuration = videoProbe.duration || 10;
      const targetRemixDuration =
        musicDurationMode === 'custom' && customDuration > 0
          ? Math.min(customDuration, videoDuration)
          : videoDuration;

      remixJobs.set(jobId, {
        status: 'rendering',
        step: 'Rendering video with FFmpeg...',
        progress: 55,
        downloadUrl: null,
        error: null
      });

      // Step 2: Build FFmpeg arguments
      // Audio filter graph for music:
      // Volume + Fade in + Fade out
      const fadeOutStart = Math.max(0, targetRemixDuration - fadeOut);
      let musicFilter = `volume=${musicVolume.toFixed(2)}`;
      if (fadeIn > 0) {
        musicFilter += `,afade=t=in:ss=0:d=${fadeIn.toFixed(1)}`;
      }
      if (fadeOut > 0 && fadeOutStart > 0) {
        musicFilter += `,afade=t=out:st=${fadeOutStart.toFixed(1)}:d=${fadeOut.toFixed(1)}`;
      }

      const ffmpegArgs = [];

      // Input 0: Video
      ffmpegArgs.push('-i', videoFile.path);

      // Input 1: Music (with loop if requested)
      if (loopMusic && musicProbe.duration < targetRemixDuration) {
        ffmpegArgs.push('-stream_loop', '-1');
      }
      if (startTime > 0) {
        ffmpegArgs.push('-ss', startTime.toString());
      }
      ffmpegArgs.push('-i', musicFile.path);

      // Duration cap: match video or custom duration
      ffmpegArgs.push('-t', targetRemixDuration.toString());

      // Audio filtering and mixing
      if (replaceOriginalAudio || !videoProbe.hasAudio) {
        // Mute original video audio or video has no audio: use only licensed music
        ffmpegArgs.push(
          '-filter_complex', `[1:a]${musicFilter}[outa]`,
          '-map', '0:v:0',
          '-map', '[outa]'
        );
      } else {
        // Keep original video audio: mix original audio and licensed music
        const originalAudioFilter = `[0:a]volume=${videoVolume.toFixed(2)}[orig_a];`;
        const licensedAudioFilter = `[1:a]${musicFilter}[lic_a];`;
        const mixFilter = `[orig_a][lic_a]amix=inputs=2:duration=first:dropout_transition=2[outa]`;

        ffmpegArgs.push(
          '-filter_complex', `${originalAudioFilter}${licensedAudioFilter}${mixFilter}`,
          '-map', '0:v:0',
          '-map', '[outa]'
        );
      }

      // Codecs: copy video stream if container allows or encode H.264
      // Use libx264 with ultrafast/fast preset to preserve video frame rate and resolution flawlessly
      ffmpegArgs.push(
        '-c:v', 'libx264',
        '-preset', 'fast',
        '-crf', '19',
        '-pix_fmt', 'yuv420p',
        '-c:a', 'aac',
        '-b:a', '192k',
        '-movflags', '+faststart',
        '-y',
        outputPath
      );

      console.log('Executing FFmpeg with args:', ffmpegArgs.join(' '));

      // Execute FFmpeg
      await new Promise((resolve, reject) => {
        const ffmpegProc = spawn('ffmpeg', ffmpegArgs);

        ffmpegProc.stderr.on('data', (data) => {
          const str = data.toString();
          // Optionally extract time progress from FFmpeg output
          const timeMatch = str.match(/time=(\d+):(\d+):(\d+\.\d+)/);
          if (timeMatch) {
            const hours = parseFloat(timeMatch[1]);
            const minutes = parseFloat(timeMatch[2]);
            const seconds = parseFloat(timeMatch[3]);
            const currentTime = hours * 3600 + minutes * 60 + seconds;
            const pct = Math.min(95, Math.round(55 + (currentTime / targetRemixDuration) * 40));
            const currentJob = remixJobs.get(jobId);
            if (currentJob) {
              remixJobs.set(jobId, { ...currentJob, progress: pct });
            }
          }
        });

        ffmpegProc.on('close', (code) => {
          if (code === 0 && fs.existsSync(outputPath) && fs.statSync(outputPath).size > 1000) {
            resolve();
          } else {
            reject(new Error(`FFmpeg processing failed with exit code ${code}`));
          }
        });

        ffmpegProc.on('error', (err) => reject(err));
      });

      // Cleanup uploaded source files to save space
      try {
        if (fs.existsSync(videoFile.path)) fs.unlinkSync(videoFile.path);
        if (fs.existsSync(musicFile.path)) fs.unlinkSync(musicFile.path);
      } catch (cleanupErr) {
        console.warn('Temporary file cleanup error:', cleanupErr.message);
      }

      const fileStats = fs.statSync(outputPath);
      const downloadUrl = `/exports/${outputFilename}`;

      remixJobs.set(jobId, {
        status: 'complete',
        step: 'Remix complete!',
        progress: 100,
        downloadUrl,
        filename: outputFilename,
        size: fileStats.size,
        duration: targetRemixDuration,
        error: null
      });

      res.json({
        jobId,
        success: true,
        downloadUrl,
        filename: outputFilename,
        size: fileStats.size,
        duration: targetRemixDuration
      });
    } catch (err) {
      console.error('Error during video remix:', err);

      remixJobs.set(jobId, {
        status: 'error',
        step: 'Remix failed',
        progress: 0,
        downloadUrl: null,
        error: err.message || 'Video processing failed'
      });

      res.status(500).json({
        jobId,
        error: err.message || 'Video remix rendering failed. Please check input files.'
      });
    }
  }
);

/**
 * Job Status Endpoint
 */
app.get('/api/remix-status/:jobId', (req, res) => {
  const job = remixJobs.get(req.params.jobId);
  if (!job) {
    return res.status(404).json({ error: 'Job not found' });
  }
  res.json(job);
});

/**
 * APK Download Endpoints
 */
const APK_FILE_PATH = path.join(__dirname, 'APK_DOWNLOAD', 'app-debug.apk');
const handleApkDownload = (_req, res) => {
  if (fs.existsSync(APK_FILE_PATH)) {
    res.setHeader('Content-Type', 'application/vnd.android.package-archive');
    res.setHeader('Content-Disposition', 'attachment; filename="LicensedMusicRemix.apk"');
    return res.sendFile(APK_FILE_PATH);
  }
  return res.status(404).json({ error: 'APK file not found' });
};

app.get('/download-apk', handleApkDownload);
app.get('/api/download-apk', handleApkDownload);
app.get('/app-debug.apk', handleApkDownload);
app.use('/download', express.static(path.join(__dirname, 'APK_DOWNLOAD')));

// Serve frontend static build in production
const DIST_DIR = path.join(__dirname, 'dist');
if (fs.existsSync(DIST_DIR)) {
  app.use(express.static(DIST_DIR));
  app.get('*', (_req, res) => {
    res.sendFile(path.join(DIST_DIR, 'index.html'));
  });
}

// Start Server
app.listen(PORT, '0.0.0.0', () => {
  console.log(`Licensed Music Remix server listening on port ${PORT}`);
});
