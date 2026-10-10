import React, { useState, useEffect, useRef } from 'react';
import {
  Play,
  Pause,
  Square,
  RotateCcw,
  RefreshCw,
  Upload,
  Music,
  Video,
  FileSpreadsheet,
  CheckCircle2,
  AlertCircle,
  Clock,
  Sparkles,
  ShieldCheck,
  Smartphone,
  Settings,
  ExternalLink,
  Trash2,
  Volume2,
  Sliders,
  Bell,
  Layers,
  Film,
  Check,
  Plus,
  Info,
  Radio,
  Eye,
  CheckSquare
} from 'lucide-react';

const YouTubeIcon = ({ className = "w-4 h-4" }: { className?: string }) => (
  <svg className={className} viewBox="0 0 24 24" fill="currentColor">
    <path d="M23.498 6.186a3.016 3.016 0 0 0-2.122-2.136C19.505 3.545 12 3.545 12 3.545s-7.505 0-9.377.505A3.017 3.017 0 0 0 .502 6.186C0 8.07 0 12 0 12s0 3.93.502 5.814a3.016 3.016 0 0 0 2.122 2.136c1.871.505 9.376.505 9.376.505s7.505 0 9.377-.505a3.015 3.015 0 0 0 2.122-2.136C24 15.93 24 12 24 12s0-3.93-.502-5.814zM9.545 15.568V8.432L15.818 12l-6.273 3.568z"/>
  </svg>
);

const GoogleSheetsIcon = ({ className = "w-4 h-4" }: { className?: string }) => (
  <svg className={className} viewBox="0 0 24 24" fill="currentColor">
    <path d="M19 3H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zM9 17H6v-2h3v2zm0-4H6v-2h3v2zm0-4H6V7h3v2zm4 8h-3v-2h3v2zm0-4h-3v-2h3v2zm0-4h-3V7h3v2zm5 8h-4v-2h4v2zm0-4h-4v-2h4v2zm0-4h-4V7h4v2z" fill="#0F9D58"/>
  </svg>
);

interface MusicTrack {
  id: string;
  filename: string;
  originalName: string;
  duration: number;
  size: number;
  uploadedAt: string;
  isDefault: boolean;
  url: string;
}

interface AutomationJob {
  id: string;
  rowIndex: number;
  videoUrl: string;
  videoTitle: string;
  videoDescription: string;
  thumbnailUrl: string;
  musicSelection: string;
  musicVolume: number;
  uploadStatus: 'READY' | 'PROCESSING' | 'UPLOADED' | 'FAILED';
  scheduledUploadTime: string;
  youtubeVideoUrl: string;
  errorMessage: string;
  downloadUrl: string;
  updatedAt: string;
}

interface QueueState {
  status: 'idle' | 'running' | 'paused' | 'stopped';
  currentJobId: string | null;
  currentJobTitle: string | null;
  currentStep: string | null;
  progress: number;
  youtubeUploadProgress: number;
  lastUpdated: string;
}

interface QueueStats {
  total: number;
  ready: number;
  processing: number;
  uploaded: number;
  failed: number;
}

interface UploadHistoryItem {
  id: string;
  title: string;
  youtubeUrl: string;
  videoId: string;
  uploadedAt: string;
  thumbnail: string;
}

interface AppSettings {
  defaultDescription: string;
  musicVolume: number;
  videoVolume: number;
  replaceOriginalAudio: boolean;
  fadeIn: number;
  fadeOut: number;
  videoPrivacy: 'public' | 'unlisted' | 'private';
  retentionHours: number;
}

export default function App() {
  const [activeTab, setActiveTab] = useState<'dashboard' | 'queue' | 'music' | 'history' | 'settings'>('dashboard');

  // Backend state
  const [queueState, setQueueState] = useState<QueueState>({
    status: 'idle',
    currentJobId: null,
    currentJobTitle: null,
    currentStep: null,
    progress: 0,
    youtubeUploadProgress: 0,
    lastUpdated: new Date().toISOString()
  });
  const [stats, setStats] = useState<QueueStats>({ total: 0, ready: 0, processing: 0, uploaded: 0, failed: 0 });
  const [jobs, setJobs] = useState<AutomationJob[]>([]);
  const [latestUpload, setLatestUpload] = useState<UploadHistoryItem | null>(null);
  const [musicLibrary, setMusicLibrary] = useState<MusicTrack[]>([]);
  const [uploadHistory, setUploadHistory] = useState<UploadHistoryItem[]>([]);
  const [settings, setSettings] = useState<AppSettings>({
    defaultDescription: '',
    musicVolume: 0.8,
    videoVolume: 1.0,
    replaceOriginalAudio: false,
    fadeIn: 1.0,
    fadeOut: 1.5,
    videoPrivacy: 'public',
    retentionHours: 24
  });

  // Auth & Sheets Config
  const [authStatus, setAuthStatus] = useState<{
    isConnected: boolean;
    googleEmail: string | null;
    channelTitle: string | null;
    channelId: string | null;
    oAuthClientId: string | null;
  }>({
    isConnected: false,
    googleEmail: null,
    channelTitle: null,
    channelId: null,
    oAuthClientId: null
  });

  const [sheetConfig, setSheetConfig] = useState<{
    spreadsheetId: string | null;
    spreadsheetUrl: string | null;
    title: string | null;
  }>({
    spreadsheetId: null,
    spreadsheetUrl: null,
    title: null
  });

  // UI state
  const [isRefreshingSheet, setIsRefreshingSheet] = useState(false);
  const [isUploadingMusic, setIsUploadingMusic] = useState(false);
  const [showAddJobModal, setShowAddJobModal] = useState(false);
  const [showConnectSheetModal, setShowConnectSheetModal] = useState(false);
  const [showAuthTokenModal, setShowAuthTokenModal] = useState(false);
  const [manualTokenInput, setManualTokenInput] = useState('');
  const [sheetUrlInput, setSheetUrlInput] = useState('');
  const [statusMessage, setStatusMessage] = useState<{ type: 'success' | 'error' | 'info'; text: string } | null>(null);

  // New Job Form State
  const [newJobVideoUrl, setNewJobVideoUrl] = useState('/uploads/videos/sample_anime_clip.mp4');
  const [newJobTitle, setNewJobTitle] = useState('🔥 Insane Anime Moment #2');
  const [newJobDescription, setNewJobDescription] = useState('');
  const [newJobMusic, setNewJobMusic] = useState('');
  const [newJobVolume, setNewJobVolume] = useState('0.8');

  const musicFileInputRef = useRef<HTMLInputElement>(null);

  // Polling for status updates every 2.5s
  useEffect(() => {
    fetchInitialData();
    const timer = setInterval(() => {
      fetchAutomationStatus();
    }, 2500);
    return () => clearInterval(timer);
  }, []);

  const showToast = (type: 'success' | 'error' | 'info', text: string) => {
    setStatusMessage({ type, text });
    setTimeout(() => setStatusMessage(null), 5000);
  };

  const fetchInitialData = async () => {
    await Promise.all([
      fetchAutomationStatus(),
      fetchMusicLibrary(),
      fetchSheetConfig(),
      fetchAuthStatus(),
      fetchSettings(),
      fetchHistory()
    ]);
  };

  const fetchAutomationStatus = async () => {
    try {
      const res = await fetch('/api/automation/status');
      if (res.ok) {
        const data = await res.json();
        setQueueState(data.queueState);
        setStats(data.stats);
        setJobs(data.jobs || []);
        if (data.latestUpload) setLatestUpload(data.latestUpload);
      }
    } catch (e) {
      // offline/transient
    }
  };

  const fetchMusicLibrary = async () => {
    try {
      const res = await fetch('/api/music');
      if (res.ok) {
        const data = await res.json();
        setMusicLibrary(data);
      }
    } catch (e) {}
  };

  const fetchSheetConfig = async () => {
    try {
      const res = await fetch('/api/sheets/config');
      if (res.ok) {
        const data = await res.json();
        setSheetConfig(data);
      }
    } catch (e) {}
  };

  const fetchAuthStatus = async () => {
    try {
      const res = await fetch('/api/auth/status');
      if (res.ok) {
        const data = await res.json();
        setAuthStatus(data);
      }
    } catch (e) {}
  };

  const fetchSettings = async () => {
    try {
      const res = await fetch('/api/settings');
      if (res.ok) {
        const data = await res.json();
        setSettings(data);
        if (!newJobDescription) {
          setNewJobDescription(data.defaultDescription);
        }
      }
    } catch (e) {}
  };

  const fetchHistory = async () => {
    try {
      const res = await fetch('/api/automation/history');
      if (res.ok) {
        const data = await res.json();
        setUploadHistory(data);
      }
    } catch (e) {}
  };

  // Google OAuth Flow with GIS client
  const handleConnectGoogle = () => {
    if (typeof (window as any).google !== 'undefined' && (window as any).google.accounts?.oauth2) {
      try {
        const client = (window as any).google.accounts.oauth2.initTokenClient({
          client_id: authStatus.oAuthClientId || '995356758986-gj5v0ev7rbvuk5ub3516tnp06id9fq3o.apps.googleusercontent.com',
          scope: 'https://www.googleapis.com/auth/spreadsheets https://www.googleapis.com/auth/youtube.upload https://www.googleapis.com/auth/youtube.readonly',
          callback: async (tokenResponse: any) => {
            if (tokenResponse && tokenResponse.access_token) {
              await saveAccessToken(tokenResponse.access_token, tokenResponse.expires_in);
            }
          }
        });
        client.requestAccessToken();
      } catch (err: any) {
        setShowAuthTokenModal(true);
      }
    } else {
      setShowAuthTokenModal(true);
    }
  };

  const saveAccessToken = async (token: string, expiresIn?: number) => {
    try {
      const res = await fetch('/api/auth/save-token', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ accessToken: token, expiresIn })
      });
      const data = await res.json();
      if (res.ok) {
        showToast('success', `Connected to Google! Channel: ${data.channelTitle || 'Authorized'}`);
        fetchAuthStatus();
        setShowAuthTokenModal(false);
        setManualTokenInput('');
      } else {
        showToast('error', data.error || 'Failed to authenticate');
      }
    } catch (err: any) {
      showToast('error', err.message);
    }
  };

  const handleDisconnectGoogle = async () => {
    await fetch('/api/auth/disconnect', { method: 'POST' });
    fetchAuthStatus();
    showToast('info', 'Disconnected Google account');
  };

  // Google Sheets Actions
  const handleCreateSheet = async () => {
    if (!authStatus.isConnected) {
      showToast('error', 'Please connect your Google Account first.');
      handleConnectGoogle();
      return;
    }
    setIsRefreshingSheet(true);
    try {
      const res = await fetch('/api/sheets/create', { method: 'POST' });
      const data = await res.json();
      if (res.ok) {
        setSheetConfig(data);
        showToast('success', 'Created automation sheet in your Google Drive!');
        fetchAutomationStatus();
      } else {
        showToast('error', data.error || 'Failed to create sheet');
      }
    } catch (e: any) {
      showToast('error', e.message);
    } finally {
      setIsRefreshingSheet(false);
    }
  };

  const handleConnectExistingSheet = async () => {
    if (!sheetUrlInput.trim()) return;
    try {
      const res = await fetch('/api/sheets/connect', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ spreadsheetIdOrUrl: sheetUrlInput.trim() })
      });
      const data = await res.json();
      if (res.ok) {
        setSheetConfig(data);
        setShowConnectSheetModal(false);
        showToast('success', 'Connected to Google Sheet!');
        fetchAutomationStatus();
      } else {
        showToast('error', data.error || 'Failed to connect sheet');
      }
    } catch (e: any) {
      showToast('error', e.message);
    }
  };

  const handleRefreshSheet = async () => {
    setIsRefreshingSheet(true);
    try {
      const res = await fetch('/api/sheets/refresh', { method: 'POST' });
      const data = await res.json();
      if (res.ok) {
        setJobs(data.jobs);
        showToast('success', 'Refreshed queue from Google Sheet');
        fetchAutomationStatus();
      } else {
        showToast('error', data.error || 'Failed to refresh sheet');
      }
    } catch (e: any) {
      showToast('error', e.message);
    } finally {
      setIsRefreshingSheet(false);
    }
  };

  const handleAddJob = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      const res = await fetch('/api/sheets/add-row', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          videoUrl: newJobVideoUrl,
          videoTitle: newJobTitle,
          videoDescription: newJobDescription,
          musicSelection: newJobMusic,
          musicVolume: newJobVolume
        })
      });
      const data = await res.json();
      if (res.ok) {
        showToast('success', 'Added video to queue and spreadsheet!');
        setShowAddJobModal(false);
        fetchAutomationStatus();
      } else {
        showToast('error', data.error || 'Failed to add video');
      }
    } catch (e: any) {
      showToast('error', e.message);
    }
  };

  // Music Library Actions
  // Notice: Strict Upload Music button ONLY (no music URL input)
  const handleMusicUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    setIsUploadingMusic(true);
    const formData = new FormData();
    formData.append('music', file);

    try {
      const res = await fetch('/api/music/upload', {
        method: 'POST',
        body: formData
      });
      const data = await res.json();
      if (res.ok) {
        showToast('success', `Uploaded "${file.name}" to Music Library!`);
        fetchMusicLibrary();
      } else {
        showToast('error', data.error || 'Failed to upload music');
      }
    } catch (err: any) {
      showToast('error', err.message);
    } finally {
      setIsUploadingMusic(false);
      if (musicFileInputRef.current) musicFileInputRef.current.value = '';
    }
  };

  const handleSetDefaultMusic = async (id: string) => {
    try {
      const res = await fetch(`/api/music/set-default/${id}`, { method: 'POST' });
      if (res.ok) {
        showToast('success', 'Set as default background track');
        fetchMusicLibrary();
      }
    } catch (e) {}
  };

  const handleDeleteMusic = async (id: string) => {
    if (!confirm('Are you sure you want to delete this audio track?')) return;
    try {
      const res = await fetch(`/api/music/${id}`, { method: 'DELETE' });
      if (res.ok) {
        showToast('info', 'Track removed from library');
        fetchMusicLibrary();
      }
    } catch (e) {}
  };

  // Queue Control Actions
  const handleStartAutomation = async () => {
    try {
      const res = await fetch('/api/automation/start', { method: 'POST' });
      if (res.ok) {
        showToast('success', 'Automation started! Background worker running.');
        fetchAutomationStatus();
        if ('Notification' in window && Notification.permission === 'default') {
          Notification.requestPermission();
        }
      }
    } catch (e: any) {
      showToast('error', e.message);
    }
  };

  const handlePauseQueue = async () => {
    try {
      const res = await fetch('/api/automation/pause', { method: 'POST' });
      if (res.ok) {
        showToast('info', 'Queue paused.');
        fetchAutomationStatus();
      }
    } catch (e: any) {
      showToast('error', e.message);
    }
  };

  const handleStopAutomation = async () => {
    try {
      const res = await fetch('/api/automation/stop', { method: 'POST' });
      if (res.ok) {
        showToast('info', 'Automation stopped.');
        fetchAutomationStatus();
      }
    } catch (e: any) {
      showToast('error', e.message);
    }
  };

  const handleRetryFailed = async () => {
    try {
      const res = await fetch('/api/automation/retry-failed', { method: 'POST' });
      if (res.ok) {
        showToast('success', 'Failed jobs reset to READY.');
        fetchAutomationStatus();
      }
    } catch (e: any) {
      showToast('error', e.message);
    }
  };

  const handleSaveSettings = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      const res = await fetch('/api/settings', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(settings)
      });
      if (res.ok) {
        showToast('success', 'Automation settings saved!');
      }
    } catch (e: any) {
      showToast('error', e.message);
    }
  };

  const formatSeconds = (sec: number) => {
    const m = Math.floor(sec / 60);
    const s = Math.floor(sec % 60);
    return `${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}`;
  };

  const formatBytes = (bytes: number) => {
    if (bytes === 0) return '0 Bytes';
    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(1)) + ' ' + sizes[i];
  };

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100 flex flex-col font-sans selection:bg-sky-500 selection:text-white pb-16 sm:pb-0">
      {/* Top Navbar */}
      <header className="border-b border-slate-800/80 bg-slate-900/80 backdrop-blur-md sticky top-0 z-40">
        <div className="max-w-7xl mx-auto px-4 py-3 flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className="h-10 w-10 rounded-xl bg-gradient-to-tr from-sky-600 via-indigo-600 to-purple-600 flex items-center justify-center shadow-lg shadow-sky-500/20 ring-1 ring-white/15">
              <Sparkles className="w-5 h-5 text-white animate-pulse" />
            </div>
            <div>
              <div className="flex items-center gap-2">
                <h1 className="font-bold text-base sm:text-lg leading-tight tracking-tight text-white">
                  AI YouTube Automation
                </h1>
                <span className="text-[10px] font-bold uppercase tracking-wider px-2 py-0.5 rounded-full bg-emerald-500/10 text-emerald-400 border border-emerald-500/25 hidden sm:inline-block">
                  Live System
                </span>
              </div>
              <p className="text-xs text-slate-400">
                Google Sheets Queue • Server-Side FFmpeg • YouTube Data API v3
              </p>
            </div>
          </div>

          <div className="flex items-center gap-2 sm:gap-3">
            {/* APK Download Button */}
            <a
              href="/api/download-apk"
              download="LicensedMusicRemix.apk"
              className="flex items-center gap-1.5 text-xs font-semibold text-sky-300 hover:text-white bg-sky-950/70 hover:bg-sky-900/80 border border-sky-700/60 px-3 py-1.5 rounded-lg transition shadow-sm"
              title="Download Android APK"
            >
              <Smartphone className="w-4 h-4 text-sky-400" />
              <span className="hidden sm:inline">Download APK</span>
              <span className="text-[10px] bg-sky-500/25 text-sky-200 px-1.5 py-0.2 rounded font-mono">
                23 MB
              </span>
            </a>

            {/* Google Account Connection Pill */}
            {authStatus.isConnected ? (
              <div className="flex items-center gap-2 bg-emerald-950/40 border border-emerald-700/50 px-3 py-1.5 rounded-lg text-xs text-emerald-300">
                <span className="w-2 h-2 rounded-full bg-emerald-400 animate-pulse"></span>
                <span className="font-medium truncate max-w-[120px] sm:max-w-[160px]">
                  {authStatus.channelTitle || authStatus.googleEmail || 'YouTube Connected'}
                </span>
                <button
                  onClick={handleDisconnectGoogle}
                  className="text-slate-400 hover:text-red-400 ml-1 text-[11px]"
                  title="Disconnect"
                >
                  ✕
                </button>
              </div>
            ) : (
              <button
                onClick={handleConnectGoogle}
                className="flex items-center gap-1.5 text-xs font-semibold text-white bg-red-600 hover:bg-red-500 px-3 py-1.5 rounded-lg transition shadow-md shadow-red-600/20"
              >
                <YouTubeIcon className="w-4 h-4" />
                <span>Connect YouTube &amp; Sheets</span>
              </button>
            )}
          </div>
        </div>

        {/* Navigation Tabs */}
        <div className="max-w-7xl mx-auto px-4 flex gap-1 sm:gap-2 overflow-x-auto border-t border-slate-800/60 py-1.5 scrollbar-none">
          <button
            onClick={() => setActiveTab('dashboard')}
            className={`px-3.5 py-1.5 rounded-lg text-xs font-semibold transition flex items-center gap-2 whitespace-nowrap ${
              activeTab === 'dashboard'
                ? 'bg-sky-600 text-white shadow-sm'
                : 'text-slate-400 hover:text-slate-200 hover:bg-slate-800/60'
            }`}
          >
            <Radio className="w-3.5 h-3.5" />
            Dashboard
          </button>
          <button
            onClick={() => setActiveTab('queue')}
            className={`px-3.5 py-1.5 rounded-lg text-xs font-semibold transition flex items-center gap-2 whitespace-nowrap ${
              activeTab === 'queue'
                ? 'bg-sky-600 text-white shadow-sm'
                : 'text-slate-400 hover:text-slate-200 hover:bg-slate-800/60'
            }`}
          >
            <FileSpreadsheet className="w-3.5 h-3.5 text-emerald-400" />
            Sheets Queue ({stats.total})
          </button>
          <button
            onClick={() => setActiveTab('music')}
            className={`px-3.5 py-1.5 rounded-lg text-xs font-semibold transition flex items-center gap-2 whitespace-nowrap ${
              activeTab === 'music'
                ? 'bg-sky-600 text-white shadow-sm'
                : 'text-slate-400 hover:text-slate-200 hover:bg-slate-800/60'
            }`}
          >
            <Music className="w-3.5 h-3.5 text-purple-400" />
            Music Library ({musicLibrary.length})
          </button>
          <button
            onClick={() => setActiveTab('history')}
            className={`px-3.5 py-1.5 rounded-lg text-xs font-semibold transition flex items-center gap-2 whitespace-nowrap ${
              activeTab === 'history'
                ? 'bg-sky-600 text-white shadow-sm'
                : 'text-slate-400 hover:text-slate-200 hover:bg-slate-800/60'
            }`}
          >
            <CheckCircle2 className="w-3.5 h-3.5 text-emerald-400" />
            Uploaded Videos ({stats.uploaded})
          </button>
          <button
            onClick={() => setActiveTab('settings')}
            className={`px-3.5 py-1.5 rounded-lg text-xs font-semibold transition flex items-center gap-2 whitespace-nowrap ${
              activeTab === 'settings'
                ? 'bg-sky-600 text-white shadow-sm'
                : 'text-slate-400 hover:text-slate-200 hover:bg-slate-800/60'
            }`}
          >
            <Settings className="w-3.5 h-3.5 text-amber-400" />
            Settings
          </button>
        </div>
      </header>

      {/* Toast Banner */}
      {statusMessage && (
        <div
          className={`max-w-7xl mx-auto px-4 mt-3 w-full animate-fadeIn transition-all`}
        >
          <div
            className={`p-3 rounded-xl border text-xs sm:text-sm flex items-center justify-between ${
              statusMessage.type === 'success'
                ? 'bg-emerald-950/70 border-emerald-700/60 text-emerald-300'
                : statusMessage.type === 'error'
                ? 'bg-red-950/70 border-red-700/60 text-red-300'
                : 'bg-sky-950/70 border-sky-700/60 text-sky-300'
            }`}
          >
            <div className="flex items-center gap-2.5">
              {statusMessage.type === 'success' && <CheckCircle2 className="w-4 h-4 shrink-0" />}
              {statusMessage.type === 'error' && <AlertCircle className="w-4 h-4 shrink-0" />}
              {statusMessage.type === 'info' && <Info className="w-4 h-4 shrink-0" />}
              <span>{statusMessage.text}</span>
            </div>
            <button onClick={() => setStatusMessage(null)} className="opacity-70 hover:opacity-100">
              ✕
            </button>
          </div>
        </div>
      )}

      {/* Main Content Body */}
      <main className="flex-1 max-w-7xl w-full mx-auto px-4 py-5 space-y-6">
        
        {/* TAB 1: DASHBOARD */}
        {activeTab === 'dashboard' && (
          <div className="space-y-6">
            {/* Primary Automation Control Header */}
            <div className="bg-gradient-to-r from-slate-900 via-slate-900 to-indigo-950/50 border border-slate-800/80 rounded-2xl p-4 sm:p-6 shadow-xl relative overflow-hidden">
              <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
                <div className="space-y-1">
                  <div className="flex items-center gap-2">
                    <span className="text-xs font-semibold tracking-wider uppercase text-sky-400">
                      Persistent Cloud Queue Worker
                    </span>
                    <span
                      className={`text-[10px] font-bold px-2 py-0.5 rounded-full uppercase ${
                        queueState.status === 'running'
                          ? 'bg-emerald-500/20 text-emerald-400 border border-emerald-500/30 animate-pulse'
                          : queueState.status === 'paused'
                          ? 'bg-amber-500/20 text-amber-400 border border-amber-500/30'
                          : queueState.status === 'stopped'
                          ? 'bg-red-500/20 text-red-400 border border-red-500/30'
                          : 'bg-slate-800 text-slate-400'
                      }`}
                    >
                      ● Status: {queueState.status}
                    </span>
                  </div>
                  <h2 className="text-xl sm:text-2xl font-extrabold text-white">
                    Automated Video &amp; Music Pipeline
                  </h2>
                  <p className="text-xs sm:text-sm text-slate-400 max-w-2xl leading-relaxed">
                    Processes Google Sheets rows marked <strong className="text-emerald-400">READY</strong> in the background. Runs continuously on the server even when your mobile phone is locked or browser is closed.
                  </p>
                </div>

                {/* Automation Action Buttons */}
                <div className="flex flex-wrap items-center gap-2.5">
                  {queueState.status !== 'running' ? (
                    <button
                      onClick={handleStartAutomation}
                      className="px-5 py-2.5 rounded-xl bg-emerald-600 hover:bg-emerald-500 text-white font-bold text-sm transition flex items-center gap-2 shadow-lg shadow-emerald-600/30"
                    >
                      <Play className="w-4 h-4 fill-white" />
                      START AUTOMATION
                    </button>
                  ) : (
                    <button
                      onClick={handlePauseQueue}
                      className="px-5 py-2.5 rounded-xl bg-amber-600 hover:bg-amber-500 text-white font-bold text-sm transition flex items-center gap-2 shadow-lg shadow-amber-600/30"
                    >
                      <Pause className="w-4 h-4" />
                      PAUSE QUEUE
                    </button>
                  )}

                  <button
                    onClick={handleStopAutomation}
                    className="px-4 py-2.5 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-200 hover:text-white font-semibold text-sm transition border border-slate-700 flex items-center gap-2"
                  >
                    <Square className="w-4 h-4 text-red-400" />
                    STOP
                  </button>

                  <button
                    onClick={handleRefreshSheet}
                    disabled={isRefreshingSheet}
                    className="px-4 py-2.5 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-200 font-semibold text-sm transition border border-slate-700 flex items-center gap-2"
                    title="Refresh spreadsheet rows"
                  >
                    <RefreshCw className={`w-4 h-4 ${isRefreshingSheet ? 'animate-spin text-sky-400' : ''}`} />
                    REFRESH SHEET
                  </button>

                  {stats.failed > 0 && (
                    <button
                      onClick={handleRetryFailed}
                      className="px-3.5 py-2.5 rounded-xl bg-red-950/80 hover:bg-red-900 border border-red-700 text-red-200 font-semibold text-xs transition flex items-center gap-1.5"
                    >
                      <RotateCcw className="w-3.5 h-3.5" />
                      RETRY FAILED ({stats.failed})
                    </button>
                  )}
                </div>
              </div>

              {/* Live Processing Card (If running or has active step) */}
              {(queueState.status === 'running' || queueState.currentJobTitle) && (
                <div className="mt-5 p-4 rounded-xl bg-slate-950/80 border border-sky-500/30 space-y-3">
                  <div className="flex items-center justify-between text-xs">
                    <span className="font-semibold text-sky-300 flex items-center gap-2">
                      <span className="w-2 h-2 rounded-full bg-sky-400 animate-ping"></span>
                      Currently Processing:
                    </span>
                    <span className="font-mono text-slate-400">
                      Overall Progress: {queueState.progress}%
                    </span>
                  </div>

                  <p className="font-bold text-sm sm:text-base text-white truncate">
                    {queueState.currentJobTitle || 'Next ready row...'}
                  </p>

                  {/* Progress Bars */}
                  <div className="space-y-1.5">
                    <div className="w-full bg-slate-800 rounded-full h-2.5 overflow-hidden">
                      <div
                        className="bg-gradient-to-r from-sky-500 to-indigo-500 h-2.5 rounded-full transition-all duration-500"
                        style={{ width: `${queueState.progress}%` }}
                      ></div>
                    </div>
                    <div className="flex items-center justify-between text-[11px] text-slate-400">
                      <span>{queueState.currentStep || 'Processing media...'}</span>
                      {queueState.youtubeUploadProgress > 0 && (
                        <span className="text-red-400 font-medium flex items-center gap-1">
                          <YouTubeIcon className="w-3 h-3" />
                          YouTube Upload: {queueState.youtubeUploadProgress}%
                        </span>
                      )}
                    </div>
                  </div>
                </div>
              )}
            </div>

            {/* Metric Statistics Cards */}
            <div className="grid grid-cols-2 sm:grid-cols-5 gap-3 sm:gap-4">
              <div className="bg-slate-900/70 border border-slate-800 rounded-2xl p-4 flex flex-col justify-between">
                <span className="text-xs font-semibold text-slate-400 uppercase tracking-wider">
                  Total in Queue
                </span>
                <div className="mt-2 flex items-baseline justify-between">
                  <span className="text-2xl sm:text-3xl font-extrabold text-white">{stats.total}</span>
                  <FileSpreadsheet className="w-5 h-5 text-slate-500" />
                </div>
              </div>

              <div className="bg-slate-900/70 border border-slate-800 rounded-2xl p-4 flex flex-col justify-between">
                <span className="text-xs font-semibold text-emerald-400 uppercase tracking-wider">
                  Uploaded to YouTube
                </span>
                <div className="mt-2 flex items-baseline justify-between">
                  <span className="text-2xl sm:text-3xl font-extrabold text-emerald-400">{stats.uploaded}</span>
                  <CheckCircle2 className="w-5 h-5 text-emerald-500/80" />
                </div>
              </div>

              <div className="bg-slate-900/70 border border-slate-800 rounded-2xl p-4 flex flex-col justify-between">
                <span className="text-xs font-semibold text-sky-400 uppercase tracking-wider">
                  Waiting (READY)
                </span>
                <div className="mt-2 flex items-baseline justify-between">
                  <span className="text-2xl sm:text-3xl font-extrabold text-sky-400">{stats.ready}</span>
                  <Clock className="w-5 h-5 text-sky-500/80" />
                </div>
              </div>

              <div className="bg-slate-900/70 border border-slate-800 rounded-2xl p-4 flex flex-col justify-between">
                <span className="text-xs font-semibold text-indigo-400 uppercase tracking-wider">
                  Processing Now
                </span>
                <div className="mt-2 flex items-baseline justify-between">
                  <span className="text-2xl sm:text-3xl font-extrabold text-indigo-400">{stats.processing}</span>
                  <Film className="w-5 h-5 text-indigo-500/80" />
                </div>
              </div>

              <div className="col-span-2 sm:col-span-1 bg-slate-900/70 border border-slate-800 rounded-2xl p-4 flex flex-col justify-between">
                <span className="text-xs font-semibold text-red-400 uppercase tracking-wider">
                  Failed Jobs
                </span>
                <div className="mt-2 flex items-baseline justify-between">
                  <span className="text-2xl sm:text-3xl font-extrabold text-red-400">{stats.failed}</span>
                  <AlertCircle className="w-5 h-5 text-red-500/80" />
                </div>
              </div>
            </div>

            {/* Quick Action Bar */}
            <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
              <button
                onClick={() => {
                  if (sheetConfig.spreadsheetUrl) {
                    window.open(sheetConfig.spreadsheetUrl, '_blank');
                  } else {
                    setShowConnectSheetModal(true);
                  }
                }}
                className="p-3.5 rounded-xl bg-slate-900 border border-slate-800 hover:border-emerald-500/50 text-left transition flex items-center justify-between group"
              >
                <div>
                  <p className="text-xs font-bold text-slate-200 group-hover:text-emerald-400">
                    {sheetConfig.spreadsheetId ? 'Open Google Sheet' : 'Connect Google Sheet'}
                  </p>
                  <p className="text-[11px] text-slate-400 truncate max-w-[140px]">
                    {sheetConfig.spreadsheetId ? 'Edit columns A-J' : 'Link spreadsheet'}
                  </p>
                </div>
                <GoogleSheetsIcon className="w-5 h-5" />
              </button>

              <button
                onClick={() => musicFileInputRef.current?.click()}
                className="p-3.5 rounded-xl bg-slate-900 border border-slate-800 hover:border-purple-500/50 text-left transition flex items-center justify-between group"
              >
                <div>
                  <p className="text-xs font-bold text-slate-200 group-hover:text-purple-400">
                    Upload Music
                  </p>
                  <p className="text-[11px] text-slate-400">
                    MP3, WAV, AAC, M4A
                  </p>
                </div>
                <Music className="w-5 h-5 text-purple-400" />
              </button>

              <button
                onClick={() => setShowAddJobModal(true)}
                className="p-3.5 rounded-xl bg-slate-900 border border-slate-800 hover:border-sky-500/50 text-left transition flex items-center justify-between group"
              >
                <div>
                  <p className="text-xs font-bold text-slate-200 group-hover:text-sky-400">
                    Add Video Row
                  </p>
                  <p className="text-[11px] text-slate-400">
                    Append to queue
                  </p>
                </div>
                <Plus className="w-5 h-5 text-sky-400" />
              </button>

              <button
                onClick={() => setActiveTab('history')}
                className="p-3.5 rounded-xl bg-slate-900 border border-slate-800 hover:border-red-500/50 text-left transition flex items-center justify-between group"
              >
                <div>
                  <p className="text-xs font-bold text-slate-200 group-hover:text-red-400">
                    View Uploaded
                  </p>
                  <p className="text-[11px] text-slate-400">
                    {stats.uploaded} published
                  </p>
                </div>
                <YouTubeIcon className="w-5 h-5 text-red-500" />
              </button>
            </div>

            {/* Latest Successful Upload Highlight */}
            {latestUpload && (
              <div className="rounded-2xl border border-emerald-900/50 bg-gradient-to-r from-emerald-950/40 via-slate-900 to-slate-900 p-4 sm:p-5 flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
                <div className="flex items-center gap-3.5 min-w-0">
                  <div className="h-12 w-12 rounded-xl bg-emerald-500/10 border border-emerald-500/30 flex items-center justify-center shrink-0 text-emerald-400">
                    <CheckCircle2 className="w-6 h-6" />
                  </div>
                  <div className="min-w-0 space-y-0.5">
                    <div className="flex items-center gap-2">
                      <span className="text-[11px] font-bold text-emerald-400 uppercase tracking-wide">
                        Latest Successful Upload
                      </span>
                      <span className="text-[11px] text-slate-500">
                        {new Date(latestUpload.uploadedAt).toLocaleTimeString()}
                      </span>
                    </div>
                    <p className="font-bold text-sm text-white truncate max-w-md">
                      {latestUpload.title}
                    </p>
                    <p className="text-xs text-slate-400 font-mono truncate">
                      {latestUpload.youtubeUrl}
                    </p>
                  </div>
                </div>

                <a
                  href={latestUpload.youtubeUrl}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="px-4 py-2 rounded-xl bg-red-600 hover:bg-red-500 text-white font-semibold text-xs transition flex items-center gap-2 shadow-md shadow-red-600/20 shrink-0"
                >
                  <YouTubeIcon className="w-4 h-4" />
                  Watch on YouTube
                  <ExternalLink className="w-3 h-3 ml-0.5" />
                </a>
              </div>
            )}

            {/* Video Queue Preview List */}
            <div className="bg-slate-900/70 border border-slate-800 rounded-2xl p-4 sm:p-5 space-y-4">
              <div className="flex items-center justify-between">
                <div>
                  <h3 className="font-bold text-base text-white flex items-center gap-2">
                    <FileSpreadsheet className="w-4 h-4 text-emerald-400" />
                    Spreadsheet Queue Items
                  </h3>
                  <p className="text-xs text-slate-400">
                    Showing next videos in queue. Only rows marked READY will be processed.
                  </p>
                </div>
                <button
                  onClick={() => setActiveTab('queue')}
                  className="text-xs text-sky-400 hover:text-sky-300 font-medium flex items-center gap-1"
                >
                  Full Sheet View →
                </button>
              </div>

              {jobs.length === 0 ? (
                <div className="text-center py-10 text-slate-500 text-sm">
                  No video rows in queue. Click "Add Video Row" or connect your Google Sheet!
                </div>
              ) : (
                <div className="divide-y divide-slate-800/80">
                  {jobs.slice(0, 5).map((job) => (
                    <div
                      key={job.id}
                      className="py-3 flex flex-col sm:flex-row sm:items-center justify-between gap-3 text-xs"
                    >
                      <div className="flex items-start gap-3 min-w-0">
                        <span className="font-mono text-slate-500 text-xs mt-0.5">
                          #{job.rowIndex}
                        </span>
                        <div className="min-w-0 space-y-0.5">
                          <p className="font-bold text-slate-100 text-sm truncate max-w-sm sm:max-w-md">
                            {job.videoTitle}
                          </p>
                          <p className="text-slate-400 text-xs truncate max-w-xs sm:max-w-md">
                            <span className="text-slate-500">Source:</span> {job.videoUrl}
                          </p>
                          {job.musicSelection && (
                            <p className="text-purple-400 text-[11px] flex items-center gap-1">
                              <Music className="w-3 h-3" />
                              {job.musicSelection}
                            </p>
                          )}
                          {job.errorMessage && (
                            <p className="text-red-400 text-[11px] font-medium flex items-center gap-1">
                              <AlertCircle className="w-3 h-3 shrink-0" />
                              {job.errorMessage}
                            </p>
                          )}
                        </div>
                      </div>

                      <div className="flex items-center gap-3 shrink-0 justify-between sm:justify-end">
                        <span
                          className={`px-2.5 py-1 rounded-full text-[10px] font-extrabold uppercase tracking-wider ${
                            job.uploadStatus === 'READY'
                              ? 'bg-sky-500/10 text-sky-400 border border-sky-500/20'
                              : job.uploadStatus === 'PROCESSING'
                              ? 'bg-amber-500/10 text-amber-400 border border-amber-500/20 animate-pulse'
                              : job.uploadStatus === 'UPLOADED'
                              ? 'bg-emerald-500/10 text-emerald-400 border border-emerald-500/20'
                              : 'bg-red-500/10 text-red-400 border border-red-500/20'
                          }`}
                        >
                          {job.uploadStatus}
                        </span>

                        {job.youtubeVideoUrl ? (
                          <a
                            href={job.youtubeVideoUrl}
                            target="_blank"
                            rel="noopener noreferrer"
                            className="px-2.5 py-1 rounded-lg bg-red-950/70 hover:bg-red-900 border border-red-800 text-red-300 font-semibold text-[11px] flex items-center gap-1"
                          >
                            <YouTubeIcon className="w-3.5 h-3.5" />
                            Watch
                          </a>
                        ) : job.downloadUrl ? (
                          <a
                            href={job.downloadUrl}
                            download
                            className="px-2.5 py-1 rounded-lg bg-slate-800 hover:bg-slate-700 border border-slate-700 text-slate-300 font-semibold text-[11px]"
                          >
                            Download MP4
                          </a>
                        ) : null}
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>
          </div>
        )}

        {/* TAB 2: GOOGLE SHEETS QUEUE */}
        {activeTab === 'queue' && (
          <div className="space-y-6">
            {/* Sheet Integration Header Card */}
            <div className="bg-slate-900/80 border border-slate-800 rounded-2xl p-5 space-y-4">
              <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
                <div className="space-y-1">
                  <div className="flex items-center gap-2">
                    <GoogleSheetsIcon className="w-5 h-5" />
                    <h2 className="text-lg font-bold text-white">
                      Google Sheets Live Synchronization
                    </h2>
                  </div>
                  <p className="text-xs text-slate-400">
                    Columns: A: Video URL | B: Title | C: Description | D: Thumbnail | E: Music | F: Volume | G: Status | H: Schedule | I: YouTube URL | J: Error
                  </p>
                </div>

                <div className="flex flex-wrap items-center gap-2">
                  {sheetConfig.spreadsheetId ? (
                    <a
                      href={sheetConfig.spreadsheetUrl || `https://docs.google.com/spreadsheets/d/${sheetConfig.spreadsheetId}`}
                      target="_blank"
                      rel="noopener noreferrer"
                      className="px-4 py-2 rounded-xl bg-emerald-600 hover:bg-emerald-500 text-white font-semibold text-xs flex items-center gap-2 transition"
                    >
                      <ExternalLink className="w-3.5 h-3.5" />
                      Open in Google Sheets
                    </a>
                  ) : (
                    <>
                      <button
                        onClick={handleCreateSheet}
                        disabled={isRefreshingSheet}
                        className="px-4 py-2 rounded-xl bg-emerald-600 hover:bg-emerald-500 text-white font-semibold text-xs flex items-center gap-2 transition"
                      >
                        <Plus className="w-3.5 h-3.5" />
                        Create Sheet in My Drive
                      </button>
                      <button
                        onClick={() => setShowConnectSheetModal(true)}
                        className="px-3.5 py-2 rounded-xl bg-slate-800 hover:bg-slate-700 border border-slate-700 text-slate-200 font-semibold text-xs transition"
                      >
                        Connect by ID/URL
                      </button>
                    </>
                  )}

                  <button
                    onClick={handleRefreshSheet}
                    disabled={isRefreshingSheet}
                    className="px-3.5 py-2 rounded-xl bg-slate-800 hover:bg-slate-700 border border-slate-700 text-slate-200 font-semibold text-xs flex items-center gap-1.5 transition"
                  >
                    <RefreshCw className={`w-3.5 h-3.5 ${isRefreshingSheet ? 'animate-spin' : ''}`} />
                    Refresh
                  </button>

                  <button
                    onClick={() => setShowAddJobModal(true)}
                    className="px-4 py-2 rounded-xl bg-sky-600 hover:bg-sky-500 text-white font-semibold text-xs flex items-center gap-1.5 transition shadow-sm"
                  >
                    <Plus className="w-3.5 h-3.5" />
                    Add Video
                  </button>
                </div>
              </div>

              {/* Status Banner */}
              <div className="bg-slate-950/60 border border-slate-800/80 rounded-xl p-3 flex flex-wrap items-center justify-between text-xs text-slate-400 gap-2">
                <div className="flex items-center gap-2">
                  <span className="font-semibold text-slate-300">Target Sheet:</span>
                  <span className="font-mono text-emerald-400">
                    {sheetConfig.spreadsheetId ? sheetConfig.spreadsheetId : 'Local Sync (Connect Sheet above)'}
                  </span>
                </div>
                <div className="flex items-center gap-3">
                  <span>Processed: <strong className="text-white">{stats.uploaded}</strong></span>
                  <span>Pending: <strong className="text-sky-400">{stats.ready}</strong></span>
                  <span>Failed: <strong className="text-red-400">{stats.failed}</strong></span>
                </div>
              </div>
            </div>

            {/* Spreadsheet Table View */}
            <div className="bg-slate-900/70 border border-slate-800 rounded-2xl overflow-hidden shadow-lg">
              <div className="overflow-x-auto">
                <table className="w-full text-left text-xs">
                  <thead className="bg-slate-950/90 text-slate-400 font-bold uppercase tracking-wider border-b border-slate-800">
                    <tr>
                      <th className="px-3.5 py-3 text-slate-500">Row</th>
                      <th className="px-3.5 py-3">A: Video URL</th>
                      <th className="px-3.5 py-3">B: Video Title</th>
                      <th className="px-3.5 py-3">E: Music Track</th>
                      <th className="px-3.5 py-3">F: Vol</th>
                      <th className="px-3.5 py-3 text-center">G: Status</th>
                      <th className="px-3.5 py-3">I: YouTube Result</th>
                      <th className="px-3.5 py-3">J: Error Log</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-800/60 font-sans">
                    {jobs.length === 0 ? (
                      <tr>
                        <td colSpan={8} className="px-4 py-8 text-center text-slate-500">
                          No rows currently in queue. Add a row or refresh from Google Sheets.
                        </td>
                      </tr>
                    ) : (
                      jobs.map((job) => (
                        <tr
                          key={job.id}
                          className="hover:bg-slate-800/30 transition-colors"
                        >
                          <td className="px-3.5 py-3 font-mono text-slate-500 font-medium">
                            {job.rowIndex}
                          </td>
                          <td className="px-3.5 py-3 max-w-[180px] truncate font-mono text-[11px] text-slate-300" title={job.videoUrl}>
                            {job.videoUrl}
                          </td>
                          <td className="px-3.5 py-3 font-medium text-slate-100 max-w-[200px] truncate" title={job.videoTitle}>
                            {job.videoTitle}
                          </td>
                          <td className="px-3.5 py-3 text-purple-300 max-w-[140px] truncate">
                            {job.musicSelection || '(Default)'}
                          </td>
                          <td className="px-3.5 py-3 font-mono text-slate-400">
                            {Math.round(job.musicVolume * 100)}%
                          </td>
                          <td className="px-3.5 py-3 text-center">
                            <span
                              className={`inline-block px-2 py-0.5 rounded-full text-[10px] font-extrabold uppercase ${
                                job.uploadStatus === 'READY'
                                  ? 'bg-sky-500/10 text-sky-400 border border-sky-500/20'
                                  : job.uploadStatus === 'PROCESSING'
                                  ? 'bg-amber-500/10 text-amber-400 border border-amber-500/20 animate-pulse'
                                  : job.uploadStatus === 'UPLOADED'
                                  ? 'bg-emerald-500/10 text-emerald-400 border border-emerald-500/20'
                                  : 'bg-red-500/10 text-red-400 border border-red-500/20'
                              }`}
                            >
                              {job.uploadStatus}
                            </span>
                          </td>
                          <td className="px-3.5 py-3">
                            {job.youtubeVideoUrl ? (
                              <a
                                href={job.youtubeVideoUrl}
                                target="_blank"
                                rel="noopener noreferrer"
                                className="text-red-400 hover:text-red-300 font-mono text-[11px] flex items-center gap-1 font-semibold"
                              >
                                <YouTubeIcon className="w-3 h-3" />
                                {job.youtubeVideoUrl.replace('https://www.youtube.com/watch?v=', '')}
                              </a>
                            ) : (
                              <span className="text-slate-600">—</span>
                            )}
                          </td>
                          <td className="px-3.5 py-3 max-w-[200px] truncate text-red-400 text-[11px]" title={job.errorMessage}>
                            {job.errorMessage || '—'}
                          </td>
                        </tr>
                      ))
                    )}
                  </tbody>
                </table>
              </div>
            </div>
          </div>
        )}

        {/* TAB 3: MUSIC LIBRARY */}
        {activeTab === 'music' && (
          <div className="space-y-6">
            <div className="bg-slate-900/80 border border-slate-800 rounded-2xl p-5 flex flex-col sm:flex-row sm:items-center justify-between gap-4">
              <div className="space-y-1">
                <div className="flex items-center gap-2">
                  <Music className="w-5 h-5 text-purple-400" />
                  <h2 className="text-lg font-bold text-white">Music Library</h2>
                </div>
                <p className="text-xs text-slate-400">
                  Upload and manage your background audio files. Supported formats: MP3, WAV, AAC, M4A. Audio is preserved 100% identically with original characteristics.
                </p>
              </div>

              {/* Upload Music Button - STRICT BUTTON ONLY (NO URL INPUT) */}
              <div>
                <input
                  type="file"
                  ref={musicFileInputRef}
                  onChange={handleMusicUpload}
                  accept=".mp3,.wav,.aac,.m4a,audio/*"
                  className="hidden"
                />
                <button
                  onClick={() => musicFileInputRef.current?.click()}
                  disabled={isUploadingMusic}
                  className="px-5 py-2.5 rounded-xl bg-purple-600 hover:bg-purple-500 text-white font-bold text-xs sm:text-sm transition flex items-center gap-2 shadow-lg shadow-purple-600/20"
                >
                  <Upload className={`w-4 h-4 ${isUploadingMusic ? 'animate-bounce' : ''}`} />
                  {isUploadingMusic ? 'Uploading Audio...' : 'UPLOAD MUSIC'}
                </button>
              </div>
            </div>

            {/* Music Tracks Grid */}
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              {musicLibrary.map((track) => (
                <div
                  key={track.id}
                  className={`bg-slate-900/70 border rounded-2xl p-4 space-y-3 transition ${
                    track.isDefault
                      ? 'border-purple-500/60 ring-1 ring-purple-500/20 bg-purple-950/10'
                      : 'border-slate-800'
                  }`}
                >
                  <div className="flex items-start justify-between gap-2">
                    <div className="flex items-start gap-3 min-w-0">
                      <div className="h-10 w-10 rounded-xl bg-purple-500/10 border border-purple-500/25 flex items-center justify-center shrink-0 text-purple-400">
                        <Music className="w-5 h-5" />
                      </div>
                      <div className="min-w-0 space-y-0.5">
                        <p className="font-bold text-sm text-white truncate max-w-xs">
                          {track.originalName}
                        </p>
                        <div className="flex items-center gap-2 text-xs text-slate-400">
                          <span className="font-mono">{formatSeconds(track.duration)}</span>
                          <span>•</span>
                          <span>{formatBytes(track.size)}</span>
                          {track.isDefault && (
                            <span className="text-[10px] font-bold bg-purple-500/20 text-purple-300 px-2 py-0.5 rounded-full border border-purple-500/30">
                              DEFAULT TRACK
                            </span>
                          )}
                        </div>
                      </div>
                    </div>

                    <div className="flex items-center gap-1.5 shrink-0">
                      {!track.isDefault && (
                        <button
                          onClick={() => handleSetDefaultMusic(track.id)}
                          className="px-2.5 py-1 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-300 font-semibold text-[11px] transition"
                          title="Make default track for queue"
                        >
                          Set Default
                        </button>
                      )}
                      <button
                        onClick={() => handleDeleteMusic(track.id)}
                        className="p-1.5 rounded-lg text-slate-500 hover:text-red-400 hover:bg-red-950/30 transition"
                        title="Delete audio"
                      >
                        <Trash2 className="w-4 h-4" />
                      </button>
                    </div>
                  </div>

                  {/* Audio Preview Player */}
                  <audio
                    controls
                    src={track.url}
                    className="w-full h-9 rounded-lg"
                    preload="none"
                  />
                </div>
              ))}
            </div>
          </div>
        )}

        {/* TAB 4: UPLOADED HISTORY */}
        {activeTab === 'history' && (
          <div className="space-y-6">
            <div className="bg-slate-900/80 border border-slate-800 rounded-2xl p-5 flex items-center justify-between">
              <div>
                <h2 className="text-lg font-bold text-white flex items-center gap-2">
                  <YouTubeIcon className="w-5 h-5 text-red-500" />
                  Successfully Uploaded Videos
                </h2>
                <p className="text-xs text-slate-400">
                  Direct list of all videos remixed with licensed audio and published to your connected YouTube channel.
                </p>
              </div>
              <span className="text-xs font-mono bg-slate-800 text-slate-300 px-3 py-1 rounded-lg">
                Total: {uploadHistory.length}
              </span>
            </div>

            {uploadHistory.length === 0 ? (
              <div className="bg-slate-900/40 border border-slate-800 rounded-2xl p-12 text-center text-slate-500 text-sm">
                No videos uploaded yet. Start the automation queue to begin uploading!
              </div>
            ) : (
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                {uploadHistory.map((item) => (
                  <div
                    key={item.id}
                    className="bg-slate-900/70 border border-slate-800 rounded-2xl p-4 flex gap-4 items-center"
                  >
                    <img
                      src={item.thumbnail}
                      alt={item.title}
                      className="w-28 h-20 object-cover rounded-xl border border-slate-800 shrink-0 bg-slate-950"
                    />
                    <div className="min-w-0 flex-1 space-y-1">
                      <p className="font-bold text-sm text-white line-clamp-2">
                        {item.title}
                      </p>
                      <p className="text-xs text-slate-400 font-mono">
                        Published: {new Date(item.uploadedAt).toLocaleString()}
                      </p>
                      <a
                        href={item.youtubeUrl}
                        target="_blank"
                        rel="noopener noreferrer"
                        className="inline-flex items-center gap-1.5 text-xs text-red-400 hover:text-red-300 font-semibold pt-1"
                      >
                        <YouTubeIcon className="w-3.5 h-3.5" />
                        Watch Video
                        <ExternalLink className="w-3 h-3" />
                      </a>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        )}

        {/* TAB 5: SETTINGS */}
        {activeTab === 'settings' && (
          <div className="max-w-3xl mx-auto space-y-6">
            <form onSubmit={handleSaveSettings} className="bg-slate-900/80 border border-slate-800 rounded-2xl p-6 space-y-6">
              <div className="flex items-center justify-between border-b border-slate-800 pb-4">
                <div>
                  <h2 className="text-lg font-bold text-white flex items-center gap-2">
                    <Settings className="w-5 h-5 text-amber-400" />
                    Automation &amp; Upload Settings
                  </h2>
                  <p className="text-xs text-slate-400">
                    Configure default music volumes, video privacy, and remixed video description.
                  </p>
                </div>
                <button
                  type="submit"
                  className="px-5 py-2 rounded-xl bg-sky-600 hover:bg-sky-500 text-white font-bold text-xs transition"
                >
                  Save Settings
                </button>
              </div>

              {/* Default Description (Pre-filled with requested anime description) */}
              <div className="space-y-2">
                <label className="block text-xs font-bold uppercase tracking-wider text-slate-300">
                  Default Remixed Video Description
                </label>
                <textarea
                  rows={8}
                  value={settings.defaultDescription}
                  onChange={(e) => setSettings({ ...settings, defaultDescription: e.target.value })}
                  className="w-full bg-slate-950 border border-slate-700/80 rounded-xl p-3 text-xs sm:text-sm text-slate-100 placeholder-slate-500 font-mono focus:outline-none focus:ring-2 focus:ring-sky-500/50"
                  placeholder="Enter default YouTube description..."
                />
                <p className="text-[11px] text-slate-400">
                  This description is automatically applied to all automated YouTube video uploads unless overridden in Google Sheets column C.
                </p>
              </div>

              {/* Audio Controls */}
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <div className="space-y-2 bg-slate-950/60 p-4 rounded-xl border border-slate-800">
                  <div className="flex justify-between text-xs font-semibold text-slate-300">
                    <span>Background Music Volume</span>
                    <span className="font-mono text-purple-400">{Math.round(settings.musicVolume * 100)}%</span>
                  </div>
                  <input
                    type="range"
                    min="0"
                    max="1.5"
                    step="0.05"
                    value={settings.musicVolume}
                    onChange={(e) => setSettings({ ...settings, musicVolume: parseFloat(e.target.value) })}
                    className="w-full accent-purple-500"
                  />
                </div>

                <div className="space-y-2 bg-slate-950/60 p-4 rounded-xl border border-slate-800">
                  <div className="flex justify-between text-xs font-semibold text-slate-300">
                    <span>Original Video Speech Volume</span>
                    <span className="font-mono text-sky-400">{Math.round(settings.videoVolume * 100)}%</span>
                  </div>
                  <input
                    type="range"
                    min="0"
                    max="1.5"
                    step="0.05"
                    value={settings.videoVolume}
                    onChange={(e) => setSettings({ ...settings, videoVolume: parseFloat(e.target.value) })}
                    className="w-full accent-sky-500"
                  />
                </div>
              </div>

              {/* Fade in / Fade out */}
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <div className="space-y-1">
                  <label className="text-xs font-semibold text-slate-300">Audio Fade In (seconds)</label>
                  <input
                    type="number"
                    min="0"
                    max="5"
                    step="0.5"
                    value={settings.fadeIn}
                    onChange={(e) => setSettings({ ...settings, fadeIn: parseFloat(e.target.value) })}
                    className="w-full bg-slate-950 border border-slate-700 rounded-xl px-3 py-2 text-sm text-slate-100"
                  />
                </div>

                <div className="space-y-1">
                  <label className="text-xs font-semibold text-slate-300">Audio Fade Out (seconds)</label>
                  <input
                    type="number"
                    min="0"
                    max="5"
                    step="0.5"
                    value={settings.fadeOut}
                    onChange={(e) => setSettings({ ...settings, fadeOut: parseFloat(e.target.value) })}
                    className="w-full bg-slate-950 border border-slate-700 rounded-xl px-3 py-2 text-sm text-slate-100"
                  />
                </div>
              </div>

              {/* YouTube Privacy */}
              <div className="space-y-2">
                <label className="block text-xs font-bold uppercase tracking-wider text-slate-300">
                  Default YouTube Video Privacy
                </label>
                <div className="grid grid-cols-3 gap-3">
                  {(['public', 'unlisted', 'private'] as const).map((privacy) => (
                    <button
                      key={privacy}
                      type="button"
                      onClick={() => setSettings({ ...settings, videoPrivacy: privacy })}
                      className={`p-3 rounded-xl border text-xs font-bold uppercase transition text-center ${
                        settings.videoPrivacy === privacy
                          ? 'border-sky-500 bg-sky-500/10 text-sky-400'
                          : 'border-slate-800 bg-slate-950 text-slate-400 hover:text-slate-200'
                      }`}
                    >
                      {privacy}
                    </button>
                  ))}
                </div>
              </div>

              {/* Mute original video audio toggle */}
              <div className="flex items-center justify-between p-3.5 rounded-xl bg-slate-950 border border-slate-800">
                <div>
                  <p className="text-xs font-bold text-slate-200">Mute Original Video Audio</p>
                  <p className="text-[11px] text-slate-400">
                    If enabled, replaces the original audio completely with the licensed music track.
                  </p>
                </div>
                <input
                  type="checkbox"
                  checked={settings.replaceOriginalAudio}
                  onChange={(e) => setSettings({ ...settings, replaceOriginalAudio: e.target.checked })}
                  className="w-4 h-4 accent-sky-500 rounded"
                />
              </div>
            </form>
          </div>
        )}
      </main>

      {/* MODAL: ADD VIDEO ROW */}
      {showAddJobModal && (
        <div className="fixed inset-0 bg-slate-950/80 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-slate-900 border border-slate-800 rounded-2xl max-w-lg w-full p-6 space-y-4 shadow-2xl animate-scaleIn">
            <div className="flex items-center justify-between border-b border-slate-800 pb-3">
              <h3 className="font-bold text-base text-white flex items-center gap-2">
                <Plus className="w-4 h-4 text-sky-400" />
                Add Video to Queue &amp; Spreadsheet
              </h3>
              <button onClick={() => setShowAddJobModal(false)} className="text-slate-400 hover:text-white">
                ✕
              </button>
            </div>

            <form onSubmit={handleAddJob} className="space-y-4">
              <div className="space-y-1">
                <label className="text-xs font-semibold text-slate-300">Video URL or Server Path</label>
                <input
                  type="text"
                  required
                  value={newJobVideoUrl}
                  onChange={(e) => setNewJobVideoUrl(e.target.value)}
                  placeholder="https://... or /uploads/videos/sample_anime_clip.mp4"
                  className="w-full bg-slate-950 border border-slate-700 rounded-xl px-3.5 py-2.5 text-xs text-white"
                />
              </div>

              <div className="space-y-1">
                <label className="text-xs font-semibold text-slate-300">Video Title</label>
                <input
                  type="text"
                  required
                  value={newJobTitle}
                  onChange={(e) => setNewJobTitle(e.target.value)}
                  placeholder="e.g. 🔥 Insane Anime Moment #2"
                  className="w-full bg-slate-950 border border-slate-700 rounded-xl px-3.5 py-2.5 text-xs text-white"
                />
              </div>

              <div className="space-y-1">
                <label className="text-xs font-semibold text-slate-300">Music Selection</label>
                <select
                  value={newJobMusic}
                  onChange={(e) => setNewJobMusic(e.target.value)}
                  className="w-full bg-slate-950 border border-slate-700 rounded-xl px-3.5 py-2.5 text-xs text-white"
                >
                  <option value="">Use Default Track</option>
                  {musicLibrary.map((t) => (
                    <option key={t.id} value={t.originalName}>
                      {t.originalName} ({formatSeconds(t.duration)})
                    </option>
                  ))}
                </select>
              </div>

              <div className="flex justify-end gap-2 pt-2">
                <button
                  type="button"
                  onClick={() => setShowAddJobModal(false)}
                  className="px-4 py-2 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-300 font-semibold text-xs"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="px-5 py-2 rounded-xl bg-sky-600 hover:bg-sky-500 text-white font-bold text-xs"
                >
                  Add Video
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* MODAL: CONNECT SPREADSHEET */}
      {showConnectSheetModal && (
        <div className="fixed inset-0 bg-slate-950/80 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-slate-900 border border-slate-800 rounded-2xl max-w-md w-full p-6 space-y-4 shadow-2xl">
            <div className="flex items-center justify-between border-b border-slate-800 pb-3">
              <h3 className="font-bold text-base text-white flex items-center gap-2">
                <GoogleSheetsIcon className="w-4 h-4" />
                Connect Existing Google Sheet
              </h3>
              <button onClick={() => setShowConnectSheetModal(false)} className="text-slate-400 hover:text-white">
                ✕
              </button>
            </div>

            <div className="space-y-3">
              <label className="text-xs font-semibold text-slate-300">
                Google Spreadsheet URL or ID
              </label>
              <input
                type="text"
                value={sheetUrlInput}
                onChange={(e) => setSheetUrlInput(e.target.value)}
                placeholder="https://docs.google.com/spreadsheets/d/1BxiMVs.../edit"
                className="w-full bg-slate-950 border border-slate-700 rounded-xl px-3.5 py-2.5 text-xs text-white"
              />
              <p className="text-[11px] text-slate-400">
                Ensure your sheet has columns A to J as specified in the dashboard.
              </p>
            </div>

            <div className="flex justify-end gap-2 pt-2">
              <button
                type="button"
                onClick={() => setShowConnectSheetModal(false)}
                className="px-4 py-2 rounded-xl bg-slate-800 text-slate-300 font-semibold text-xs"
              >
                Cancel
              </button>
              <button
                onClick={handleConnectExistingSheet}
                className="px-5 py-2 rounded-xl bg-emerald-600 hover:bg-emerald-500 text-white font-bold text-xs"
              >
                Connect Sheet
              </button>
            </div>
          </div>
        </div>
      )}

      {/* MODAL: GOOGLE OAUTH TOKEN */}
      {showAuthTokenModal && (
        <div className="fixed inset-0 bg-slate-950/80 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-slate-900 border border-slate-800 rounded-2xl max-w-md w-full p-6 space-y-4 shadow-2xl">
            <div className="flex items-center justify-between border-b border-slate-800 pb-3">
              <h3 className="font-bold text-base text-white flex items-center gap-2">
                <YouTubeIcon className="w-4 h-4 text-red-500" />
                Connect Google Account
              </h3>
              <button onClick={() => setShowAuthTokenModal(false)} className="text-slate-400 hover:text-white">
                ✕
              </button>
            </div>

            <div className="space-y-3 text-xs">
              <p className="text-slate-300 leading-relaxed">
                Client ID configured: <span className="font-mono text-[11px] text-sky-400">{authStatus.oAuthClientId}</span>
              </p>
              <label className="text-xs font-semibold text-slate-300">
                Paste Google OAuth Access Token
              </label>
              <input
                type="text"
                value={manualTokenInput}
                onChange={(e) => setManualTokenInput(e.target.value)}
                placeholder="ya29.a0AfH6SM..."
                className="w-full bg-slate-950 border border-slate-700 rounded-xl px-3.5 py-2.5 text-xs text-white font-mono"
              />
              <p className="text-[11px] text-slate-500">
                Stored securely on the backend server; never exposed to third parties.
              </p>
            </div>

            <div className="flex justify-end gap-2 pt-2">
              <button
                type="button"
                onClick={() => setShowAuthTokenModal(false)}
                className="px-4 py-2 rounded-xl bg-slate-800 text-slate-300 font-semibold text-xs"
              >
                Cancel
              </button>
              <button
                onClick={() => saveAccessToken(manualTokenInput.trim())}
                disabled={!manualTokenInput.trim()}
                className="px-5 py-2 rounded-xl bg-red-600 hover:bg-red-500 text-white font-bold text-xs disabled:opacity-50"
              >
                Save &amp; Connect
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
