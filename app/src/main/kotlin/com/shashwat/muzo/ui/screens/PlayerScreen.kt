package com.shashwat.muzo.ui.screens

import androidx.compose.animation.core.*
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
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
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
import kotlin.random.Random

enum class PlayerViewMode {
    COVER,
    VISUALIZER,
    LYRICS
}

@OptIn(ExperimentalMaterial3Api::class)
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
    var viewMode by remember { mutableStateOf(PlayerViewMode.COVER) }
    var showQueueSheet by remember { mutableStateOf(false) }
    var showSpeedDialog by remember { mutableStateOf(false) }

    val lyricsService = remember { LyricsService() }
    var lyrics by remember { mutableStateOf<Lyrics?>(null) }
    var isLoadingLyrics by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val lyricsListState = rememberLazyListState()

    // Fetch lyrics when song changes
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

    // Auto-scroll lyrics with current position
    val currentLineIndex = remember(lyrics, currentPosition) {
        lyrics?.syncedLines?.indexOfLast { it.timestampMs <= currentPosition } ?: -1
    }

    LaunchedEffect(currentLineIndex, viewMode) {
        if (viewMode == PlayerViewMode.LYRICS && currentLineIndex >= 0) {
            lyricsListState.animateScrollToItem((currentLineIndex - 2).coerceAtLeast(0))
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        MuzoPurple.copy(alpha = 0.45f),
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.background
                    )
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("full_player_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header: Collapse, ViMusic Mode Switcher (Cover / Visualizer / Lyrics), Add to Playlist
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onCollapse,
                    modifier = Modifier.testTag("player_collapse_button")
                ) {
                    Icon(
                        Icons.Default.KeyboardArrowDown,
                        contentDescription = "Collapse Player",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(32.dp)
                    )
                }

                // ViMusic Mode Switcher Pill
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.height(36.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PlayerPillTab(
                            title = "Cover",
                            isSelected = viewMode == PlayerViewMode.COVER,
                            onClick = { viewMode = PlayerViewMode.COVER }
                        )
                        PlayerPillTab(
                            title = "Equalizer",
                            isSelected = viewMode == PlayerViewMode.VISUALIZER,
                            onClick = { viewMode = PlayerViewMode.VISUALIZER }
                        )
                        PlayerPillTab(
                            title = "Lyrics",
                            isSelected = viewMode == PlayerViewMode.LYRICS,
                            onClick = { viewMode = PlayerViewMode.LYRICS }
                        )
                    }
                }

                IconButton(onClick = onAddToPlaylist) {
                    Icon(
                        Icons.Default.PlaylistAdd,
                        contentDescription = "Add to playlist",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.weight(0.2f))

            // Center Content: Cover Art OR ViMusic Visualizer OR Synced Lyrics
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(3.6f),
                contentAlignment = Alignment.Center
            ) {
                when (viewMode) {
                    PlayerViewMode.COVER -> {
                        // High Res Artwork with Ambient Glow
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.88f)
                                .aspectRatio(1f)
                                .shadow(32.dp, RoundedCornerShape(28.dp), ambientColor = MuzoCyan, spotColor = MuzoPurple)
                                .clip(RoundedCornerShape(28.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            AsyncImage(
                                model = currentSong.displayThumbnail,
                                contentDescription = currentSong.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }

                    PlayerViewMode.VISUALIZER -> {
                        // ViMusic Audio Waveform Visualizer
                        ViMusicVisualizer(isPlaying = isPlaying)
                    }

                    PlayerViewMode.LYRICS -> {
                        // Synced Lyrics View with Smooth Karaoke Glow
                        if (isLoadingLyrics) {
                            CircularProgressIndicator(color = MuzoCyan)
                        } else if (lyrics == null || (lyrics?.syncedLines.isNullOrEmpty() && lyrics?.plainLyrics.isNullOrEmpty())) {
                            Text(
                                text = "No lyrics found for this song",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else if (!lyrics?.syncedLines.isNullOrEmpty()) {
                            LazyColumn(
                                state = lyricsListState,
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(vertical = 48.dp)
                            ) {
                                itemsIndexed(lyrics!!.syncedLines) { index, line ->
                                    val isHighlighted = index == currentLineIndex
                                    Text(
                                        text = line.text,
                                        style = MaterialTheme.typography.headlineMedium.copy(
                                            fontSize = if (isHighlighted) 22.sp else 16.sp,
                                            lineHeight = 28.sp
                                        ),
                                        fontWeight = if (isHighlighted) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isHighlighted) MuzoCyan else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable { onSeekTo(line.timestampMs) }
                                            .padding(vertical = 10.dp, horizontal = 12.dp)
                                    )
                                }
                            }
                        } else {
                            // Plain lyrics
                            LazyColumn(modifier = Modifier.fillMaxSize()) {
                                item {
                                    Text(
                                        text = lyrics!!.plainLyrics,
                                        style = MaterialTheme.typography.bodyLarge,
                                        lineHeight = 26.sp,
                                        textAlign = TextAlign.Center,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(0.2f))

            // Track Details & Favorite Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = currentSong.title,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = currentSong.displayArtist,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (isFavorite) MuzoPink else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            // Scrubber Slider
            var sliderPosition by remember { mutableStateOf<Float?>(null) }
            val currentPos = sliderPosition ?: (if (duration > 0) currentPosition.toFloat() / duration else 0f)

            Column(modifier = Modifier.fillMaxWidth()) {
                Slider(
                    value = currentPos.coerceIn(0f, 1f),
                    onValueChange = { sliderPosition = it },
                    onValueChangeFinished = {
                        sliderPosition?.let {
                            onSeekTo((it * duration).toLong())
                            sliderPosition = null
                        }
                    },
                    colors = SliderDefaults.colors(
                        thumbColor = MuzoCyan,
                        activeTrackColor = MuzoCyan,
                        inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("player_progress_slider")
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formatTime(sliderPosition?.let { (it * duration).toLong() } ?: currentPosition),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatTime(duration),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Playback Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Shuffle
                IconButton(onClick = onToggleShuffle) {
                    Icon(
                        Icons.Default.Shuffle,
                        contentDescription = "Shuffle",
                        tint = if (isShuffle) MuzoCyan else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }

                // Previous
                IconButton(
                    onClick = onPrevious,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        Icons.Default.SkipPrevious,
                        contentDescription = "Previous Track",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(36.dp)
                    )
                }

                // Play / Pause FAB
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(MuzoCyan, MuzoPurple)
                            )
                        )
                        .clickable(onClick = onTogglePlayPause)
                        .testTag("player_play_pause_button"),
                    contentAlignment = Alignment.Center
                ) {
                    if (isBuffering) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(32.dp),
                            color = Color.White,
                            strokeWidth = 3.dp
                        )
                    } else {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = Color.White,
                            modifier = Modifier.size(38.dp)
                        )
                    }
                }

                // Next
                IconButton(
                    onClick = onNext,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        Icons.Default.SkipNext,
                        contentDescription = "Next Track",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(36.dp)
                    )
                }

                // Repeat
                IconButton(onClick = onToggleRepeat) {
                    val repeatIcon = when (repeatMode) {
                        Player.REPEAT_MODE_ONE -> Icons.Default.RepeatOne
                        else -> Icons.Default.Repeat
                    }
                    val isRepeatActive = repeatMode != Player.REPEAT_MODE_OFF
                    Icon(
                        repeatIcon,
                        contentDescription = "Repeat",
                        tint = if (isRepeatActive) MuzoCyan else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // ViMusic Bottom Action Bar: Speed selector, Sleep timer, Up Next Queue
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Playback Speed
                TextButton(onClick = { showSpeedDialog = true }) {
                    Icon(
                        Icons.Outlined.Speed,
                        contentDescription = null,
                        tint = if (playbackSpeed != 1.0f) MuzoCyan else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${playbackSpeed}x",
                        color = if (playbackSpeed != 1.0f) MuzoCyan else MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                // Sleep Timer
                TextButton(onClick = onOpenSleepTimer) {
                    Icon(
                        Icons.Default.Timer,
                        contentDescription = null,
                        tint = if (sleepTimerSeconds != null) MuzoCyan else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (sleepTimerSeconds != null) "${sleepTimerSeconds / 60}m" else "Sleep Timer",
                        color = if (sleepTimerSeconds != null) MuzoCyan else MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                // Queue
                TextButton(onClick = { showQueueSheet = true }) {
                    Icon(
                        Icons.Default.QueueMusic,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Queue (${queue.size})",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }

    // Playback Speed Selector Dialog
    if (showSpeedDialog) {
        val speeds = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f)
        AlertDialog(
            onDismissRequest = { showSpeedDialog = false },
            title = { Text("Playback Speed", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    speeds.forEach { speed ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    onSetPlaybackSpeed(speed)
                                    showSpeedDialog = false
                                }
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = playbackSpeed == speed,
                                onClick = {
                                    onSetPlaybackSpeed(speed)
                                    showSpeedDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (speed == 1.0f) "1.0x (Normal)" else "${speed}x",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (playbackSpeed == speed) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSpeedDialog = false }) { Text("Close") }
            }
        )
    }

    // Up Next Queue Bottom Sheet
    if (showQueueSheet) {
        ModalBottomSheet(
            onDismissRequest = { showQueueSheet = false },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Text(
                    text = "Up Next Queue (${queue.size})",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 12.dp)
                )

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp)
                ) {
                    itemsIndexed(queue) { index, item ->
                        val isCurrent = item.id == currentSong.id
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    onPlayQueueSong(item)
                                    showQueueSheet = false
                                },
                            color = if (isCurrent) MuzoCyan.copy(alpha = 0.15f) else Color.Transparent
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${index + 1}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isCurrent) MuzoCyan else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.width(28.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.title,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isCurrent) MuzoCyan else MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = item.displayArtist,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
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

@Composable
private fun PlayerPillTab(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = if (isSelected) MuzoCyan.copy(alpha = 0.25f) else Color.Transparent,
        modifier = Modifier
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = if (isSelected) MuzoCyan else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ViMusicVisualizer(isPlaying: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "visualizer")
    val bars = 24

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 0 until bars) {
            val duration = remember(i) { 350 + (i % 7) * 90 }
            val minHeight = remember(i) { 15f + (i % 5) * 5f }
            val maxHeight = remember(i) { 100f + (i % 6) * 16f }

            val heightAnim by infiniteTransition.animateFloat(
                initialValue = if (isPlaying) minHeight else 10f,
                targetValue = if (isPlaying) maxHeight else 10f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = duration, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "bar_$i"
            )

            Box(
                modifier = Modifier
                    .width(6.dp)
                    .height(heightAnim.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(MuzoCyan, MuzoPurple)
                        )
                    )
            )
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
