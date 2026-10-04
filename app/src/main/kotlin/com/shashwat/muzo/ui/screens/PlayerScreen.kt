package com.shashwat.muzo.ui.screens

import android.content.Context
import android.media.AudioManager
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import coil.compose.AsyncImage
import com.shashwat.muzo.data.api.LyricsService
import com.shashwat.muzo.data.model.Lyrics
import com.shashwat.muzo.data.model.MuzoItem
import com.shashwat.muzo.ui.theme.MuzoCyan
import com.shashwat.muzo.ui.theme.MuzoPink
import com.shashwat.muzo.ui.theme.MuzoPurple
import kotlinx.coroutines.launch

/**
 * Exact implementation of standard_player.dart from Shashwat-CODING/Muzo:
 * - Dynamic blurred album art background with gradient overlay
 * - Top pull-down handle (36x5 pill)
 * - 3 horizontal modes: Lyrics (0), Main Artwork (1), Queue (2)
 * - Meta row with circular star favorite and options button
 * - Progress bar with elapsed and negative remaining time (-mm:ss)
 * - Playback controls + real volume slider
 * - Bottom tab bar: Lyrics / Player / Queue
 */

@Composable
fun PlayerScreen(
    currentSong: MuzoItem,
    isPlaying: Boolean,
    isBuffering: Boolean,
    currentPosition: Long,
    duration: Long,
    repeatMode: Int,
    isShuffle: Boolean,
    isFavorite: Boolean,
    queue: List<MuzoItem>,
    sleepTimerSeconds: Int?,
    playbackSpeed: Float = 1.0f,
    onTogglePlayPause: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onToggleShuffle: () -> Unit,
    onToggleRepeat: () -> Unit,
    onToggleFavorite: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onOpenSleepTimer: () -> Unit,
    onSetPlaybackSpeed: (Float) -> Unit = {},
    onPlayQueueSong: (MuzoItem) -> Unit,
    onCollapse: () -> Unit,
    modifier: Modifier = Modifier
) {
    // 0: Lyrics, 1: Main Player, 2: Queue
    var currentPage by remember { mutableIntStateOf(1) }

    val context = LocalContext.current
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val maxVol = remember { audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).toFloat() }
    var currentVol by remember {
        mutableFloatStateOf(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat())
    }

    val lyricsService = remember { LyricsService() }
    var lyrics by remember { mutableStateOf<Lyrics?>(null) }
    var isLoadingLyrics by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val lyricsListState = rememberLazyListState()

    // Fetch lyrics
    LaunchedEffect(currentSong.id) {
        lyrics = null
        isLoadingLyrics = true
        scope.launch {
            try {
                lyrics = lyricsService.getLyrics(
                    currentSong.title,
                    currentSong.displayArtist,
                    currentSong.durationSeconds
                )
            } catch (_: Exception) {}
            isLoadingLyrics = false
        }
    }

    val currentLineIndex = remember(lyrics, currentPosition) {
        lyrics?.syncedLines?.indexOfLast { it.timestampMs <= currentPosition } ?: -1
    }

    LaunchedEffect(currentLineIndex, currentPage) {
        if (currentPage == 0 && currentLineIndex >= 0) {
            lyricsListState.animateScrollToItem((currentLineIndex - 2).coerceAtLeast(0))
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("full_player_screen")
    ) {
        // ── 1. Dynamic Blurred Album Art Background ──
        AsyncImage(
            model = currentSong.displayThumbnail,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .blur(50.dp)
        )

        // Gradient tint overlay (from standard_player.dart)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Black.copy(alpha = 0.50f),
                            Color.Black.copy(alpha = 0.88f)
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // ── 2. Top Pull-Down Handle (36x5) ──
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp)
                    .clickable(onClick = onCollapse),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 36.dp, height = 5.dp)
                        .clip(RoundedCornerShape(2.5.dp))
                        .background(Color.White.copy(alpha = 0.35f))
                )
            }

            // ── 3. Page Content: Lyrics (0), Main (1), Queue (2) ──
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                AnimatedContent(
                    targetState = currentPage,
                    transitionSpec = { fadeIn(tween(250)) togetherWith fadeOut(tween(250)) },
                    label = "player_page_content"
                ) { page ->
                    when (page) {
                        0 -> {
                            // Lyrics Page
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = currentSong.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = currentSong.displayArtist,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.60f)
                                )
                                Spacer(modifier = Modifier.height(16.dp))

                                if (isLoadingLyrics) {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(color = MuzoCyan)
                                    }
                                } else if (lyrics == null || (lyrics?.syncedLines.isNullOrEmpty() && lyrics?.plainLyrics.isNullOrEmpty())) {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "No lyrics found",
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = Color.White.copy(alpha = 0.50f)
                                        )
                                    }
                                } else if (!lyrics?.syncedLines.isNullOrEmpty()) {
                                    LazyColumn(
                                        state = lyricsListState,
                                        modifier = Modifier.fillMaxSize(),
                                        contentPadding = PaddingValues(vertical = 24.dp)
                                    ) {
                                        itemsIndexed(lyrics!!.syncedLines) { index, line ->
                                            val isHighlighted = index == currentLineIndex
                                            Text(
                                                text = line.text,
                                                style = MaterialTheme.typography.headlineSmall.copy(
                                                    fontSize = if (isHighlighted) 22.sp else 16.sp,
                                                    lineHeight = 28.sp
                                                ),
                                                fontWeight = if (isHighlighted) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isHighlighted) MuzoCyan else Color.White.copy(alpha = 0.35f),
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable { onSeekTo(line.timestampMs) }
                                                    .padding(vertical = 10.dp)
                                            )
                                        }
                                    }
                                } else {
                                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                                        item {
                                            Text(
                                                text = lyrics!!.plainLyrics,
                                                style = MaterialTheme.typography.bodyLarge,
                                                lineHeight = 26.sp,
                                                textAlign = TextAlign.Center,
                                                color = Color.White.copy(alpha = 0.80f),
                                                modifier = Modifier.padding(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        1 -> {
                            // Main Album Art Page
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Spacer(modifier = Modifier.weight(0.15f))

                                // Large Album Art (16dp radius, deep shadow)
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(0.88f)
                                        .aspectRatio(1f)
                                        .shadow(32.dp, RoundedCornerShape(16.dp))
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(Color.White.copy(alpha = 0.1f))
                                ) {
                                    AsyncImage(
                                        model = currentSong.displayThumbnail,
                                        contentDescription = currentSong.title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }

                                Spacer(modifier = Modifier.weight(0.2f))

                                // Title, Artist, Circular Star Favorite, Options
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = currentSong.title,
                                            style = MaterialTheme.typography.headlineSmall.copy(
                                                fontSize = 22.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                letterSpacing = (-0.5).sp
                                            ),
                                            color = Color.White,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = currentSong.displayArtist,
                                            style = MaterialTheme.typography.bodyLarge.copy(
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Medium
                                            ),
                                            color = Color.White.copy(alpha = 0.65f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    // Circular Star Favorite button (from standard_player.dart)
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(CircleShape)
                                            .background(Color.White.copy(alpha = 0.15f))
                                            .clickable(onClick = onToggleFavorite),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = if (isFavorite) Icons.Default.Star else Icons.Outlined.StarOutline,
                                            contentDescription = "Favorite",
                                            tint = if (isFavorite) Color(0xFFFFD600) else Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    // Circular Options button
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(CircleShape)
                                            .background(Color.White.copy(alpha = 0.15f))
                                            .clickable(onClick = onAddToPlaylist),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.MoreHoriz,
                                            contentDescription = "Options",
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))
                            }
                        }

                        2 -> {
                            // Queue Page
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 20.dp)
                            ) {
                                // Header: Playing Next
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "Playing Next",
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = Color.White
                                        )
                                        Text(
                                            text = "Up Next (${queue.size} songs)",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.White.copy(alpha = 0.50f)
                                        )
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        IconButton(onClick = onToggleShuffle) {
                                            Icon(
                                                Icons.Default.Shuffle,
                                                contentDescription = "Shuffle",
                                                tint = if (isShuffle) MuzoCyan else Color.White.copy(alpha = 0.5f),
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                        IconButton(onClick = onToggleRepeat) {
                                            val icon = if (repeatMode == Player.REPEAT_MODE_ONE) Icons.Default.RepeatOne else Icons.Default.Repeat
                                            Icon(
                                                icon,
                                                contentDescription = "Repeat",
                                                tint = if (repeatMode != Player.REPEAT_MODE_OFF) MuzoCyan else Color.White.copy(alpha = 0.5f),
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }

                                LazyColumn(modifier = Modifier.fillMaxSize()) {
                                    itemsIndexed(queue) { index, item ->
                                        val isCurrent = item.id == currentSong.id
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .clickable { onPlayQueueSong(item) }
                                                .padding(vertical = 6.dp, horizontal = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(44.dp)
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(Color.White.copy(alpha = 0.1f))
                                            ) {
                                                AsyncImage(
                                                    model = item.displayThumbnail,
                                                    contentDescription = item.title,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            }

                                            Spacer(modifier = Modifier.width(12.dp))

                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = item.title,
                                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                                    color = if (isCurrent) MuzoCyan else Color.White,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = item.displayArtist,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = Color.White.copy(alpha = 0.60f),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ── 4. Sticky Bottom Controls (from standard_player.dart) ──
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 12.dp)
            ) {
                // Progress Slider (bar height 8dp, thumb hidden, time labels)
                var sliderPos by remember { mutableStateOf<Float?>(null) }
                val currentFraction = sliderPos ?: (if (duration > 0) currentPosition.toFloat() / duration else 0f)

                Slider(
                    value = currentFraction.coerceIn(0f, 1f),
                    onValueChange = { sliderPos = it },
                    onValueChangeFinished = {
                        sliderPos?.let {
                            onSeekTo((it * duration).toLong())
                            sliderPos = null
                        }
                    },
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = Color.White.copy(alpha = 0.85f),
                        inactiveTrackColor = Color.White.copy(alpha = 0.18f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Elapsed time & Negative remaining time (-mm:ss)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val pos = sliderPos?.let { (it * duration).toLong() } ?: currentPosition
                    Text(
                        text = formatTime(pos),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        color = Color.White.copy(alpha = 0.60f)
                    )
                    val remaining = (duration - pos).coerceAtLeast(0L)
                    Text(
                        text = "-${formatTime(remaining)}",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        color = Color.White.copy(alpha = 0.60f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Playback Controls Row (Backward, Play/Pause, Forward)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onPrevious,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            Icons.Default.SkipPrevious,
                            contentDescription = "Previous",
                            tint = Color.White,
                            modifier = Modifier.size(34.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clickable(onClick = onTogglePlayPause),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isBuffering) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(32.dp))
                        } else {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                tint = Color.White,
                                modifier = Modifier.size(46.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = onNext,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            Icons.Default.SkipNext,
                            contentDescription = "Next",
                            tint = Color.White,
                            modifier = Modifier.size(34.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Volume Slider (from standard_player.dart)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.VolumeDown,
                        contentDescription = "Volume Down",
                        tint = Color.White.copy(alpha = 0.60f),
                        modifier = Modifier.size(16.dp)
                    )
                    Slider(
                        value = if (maxVol > 0) (currentVol / maxVol).coerceIn(0f, 1f) else 0.5f,
                        onValueChange = { frac ->
                            currentVol = frac * maxVol
                            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, currentVol.toInt(), 0)
                        },
                        colors = SliderDefaults.colors(
                            thumbColor = Color.Transparent,
                            activeTrackColor = Color.White.copy(alpha = 0.85f),
                            inactiveTrackColor = Color.White.copy(alpha = 0.20f)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp)
                    )
                    Icon(
                        Icons.Default.VolumeUp,
                        contentDescription = "Volume Up",
                        tint = Color.White.copy(alpha = 0.60f),
                        modifier = Modifier.size(16.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // ── 5. Bottom Navigation Icon Bar (Lyrics, Player, Queue) ──
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Lyrics Toggle (Page 0)
                    IconButton(onClick = { currentPage = if (currentPage == 0) 1 else 0 }) {
                        Icon(
                            Icons.Outlined.ChatBubbleOutline,
                            contentDescription = "Lyrics",
                            tint = if (currentPage == 0) MuzoCyan else Color.White.copy(alpha = 0.60f),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Main Player Artwork Toggle (Page 1)
                    IconButton(onClick = { currentPage = 1 }) {
                        Icon(
                            Icons.Filled.MusicNote,
                            contentDescription = "Player",
                            tint = if (currentPage == 1) MuzoCyan else Color.White.copy(alpha = 0.60f),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Queue Toggle (Page 2)
                    IconButton(onClick = { currentPage = if (currentPage == 2) 1 else 2 }) {
                        Icon(
                            Icons.Outlined.FormatListBulleted,
                            contentDescription = "Queue",
                            tint = if (currentPage == 2) MuzoCyan else Color.White.copy(alpha = 0.60f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun formatTime(millis: Long): String {
    if (millis <= 0) return "0:00"
    val totalSeconds = millis / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "$minutes:${seconds.toString().padStart(2, '0')}"
}
