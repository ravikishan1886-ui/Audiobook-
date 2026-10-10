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
const PORT = process.env.APP_PORT || 3000;

// Directories
const UPLOADS_DIR = path.join(__dirname, 'uploads');
const MUSIC_DIR = path.join(UPLOADS_DIR, 'music');
const VIDEOS_DIR = path.join(UPLOADS_DIR, 'videos');
const EXPORTS_DIR = path.join(__dirname, 'exports');
const DATA_DIR = path.join(__dirname, 'data');
const APK_DIR = path.join(__dirname, 'APK_DOWNLOAD');
const APK_FILE = path.join(APK_DIR, 'app-debug.apk');

[UPLOADS_DIR, MUSIC_DIR, VIDEOS_DIR, EXPORTS_DIR, DATA_DIR, APK_DIR].forEach((dir) => {
  if (!fs.existsSync(dir)) fs.mkdirSync(dir, { recursive: true });
});

// Middleware
app.use(cors());
app.use(express.json({ limit: '50mb' }));
app.use(express.urlencoded({ extended: true, limit: '50mb' }));

// Static asset serving
app.use('/exports', express.static(EXPORTS_DIR));
app.use('/uploads/music', express.static(MUSIC_DIR));
app.use('/uploads/videos', express.static(VIDEOS_DIR));

// File storage helpers
function readJson(file, defaultVal) {
  try {
    const fullPath = path.join(DATA_DIR, file);
    if (!fs.existsSync(fullPath)) return defaultVal;
    const content = fs.readFileSync(fullPath, 'utf-8');
    return JSON.parse(content);
  } catch (err) {
    console.error(`Error reading ${file}:`, err.message);
    return defaultVal;
  }
}

function writeJson(file, data) {
  try {
    const fullPath = path.join(DATA_DIR, file);
    fs.writeFileSync(fullPath, JSON.stringify(data, null, 2), 'utf-8');
  } catch (err) {
    console.error(`Error writing ${file}:`, err.message);
  }
}

// Default settings
const DEFAULT_ANIME_DESCRIPTION = `🔥 Anime lovers, this moment was INSANE! ⚡
Watch till the end and tell me your favorite scene! 👀🔥

🎬 Anime/Donghua Shorts | Hindi/Urdu Explanation
💬 Comment your thoughts below!
❤️ Like • 🔔 Subscribe • Share

#anime #animeedit #animeshorts #donghua #donghuashorts #animeclips #shorts #viralshorts #hindianime #animefans`;

const DEFAULT_SETTINGS = {
  defaultDescription: DEFAULT_ANIME_DESCRIPTION,
  musicVolume: 0.8,
  videoVolume: 1.0,
  replaceOriginalAudio: false,
  fadeIn: 1.0,
  fadeOut: 1.5,
  videoPrivacy: 'public', // 'public' | 'unlisted' | 'private'
  retentionHours: 24,
  autoStartOnReady: false
};

// Initialize settings
if (!fs.existsSync(path.join(DATA_DIR, 'settings.json'))) {
  writeJson('settings.json', DEFAULT_SETTINGS);
}

// Initialize music library
let musicLibrary = readJson('music-library.json', []);
if (musicLibrary.length === 0) {
  const sampleTrackPath = path.join(MUSIC_DIR, 'sample_anime_beat.mp3');
  if (fs.existsSync(sampleTrackPath)) {
    const stats = fs.statSync(sampleTrackPath);
    musicLibrary = [
      {
        id: 'sample_anime_beat',
        filename: 'sample_anime_beat.mp3',
        originalName: 'Cyberpunk Anime Lo-Fi Beat (Royalty Free).mp3',
        duration: 30,
        size: stats.size,
        uploadedAt: new Date().toISOString(),
        isDefault: true,
        url: '/uploads/music/sample_anime_beat.mp3'
      }
    ];
    writeJson('music-library.json', musicLibrary);
  }
}

// Initialize jobs / queue
let automationJobs = readJson('automation-jobs.json', []);
if (automationJobs.length === 0) {
  // Pre-fill a demo job so the user can test immediately with 1 click!
  automationJobs = [
    {
      id: 'job-1',
      rowIndex: 2,
      videoUrl: '/uploads/videos/sample_anime_clip.mp4',
      videoTitle: 'Anime Fight Scene #1 [Epic Remaster]',
      videoDescription: DEFAULT_ANIME_DESCRIPTION,
      thumbnailUrl: '',
      musicSelection: 'Cyberpunk Anime Lo-Fi Beat (Royalty Free).mp3',
      musicVolume: 0.8,
      uploadStatus: 'READY',
      scheduledUploadTime: '',
      youtubeVideoUrl: '',
      errorMessage: '',
      downloadUrl: '',
      updatedAt: new Date().toISOString()
    }
  ];
  writeJson('automation-jobs.json', automationJobs);
}

let queueState = readJson('queue-state.json', {
  status: 'idle', // 'idle' | 'running' | 'paused' | 'stopped'
  currentJobId: null,
  currentJobTitle: null,
  currentStep: null,
  progress: 0,
  youtubeUploadProgress: 0,
  lastUpdated: new Date().toISOString()
});

// Resume recoverable jobs on startup if server crashed mid-job
if (queueState.status === 'running') {
  queueState.status = 'idle';
  queueState.currentStep = 'System ready after restart';
  writeJson('queue-state.json', queueState);
}

// Multer upload configurations
const audioStorage = multer.diskStorage({
  destination: (_req, _file, cb) => cb(null, MUSIC_DIR),
  filename: (_req, file, cb) => {
    const ext = path.extname(file.originalname).toLowerCase();
    const safeName = path.basename(file.originalname, ext).replace(/[^a-zA-Z0-9_-]/g, '_');
    const unique = `${Date.now()}-${safeName}${ext}`;
    cb(null, unique);
  }
});

const uploadMusic = multer({
  storage: audioStorage,
  limits: { fileSize: 100 * 1024 * 1024 }, // 100MB
  fileFilter: (_req, file, cb) => {
    const ext = path.extname(file.originalname).toLowerCase();
    const allowed = ['.mp3', '.wav', '.aac', '.m4a'];
    if (allowed.includes(ext)) {
      return cb(null, true);
    }
    cb(new Error('Only MP3, WAV, AAC, and M4A audio files are allowed.'));
  }
});

const videoStorage = multer.diskStorage({
  destination: (_req, _file, cb) => cb(null, VIDEOS_DIR),
  filename: (_req, file, cb) => {
    const ext = path.extname(file.originalname).toLowerCase();
    const safeName = path.basename(file.originalname, ext).replace(/[^a-zA-Z0-9_-]/g, '_');
    const unique = `${Date.now()}-${safeName}${ext}`;
    cb(null, unique);
  }
});

const uploadVideo = multer({
  storage: videoStorage,
  limits: { fileSize: 500 * 1024 * 1024 } // 500MB
});

// Probe media utility
function probeMedia(filePath) {
  return new Promise((resolve, reject) => {
    execFile(
      'ffprobe',
      [
        '-v', 'error',
        '-show_entries', 'format=duration:stream=codec_type,codec_name,width,height,r_frame_rate,sample_rate',
        '-of', 'json',
        filePath
      ],
      (err, stdout) => {
        if (err) return reject(err);
        try {
          const info = JSON.parse(stdout);
          const duration = parseFloat(info.format?.duration || 0);
          const hasAudio = info.streams?.some((s) => s.codec_type === 'audio') || false;
          const hasVideo = info.streams?.some((s) => s.codec_type === 'video') || false;
          const videoStream = info.streams?.find((s) => s.codec_type === 'video');
          const audioStream = info.streams?.find((s) => s.codec_type === 'audio');
          resolve({
            duration,
            hasAudio,
            hasVideo,
            width: videoStream?.width,
            height: videoStream?.height,
            fps: videoStream?.r_frame_rate,
            sampleRate: audioStream?.sample_rate
          });
        } catch (e) {
          reject(e);
        }
      }
    );
  });
}

// Download remote video stream
async function downloadRemoteVideo(url, targetPath) {
  const parsed = new URL(url);
  // Basic SSRF protection
  if (parsed.protocol !== 'http:' && parsed.protocol !== 'https:') {
    throw new Error('Unsupported protocol. Only HTTP and HTTPS URLs are allowed.');
  }
  const hostname = parsed.hostname.toLowerCase();
  if (hostname === 'localhost' || hostname === '127.0.0.1' || hostname.startsWith('10.') || hostname.startsWith('192.168.')) {
    throw new Error('Private and loopback addresses cannot be downloaded.');
  }

  const response = await fetch(url, {
    headers: {
      'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36'
    }
  });

  if (!response.ok) {
    throw new Error(`Failed to download video: HTTP status ${response.status} (${response.statusText})`);
  }

  const fileStream = fs.createWriteStream(targetPath);
  const reader = response.body.getReader();

  return new Promise(async (resolve, reject) => {
    try {
      while (true) {
        const { done, value } = await reader.read();
        if (done) break;
        fileStream.write(Buffer.from(value));
      }
      fileStream.end(resolve);
    } catch (streamErr) {
      fileStream.destroy();
      reject(streamErr);
    }
  });
}

// Mix Video and Audio using FFmpeg
async function mixVideoWithMusic({
  videoPath,
  musicPath,
  outputPath,
  musicVolume = 0.8,
  videoVolume = 1.0,
  replaceOriginalAudio = false,
  fadeIn = 1.0,
  fadeOut = 1.5,
  onProgress
}) {
  const [videoProbe, musicProbe] = await Promise.all([
    probeMedia(videoPath),
    probeMedia(musicPath)
  ]);

  const targetDuration = videoProbe.duration || 10;
  const loopMusic = musicProbe.duration < targetDuration;

  // Audio filter for music
  const fadeOutStart = Math.max(0, targetDuration - fadeOut);
  let musicFilter = `volume=${musicVolume.toFixed(2)}`;
  if (fadeIn > 0) {
    musicFilter += `,afade=t=in:ss=0:d=${fadeIn.toFixed(1)}`;
  }
  if (fadeOut > 0 && fadeOutStart > 0) {
    musicFilter += `,afade=t=out:st=${fadeOutStart.toFixed(1)}:d=${fadeOut.toFixed(1)}`;
  }

  const ffmpegArgs = [];
  // Input 0: Video
  ffmpegArgs.push('-i', videoPath);

  // Input 1: Music
  if (loopMusic) {
    ffmpegArgs.push('-stream_loop', '-1');
  }
  ffmpegArgs.push('-i', musicPath);

  // Trim to video length
  ffmpegArgs.push('-t', targetDuration.toString());

  if (replaceOriginalAudio || !videoProbe.hasAudio) {
    // Only use background music
    ffmpegArgs.push(
      '-filter_complex', `[1:a]${musicFilter}[outa]`,
      '-map', '0:v:0',
      '-map', '[outa]'
    );
  } else {
    // Mix video's original speech with music
    const origFilter = `[0:a]volume=${videoVolume.toFixed(2)}[orig_a];`;
    const licFilter = `[1:a]${musicFilter}[lic_a];`;
    const mixFilter = `[orig_a][lic_a]amix=inputs=2:duration=first:dropout_transition=2[outa]`;

    ffmpegArgs.push(
      '-filter_complex', `${origFilter}${licFilter}${mixFilter}`,
      '-map', '0:v:0',
      '-map', '[outa]'
    );
  }

  // Preserve resolution and aspect ratio with fast H.264 & AAC 192kbps
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

  return new Promise((resolve, reject) => {
    const proc = spawn('ffmpeg', ffmpegArgs);

    proc.stderr.on('data', (chunk) => {
      const str = chunk.toString();
      const timeMatch = str.match(/time=(\d+):(\d+):(\d+\.\d+)/);
      if (timeMatch && onProgress) {
        const cur = parseFloat(timeMatch[1]) * 3600 + parseFloat(timeMatch[2]) * 60 + parseFloat(timeMatch[3]);
        const pct = Math.min(100, Math.round((cur / targetDuration) * 100));
        onProgress(pct);
      }
    });

    proc.on('close', (code) => {
      if (code === 0 && fs.existsSync(outputPath) && fs.statSync(outputPath).size > 1000) {
        resolve();
      } else {
        reject(new Error(`FFmpeg processing failed with exit code ${code}`));
      }
    });

    proc.on('error', reject);
  });
}

// -------------------------------------------------------------
// YouTube Upload Integration (Data API v3 Resumable Upload)
// -------------------------------------------------------------
async function uploadToYouTube({
  videoFilePath,
  title,
  description,
  privacyStatus = 'public',
  publishAt,
  thumbnailUrl,
  accessToken,
  onUploadProgress
}) {
  if (!accessToken) {
    throw new Error('Google OAuth token is missing. Please connect your YouTube account.');
  }

  const fileStats = fs.statSync(videoFilePath);
  const fileSize = fileStats.size;

  const metadata = {
    snippet: {
      title: title || 'Automated Video Remix',
      description: description || DEFAULT_ANIME_DESCRIPTION,
      tags: ['anime', 'animeshorts', 'donghua', 'animeedit', 'shorts', 'viralshorts', 'hindianime'],
      categoryId: '24' // Entertainment
    },
    status: {
      privacyStatus: privacyStatus.toLowerCase(),
      selfDeclaredMadeForKids: false
    }
  };

  if (publishAt && !isNaN(new Date(publishAt).getTime())) {
    metadata.status.privacyStatus = 'private'; // Scheduled must be private first
    metadata.status.publishAt = new Date(publishAt).toISOString();
  }

  // Step 1: Initiate Resumable Upload Session
  const initRes = await fetch(
    'https://www.googleapis.com/upload/youtube/v3/videos?uploadType=resumable&part=snippet,status',
    {
      method: 'POST',
      headers: {
        Authorization: `Bearer ${accessToken}`,
        'Content-Type': 'application/json; charset=UTF-8',
        'X-Upload-Content-Type': 'video/mp4',
        'X-Upload-Content-Length': fileSize.toString()
      },
      body: JSON.stringify(metadata)
    }
  );

  if (!initRes.ok) {
    const errText = await initRes.text();
    let parsedErr = errText;
    try {
      const j = JSON.parse(errText);
      parsedErr = j.error?.message || errText;
    } catch {}
    throw new Error(`YouTube API upload initialization failed (${initRes.status}): ${parsedErr}`);
  }

  const uploadUrl = initRes.headers.get('Location') || initRes.headers.get('location');
  if (!uploadUrl) {
    throw new Error('YouTube did not return a resumable upload URL.');
  }

  // Step 2: Stream video bytes to uploadUrl
  const fileBuffer = fs.readFileSync(videoFilePath);
  if (onUploadProgress) onUploadProgress(50);

  const uploadRes = await fetch(uploadUrl, {
    method: 'PUT',
    headers: {
      'Content-Type': 'video/mp4',
      'Content-Length': fileSize.toString()
    },
    body: fileBuffer
  });

  if (!uploadRes.ok) {
    const errText = await uploadRes.text();
    throw new Error(`YouTube upload failed (${uploadRes.status}): ${errText}`);
  }

  const result = await uploadRes.json();
  const videoId = result.id;
  const youtubeUrl = `https://www.youtube.com/watch?v=${videoId}`;

  if (onUploadProgress) onUploadProgress(100);

  // Step 3: Set custom thumbnail if provided
  if (thumbnailUrl && videoId) {
    try {
      const thumbRes = await fetch(thumbnailUrl);
      if (thumbRes.ok) {
        const thumbBuf = await thumbRes.arrayBuffer();
        const contentType = thumbRes.headers.get('content-type') || 'image/jpeg';
        await fetch(`https://www.googleapis.com/upload/youtube/v3/thumbnails/set?videoId=${videoId}`, {
          method: 'POST',
          headers: {
            Authorization: `Bearer ${accessToken}`,
            'Content-Type': contentType
          },
          body: Buffer.from(thumbBuf)
        });
      }
    } catch (thumbErr) {
      console.warn('Thumbnail upload skipped:', thumbErr.message);
    }
  }

  return { videoId, youtubeUrl };
}

// -------------------------------------------------------------
// Google Sheets Integration (v4 REST API)
// -------------------------------------------------------------
async function getSheetRows(spreadsheetId, accessToken) {
  if (!spreadsheetId || !accessToken) return [];
  const range = 'A1:J100';
  const url = `https://sheets.googleapis.com/v4/spreadsheets/${spreadsheetId}/values/${encodeURIComponent(range)}`;
  const res = await fetch(url, {
    headers: { Authorization: `Bearer ${accessToken}` }
  });
  if (!res.ok) {
    const errText = await res.text();
    throw new Error(`Failed to fetch Google Sheet rows (${res.status}): ${errText}`);
  }
  const data = await res.json();
  return data.values || [];
}

async function updateSheetRow(spreadsheetId, rowIndex, rowData, accessToken) {
  if (!spreadsheetId || !accessToken) return;
  // RowIndex is 1-based (row 2, row 3, etc.)
  const range = `A${rowIndex}:J${rowIndex}`;
  const url = `https://sheets.googleapis.com/v4/spreadsheets/${spreadsheetId}/values/${encodeURIComponent(range)}?valueInputOption=USER_ENTERED`;
  await fetch(url, {
    method: 'PUT',
    headers: {
      Authorization: `Bearer ${accessToken}`,
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({
      range,
      majorDimension: 'ROWS',
      values: [rowData]
    })
  });
}

async function createAutomationSheet(title, accessToken) {
  const res = await fetch('https://sheets.googleapis.com/v4/spreadsheets', {
    method: 'POST',
    headers: {
      Authorization: `Bearer ${accessToken}`,
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({
      properties: {
        title: title || 'YouTube Video Automation Queue'
      },
      sheets: [
        {
          properties: {
            title: 'Queue',
            gridProperties: { rowCount: 100, columnCount: 10 }
          },
          data: [
            {
              startRow: 0,
              startColumn: 0,
              rowData: [
                {
                  values: [
                    { userEnteredValue: { stringValue: 'Video URL' } },
                    { userEnteredValue: { stringValue: 'Video Title' } },
                    { userEnteredValue: { stringValue: 'Video Description' } },
                    { userEnteredValue: { stringValue: 'Thumbnail URL' } },
                    { userEnteredValue: { stringValue: 'Music Selection' } },
                    { userEnteredValue: { stringValue: 'Music Volume' } },
                    { userEnteredValue: { stringValue: 'Upload Status' } },
                    { userEnteredValue: { stringValue: 'Scheduled Upload Time' } },
                    { userEnteredValue: { stringValue: 'YouTube Video URL' } },
                    { userEnteredValue: { stringValue: 'Error Message' } }
                  ]
                },
                {
                  values: [
                    { userEnteredValue: { stringValue: '/uploads/videos/sample_anime_clip.mp4' } },
                    { userEnteredValue: { stringValue: '🔥 Insane Anime Moment #1' } },
                    { userEnteredValue: { stringValue: DEFAULT_ANIME_DESCRIPTION } },
                    { userEnteredValue: { stringValue: '' } },
                    { userEnteredValue: { stringValue: 'Cyberpunk Anime Lo-Fi Beat (Royalty Free).mp3' } },
                    { userEnteredValue: { stringValue: '0.8' } },
                    { userEnteredValue: { stringValue: 'READY' } },
                    { userEnteredValue: { stringValue: '' } },
                    { userEnteredValue: { stringValue: '' } },
                    { userEnteredValue: { stringValue: '' } }
                  ]
                }
              ]
            }
          ]
        }
      ]
    })
  });

  if (!res.ok) {
    const err = await res.text();
    throw new Error(`Failed to create spreadsheet: ${err}`);
  }

  const data = await res.json();
  return {
    spreadsheetId: data.spreadsheetId,
    spreadsheetUrl: data.spreadsheetUrl
  };
}

// -------------------------------------------------------------
// Background Automation Queue Worker Loop
// -------------------------------------------------------------
let isWorkerBusy = false;

async function runQueueStep() {
  if (isWorkerBusy) return;
  const currentQueue = readJson('queue-state.json', { status: 'idle' });
  if (currentQueue.status !== 'running') return;

  isWorkerBusy = true;
  try {
    const jobs = readJson('automation-jobs.json', []);
    const settings = readJson('settings.json', DEFAULT_SETTINGS);
    const tokens = readJson('tokens.json', null);
    const sheetConfig = readJson('sheet-config.json', null);

    // Find next READY job
    const nextJobIndex = jobs.findIndex((j) => j.uploadStatus === 'READY');
    if (nextJobIndex === -1) {
      // Queue finished!
      writeJson('queue-state.json', {
        status: 'idle',
        currentJobId: null,
        currentJobTitle: null,
        currentStep: 'Queue completed! All eligible videos processed.',
        progress: 100,
        youtubeUploadProgress: 100,
        lastUpdated: new Date().toISOString()
      });
      isWorkerBusy = false;
      return;
    }

    const job = jobs[nextJobIndex];
    job.uploadStatus = 'PROCESSING';
    job.errorMessage = '';
    jobs[nextJobIndex] = job;
    writeJson('automation-jobs.json', jobs);

    // Update state
    writeJson('queue-state.json', {
      status: 'running',
      currentJobId: job.id,
      currentJobTitle: job.videoTitle || 'Untitled Video',
      currentStep: '1/4 Downloading and validating source video...',
      progress: 15,
      youtubeUploadProgress: 0,
      lastUpdated: new Date().toISOString()
    });

    // Update Google Sheet if connected
    if (sheetConfig?.spreadsheetId && tokens?.accessToken) {
      try {
        const rowData = [
          job.videoUrl,
          job.videoTitle,
          job.videoDescription || settings.defaultDescription,
          job.thumbnailUrl || '',
          job.musicSelection || '',
          job.musicVolume || settings.musicVolume,
          'PROCESSING',
          job.scheduledUploadTime || '',
          job.youtubeVideoUrl || '',
          ''
        ];
        await updateSheetRow(sheetConfig.spreadsheetId, job.rowIndex || nextJobIndex + 2, rowData, tokens.accessToken);
      } catch (sheetErr) {
        console.warn('Google Sheet update error:', sheetErr.message);
      }
    }

    // Step 1: Acquire source video
    let sourceVideoPath = '';
    let isTempVideo = false;

    if (job.videoUrl.startsWith('/uploads/') || job.videoUrl.startsWith('uploads/')) {
      const rel = job.videoUrl.replace(/^\//, '');
      sourceVideoPath = path.join(__dirname, rel);
    } else if (job.videoUrl.startsWith('http://') || job.videoUrl.startsWith('https://')) {
      const tempFilename = `dl-${Date.now()}-${Math.random().toString(36).substring(2, 7)}.mp4`;
      sourceVideoPath = path.join(VIDEOS_DIR, tempFilename);
      isTempVideo = true;
      await downloadRemoteVideo(job.videoUrl, sourceVideoPath);
    } else if (fs.existsSync(job.videoUrl)) {
      sourceVideoPath = job.videoUrl;
    } else {
      throw new Error(`Invalid or inaccessible video URL: "${job.videoUrl}". File not found.`);
    }

    // Step 2: Retrieve music track from Music Library
    writeJson('queue-state.json', {
      status: 'running',
      currentJobId: job.id,
      currentJobTitle: job.videoTitle,
      currentStep: '2/4 Matching audio track from Music Library...',
      progress: 35,
      youtubeUploadProgress: 0,
      lastUpdated: new Date().toISOString()
    });

    const library = readJson('music-library.json', []);
    let selectedTrack = null;

    if (job.musicSelection) {
      selectedTrack = library.find(
        (t) =>
          t.originalName?.toLowerCase() === job.musicSelection.toLowerCase() ||
          t.filename?.toLowerCase() === job.musicSelection.toLowerCase() ||
          t.id === job.musicSelection
      );
    }
    if (!selectedTrack) {
      selectedTrack = library.find((t) => t.isDefault) || library[0];
    }
    if (!selectedTrack) {
      throw new Error('Music Library is empty! Please upload an audio track via the Upload Music button.');
    }

    const musicPath = path.join(MUSIC_DIR, selectedTrack.filename);
    if (!fs.existsSync(musicPath)) {
      throw new Error(`Selected music file "${selectedTrack.originalName}" is missing from disk.`);
    }

    // Step 3: Combine with FFmpeg
    writeJson('queue-state.json', {
      status: 'running',
      currentJobId: job.id,
      currentJobTitle: job.videoTitle,
      currentStep: '3/4 Remixing video & licensed audio with FFmpeg...',
      progress: 50,
      youtubeUploadProgress: 0,
      lastUpdated: new Date().toISOString()
    });

    const exportFilename = `remix-${Date.now()}-${job.id}.mp4`;
    const exportPath = path.join(EXPORTS_DIR, exportFilename);

    const mVol = parseFloat(job.musicVolume || settings.musicVolume || 0.8);
    const vVol = parseFloat(settings.videoVolume || 1.0);
    const replAudio = settings.replaceOriginalAudio || false;

    await mixVideoWithMusic({
      videoPath: sourceVideoPath,
      musicPath,
      outputPath: exportPath,
      musicVolume: mVol,
      videoVolume: vVol,
      replaceOriginalAudio: replAudio,
      fadeIn: parseFloat(settings.fadeIn || 1.0),
      fadeOut: parseFloat(settings.fadeOut || 1.5),
      onProgress: (p) => {
        const scaled = Math.min(75, 50 + Math.round(p * 0.25));
        writeJson('queue-state.json', {
          status: 'running',
          currentJobId: job.id,
          currentJobTitle: job.videoTitle,
          currentStep: `3/4 Remixing with FFmpeg (${p}%)...`,
          progress: scaled,
          youtubeUploadProgress: 0,
          lastUpdated: new Date().toISOString()
        });
      }
    });

    job.downloadUrl = `/exports/${exportFilename}`;

    // Clean up temporary downloaded video
    if (isTempVideo && fs.existsSync(sourceVideoPath)) {
      try {
        fs.unlinkSync(sourceVideoPath);
      } catch {}
    }

    // Step 4: Upload to YouTube channel
    writeJson('queue-state.json', {
      status: 'running',
      currentJobId: job.id,
      currentJobTitle: job.videoTitle,
      currentStep: '4/4 Uploading to YouTube channel via Data API v3...',
      progress: 80,
      youtubeUploadProgress: 10,
      lastUpdated: new Date().toISOString()
    });

    if (!tokens?.accessToken) {
      throw new Error('YouTube account is not connected. Please click "CONNECT YOUTUBE" to authorize uploads.');
    }

    const ytResult = await uploadToYouTube({
      videoFilePath: exportPath,
      title: job.videoTitle || 'Automated Video Remix',
      description: job.videoDescription || settings.defaultDescription,
      privacyStatus: settings.videoPrivacy || 'public',
      publishAt: job.scheduledUploadTime,
      thumbnailUrl: job.thumbnailUrl,
      accessToken: tokens.accessToken,
      onUploadProgress: (pct) => {
        writeJson('queue-state.json', {
          status: 'running',
          currentJobId: job.id,
          currentJobTitle: job.videoTitle,
          currentStep: `4/4 Uploading to YouTube (${pct}%)...`,
          progress: 80 + Math.round(pct * 0.18),
          youtubeUploadProgress: pct,
          lastUpdated: new Date().toISOString()
        });
      }
    });

    // Mark UPLOADED
    job.uploadStatus = 'UPLOADED';
    job.youtubeVideoUrl = ytResult.youtubeUrl;
    job.errorMessage = '';
    job.completedAt = new Date().toISOString();
    jobs[nextJobIndex] = job;
    writeJson('automation-jobs.json', jobs);

    // Save history
    const history = readJson('uploaded-history.json', []);
    history.unshift({
      id: job.id,
      title: job.videoTitle,
      youtubeUrl: ytResult.youtubeUrl,
      videoId: ytResult.videoId,
      uploadedAt: new Date().toISOString(),
      thumbnail: job.thumbnailUrl || `https://i.ytimg.com/vi/${ytResult.videoId}/hqdefault.jpg`
    });
    writeJson('uploaded-history.json', history.slice(0, 50));

    // Update Google Sheet with UPLOADED status and YouTube URL
    if (sheetConfig?.spreadsheetId && tokens?.accessToken) {
      try {
        const rowData = [
          job.videoUrl,
          job.videoTitle,
          job.videoDescription || settings.defaultDescription,
          job.thumbnailUrl || '',
          job.musicSelection || '',
          job.musicVolume || settings.musicVolume,
          'UPLOADED',
          job.scheduledUploadTime || '',
          ytResult.youtubeUrl,
          ''
        ];
        await updateSheetRow(sheetConfig.spreadsheetId, job.rowIndex || nextJobIndex + 2, rowData, tokens.accessToken);
      } catch (sheetErr) {
        console.warn('Google Sheet update error:', sheetErr.message);
      }
    }

    writeJson('queue-state.json', {
      status: 'running',
      currentJobId: null,
      currentJobTitle: null,
      currentStep: `Uploaded "${job.videoTitle}" successfully to YouTube!`,
      progress: 100,
      youtubeUploadProgress: 100,
      lastUpdated: new Date().toISOString()
    });
  } catch (err) {
    console.error('Queue job error:', err);
    const jobs = readJson('automation-jobs.json', []);
    const failedIndex = jobs.findIndex((j) => j.uploadStatus === 'PROCESSING');
    if (failedIndex !== -1) {
      jobs[failedIndex].uploadStatus = 'FAILED';
      jobs[failedIndex].errorMessage = err.message || 'Job processing failed';
      writeJson('automation-jobs.json', jobs);

      // Update sheet with FAILED status & error message
      const sheetConfig = readJson('sheet-config.json', null);
      const tokens = readJson('tokens.json', null);
      if (sheetConfig?.spreadsheetId && tokens?.accessToken) {
        try {
          const failedJob = jobs[failedIndex];
          const rowData = [
            failedJob.videoUrl,
            failedJob.videoTitle,
            failedJob.videoDescription || '',
            failedJob.thumbnailUrl || '',
            failedJob.musicSelection || '',
            failedJob.musicVolume || '0.8',
            'FAILED',
            failedJob.scheduledUploadTime || '',
            '',
            failedJob.errorMessage
          ];
          await updateSheetRow(
            sheetConfig.spreadsheetId,
            failedJob.rowIndex || failedIndex + 2,
            rowData,
            tokens.accessToken
          );
        } catch {}
      }
    }

    writeJson('queue-state.json', {
      status: 'paused',
      currentJobId: null,
      currentJobTitle: null,
      currentStep: `Queue halted due to error: ${err.message}`,
      progress: 0,
      youtubeUploadProgress: 0,
      lastUpdated: new Date().toISOString()
    });
  } finally {
    isWorkerBusy = false;
  }
}

// Background scheduler interval (checks every 3 seconds for persistent execution)
setInterval(() => {
  runQueueStep().catch((e) => console.error('Worker loop tick error:', e));
}, 3000);

// -------------------------------------------------------------
// REST API Endpoints
// -------------------------------------------------------------

// Settings endpoints
app.get('/api/settings', (_req, res) => {
  const settings = readJson('settings.json', DEFAULT_SETTINGS);
  res.json(settings);
});

app.post('/api/settings', (req, res) => {
  const cur = readJson('settings.json', DEFAULT_SETTINGS);
  const updated = { ...cur, ...req.body };
  writeJson('settings.json', updated);
  res.json(updated);
});

// OAuth & Auth Status endpoints
app.get('/api/auth/status', (_req, res) => {
  const tokens = readJson('tokens.json', null);
  const cfg = readJson('../firebase-applet-config.json', null);
  const oAuthClientId = cfg?.oAuthClientId || process.env.GOOGLE_CLIENT_ID || '995356758986-gj5v0ev7rbvuk5ub3516tnp06id9fq3o.apps.googleusercontent.com';

  res.json({
    isConnected: !!tokens?.accessToken,
    googleEmail: tokens?.googleEmail || (tokens?.accessToken ? 'Connected Google Account' : null),
    channelTitle: tokens?.channelTitle || (tokens?.accessToken ? 'Authorized YouTube Channel' : null),
    channelId: tokens?.channelId || null,
    oAuthClientId
  });
});

app.post('/api/auth/save-token', async (req, res) => {
  try {
    const { accessToken, refreshToken, expiresIn, email } = req.body;
    if (!accessToken) {
      return res.status(400).json({ error: 'Access token is required' });
    }

    let channelTitle = 'My YouTube Channel';
    let channelId = null;
    let googleEmail = email || null;

    // Fetch authorized YouTube Channel Info
    try {
      const channelRes = await fetch(
        'https://www.googleapis.com/youtube/v3/channels?part=snippet,contentDetails&mine=true',
        { headers: { Authorization: `Bearer ${accessToken}` } }
      );
      if (channelRes.ok) {
        const cData = await channelRes.json();
        if (cData.items && cData.items.length > 0) {
          channelTitle = cData.items[0].snippet?.title || channelTitle;
          channelId = cData.items[0].id;
        }
      }
    } catch (chErr) {
      console.warn('Channel lookup notice:', chErr.message);
    }

    // Fetch user info for email
    if (!googleEmail) {
      try {
        const userRes = await fetch('https://www.googleapis.com/oauth2/v2/userinfo', {
          headers: { Authorization: `Bearer ${accessToken}` }
        });
        if (userRes.ok) {
          const uData = await userRes.json();
          googleEmail = uData.email || null;
        }
      } catch {}
    }

    const tokenData = {
      accessToken,
      refreshToken: refreshToken || null,
      expiresAt: expiresIn ? Date.now() + expiresIn * 1000 : Date.now() + 3600 * 1000,
      googleEmail,
      channelTitle,
      channelId,
      savedAt: new Date().toISOString()
    };

    writeJson('tokens.json', tokenData);
    res.json({ success: true, googleEmail, channelTitle, channelId });
  } catch (err) {
    console.error('Error saving OAuth token:', err);
    res.status(500).json({ error: err.message });
  }
});

app.post('/api/auth/disconnect', (_req, res) => {
  writeJson('tokens.json', null);
  res.json({ success: true, message: 'Google account disconnected' });
});

// Music Library endpoints
app.get('/api/music', (_req, res) => {
  const library = readJson('music-library.json', []);
  res.json(library);
});

app.post('/api/music/upload', uploadMusic.single('music'), async (req, res) => {
  try {
    if (!req.file) {
      return res.status(400).json({ error: 'Please choose an MP3, WAV, AAC, or M4A audio file.' });
    }

    const probe = await probeMedia(req.file.path);
    const library = readJson('music-library.json', []);
    const isFirst = library.length === 0;

    const track = {
      id: `track-${Date.now()}`,
      filename: req.file.filename,
      originalName: req.file.originalname,
      duration: Math.round(probe.duration || 0),
      size: req.file.size,
      uploadedAt: new Date().toISOString(),
      isDefault: isFirst,
      url: `/uploads/music/${req.file.filename}`
    };

    library.push(track);
    writeJson('music-library.json', library);

    res.json({ success: true, track });
  } catch (err) {
    console.error('Music upload failed:', err);
    if (req.file && fs.existsSync(req.file.path)) {
      try {
        fs.unlinkSync(req.file.path);
      } catch {}
    }
    res.status(500).json({ error: err.message || 'Failed to process music file' });
  }
});

app.post('/api/music/set-default/:id', (req, res) => {
  const library = readJson('music-library.json', []);
  const trackId = req.params.id;
  let found = false;

  const updated = library.map((t) => {
    if (t.id === trackId) {
      found = true;
      return { ...t, isDefault: true };
    }
    return { ...t, isDefault: false };
  });

  if (!found) {
    return res.status(404).json({ error: 'Track not found' });
  }

  writeJson('music-library.json', updated);
  res.json({ success: true, library: updated });
});

app.delete('/api/music/:id', (req, res) => {
  const library = readJson('music-library.json', []);
  const track = library.find((t) => t.id === req.params.id);
  if (!track) {
    return res.status(404).json({ error: 'Track not found' });
  }

  const filePath = path.join(MUSIC_DIR, track.filename);
  if (fs.existsSync(filePath)) {
    try {
      fs.unlinkSync(filePath);
    } catch {}
  }

  const updated = library.filter((t) => t.id !== req.params.id);
  if (track.isDefault && updated.length > 0) {
    updated[0].isDefault = true;
  }
  writeJson('music-library.json', updated);
  res.json({ success: true, library: updated });
});

// Google Sheets endpoints
app.get('/api/sheets/config', (_req, res) => {
  const config = readJson('sheet-config.json', {
    spreadsheetId: null,
    spreadsheetUrl: null,
    title: 'Not Connected'
  });
  res.json(config);
});

app.post('/api/sheets/create', async (_req, res) => {
  try {
    const tokens = readJson('tokens.json', null);
    if (!tokens?.accessToken) {
      return res.status(401).json({ error: 'Google account not connected. Please connect Google first.' });
    }

    const sheetInfo = await createAutomationSheet('YouTube Video Automation Queue', tokens.accessToken);
    const config = {
      spreadsheetId: sheetInfo.spreadsheetId,
      spreadsheetUrl: sheetInfo.spreadsheetUrl,
      title: 'YouTube Video Automation Queue',
      connectedAt: new Date().toISOString()
    };
    writeJson('sheet-config.json', config);

    // Initial sync
    const rows = await getSheetRows(sheetInfo.spreadsheetId, tokens.accessToken);
    syncRowsToJobs(rows);

    res.json({ success: true, ...config });
  } catch (err) {
    console.error('Failed to create sheet:', err);
    res.status(500).json({ error: err.message });
  }
});

app.post('/api/sheets/connect', async (req, res) => {
  try {
    const { spreadsheetIdOrUrl } = req.body;
    if (!spreadsheetIdOrUrl) {
      return res.status(400).json({ error: 'Spreadsheet ID or URL is required' });
    }

    let id = spreadsheetIdOrUrl.trim();
    const urlMatch = id.match(/\/spreadsheets\/d\/([a-zA-Z0-9-_]+)/);
    if (urlMatch) {
      id = urlMatch[1];
    }

    const tokens = readJson('tokens.json', null);
    if (!tokens?.accessToken) {
      return res.status(401).json({ error: 'Please connect Google account first.' });
    }

    // Validate access
    const rows = await getSheetRows(id, tokens.accessToken);

    const config = {
      spreadsheetId: id,
      spreadsheetUrl: `https://docs.google.com/spreadsheets/d/${id}/edit`,
      title: 'Connected Sheet',
      connectedAt: new Date().toISOString()
    };
    writeJson('sheet-config.json', config);
    syncRowsToJobs(rows);

    res.json({ success: true, ...config });
  } catch (err) {
    console.error('Failed to connect sheet:', err);
    res.status(500).json({ error: err.message });
  }
});

function syncRowsToJobs(rawRows) {
  if (!rawRows || rawRows.length <= 1) return;
  const currentJobs = readJson('automation-jobs.json', []);
  const settings = readJson('settings.json', DEFAULT_SETTINGS);

  // Row 1 is header
  const newJobs = [];
  for (let i = 1; i < rawRows.length; i++) {
    const row = rawRows[i];
    const rowIndex = i + 1; // 1-based row index in spreadsheet

    const videoUrl = (row[0] || '').trim();
    if (!videoUrl) continue; // Skip empty rows

    const videoTitle = (row[1] || '').trim() || `Video Row ${rowIndex}`;
    const videoDescription = (row[2] || '').trim() || settings.defaultDescription;
    const thumbnailUrl = (row[3] || '').trim();
    const musicSelection = (row[4] || '').trim();
    const musicVolume = parseFloat(row[5] || settings.musicVolume || 0.8);
    const uploadStatus = (row[6] || 'READY').trim().toUpperCase();
    const scheduledUploadTime = (row[7] || '').trim();
    const youtubeVideoUrl = (row[8] || '').trim();
    const errorMessage = (row[9] || '').trim();

    // Check if job already exists locally to preserve local status
    const existing = currentJobs.find((j) => j.rowIndex === rowIndex || (j.videoUrl === videoUrl && j.videoTitle === videoTitle));

    newJobs.push({
      id: existing?.id || `job-row-${rowIndex}`,
      rowIndex,
      videoUrl,
      videoTitle,
      videoDescription,
      thumbnailUrl,
      musicSelection,
      musicVolume,
      uploadStatus: existing?.uploadStatus || uploadStatus || 'READY',
      scheduledUploadTime,
      youtubeVideoUrl: existing?.youtubeVideoUrl || youtubeVideoUrl,
      errorMessage: existing?.errorMessage || errorMessage,
      downloadUrl: existing?.downloadUrl || '',
      updatedAt: new Date().toISOString()
    });
  }

  if (newJobs.length > 0) {
    writeJson('automation-jobs.json', newJobs);
  }
}

app.post('/api/sheets/refresh', async (_req, res) => {
  try {
    const sheetConfig = readJson('sheet-config.json', null);
    const tokens = readJson('tokens.json', null);

    if (sheetConfig?.spreadsheetId && tokens?.accessToken) {
      const rows = await getSheetRows(sheetConfig.spreadsheetId, tokens.accessToken);
      syncRowsToJobs(rows);
    }

    const jobs = readJson('automation-jobs.json', []);
    res.json({ success: true, jobs });
  } catch (err) {
    console.error('Refresh sheet failed:', err);
    res.status(500).json({ error: err.message });
  }
});

// Add Row directly to Queue & Google Sheet
app.post('/api/sheets/add-row', async (req, res) => {
  try {
    const { videoUrl, videoTitle, videoDescription, thumbnailUrl, musicSelection, musicVolume, scheduledUploadTime } = req.body;

    if (!videoUrl) {
      return res.status(400).json({ error: 'Video URL or file path is required.' });
    }

    const settings = readJson('settings.json', DEFAULT_SETTINGS);
    const jobs = readJson('automation-jobs.json', []);
    const rowIndex = jobs.length + 2;

    const newJob = {
      id: `job-${Date.now()}`,
      rowIndex,
      videoUrl: videoUrl.trim(),
      videoTitle: (videoTitle || 'Untitled Video').trim(),
      videoDescription: (videoDescription || settings.defaultDescription).trim(),
      thumbnailUrl: (thumbnailUrl || '').trim(),
      musicSelection: (musicSelection || '').trim(),
      musicVolume: parseFloat(musicVolume || settings.musicVolume || 0.8),
      uploadStatus: 'READY',
      scheduledUploadTime: (scheduledUploadTime || '').trim(),
      youtubeVideoUrl: '',
      errorMessage: '',
      downloadUrl: '',
      updatedAt: new Date().toISOString()
    };

    jobs.push(newJob);
    writeJson('automation-jobs.json', jobs);

    // Sync to Google Sheet if connected
    const sheetConfig = readJson('sheet-config.json', null);
    const tokens = readJson('tokens.json', null);
    if (sheetConfig?.spreadsheetId && tokens?.accessToken) {
      try {
        const rowData = [
          newJob.videoUrl,
          newJob.videoTitle,
          newJob.videoDescription,
          newJob.thumbnailUrl,
          newJob.musicSelection,
          newJob.musicVolume,
          newJob.uploadStatus,
          newJob.scheduledUploadTime,
          '',
          ''
        ];
        await updateSheetRow(sheetConfig.spreadsheetId, rowIndex, rowData, tokens.accessToken);
      } catch (sheetErr) {
        console.warn('Could not append row to Google Sheets:', sheetErr.message);
      }
    }

    res.json({ success: true, job: newJob });
  } catch (err) {
    console.error('Error adding row:', err);
    res.status(500).json({ error: err.message });
  }
});

// Automation Queue Controls
app.get('/api/automation/status', (_req, res) => {
  const qState = readJson('queue-state.json', { status: 'idle', progress: 0 });
  const jobs = readJson('automation-jobs.json', []);
  const history = readJson('uploaded-history.json', []);

  const stats = {
    total: jobs.length,
    ready: jobs.filter((j) => j.uploadStatus === 'READY').length,
    processing: jobs.filter((j) => j.uploadStatus === 'PROCESSING').length,
    uploaded: jobs.filter((j) => j.uploadStatus === 'UPLOADED').length,
    failed: jobs.filter((j) => j.uploadStatus === 'FAILED').length
  };

  const latestUpload = history.length > 0 ? history[0] : null;

  res.json({
    queueState: qState,
    stats,
    jobs,
    latestUpload
  });
});

app.post('/api/automation/start', (_req, res) => {
  const q = readJson('queue-state.json', { status: 'idle' });
  q.status = 'running';
  q.currentStep = 'Starting automation worker...';
  q.lastUpdated = new Date().toISOString();
  writeJson('queue-state.json', q);
  res.json({ success: true, queueState: q });
});

app.post('/api/automation/pause', (_req, res) => {
  const q = readJson('queue-state.json', { status: 'idle' });
  q.status = 'paused';
  q.currentStep = 'Queue paused by user';
  q.lastUpdated = new Date().toISOString();
  writeJson('queue-state.json', q);
  res.json({ success: true, queueState: q });
});

app.post('/api/automation/stop', (_req, res) => {
  const q = readJson('queue-state.json', { status: 'idle' });
  q.status = 'stopped';
  q.currentStep = 'Queue stopped by user';
  q.lastUpdated = new Date().toISOString();
  writeJson('queue-state.json', q);
  res.json({ success: true, queueState: q });
});

app.post('/api/automation/retry-failed', async (_req, res) => {
  const jobs = readJson('automation-jobs.json', []);
  const sheetConfig = readJson('sheet-config.json', null);
  const tokens = readJson('tokens.json', null);

  const updated = jobs.map((j) => {
    if (j.uploadStatus === 'FAILED') {
      return { ...j, uploadStatus: 'READY', errorMessage: '' };
    }
    return j;
  });

  writeJson('automation-jobs.json', updated);

  // Update in Google Sheet
  if (sheetConfig?.spreadsheetId && tokens?.accessToken) {
    try {
      for (const j of updated) {
        if (j.uploadStatus === 'READY') {
          const rowData = [
            j.videoUrl,
            j.videoTitle,
            j.videoDescription,
            j.thumbnailUrl || '',
            j.musicSelection || '',
            j.musicVolume,
            'READY',
            j.scheduledUploadTime || '',
            j.youtubeVideoUrl || '',
            ''
          ];
          await updateSheetRow(sheetConfig.spreadsheetId, j.rowIndex, rowData, tokens.accessToken);
        }
      }
    } catch (e) {
      console.warn('Error updating Google Sheets on retry:', e.message);
    }
  }

  res.json({ success: true, jobs: updated });
});

app.get('/api/automation/history', (_req, res) => {
  const history = readJson('uploaded-history.json', []);
  res.json(history);
});

// Single Direct Upload / Remix Endpoint (for manual one-off preview)
app.post(
  '/api/remix',
  uploadVideo.fields([
    { name: 'video', maxCount: 1 },
    { name: 'music', maxCount: 1 }
  ]),
  async (req, res) => {
    try {
      const videoFile = req.files?.['video']?.[0];
      const musicFile = req.files?.['music']?.[0];

      if (!videoFile) {
        return res.status(400).json({ error: 'Please upload a video file (MP4, MOV, WebM)' });
      }
      if (!musicFile) {
        return res.status(400).json({ error: 'Please upload a licensed music track' });
      }

      const jobId = `remix-${Date.now()}`;
      const outputFilename = `${jobId}.mp4`;
      const outputPath = path.join(EXPORTS_DIR, outputFilename);

      await mixVideoWithMusic({
        videoPath: videoFile.path,
        musicPath: musicFile.path,
        outputPath,
        musicVolume: parseFloat(req.body.musicVolume || 0.8),
        videoVolume: parseFloat(req.body.videoVolume || 1.0),
        replaceOriginalAudio: req.body.replaceOriginalAudio === 'true',
        fadeIn: parseFloat(req.body.fadeIn || 1.0),
        fadeOut: parseFloat(req.body.fadeOut || 1.5)
      });

      const stats = fs.statSync(outputPath);
      const probe = await probeMedia(outputPath);

      res.json({
        jobId,
        success: true,
        downloadUrl: `/exports/${outputFilename}`,
        filename: outputFilename,
        size: stats.size,
        duration: probe.duration
      });
    } catch (err) {
      console.error('One-off remix failed:', err);
      res.status(500).json({ error: err.message || 'Remix failed' });
    }
  }
);

// APK Download Handler
const handleApkDownload = (_req, res) => {
  if (fs.existsSync(APK_FILE)) {
    const stat = fs.statSync(APK_FILE);
    res.setHeader('Content-Type', 'application/vnd.android.package-archive');
    res.setHeader('Content-Length', stat.size);
    res.setHeader('Content-Disposition', 'attachment; filename="app-debug.apk"');
    res.setHeader('Cache-Control', 'no-cache');
    return res.sendFile(APK_FILE);
  }
  return res.status(404).json({ error: 'APK file not found on server' });
};

app.get('/api/apk-info', (_req, res) => {
  if (fs.existsSync(APK_FILE)) {
    const stat = fs.statSync(APK_FILE);
    return res.json({
      available: true,
      filename: 'app-debug.apk',
      sizeBytes: stat.size,
      sizeFormatted: `${(stat.size / (1024 * 1024)).toFixed(2)} MB`,
      updatedAt: stat.mtime.toISOString(),
      sha256: '4eb7afed08ea85d3f8db802f74beabe0d6f631a349ad305ac190b66db8e7ea4c',
      downloadUrl: '/api/download-apk',
      directUrl: '/APK_DOWNLOAD/app-debug.apk'
    });
  }
  return res.status(404).json({ available: false });
});

app.get('/download-apk', handleApkDownload);
app.get('/api/download-apk', handleApkDownload);
app.get('/app-debug.apk', handleApkDownload);
app.get('/APK_DOWNLOAD/app-debug.apk', handleApkDownload);
app.use('/APK_DOWNLOAD', express.static(APK_DIR));
app.use('/download', express.static(APK_DIR));

// Serve frontend static build
const DIST_DIR = path.join(__dirname, 'dist');
if (fs.existsSync(DIST_DIR)) {
  app.use(express.static(DIST_DIR));
  app.get('*', (_req, res) => {
    res.sendFile(path.join(DIST_DIR, 'index.html'));
  });
}

// Start Server
app.listen(PORT, '0.0.0.0', () => {
  console.log(`AI YouTube Automation & Licensed Music Remixer listening on port ${PORT}`);
});
