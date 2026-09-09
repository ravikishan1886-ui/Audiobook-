package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.RemixViewMode
import com.example.data.model.YouTubePrivacy
import com.example.ui.components.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudiobookDashboard(
    viewModel: AudiobookViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val remixState by viewModel.remixState.collectAsStateWithLifecycle()
    val playerState by viewModel.playerManager.playerState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }

    // FULLSCREEN DISPLAY (When in preview mode)
    if (remixState.currentView == RemixViewMode.PREVIEW) {
        VideoRemixFullscreenPreview(
            remixState = remixState,
            onEditClick = { viewModel.backToRemixEdit() },
            onProcessVideoClick = { viewModel.startVideoProcessing() },
            onPrivacyChange = { viewModel.setRemixPrivacy(it) }
        )
        return
    }

    // 5. PROCESSING SCREEN (When processing video)
    if (remixState.currentView == RemixViewMode.PROCESSING) {
        VideoRemixProcessingScreen(
            remixState = remixState,
            onBackToEdit = { viewModel.backToRemixEdit() },
            onSaveToGallery = { viewModel.saveRemixedVideoToGallery() },
            onShareVideo = { viewModel.shareRemixedVideo() },
            onWatchYouTube = { url ->
                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    context.startActivity(intent)
                } catch (_: Exception) {}
            },
            onRetry = { viewModel.startVideoProcessing() },
            onProcessAnother = { viewModel.resetRemixState() }
        )
        return
    }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.notificationMessage) {
        uiState.notificationMessage?.let { msg ->
            snackbarHostState.showSnackbar(
                message = msg,
                duration = SnackbarDuration.Short
            )
            viewModel.dismissNotification()
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("audiobook_dashboard_scaffold"),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (selectedTab == 0) Icons.Filled.Audiotrack else Icons.Filled.Movie,
                                    contentDescription = "Logo",
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = if (selectedTab == 0) "AI Audiobook Studio" else "Video & Music Remixer",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (selectedTab == 0) "Gemini AI & cvoice.ai Engine" else "Hardware Muxer & YouTube Upload",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { viewModel.openSettings(true) },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("open_settings_button")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Settings,
                                contentDescription = "Settings",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )

                PrimaryTabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("main_navigation_tab_row")
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Filled.MenuBook, contentDescription = null, modifier = Modifier.size(18.dp))
                                Text("AI Audiobook", fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal)
                            }
                        },
                        modifier = Modifier.testTag("tab_audiobook")
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Filled.Movie, contentDescription = null, modifier = Modifier.size(18.dp))
                                Text("Video Remixer", fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal)
                            }
                        },
                        modifier = Modifier.testTag("tab_remixer")
                    )
                }
            }
        }
    ) { innerPadding ->
        if (selectedTab == 1) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                VideoRemixInputSection(
                    remixState = remixState,
                    onVideoUrlChange = { viewModel.updateRemixVideoUrl(it) },
                    onMusicSelected = { uri, name -> viewModel.selectRemixMusic(uri, name) },
                    onSampleVideoSelected = { viewModel.selectSampleVideo(it) },
                    onSampleMusicSelected = { viewModel.selectSampleMusic(it) },
                    onRightsConfirmedChange = { viewModel.setRemixRightsConfirmed(it) },
                    onNextToPreview = { viewModel.openFullscreenPreview() }
                )
            }
        } else {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            val isWideScreen = maxWidth >= 760.dp
            val scrollState = rememberScrollState()
            val coroutineScope = rememberCoroutineScope()

            val previousChapterCount = remember { mutableIntStateOf(uiState.chapters.size) }
            LaunchedEffect(uiState.chapters.size) {
                if (uiState.chapters.isNotEmpty() && previousChapterCount.intValue == 0) {
                    scrollState.animateScrollTo(if (isWideScreen) 250 else 600)
                }
                previousChapterCount.intValue = uiState.chapters.size
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // cvoice.ai Error / Diagnostics Banner if present
                AnimatedVisibility(
                    visible = uiState.cvoiceApiError != null,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("cvoice_error_banner")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Warning,
                                    contentDescription = "Warning",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(24.dp)
                                )
                                Column {
                                    Text(
                                        text = "cvoice.ai API Notice",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                    Text(
                                        text = uiState.cvoiceApiError ?: "",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Button(
                                    onClick = { viewModel.openSettings(true) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.error,
                                        contentColor = MaterialTheme.colorScheme.onError
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    modifier = Modifier.testTag("fix_api_key_button")
                                ) {
                                    Icon(Icons.Filled.Settings, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Settings & Test", style = MaterialTheme.typography.labelSmall)
                                }
                                IconButton(
                                    onClick = { viewModel.dismissCvoiceError() },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Close,
                                        contentDescription = "Dismiss",
                                        tint = MaterialTheme.colorScheme.onErrorContainer,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Progress Indicator Card
                AnimatedVisibility(
                    visible = uiState.progress.isGenerating,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("generation_progress_card")
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        strokeWidth = 2.5.dp
                                    )
                                    Text(
                                        text = uiState.progress.currentStep,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                                Text(
                                    text = "${(uiState.progress.progressPercent * 100).toInt()}%",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }

                            LinearProgressIndicator(
                                progress = { uiState.progress.progressPercent.coerceIn(0f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )

                            Text(
                                text = uiState.progress.detailMessage,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }
                    }
                }

                if (isWideScreen) {
                    // Two-Column Layout for Desktop / Tablet
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Left Column: Manuscript & Voice setup
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            BookInputSection(
                                title = uiState.bookTitle,
                                onTitleChange = { viewModel.updateBookTitle(it) },
                                author = uiState.authorName,
                                onAuthorChange = { viewModel.updateAuthorName(it) },
                                manuscriptText = uiState.manuscriptText,
                                onManuscriptTextChange = { viewModel.updateManuscriptText(it) },
                                rightsConfirmed = uiState.rightsConfirmed,
                                onRightsConfirmedChange = { viewModel.setRightsConfirmed(it) },
                                onSampleSelected = { viewModel.loadSampleBook(it) },
                                onFileUpload = { viewModel.onFileUploaded(it) },
                                onCleanAndSplitClick = { viewModel.cleanAndSplitWithGemini() },
                                isProcessing = uiState.progress.isGenerating
                            )

                            VoiceSelectorSection(
                                selectedVoice = uiState.selectedVoice,
                                onVoiceSelected = { viewModel.selectVoice(it) }
                            )
                        }

                        // Right Column: Chapters, Player & YouTube Export
                        Column(
                            modifier = Modifier.weight(1.2f),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            AudioPlayerBar(
                                playerState = playerState,
                                fullAudiobookFile = uiState.fullAudiobookFile,
                                onTogglePlayPause = { viewModel.playerManager.togglePlayPause() },
                                onSeek = { viewModel.playerManager.seekTo(it) },
                                onSpeedChange = { viewModel.playerManager.setPlaybackSpeed(it) },
                                onPlayFullAudiobook = { viewModel.playFullAudiobook() },
                                onDownloadFullAudiobook = { viewModel.downloadCompletedAudiobook() },
                                onShareAudiobook = { viewModel.shareCompletedAudiobook() }
                            )

                            YouTubeExportCard(
                                youTubeState = uiState.youTubeState,
                                hasFullAudiobook = uiState.fullAudiobookFile != null && uiState.fullAudiobookFile!!.exists(),
                                bookTitle = uiState.bookTitle,
                                author = uiState.authorName,
                                onConnectChannel = { viewModel.connectYouTubeChannel() },
                                onDisconnectChannel = { viewModel.disconnectYouTubeChannel() },
                                onGenerateVideo = { viewModel.generateYouTubeVideoFile() },
                                onSaveVideoToDevice = { viewModel.saveVideoToGallery() },
                                onShareToYouTubeApp = { title, desc, markers ->
                                    viewModel.shareToYouTubeApp(title, desc, markers)
                                },
                                onUploadToYouTube = { title, desc, privacy, markers ->
                                    viewModel.uploadAudiobookToYouTube(title, desc, privacy, markers)
                                },
                                onDismissError = { viewModel.dismissYouTubeError() }
                            )

                            ChapterListSection(
                                chapters = uiState.chapters,
                                currentlyPlayingChapterId = playerState.currentChapterId,
                                isPlaying = playerState.isPlaying,
                                isGeneratingAudio = uiState.progress.isGenerating,
                                onSynthesizeAllClick = { viewModel.generateCompleteAudiobook() },
                                onSynthesizeSingleChapter = { viewModel.synthesizeSingleChapter(it) },
                                onPreviewChapter = { viewModel.previewChapter(it) },
                                onViewScript = { viewModel.openChapterTextPreview(it) },
                                onDownloadChapter = { viewModel.downloadChapterAudio(it) }
                            )
                        }
                    }
                } else {
                    // Mobile Phone Single Column Layout
                    BookInputSection(
                        title = uiState.bookTitle,
                        onTitleChange = { viewModel.updateBookTitle(it) },
                        author = uiState.authorName,
                        onAuthorChange = { viewModel.updateAuthorName(it) },
                        manuscriptText = uiState.manuscriptText,
                        onManuscriptTextChange = { viewModel.updateManuscriptText(it) },
                        rightsConfirmed = uiState.rightsConfirmed,
                        onRightsConfirmedChange = { viewModel.setRightsConfirmed(it) },
                        onSampleSelected = { viewModel.loadSampleBook(it) },
                        onFileUpload = { viewModel.onFileUploaded(it) },
                        onCleanAndSplitClick = { viewModel.cleanAndSplitWithGemini() },
                        isProcessing = uiState.progress.isGenerating
                    )

                    VoiceSelectorSection(
                        selectedVoice = uiState.selectedVoice,
                        onVoiceSelected = { viewModel.selectVoice(it) }
                    )

                    ChapterListSection(
                        chapters = uiState.chapters,
                        currentlyPlayingChapterId = playerState.currentChapterId,
                        isPlaying = playerState.isPlaying,
                        isGeneratingAudio = uiState.progress.isGenerating,
                        onSynthesizeAllClick = { viewModel.generateCompleteAudiobook() },
                        onSynthesizeSingleChapter = { viewModel.synthesizeSingleChapter(it) },
                        onPreviewChapter = { viewModel.previewChapter(it) },
                        onViewScript = { viewModel.openChapterTextPreview(it) },
                        onDownloadChapter = { viewModel.downloadChapterAudio(it) }
                    )

                    AudioPlayerBar(
                        playerState = playerState,
                        fullAudiobookFile = uiState.fullAudiobookFile,
                        onTogglePlayPause = { viewModel.playerManager.togglePlayPause() },
                        onSeek = { viewModel.playerManager.seekTo(it) },
                        onSpeedChange = { viewModel.playerManager.setPlaybackSpeed(it) },
                        onPlayFullAudiobook = { viewModel.playFullAudiobook() },
                        onDownloadFullAudiobook = { viewModel.downloadCompletedAudiobook() },
                        onShareAudiobook = { viewModel.shareCompletedAudiobook() }
                    )

                    YouTubeExportCard(
                        youTubeState = uiState.youTubeState,
                        hasFullAudiobook = uiState.fullAudiobookFile != null && uiState.fullAudiobookFile!!.exists(),
                        bookTitle = uiState.bookTitle,
                        author = uiState.authorName,
                        onConnectChannel = { viewModel.connectYouTubeChannel() },
                        onDisconnectChannel = { viewModel.disconnectYouTubeChannel() },
                        onGenerateVideo = { viewModel.generateYouTubeVideoFile() },
                        onSaveVideoToDevice = { viewModel.saveVideoToGallery() },
                        onShareToYouTubeApp = { title, desc, markers ->
                            viewModel.shareToYouTubeApp(title, desc, markers)
                        },
                        onUploadToYouTube = { title, desc, privacy, markers ->
                            viewModel.uploadAudiobookToYouTube(title, desc, privacy, markers)
                        },
                        onDismissError = { viewModel.dismissYouTubeError() }
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
        }
    }

    // Dialogs
    if (uiState.showYouTubeAuthDialog) {
        YouTubeAuthDialog(
            initialToken = uiState.customYouTubeToken,
            isConnected = uiState.youTubeState.isConnected,
            channelName = uiState.youTubeState.userName.ifBlank { uiState.youTubeState.userEmail.ifBlank { "YouTube Channel" } },
            onSaveToken = { viewModel.saveYouTubeToken(it) },
            onSaveTokenAndUpload = { token ->
                viewModel.saveYouTubeTokenAndUpload(
                    token = token,
                    customTitle = "${uiState.bookTitle} by ${uiState.authorName} (Full Audiobook)",
                    customDescription = "",
                    privacy = YouTubePrivacy.PRIVATE,
                    includeMarkers = true
                )
            },
            onDismiss = { viewModel.openYouTubeAuthDialog(false) },
            onUploadViaAppInstead = {
                viewModel.shareToYouTubeApp(
                    customTitle = "${uiState.bookTitle} by ${uiState.authorName} (Full Audiobook)",
                    customDescription = "",
                    includeMarkers = true
                )
            }
        )
    }

    if (uiState.showSettingsDialog) {
        ApiKeySettingsDialog(
            currentGeminiKey = uiState.customGeminiKey,
            currentCVoiceKey = uiState.customCVoiceKey,
            currentCVoiceUrl = uiState.customCVoiceUrl,
            currentAutoFallback = uiState.isAutoLocalFallbackEnabled,
            onTestCVoice = { key, url ->
                viewModel.testCVoiceApi(key, url)
            },
            onSave = { geminiKey, cvoiceKey, cvoiceUrl, autoFallback ->
                viewModel.saveApiSettings(geminiKey, cvoiceKey, cvoiceUrl, autoFallback)
            },
            onDismiss = { viewModel.openSettings(false) }
        )
    }

    uiState.previewChapterText?.let { chapter ->
        ScriptEditorDialog(
            chapter = chapter,
            onSave = { newNarration ->
                viewModel.updateChapterNarration(chapter.id, newNarration)
                viewModel.openChapterTextPreview(null)
            },
            onDismiss = { viewModel.openChapterTextPreview(null) }
        )
    }
}
