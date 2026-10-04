package com.shashwat.muzo.ui

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.shashwat.muzo.data.api.ImportProgress
import com.shashwat.muzo.data.api.InnerTubeClient
import com.shashwat.muzo.data.api.SpotifyImportService
import com.shashwat.muzo.data.auth.AuthManager
import com.shashwat.muzo.data.local.LocalMusicStore
import com.shashwat.muzo.data.model.MuzoItem
import com.shashwat.muzo.service.MusicPlayerManager
import com.shashwat.muzo.ui.components.*
import com.shashwat.muzo.ui.screens.*
import com.shashwat.muzo.ui.theme.MuzoTheme
import kotlinx.coroutines.launch

@Composable
fun MuzoApp() {
    val context = LocalContext.current
    val playerManager = remember { MusicPlayerManager.getInstance(context) }
    val localStore = remember { LocalMusicStore(context) }
    val authManager = remember { AuthManager.getInstance(context) }
    val spotifyService = remember { SpotifyImportService() }
    val innerTubeClient = remember { InnerTubeClient() }
    val scope = rememberCoroutineScope()

    // Player states
    val currentSong by playerManager.currentSong.collectAsState()
    val isPlaying by playerManager.isPlaying.collectAsState()
    val isBuffering by playerManager.isBuffering.collectAsState()
    val currentPosition by playerManager.currentPosition.collectAsState()
    val duration by playerManager.duration.collectAsState()
    val repeatMode by playerManager.repeatMode.collectAsState()
    val shuffleEnabled by playerManager.shuffleEnabled.collectAsState()
    val playbackSpeed by playerManager.playbackSpeed.collectAsState()
    val queue by playerManager.queue.collectAsState()
    val sleepTimerSeconds by playerManager.sleepTimerSeconds.collectAsState()

    // Auth & User State
    val userState by authManager.userState.collectAsState()

    // Local data states
    val favorites by localStore.favorites.collectAsState()
    val history by localStore.history.collectAsState()
    val playlists by localStore.playlists.collectAsState()
    val themeMode by localStore.themeMode.collectAsState()
    val audioQuality by localStore.audioQuality.collectAsState()

    val favoriteIds = remember(favorites) { favorites.map { it.id }.toSet() }

    // Navigation and UI state
    var selectedTab by remember { mutableIntStateOf(0) }
    var isPlayerExpanded by remember { mutableStateOf(false) }

    // Playlist detail view state
    var activeDetailPlaylist by remember {
        mutableStateOf<Triple<String, List<MuzoItem>, String?>?>(null)
    }

    // Dialog states
    var songForAddToPlaylist by remember { mutableStateOf<MuzoItem?>(null) }
    var showSleepTimerDialog by remember { mutableStateOf(false) }
    var showSpotifyImportDialog by remember { mutableStateOf(false) }
    var isImportingSpotify by remember { mutableStateOf(false) }
    var spotifyImportProgress by remember { mutableStateOf<ImportProgress?>(null) }

    // Google Sign-In Launcher
    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            scope.launch {
                authManager.handleSignInResult(result.data)
            }
        }
    }

    // Handle back button when full player is expanded
    BackHandler(enabled = isPlayerExpanded) {
        isPlayerExpanded = false
    }

    MuzoTheme(themeMode = themeMode) {
        AmbientLiquidBackdrop {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                containerColor = androidx.compose.ui.graphics.Color.Transparent,
                contentWindowInsets = WindowInsets.systemBars
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                // Main Content area
                if (activeDetailPlaylist != null) {
                    val (title, songs, playlistId) = activeDetailPlaylist!!
                    PlaylistDetailScreen(
                        title = title,
                        songs = songs,
                        playlistId = playlistId,
                        currentSong = currentSong,
                        isPlaying = isPlaying,
                        favoriteIds = favoriteIds,
                        onBack = { activeDetailPlaylist = null },
                        onPlaySong = { song, list -> playerManager.playSong(song, list) },
                        onPlayAll = { list, shuffle ->
                            val playQueue = if (shuffle) list.shuffled() else list
                            playQueue.firstOrNull()?.let { playerManager.playSong(it, playQueue) }
                        },
                        onToggleFavorite = { localStore.toggleFavorite(it) },
                        onAddToPlaylist = { songForAddToPlaylist = it }
                    )
                } else {
                    when (selectedTab) {
                        0 -> HomeScreen(
                            currentSong = currentSong,
                            isPlaying = isPlaying,
                            favoriteIds = favoriteIds,
                            favorites = favorites,
                            history = history,
                            playlists = playlists,
                            userDisplayName = userState.displayName,
                            userPhotoUrl = userState.photoUrl,
                            onPlaySong = { song, list -> playerManager.playSong(song, list) },
                            onToggleFavorite = { localStore.toggleFavorite(it) },
                            onAddToPlaylist = { songForAddToPlaylist = it },
                            onOpenPlaylist = { playlistId, title ->
                                scope.launch {
                                    val tracks = innerTubeClient.getRelatedTracks(playlistId)
                                    activeDetailPlaylist = Triple(title, tracks, null)
                                }
                            },
                            onOpenAccount = { selectedTab = 4 }
                        )
                        1 -> SearchScreen(
                            currentSong = currentSong,
                            isPlaying = isPlaying,
                            favoriteIds = favoriteIds,
                            onPlaySong = { song, list -> playerManager.playSong(song, list) },
                            onToggleFavorite = { localStore.toggleFavorite(it) },
                            onAddToPlaylist = { songForAddToPlaylist = it }
                        )
                        2 -> CommunityScreen(
                            currentSong = currentSong,
                            isPlaying = isPlaying,
                            favoriteIds = favoriteIds,
                            onPlaySong = { song, list -> playerManager.playSong(song, list) },
                            onToggleFavorite = { localStore.toggleFavorite(it) },
                            onAddToPlaylist = { songForAddToPlaylist = it }
                        )
                        3 -> LibraryScreen(
                            favorites = favorites,
                            history = history,
                            playlists = playlists,
                            onCreatePlaylist = {
                                localStore.createPlaylist("Playlist ${playlists.size + 1}")
                            },
                            onOpenPlaylistDetail = { title, songs, plId ->
                                activeDetailPlaylist = Triple(title, songs, plId)
                            },
                            onOpenSpotifyImport = { showSpotifyImportDialog = true },
                            onDeletePlaylist = { plId -> localStore.deletePlaylist(plId) }
                        )
                        4 -> SettingsScreen(
                            currentTheme = themeMode,
                            currentQuality = audioQuality,
                            userState = userState,
                            onSignInWithGoogle = {
                                val intent = authManager.getGoogleSignInIntent()
                                if (intent != null) {
                                    googleSignInLauncher.launch(intent)
                                }
                            },
                            onSignOut = { authManager.signOut() },
                            onSelectTheme = { localStore.setThemeMode(it) },
                            onSelectQuality = { localStore.setAudioQuality(it) },
                            onOpenSleepTimer = { showSleepTimerDialog = true },
                            onClearHistory = { localStore.clearHistory() }
                        )
                    }
                }

                // Mini Player & Bottom Navigation Bar Floating Layer
                if (!isPlayerExpanded) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                    ) {
                        if (currentSong != null) {
                            val progress = if (duration > 0) currentPosition.toFloat() / duration else 0f
                            MiniPlayer(
                                currentSong = currentSong,
                                isPlaying = isPlaying,
                                isBuffering = isBuffering,
                                progress = progress,
                                onTogglePlayPause = { playerManager.togglePlayPause() },
                                onNext = { playerManager.playNext() },
                                onExpandPlayer = { isPlayerExpanded = true }
                            )
                        }

                        MuzoBottomNav(
                            selectedIndex = selectedTab,
                            onTabSelected = {
                                selectedTab = it
                                activeDetailPlaylist = null
                            }
                        )
                    }
                }

                // Full Screen ViMusic Player Modal with Smooth Slide In
                AnimatedVisibility(
                    visible = isPlayerExpanded && currentSong != null,
                    enter = slideInVertically(initialOffsetY = { it }),
                    exit = slideOutVertically(targetOffsetY = { it })
                ) {
                    if (currentSong != null) {
                        PlayerScreen(
                            currentSong = currentSong!!,
                            isPlaying = isPlaying,
                            isBuffering = isBuffering,
                            currentPosition = currentPosition,
                            duration = duration,
                            repeatMode = repeatMode,
                            isShuffle = shuffleEnabled,
                            isFavorite = favoriteIds.contains(currentSong!!.id),
                            queue = queue,
                            sleepTimerSeconds = sleepTimerSeconds,
                            playbackSpeed = playbackSpeed,
                            onTogglePlayPause = { playerManager.togglePlayPause() },
                            onSeekTo = { playerManager.seekTo(it) },
                            onNext = { playerManager.playNext() },
                            onPrevious = { playerManager.playPrevious() },
                            onToggleShuffle = { playerManager.toggleShuffle() },
                            onToggleRepeat = { playerManager.toggleRepeatMode() },
                            onToggleFavorite = { localStore.toggleFavorite(currentSong!!) },
                            onAddToPlaylist = { songForAddToPlaylist = currentSong },
                            onOpenSleepTimer = { showSleepTimerDialog = true },
                            onSetPlaybackSpeed = { playerManager.setPlaybackSpeed(it) },
                            onPlayQueueSong = { playerManager.playSong(it, queue) },
                            onCollapse = { isPlayerExpanded = false }
                        )
                    }
                }
            }
        }

        // Add to Playlist Dialog
        songForAddToPlaylist?.let { song ->
            AddToPlaylistDialog(
                song = song,
                playlists = playlists,
                onDismiss = { songForAddToPlaylist = null },
                onAddToPlaylist = { plId -> localStore.addToPlaylist(plId, song) },
                onCreateNewPlaylist = { name ->
                    localStore.createPlaylist(name, listOf(song))
                }
            )
        }

        // Sleep Timer Dialog
        if (showSleepTimerDialog) {
            SleepTimerDialog(
                currentRemainingSecs = sleepTimerSeconds,
                onDismiss = { showSleepTimerDialog = false },
                onSetTimer = { mins -> playerManager.startSleepTimer(mins) },
                onCancelTimer = { playerManager.cancelSleepTimer() }
            )
        }

        // Spotify Import Dialog
        if (showSpotifyImportDialog) {
            SpotifyImportDialog(
                isImporting = isImportingSpotify,
                progress = spotifyImportProgress,
                onDismiss = {
                    showSpotifyImportDialog = false
                    spotifyImportProgress = null
                },
                onStartImport = { url ->
                    isImportingSpotify = true
                    scope.launch {
                        spotifyService.importSpotifyPlaylist(url).collect { prog ->
                            spotifyImportProgress = prog
                            if (prog.isComplete) {
                                isImportingSpotify = false
                                if (prog.importedSongs.isNotEmpty()) {
                                    localStore.createPlaylist(
                                        name = prog.playlistName.ifEmpty { "Imported Spotify Playlist" },
                                        initialSongs = prog.importedSongs
                                    )
                                }
                            }
                        }
                    }
                }
            )
        }
        }
    }
}
