package com.shashwat.muzo.service

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.shashwat.muzo.data.api.InnerTubeClient
import com.shashwat.muzo.data.api.StreamExtractor
import com.shashwat.muzo.data.local.LocalMusicStore
import com.shashwat.muzo.data.model.MuzoItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class MusicPlayerManager private constructor(private val context: Context) {
    val player: ExoPlayer = ExoPlayer.Builder(context).build()
    private val streamExtractor = StreamExtractor()
    private val innerTubeClient = InnerTubeClient()
    private val localStore = LocalMusicStore(context)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _currentSong = MutableStateFlow<MuzoItem?>(null)
    val currentSong: StateFlow<MuzoItem?> = _currentSong.asStateFlow()

    private val _queue = MutableStateFlow<List<MuzoItem>>(emptyList())
    val queue: StateFlow<List<MuzoItem>> = _queue.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _isBuffering = MutableStateFlow(false)
    val isBuffering: StateFlow<Boolean> = _isBuffering.asStateFlow()

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    private val _repeatMode = MutableStateFlow(Player.REPEAT_MODE_OFF)
    val repeatMode: StateFlow<Int> = _repeatMode.asStateFlow()

    private val _shuffleEnabled = MutableStateFlow(false)
    val shuffleEnabled: StateFlow<Boolean> = _shuffleEnabled.asStateFlow()

    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    private val _sleepTimerSeconds = MutableStateFlow<Int?>(null)
    val sleepTimerSeconds: StateFlow<Int?> = _sleepTimerSeconds.asStateFlow()

    private var progressJob: Job? = null
    private var sleepTimerJob: Job? = null

    init {
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                _isPlaying.value = playing
                if (playing) {
                    startProgressTicker()
                } else {
                    stopProgressTicker()
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                _isBuffering.value = playbackState == Player.STATE_BUFFERING
                if (playbackState == Player.STATE_READY) {
                    _duration.value = player.duration.coerceAtLeast(0L)
                } else if (playbackState == Player.STATE_ENDED) {
                    handleTrackEnded()
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                Log.e("MusicPlayerManager", "Player error: ${error.message}", error)
                _isBuffering.value = false
                _isPlaying.value = false
            }
        })
    }

    private fun startProgressTicker() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive) {
                if (player.isPlaying) {
                    _currentPosition.value = player.currentPosition.coerceAtLeast(0L)
                    val dur = player.duration
                    if (dur > 0L) {
                        _duration.value = dur
                    }
                }
                delay(250)
            }
        }
    }

    private fun stopProgressTicker() {
        progressJob?.cancel()
        progressJob = null
    }

    private fun handleTrackEnded() {
        if (_repeatMode.value == Player.REPEAT_MODE_ONE) {
            player.seekTo(0)
            player.play()
        } else {
            playNext()
        }
    }

    fun playSong(song: MuzoItem, queueList: List<MuzoItem> = emptyList()) {
        _currentSong.value = song
        if (queueList.isNotEmpty()) {
            _queue.value = queueList
        } else if (_queue.value.none { it.id == song.id }) {
            _queue.value = listOf(song) + _queue.value
        }

        localStore.addToHistory(song)

        scope.launch {
            _isBuffering.value = true
            _currentPosition.value = 0L
            _duration.value = (song.durationSeconds?.times(1000L)) ?: 0L

            try {
                // Ensure foreground service is running
                val intent = Intent(context, MusicPlaybackService::class.java)
                try {
                    context.startService(intent)
                } catch (_: Exception) {}

                val streamUrl = streamExtractor.getStreamUrl(song)
                if (streamUrl.isNullOrEmpty()) {
                    Log.e("MusicPlayerManager", "Failed to resolve stream for ${song.title}")
                    _isBuffering.value = false
                    return@launch
                }

                val metadata = MediaMetadata.Builder()
                    .setTitle(song.title)
                    .setArtist(song.displayArtist)
                    .setArtworkUri(android.net.Uri.parse(song.displayThumbnail))
                    .build()

                val mediaItem = MediaItem.Builder()
                    .setUri(streamUrl)
                    .setMediaMetadata(metadata)
                    .build()

                player.setMediaItem(mediaItem)
                player.prepare()
                player.play()

                // Auto-fetch related queue tracks in background if queue is small (ViMusic Radio feature!)
                if (_queue.value.size <= 1 && song.videoId != null) {
                    launch(Dispatchers.IO) {
                        try {
                            val related = innerTubeClient.getRelatedTracks(song.videoId)
                            if (related.isNotEmpty()) {
                                _queue.value = listOf(song) + related
                            }
                        } catch (_: Exception) {}
                    }
                }
            } catch (e: Exception) {
                Log.e("MusicPlayerManager", "Error starting playback: ${e.message}", e)
                _isBuffering.value = false
            }
        }
    }

    fun togglePlayPause() {
        if (player.isPlaying) {
            player.pause()
        } else {
            if (player.playbackState == Player.STATE_IDLE || player.playbackState == Player.STATE_ENDED) {
                _currentSong.value?.let { playSong(it, _queue.value) }
            } else {
                player.play()
            }
        }
    }

    fun seekTo(positionMs: Long) {
        player.seekTo(positionMs)
        _currentPosition.value = positionMs
    }

    fun setPlaybackSpeed(speed: Float) {
        _playbackSpeed.value = speed
        player.playbackParameters = PlaybackParameters(speed)
    }

    fun playNext() {
        val current = _currentSong.value ?: return
        val currentQueue = _queue.value
        if (currentQueue.isEmpty()) return

        val currentIndex = currentQueue.indexOfFirst { it.id == current.id }
        val nextIndex = if (_shuffleEnabled.value && currentQueue.size > 1) {
            var rand = (currentQueue.indices).random()
            while (rand == currentIndex && currentQueue.size > 1) {
                rand = (currentQueue.indices).random()
            }
            rand
        } else {
            if (currentIndex in 0 until currentQueue.size - 1) {
                currentIndex + 1
            } else if (_repeatMode.value == Player.REPEAT_MODE_ALL) {
                0
            } else {
                null
            }
        }

        if (nextIndex != null) {
            playSong(currentQueue[nextIndex], currentQueue)
        }
    }

    fun playPrevious() {
        if (player.currentPosition > 3000L) {
            player.seekTo(0)
            _currentPosition.value = 0L
            return
        }

        val current = _currentSong.value ?: return
        val currentQueue = _queue.value
        if (currentQueue.isEmpty()) return

        val currentIndex = currentQueue.indexOfFirst { it.id == current.id }
        if (currentIndex > 0) {
            playSong(currentQueue[currentIndex - 1], currentQueue)
        } else {
            player.seekTo(0)
        }
    }

    fun toggleShuffle() {
        _shuffleEnabled.value = !_shuffleEnabled.value
    }

    fun toggleRepeatMode() {
        val nextMode = when (_repeatMode.value) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
        _repeatMode.value = nextMode
        player.repeatMode = nextMode
    }

    fun startSleepTimer(minutes: Int) {
        sleepTimerJob?.cancel()
        val totalSecs = minutes * 60
        _sleepTimerSeconds.value = totalSecs

        sleepTimerJob = scope.launch {
            var remaining = totalSecs
            while (remaining > 0 && isActive) {
                delay(1000)
                remaining--
                _sleepTimerSeconds.value = remaining
            }
            if (remaining <= 0) {
                player.pause()
                _sleepTimerSeconds.value = null
            }
        }
    }

    fun cancelSleepTimer() {
        sleepTimerJob?.cancel()
        sleepTimerJob = null
        _sleepTimerSeconds.value = null
    }

    companion object {
        @Volatile
        private var instance: MusicPlayerManager? = null

        fun getInstance(context: Context): MusicPlayerManager {
            return instance ?: synchronized(this) {
                instance ?: MusicPlayerManager(context.applicationContext).also { instance = it }
            }
        }
    }
}
