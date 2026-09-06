package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.os.Build
import android.util.Log
import com.example.data.model.PlayerState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

class AudioPlayerManager(private val context: Context) {
    private val TAG = "AudioPlayerManager"
    private var mediaPlayer: MediaPlayer? = null
    private val scope = CoroutineScope(Dispatchers.Main)
    private var progressJob: Job? = null

    private val _playerState = MutableStateFlow(PlayerState())
    val playerState: StateFlow<PlayerState> = _playerState.asStateFlow()

    fun playAudio(
        filePath: String,
        audioTitle: String,
        chapterId: String? = null,
        isFullAudiobook: Boolean = false
    ) {
        val file = File(filePath)
        if (!file.exists()) {
            Log.e(TAG, "Audio file does not exist: $filePath")
            return
        }

        try {
            stop()
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setDataSource(filePath)
                prepare()
                val currentSpeed = _playerState.value.playbackSpeed
                applyPlaybackSpeed(this, currentSpeed)
                start()

                _playerState.value = _playerState.value.copy(
                    isPlaying = true,
                    currentChapterId = chapterId,
                    currentPositionMs = 0L,
                    totalDurationMs = duration.toLong(),
                    isFullAudiobookMode = isFullAudiobook,
                    audioTitle = audioTitle
                )

                setOnCompletionListener {
                    _playerState.value = _playerState.value.copy(
                        isPlaying = false,
                        currentPositionMs = duration.toLong()
                    )
                    stopProgressTracking()
                }

                setOnErrorListener { _, what, extra ->
                    Log.e(TAG, "MediaPlayer error: what=$what, extra=$extra")
                    _playerState.value = _playerState.value.copy(isPlaying = false)
                    stopProgressTracking()
                    true
                }
            }

            startProgressTracking()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to play audio", e)
        }
    }

    fun togglePlayPause() {
        val player = mediaPlayer ?: return
        if (player.isPlaying) {
            player.pause()
            _playerState.value = _playerState.value.copy(isPlaying = false)
            stopProgressTracking()
        } else {
            player.start()
            _playerState.value = _playerState.value.copy(isPlaying = true)
            startProgressTracking()
        }
    }

    fun pause() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.pause()
                _playerState.value = _playerState.value.copy(isPlaying = false)
                stopProgressTracking()
            }
        }
    }

    fun seekTo(positionMs: Long) {
        mediaPlayer?.let {
            it.seekTo(positionMs.toInt())
            _playerState.value = _playerState.value.copy(currentPositionMs = positionMs)
        }
    }

    fun setPlaybackSpeed(speed: Float) {
        _playerState.value = _playerState.value.copy(playbackSpeed = speed)
        mediaPlayer?.let { applyPlaybackSpeed(it, speed) }
    }

    private fun applyPlaybackSpeed(player: MediaPlayer, speed: Float) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                val params = player.playbackParams ?: PlaybackParams()
                params.speed = speed
                player.playbackParams = params
            } catch (e: Exception) {
                Log.w(TAG, "Could not set playback speed", e)
            }
        }
    }

    private fun startProgressTracking() {
        stopProgressTracking()
        progressJob = scope.launch {
            while (isActive) {
                mediaPlayer?.let {
                    if (it.isPlaying) {
                        _playerState.value = _playerState.value.copy(
                            currentPositionMs = it.currentPosition.toLong(),
                            totalDurationMs = it.duration.toLong()
                        )
                    }
                }
                delay(200)
            }
        }
    }

    private fun stopProgressTracking() {
        progressJob?.cancel()
        progressJob = null
    }

    fun stop() {
        stopProgressTracking()
        val player = mediaPlayer
        mediaPlayer = null
        if (player != null) {
            try {
                if (player.isPlaying) {
                    player.stop()
                }
            } catch (e: Exception) {
                Log.w(TAG, "Notice stopping MediaPlayer: ${e.message}")
            }
            try {
                player.reset()
            } catch (e: Exception) {
                Log.w(TAG, "Notice resetting MediaPlayer: ${e.message}")
            }
            try {
                player.release()
            } catch (e: Exception) {
                Log.w(TAG, "Notice releasing MediaPlayer: ${e.message}")
            }
        }
        _playerState.value = _playerState.value.copy(isPlaying = false)
    }

    fun release() {
        stop()
    }
}
