import React, { useState, useRef, useEffect } from 'react';
import {
  Upload,
  Music,
  Video,
  Play,
  Download,
  CheckCircle2,
  AlertCircle,
  Sliders,
  Volume2,
  VolumeX,
  Clock,
  RotateCw,
  Sparkles,
  ShieldCheck,
  FileCheck,
  RefreshCw,
  Info
} from 'lucide-react';

const YouTubeIcon = ({ className = "w-4 h-4" }: { className?: string }) => (
  <svg className={className} viewBox="0 0 24 24" fill="currentColor">
    <path d="M23.498 6.186a3.016 3.016 0 0 0-2.122-2.136C19.505 3.545 12 3.545 12 3.545s-7.505 0-9.377.505A3.017 3.017 0 0 0 .502 6.186C0 8.07 0 12 0 12s0 3.93.502 5.814a3.016 3.016 0 0 0 2.122 2.136c1.871.505 9.376.505 9.376.505s7.505 0 9.377-.505a3.015 3.015 0 0 0 2.122-2.136C24 15.93 24 12 24 12s0-3.93-.502-5.814zM9.545 15.568V8.432L15.818 12l-6.273 3.568z"/>
  </svg>
);

interface YouTubeMetadata {
  videoId: string;
  title: string;
  channelTitle: string;
  thumbnail: string;
}

interface RemixResult {
  jobId: string;
  success: boolean;
  downloadUrl: string;
  filename: string;
  size: number;
  duration: number;
}

export default function App() {
  // Step 1: YouTube Metadata
  const [youtubeUrl, setYoutubeUrl] = useState('');
  const [isFindingVideo, setIsFindingVideo] = useState(false);
  const [youtubeMetadata, setYoutubeMetadata] = useState<YouTubeMetadata | null>(null);
  const [youtubeError, setYoutubeError] = useState<string | null>(null);

  // Step 2: Upload Video
  const [videoFile, setVideoFile] = useState<File | null>(null);
  const [videoPreviewUrl, setVideoPreviewUrl] = useState<string | null>(null);
  const [videoDuration, setVideoDuration] = useState<number>(0);
  const videoInputRef = useRef<HTMLInputElement>(null);
  const videoPlayerRef = useRef<HTMLVideoElement>(null);

  // Step 3: Upload Licensed Music
  const [musicFile, setMusicFile] = useState<File | null>(null);
  const [musicPreviewUrl, setMusicPreviewUrl] = useState<string | null>(null);
  const [musicDuration, setMusicDuration] = useState<number>(0);
  const [hasLicensePermission, setHasLicensePermission] = useState(true);
  const musicInputRef = useRef<HTMLInputElement>(null);

  // Step 4: Music Controls
  const [musicVolume, setMusicVolume] = useState<number>(100); // 0–100%
  const [videoVolume, setVideoVolume] = useState<number>(100); // 0–100%
  const [replaceOriginalAudio, setReplaceOriginalAudio] = useState<boolean>(true);
  const [startTime, setStartTime] = useState<number>(0); // seconds
  const [fadeIn, setFadeIn] = useState<number>(0); // seconds
  const [fadeOut, setFadeOut] = useState<number>(0); // seconds
  const [musicDurationMode, setMusicDurationMode] = useState<'full_video' | 'custom'>('full_video');
  const [customDuration, setCustomDuration] = useState<number>(30); // seconds
  const [loopMusic, setLoopMusic] = useState<boolean>(true);

  // Step 5: Remix Processing State
  const [isRemixing, setIsRemixing] = useState(false);
  const [remixStage, setRemixStage] = useState<'idle' | 'uploading' | 'processing' | 'rendering' | 'complete' | 'error'>('idle');
  const [remixProgress, setRemixProgress] = useState(0);
  const [remixError, setRemixError] = useState<string | null>(null);
  const [remixResult, setRemixResult] = useState<RemixResult | null>(null);

  // Handle Video file upload
  const handleVideoSelect = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    const validExtensions = ['.mp4', '.mov', '.webm'];
    const ext = file.name.slice(file.name.lastIndexOf('.')).toLowerCase();
    if (!validExtensions.includes(ext)) {
      alert('Please upload an MP4, MOV, or WebM video file.');
      return;
    }

    setVideoFile(file);
    const objectUrl = URL.createObjectURL(file);
    setVideoPreviewUrl(objectUrl);
    setRemixResult(null);
  };

  // Handle Music file upload
  const handleMusicSelect = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    const validExtensions = ['.mp3', '.wav', '.m4a'];
    const ext = file.name.slice(file.name.lastIndexOf('.')).toLowerCase();
    if (!validExtensions.includes(ext)) {
      alert('Please upload an MP3, WAV, or M4A music file.');
      return;
    }

    setMusicFile(file);
    const objectUrl = URL.createObjectURL(file);
    setMusicPreviewUrl(objectUrl);
    setRemixResult(null);
  };

  // Find YouTube video metadata
  const handleFindVideo = async () => {
    if (!youtubeUrl.trim()) {
      setYoutubeError('Please enter a YouTube video URL.');
      return;
    }

    setIsFindingVideo(true);
    setYoutubeError(null);

    try {
      const res = await fetch(`/api/youtube-metadata?url=${encodeURIComponent(youtubeUrl.trim())}`);
      const data = await res.json();

      if (!res.ok) {
        throw new Error(data.error || 'Failed to retrieve YouTube metadata');
      }

      setYoutubeMetadata(data);
    } catch (err: any) {
      setYoutubeError(err.message || 'Could not fetch video metadata');
      setYoutubeMetadata(null);
    } finally {
      setIsFindingVideo(false);
    }
  };

  // Create Remix execution
  const handleCreateRemix = async () => {
    if (!videoFile) {
      alert('Please upload your video file first.');
      return;
    }
    if (!musicFile) {
      alert('Please upload your licensed music track.');
      return;
    }
    if (!hasLicensePermission) {
      alert('You must confirm you have the right or license to use the uploaded music.');
      return;
    }

    setIsRemixing(true);
    setRemixStage('uploading');
    setRemixProgress(15);
    setRemixError(null);
    setRemixResult(null);

    const formData = new FormData();
    formData.append('video', videoFile);
    formData.append('music', musicFile);
    formData.append('musicVolume', (musicVolume / 100).toString());
    formData.append('videoVolume', (videoVolume / 100).toString());
    formData.append('replaceOriginalAudio', replaceOriginalAudio.toString());
    formData.append('startTime', startTime.toString());
    formData.append('fadeIn', fadeIn.toString());
    formData.append('fadeOut', fadeOut.toString());
    formData.append('musicDurationMode', musicDurationMode);
    formData.append('customDuration', customDuration.toString());
    formData.append('loopMusic', loopMusic.toString());

    try {
      // Simulate progress progression
      const progressTimer = setInterval(() => {
        setRemixProgress((prev) => {
          if (prev < 30) {
            setRemixStage('uploading');
            return prev + 5;
          } else if (prev < 65) {
            setRemixStage('processing');
            return prev + 4;
          } else if (prev < 90) {
            setRemixStage('rendering');
            return prev + 2;
          }
          return prev;
        });
      }, 700);

      const response = await fetch('/api/remix', {
        method: 'POST',
        body: formData
      });

      clearInterval(progressTimer);

      const result = await response.json();

      if (!response.ok) {
        throw new Error(result.error || 'Remix failed to render');
      }

      setRemixStage('complete');
      setRemixProgress(100);
      setRemixResult(result);
    } catch (err: any) {
      console.error('Remix error:', err);
      setRemixStage('error');
      setRemixError(err.message || 'An unexpected error occurred during rendering.');
    } finally {
      setIsRemixing(false);
    }
  };

  const formatSeconds = (sec: number) => {
    const m = Math.floor(sec / 60);
    const s = Math.floor(sec % 60);
    return `${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}`;
  };

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100 flex flex-col">
      {/* Top Navbar */}
      <header className="border-b border-slate-800/80 bg-slate-900/60 backdrop-blur-md sticky top-0 z-50">
        <div className="max-w-6xl mx-auto px-4 py-3.5 flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className="h-10 w-10 rounded-xl bg-gradient-to-tr from-sky-600 to-indigo-600 flex items-center justify-center shadow-lg shadow-sky-500/20 ring-1 ring-white/10">
              <Sparkles className="w-5 h-5 text-white" />
            </div>
            <div>
              <h1 className="font-bold text-lg leading-tight tracking-tight text-white flex items-center gap-2">
                Licensed Music Remix
                <span className="text-[11px] font-semibold tracking-wide uppercase px-2 py-0.5 rounded-full bg-sky-500/10 text-sky-400 border border-sky-500/20">
                  Web Edition
                </span>
              </h1>
              <p className="text-xs text-slate-400">YouTube Metadata Lookup &amp; Licensed Audio Video Remixer</p>
            </div>
          </div>
          <div className="flex items-center gap-2 text-xs text-emerald-400 font-medium bg-emerald-950/40 border border-emerald-800/50 px-3 py-1.5 rounded-lg">
            <ShieldCheck className="w-4 h-4" />
            <span>Strict Copyright Safe</span>
          </div>
        </div>
      </header>

      {/* Main Content */}
      <main className="flex-1 max-w-6xl w-full mx-auto px-4 py-8 space-y-8">
        
        {/* Compliance Banner */}
        <div className="rounded-2xl border border-sky-900/40 bg-gradient-to-r from-sky-950/40 via-slate-900/60 to-indigo-950/40 p-4 sm:p-5 flex items-start gap-4 shadow-sm">
          <div className="p-2.5 rounded-xl bg-sky-500/10 text-sky-400 border border-sky-500/20 shrink-0 mt-0.5">
            <Info className="w-5 h-5" />
          </div>
          <div className="text-sm space-y-1">
            <p className="font-semibold text-slate-200">Authorized &amp; Licensed Music Workflow</p>
            <p className="text-slate-400 leading-relaxed text-xs sm:text-sm">
              The YouTube URL is used exclusively to retrieve and preview public video metadata via the official YouTube API. The music used for remixing must be uploaded by you and owned or properly licensed. This platform never extracts, downloads, or scrapes unauthorized audio from YouTube.
            </p>
          </div>
        </div>

        <div className="grid grid-cols-1 lg:grid-cols-12 gap-8">
          
          {/* Left Column: Inputs & Controls (7 Cols) */}
          <div className="lg:col-span-7 space-y-6">
            
            {/* Step 1: YouTube Video URL Input */}
            <section className="bg-slate-900/70 border border-slate-800/80 rounded-2xl p-5 sm:p-6 shadow-md">
              <div className="flex items-center gap-2.5 mb-4">
                <div className="h-7 w-7 rounded-lg bg-red-500/10 text-red-400 flex items-center justify-center font-bold text-xs border border-red-500/20">
                  1
                </div>
                <h2 className="font-bold text-base text-slate-100 flex items-center gap-2">
                  <YouTubeIcon className="w-4 h-4 text-red-500" />
                  YouTube Video Reference
                </h2>
              </div>

              <div className="space-y-3">
                <label className="block text-xs font-semibold uppercase tracking-wider text-slate-400">
                  YouTube Video URL
                </label>
                <div className="flex flex-col sm:flex-row gap-2.5">
                  <input
                    type="text"
                    value={youtubeUrl}
                    onChange={(e) => setYoutubeUrl(e.target.value)}
                    placeholder="https://www.youtube.com/watch?v=... or shorts/..."
                    className="flex-1 bg-slate-950/80 border border-slate-700/80 rounded-xl px-4 py-2.5 text-sm text-slate-100 placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-sky-500/50 focus:border-sky-500"
                  />
                  <button
                    onClick={handleFindVideo}
                    disabled={isFindingVideo}
                    className="px-5 py-2.5 rounded-xl bg-red-600 hover:bg-red-500 text-white font-semibold text-sm transition flex items-center justify-center gap-2 disabled:opacity-50 shadow-md shadow-red-600/20 shrink-0"
                  >
                    {isFindingVideo ? (
                      <RefreshCw className="w-4 h-4 animate-spin" />
                    ) : (
                      <YouTubeIcon className="w-4 h-4" />
                    )}
                    Find Video
                  </button>
                </div>

                {youtubeError && (
                  <div className="flex items-center gap-2 text-xs text-red-400 bg-red-950/30 border border-red-800/40 p-3 rounded-xl">
                    <AlertCircle className="w-4 h-4 shrink-0" />
                    <span>{youtubeError}</span>
                  </div>
                )}

                {/* Displayed YouTube Metadata */}
                {youtubeMetadata && (
                  <div className="mt-4 rounded-xl border border-slate-700/70 bg-slate-950/60 p-3.5 flex gap-3.5 items-center">
                    <img
                      src={youtubeMetadata.thumbnail}
                      alt={youtubeMetadata.title}
                      className="w-28 h-18 object-cover rounded-lg border border-slate-800 shrink-0 shadow-sm"
                    />
                    <div className="min-w-0 flex-1 space-y-1">
                      <p className="text-sm font-bold text-slate-100 line-clamp-1">
                        {youtubeMetadata.title}
                      </p>
                      <p className="text-xs text-slate-400 flex items-center gap-1.5">
                        <span className="font-medium text-slate-300">{youtubeMetadata.channelTitle}</span>
                        <span>•</span>
                        <span className="font-mono text-slate-500">ID: {youtubeMetadata.videoId}</span>
                      </p>
                      <span className="inline-flex items-center gap-1 text-[10px] font-semibold text-emerald-400 bg-emerald-950/50 px-2 py-0.5 rounded-md border border-emerald-800/40">
                        <CheckCircle2 className="w-3 h-3" />
                        Metadata Retrieved (No media downloaded)
                      </span>
                    </div>
                  </div>
                )}
              </div>
            </section>

            {/* Step 2: Upload My Video */}
            <section className="bg-slate-900/70 border border-slate-800/80 rounded-2xl p-5 sm:p-6 shadow-md">
              <div className="flex items-center justify-between mb-4">
                <div className="flex items-center gap-2.5">
                  <div className="h-7 w-7 rounded-lg bg-sky-500/10 text-sky-400 flex items-center justify-center font-bold text-xs border border-sky-500/20">
                    2
                  </div>
                  <h2 className="font-bold text-base text-slate-100 flex items-center gap-2">
                    <Video className="w-4 h-4 text-sky-400" />
                    Upload My Video
                  </h2>
                </div>
                <span className="text-xs text-slate-400">MP4, MOV, WebM (Max 200MB)</span>
              </div>

              <input
                type="file"
                ref={videoInputRef}
                onChange={handleVideoSelect}
                accept=".mp4,.mov,.webm,video/mp4,video/quicktime,video/webm"
                className="hidden"
              />

              {!videoFile ? (
                <div
                  onClick={() => videoInputRef.current?.click()}
                  className="border-2 border-dashed border-slate-700/80 hover:border-sky-500/70 rounded-2xl p-6 text-center cursor-pointer transition bg-slate-950/40 hover:bg-slate-900/60 group"
                >
                  <div className="w-12 h-12 rounded-xl bg-sky-500/10 text-sky-400 border border-sky-500/20 flex items-center justify-center mx-auto mb-3 group-hover:scale-105 transition">
                    <Upload className="w-6 h-6" />
                  </div>
                  <p className="font-semibold text-sm text-slate-200">Click to Upload My Video</p>
                  <p className="text-xs text-slate-400 mt-1">Supports high-definition MP4, MOV, and WebM</p>
                </div>
              ) : (
                <div className="space-y-3">
                  <div className="flex items-center justify-between p-3 rounded-xl bg-slate-950/70 border border-slate-800">
                    <div className="flex items-center gap-3 min-w-0">
                      <div className="p-2 rounded-lg bg-sky-500/10 text-sky-400 border border-sky-500/20 shrink-0">
                        <FileCheck className="w-4 h-4" />
                      </div>
                      <div className="min-w-0">
                        <p className="text-sm font-semibold text-slate-200 truncate">{videoFile.name}</p>
                        <p className="text-xs text-slate-400">
                          {(videoFile.size / (1024 * 1024)).toFixed(1)} MB
                        </p>
                      </div>
                    </div>
                    <button
                      onClick={() => videoInputRef.current?.click()}
                      className="text-xs text-sky-400 hover:text-sky-300 font-semibold px-2.5 py-1.5 rounded-lg border border-sky-500/20 hover:bg-sky-500/10 transition"
                    >
                      Change Video
                    </button>
                  </div>

                  {/* Video Preview */}
                  {videoPreviewUrl && (
                    <div className="rounded-xl overflow-hidden border border-slate-800 bg-black aspect-video max-h-56 mx-auto flex items-center justify-center">
                      <video
                        ref={videoPlayerRef}
                        src={videoPreviewUrl}
                        controls
                        className="w-full h-full object-contain"
                        onLoadedMetadata={(e) => {
                          setVideoDuration(e.currentTarget.duration);
                        }}
                      />
                    </div>
                  )}
                </div>
              )}
            </section>

            {/* Step 3: Upload Licensed Music */}
            <section className="bg-slate-900/70 border border-slate-800/80 rounded-2xl p-5 sm:p-6 shadow-md">
              <div className="flex items-center justify-between mb-4">
                <div className="flex items-center gap-2.5">
                  <div className="h-7 w-7 rounded-lg bg-purple-500/10 text-purple-400 flex items-center justify-center font-bold text-xs border border-purple-500/20">
                    3
                  </div>
                  <h2 className="font-bold text-base text-slate-100 flex items-center gap-2">
                    <Music className="w-4 h-4 text-purple-400" />
                    Upload Licensed Music
                  </h2>
                </div>
                <span className="text-xs text-slate-400">MP3, WAV, M4A (Max 50MB)</span>
              </div>

              <input
                type="file"
                ref={musicInputRef}
                onChange={handleMusicSelect}
                accept=".mp3,.wav,.m4a,audio/mpeg,audio/wav,audio/mp4,audio/x-m4a"
                className="hidden"
              />

              {!musicFile ? (
                <div
                  onClick={() => musicInputRef.current?.click()}
                  className="border-2 border-dashed border-slate-700/80 hover:border-purple-500/70 rounded-2xl p-6 text-center cursor-pointer transition bg-slate-950/40 hover:bg-slate-900/60 group"
                >
                  <div className="w-12 h-12 rounded-xl bg-purple-500/10 text-purple-400 border border-purple-500/20 flex items-center justify-center mx-auto mb-3 group-hover:scale-105 transition">
                    <Upload className="w-6 h-6" />
                  </div>
                  <p className="font-semibold text-sm text-slate-200">Click to Upload Licensed Music Track</p>
                  <p className="text-xs text-slate-400 mt-1">Accepts MP3, WAV, and M4A audio files</p>
                </div>
              ) : (
                <div className="space-y-3">
                  <div className="flex items-center justify-between p-3 rounded-xl bg-slate-950/70 border border-slate-800">
                    <div className="flex items-center gap-3 min-w-0">
                      <div className="p-2 rounded-lg bg-purple-500/10 text-purple-400 border border-purple-500/20 shrink-0">
                        <FileCheck className="w-4 h-4" />
                      </div>
                      <div className="min-w-0">
                        <p className="text-sm font-semibold text-slate-200 truncate">{musicFile.name}</p>
                        <p className="text-xs text-slate-400">
                          {(musicFile.size / (1024 * 1024)).toFixed(1)} MB
                        </p>
                      </div>
                    </div>
                    <button
                      onClick={() => musicInputRef.current?.click()}
                      className="text-xs text-purple-400 hover:text-purple-300 font-semibold px-2.5 py-1.5 rounded-lg border border-purple-500/20 hover:bg-purple-500/10 transition"
                    >
                      Change Music
                    </button>
                  </div>

                  {musicPreviewUrl && (
                    <div className="p-3 bg-slate-950/80 rounded-xl border border-slate-800">
                      <audio
                        src={musicPreviewUrl}
                        controls
                        className="w-full h-9"
                        onLoadedMetadata={(e) => {
                          setMusicDuration(e.currentTarget.duration);
                        }}
                      />
                    </div>
                  )}

                  {/* Mandatory Rights & Permission Acknowledgment */}
                  <label className="flex items-start gap-2.5 p-3 rounded-xl bg-purple-950/20 border border-purple-900/30 text-xs text-slate-300 cursor-pointer select-none">
                    <input
                      type="checkbox"
                      checked={hasLicensePermission}
                      onChange={(e) => setHasLicensePermission(e.target.checked)}
                      className="mt-0.5 rounded border-slate-700 text-purple-600 focus:ring-purple-500 bg-slate-900"
                    />
                    <span>
                      I confirm I own this music track or have legitimate permission / license to use it.
                    </span>
                  </label>
                </div>
              )}
            </section>

            {/* Step 4: Music Controls */}
            <section className="bg-slate-900/70 border border-slate-800/80 rounded-2xl p-5 sm:p-6 shadow-md space-y-5">
              <div className="flex items-center gap-2.5">
                <div className="h-7 w-7 rounded-lg bg-emerald-500/10 text-emerald-400 flex items-center justify-center font-bold text-xs border border-emerald-500/20">
                  4
                </div>
                <h2 className="font-bold text-base text-slate-100 flex items-center gap-2">
                  <Sliders className="w-4 h-4 text-emerald-400" />
                  Remix &amp; Audio Controls
                </h2>
              </div>

              <div className="space-y-4">
                {/* Audio Mode: Replace vs Keep Original Audio */}
                <div className="p-3.5 rounded-xl bg-slate-950/70 border border-slate-800 space-y-2.5">
                  <span className="text-xs font-semibold text-slate-300 block">Source Video Audio Mixing</span>
                  <div className="grid grid-cols-2 gap-2">
                    <button
                      type="button"
                      onClick={() => setReplaceOriginalAudio(true)}
                      className={`px-3 py-2 rounded-lg text-xs font-semibold transition border flex items-center justify-center gap-2 ${
                        replaceOriginalAudio
                          ? 'bg-sky-600/20 border-sky-500 text-sky-300'
                          : 'bg-slate-900 border-slate-800 text-slate-400 hover:text-slate-200'
                      }`}
                    >
                      <VolumeX className="w-3.5 h-3.5" />
                      Replace Original Audio
                    </button>
                    <button
                      type="button"
                      onClick={() => setReplaceOriginalAudio(false)}
                      className={`px-3 py-2 rounded-lg text-xs font-semibold transition border flex items-center justify-center gap-2 ${
                        !replaceOriginalAudio
                          ? 'bg-emerald-600/20 border-emerald-500 text-emerald-300'
                          : 'bg-slate-900 border-slate-800 text-slate-400 hover:text-slate-200'
                      }`}
                    >
                      <Volume2 className="w-3.5 h-3.5" />
                      Keep &amp; Mix Original Audio
                    </button>
                  </div>
                </div>

                {/* Sliders: Music Volume & Video Volume */}
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                  {/* Music Volume */}
                  <div className="p-3.5 rounded-xl bg-slate-950/70 border border-slate-800 space-y-2">
                    <div className="flex justify-between items-center text-xs">
                      <span className="font-medium text-slate-300 flex items-center gap-1.5">
                        <Music className="w-3.5 h-3.5 text-purple-400" />
                        Music Volume
                      </span>
                      <span className="font-mono text-purple-400 font-bold">{musicVolume}%</span>
                    </div>
                    <input
                      type="range"
                      min="0"
                      max="150"
                      value={musicVolume}
                      onChange={(e) => setMusicVolume(parseInt(e.target.value))}
                      className="w-full accent-purple-500 h-1.5 bg-slate-800 rounded-lg cursor-pointer"
                    />
                  </div>

                  {/* Video Volume (enabled if keeping original audio) */}
                  <div className={`p-3.5 rounded-xl bg-slate-950/70 border border-slate-800 space-y-2 ${replaceOriginalAudio ? 'opacity-40 pointer-events-none' : ''}`}>
                    <div className="flex justify-between items-center text-xs">
                      <span className="font-medium text-slate-300 flex items-center gap-1.5">
                        <Video className="w-3.5 h-3.5 text-sky-400" />
                        Original Voice/Audio
                      </span>
                      <span className="font-mono text-sky-400 font-bold">{replaceOriginalAudio ? 'Muted' : `${videoVolume}%`}</span>
                    </div>
                    <input
                      type="range"
                      min="0"
                      max="150"
                      disabled={replaceOriginalAudio}
                      value={videoVolume}
                      onChange={(e) => setVideoVolume(parseInt(e.target.value))}
                      className="w-full accent-sky-500 h-1.5 bg-slate-800 rounded-lg cursor-pointer"
                    />
                  </div>
                </div>

                {/* Timing Controls: Start Time, Fade In, Fade Out */}
                <div className="grid grid-cols-3 gap-3">
                  <div className="p-3 rounded-xl bg-slate-950/70 border border-slate-800 space-y-1">
                    <label className="text-[11px] font-medium text-slate-400 block">Start Time (s)</label>
                    <input
                      type="number"
                      min="0"
                      step="0.5"
                      value={startTime}
                      onChange={(e) => setStartTime(Math.max(0, parseFloat(e.target.value) || 0))}
                      className="w-full bg-slate-900 border border-slate-800 rounded-lg px-2.5 py-1.5 text-xs text-slate-100 focus:outline-none focus:border-sky-500 font-mono"
                    />
                  </div>

                  <div className="p-3 rounded-xl bg-slate-950/70 border border-slate-800 space-y-1">
                    <label className="text-[11px] font-medium text-slate-400 block">Fade In (s)</label>
                    <input
                      type="number"
                      min="0"
                      max="10"
                      step="0.5"
                      value={fadeIn}
                      onChange={(e) => setFadeIn(Math.max(0, parseFloat(e.target.value) || 0))}
                      className="w-full bg-slate-900 border border-slate-800 rounded-lg px-2.5 py-1.5 text-xs text-slate-100 focus:outline-none focus:border-sky-500 font-mono"
                    />
                  </div>

                  <div className="p-3 rounded-xl bg-slate-950/70 border border-slate-800 space-y-1">
                    <label className="text-[11px] font-medium text-slate-400 block">Fade Out (s)</label>
                    <input
                      type="number"
                      min="0"
                      max="10"
                      step="0.5"
                      value={fadeOut}
                      onChange={(e) => setFadeOut(Math.max(0, parseFloat(e.target.value) || 0))}
                      className="w-full bg-slate-900 border border-slate-800 rounded-lg px-2.5 py-1.5 text-xs text-slate-100 focus:outline-none focus:border-sky-500 font-mono"
                    />
                  </div>
                </div>

                {/* Music Duration Mode */}
                <div className="p-3.5 rounded-xl bg-slate-950/70 border border-slate-800 space-y-3">
                  <span className="text-xs font-semibold text-slate-300 block">Music Duration Scope</span>
                  <div className="grid grid-cols-2 gap-2">
                    <button
                      type="button"
                      onClick={() => setMusicDurationMode('full_video')}
                      className={`px-3 py-2 rounded-lg text-xs font-semibold transition border ${
                        musicDurationMode === 'full_video'
                          ? 'bg-sky-600/20 border-sky-500 text-sky-300'
                          : 'bg-slate-900 border-slate-800 text-slate-400 hover:text-slate-200'
                      }`}
                    >
                      a) Full Video Duration
                    </button>
                    <button
                      type="button"
                      onClick={() => setMusicDurationMode('custom')}
                      className={`px-3 py-2 rounded-lg text-xs font-semibold transition border ${
                        musicDurationMode === 'custom'
                          ? 'bg-sky-600/20 border-sky-500 text-sky-300'
                          : 'bg-slate-900 border-slate-800 text-slate-400 hover:text-slate-200'
                      }`}
                    >
                      b) Custom Duration
                    </button>
                  </div>

                  {musicDurationMode === 'custom' && (
                    <div className="flex items-center gap-2 pt-1">
                      <Clock className="w-4 h-4 text-slate-400" />
                      <input
                        type="number"
                        min="1"
                        step="1"
                        value={customDuration}
                        onChange={(e) => setCustomDuration(Math.max(1, parseInt(e.target.value) || 1))}
                        className="w-24 bg-slate-900 border border-slate-800 rounded-lg px-2.5 py-1 text-xs text-slate-100 font-mono"
                      />
                      <span className="text-xs text-slate-400">seconds (max capped at video length)</span>
                    </div>
                  )}

                  {/* Loop Option */}
                  <label className="flex items-center gap-2 text-xs text-slate-300 cursor-pointer pt-1">
                    <input
                      type="checkbox"
                      checked={loopMusic}
                      onChange={(e) => setLoopMusic(e.target.checked)}
                      className="rounded border-slate-700 text-sky-600 focus:ring-sky-500 bg-slate-900"
                    />
                    <RotateCw className="w-3.5 h-3.5 text-slate-400" />
                    <span>Loop music track seamlessly if shorter than target duration</span>
                  </label>
                </div>
              </div>
            </section>

            {/* Step 6: Create Remix Button */}
            <button
              onClick={handleCreateRemix}
              disabled={isRemixing || !videoFile || !musicFile}
              className="w-full py-4 rounded-2xl bg-gradient-to-r from-sky-600 via-indigo-600 to-purple-600 hover:from-sky-500 hover:to-purple-500 text-white font-bold text-base shadow-xl shadow-sky-600/25 transition flex items-center justify-center gap-2.5 disabled:opacity-50 disabled:cursor-not-allowed cursor-pointer"
            >
              {isRemixing ? (
                <>
                  <RefreshCw className="w-5 h-5 animate-spin" />
                  <span>Processing Video Remix...</span>
                </>
              ) : (
                <>
                  <Sparkles className="w-5 h-5" />
                  <span>Create Remix</span>
                </>
              )}
            </button>
          </div>

          {/* Right Column: Progress & Output Preview (5 Cols) */}
          <div className="lg:col-span-5 space-y-6">
            
            {/* Step 8: Progress Bar */}
            <section className="bg-slate-900/70 border border-slate-800/80 rounded-2xl p-5 sm:p-6 shadow-md space-y-4">
              <h3 className="font-bold text-sm text-slate-200 flex items-center justify-between">
                <span>Remix Pipeline Status</span>
                <span className="text-xs font-mono font-bold text-sky-400">{remixProgress}%</span>
              </h3>

              {/* Progress Tracker Bar */}
              <div className="w-full bg-slate-950 rounded-full h-3 overflow-hidden border border-slate-800 p-0.5">
                <div
                  className="bg-gradient-to-r from-sky-500 via-indigo-500 to-purple-500 h-full rounded-full transition-all duration-500"
                  style={{ width: `${remixProgress}%` }}
                />
              </div>

              {/* Step Badges: Uploading → Processing → Rendering → Complete */}
              <div className="grid grid-cols-4 gap-1 text-center pt-2">
                <div className={`p-1.5 rounded-lg text-[10px] font-semibold flex flex-col items-center gap-1 ${
                  remixStage === 'uploading'
                    ? 'bg-sky-500/20 text-sky-300 border border-sky-500/40'
                    : remixProgress > 25
                    ? 'text-emerald-400'
                    : 'text-slate-500'
                }`}>
                  <Upload className="w-3 h-3" />
                  <span>Uploading</span>
                </div>

                <div className={`p-1.5 rounded-lg text-[10px] font-semibold flex flex-col items-center gap-1 ${
                  remixStage === 'processing'
                    ? 'bg-indigo-500/20 text-indigo-300 border border-indigo-500/40'
                    : remixProgress > 55
                    ? 'text-emerald-400'
                    : 'text-slate-500'
                }`}>
                  <Sliders className="w-3 h-3" />
                  <span>Processing</span>
                </div>

                <div className={`p-1.5 rounded-lg text-[10px] font-semibold flex flex-col items-center gap-1 ${
                  remixStage === 'rendering'
                    ? 'bg-purple-500/20 text-purple-300 border border-purple-500/40'
                    : remixProgress > 95
                    ? 'text-emerald-400'
                    : 'text-slate-500'
                }`}>
                  <Video className="w-3 h-3" />
                  <span>Rendering</span>
                </div>

                <div className={`p-1.5 rounded-lg text-[10px] font-semibold flex flex-col items-center gap-1 ${
                  remixStage === 'complete'
                    ? 'bg-emerald-500/20 text-emerald-300 border border-emerald-500/40'
                    : 'text-slate-500'
                }`}>
                  <CheckCircle2 className="w-3 h-3" />
                  <span>Complete</span>
                </div>
              </div>

              {remixError && (
                <div className="p-3 bg-red-950/40 border border-red-800/50 rounded-xl text-xs text-red-400 flex items-start gap-2">
                  <AlertCircle className="w-4 h-4 shrink-0 mt-0.5" />
                  <span>{remixError}</span>
                </div>
              )}
            </section>

            {/* Step 9: Finished Video Preview & Save/Download Button */}
            <section className="bg-slate-900/70 border border-slate-800/80 rounded-2xl p-5 sm:p-6 shadow-md space-y-4">
              <h3 className="font-bold text-sm text-slate-200 flex items-center gap-2">
                <Play className="w-4 h-4 text-sky-400" />
                Finished Remix Output
              </h3>

              {remixResult ? (
                <div className="space-y-4">
                  <div className="rounded-xl overflow-hidden border border-slate-800 bg-black aspect-video flex items-center justify-center shadow-lg">
                    <video
                      src={remixResult.downloadUrl}
                      controls
                      autoPlay
                      className="w-full h-full object-contain"
                    />
                  </div>

                  <div className="p-3 rounded-xl bg-slate-950/70 border border-slate-800 text-xs space-y-1.5">
                    <div className="flex justify-between text-slate-400">
                      <span>Target Duration:</span>
                      <span className="font-mono text-slate-200">{formatSeconds(remixResult.duration)}</span>
                    </div>
                    <div className="flex justify-between text-slate-400">
                      <span>File Size:</span>
                      <span className="font-mono text-slate-200">
                        {(remixResult.size / (1024 * 1024)).toFixed(2)} MB
                      </span>
                    </div>
                    <div className="flex justify-between text-slate-400">
                      <span>Audio Track:</span>
                      <span className="text-emerald-400 font-medium">Licensed Audio Preserved</span>
                    </div>
                  </div>

                  <a
                    href={remixResult.downloadUrl}
                    download={remixResult.filename}
                    className="w-full py-3 rounded-xl bg-emerald-600 hover:bg-emerald-500 text-white font-bold text-sm transition flex items-center justify-center gap-2 shadow-lg shadow-emerald-600/20"
                  >
                    <Download className="w-4 h-4" />
                    Save / Download MP4
                  </a>
                </div>
              ) : (
                <div className="border border-dashed border-slate-800 rounded-xl p-8 text-center bg-slate-950/30 text-slate-500 space-y-2">
                  <Video className="w-8 h-8 mx-auto opacity-30" />
                  <p className="text-xs">Your completed remix preview and download button will appear here once rendering is complete.</p>
                </div>
              )}
            </section>

          </div>
        </div>

      </main>

      {/* Footer */}
      <footer className="border-t border-slate-900 bg-slate-950/80 py-4 text-center text-xs text-slate-500">
        <p>Licensed Music Remix • Safe Copyright Architecture • YouTube Data API v3 &amp; FFmpeg Engine</p>
      </footer>
    </div>
  );
}
