package com.shashwat.muzo.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.shashwat.muzo.data.api.InnerTubeClient
import com.shashwat.muzo.data.model.MuzoItem
import com.shashwat.muzo.data.model.UserPlaylist
import com.shashwat.muzo.ui.components.SongListItem
import com.shashwat.muzo.ui.theme.MuzoCyan
import com.shashwat.muzo.ui.theme.MuzoPurple
import kotlinx.coroutines.launch

/**
 * Replicates the exact layout from Shashwat-CODING/Muzo home_screen.dart:
 * - Clean Muzo Logo Header + User Avatar
 * - Quick Picks horizontal carousel
 * - Top on Muzo (Trending) 152dp cards
 * - Recently Played & Favourites sections
 */

@Composable
fun HomeScreen(
    currentSong: MuzoItem?,
    isPlaying: Boolean,
    favoriteIds: Set<String>,
    favorites: List<MuzoItem> = emptyList(),
    history: List<MuzoItem> = emptyList(),
    playlists: List<UserPlaylist> = emptyList(),
    onPlaySong: (MuzoItem, List<MuzoItem>) -> Unit,
    onToggleFavorite: (MuzoItem) -> Unit,
    onAddToPlaylist: (MuzoItem) -> Unit,
    onOpenPlaylist: (String, String) -> Unit,
    onOpenAccount: () -> Unit = {},
    userDisplayName: String = "User",
    userPhotoUrl: String? = null,
    modifier: Modifier = Modifier
) {
    val innerTubeClient = remember { InnerTubeClient() }
    val scope = rememberCoroutineScope()

    var trendingSongs by remember { mutableStateOf<List<MuzoItem>>(emptyList()) }
    var quickPicks by remember { mutableStateOf<List<MuzoItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        isLoading = true
        scope.launch {
            try {
                val songs = innerTubeClient.getHomeFeed(null)
                trendingSongs = songs
                quickPicks = if (songs.size > 8) songs.take(8) else songs
            } catch (_: Exception) {}
            isLoading = false
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("home_screen"),
        contentPadding = PaddingValues(bottom = 150.dp)
    ) {
        // ── 1. Muzo Header (Logo + Muzo title on left, User Avatar on right) ──
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Logo + Muzo Title
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                Brush.linearGradient(listOf(MuzoCyan, MuzoPurple))
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.MusicNote,
                            contentDescription = "Muzo",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Muzo",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = (-0.8).sp
                        ),
                        color = Color.White
                    )
                }

                // Avatar button
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .border(1.dp, Color.White.copy(alpha = 0.18f), CircleShape)
                        .clickable(onClick = onOpenAccount),
                    contentAlignment = Alignment.Center
                ) {
                    if (!userPhotoUrl.isNullOrEmpty()) {
                        AsyncImage(
                            model = userPhotoUrl,
                            contentDescription = userDisplayName,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.White.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.85f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }

        if (isLoading) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MuzoCyan, strokeWidth = 2.5.dp)
                }
            }
        } else {
            // ── 2. Quick Picks Section ──
            if (quickPicks.isNotEmpty()) {
                item {
                    MuzoSectionHeader(title = "Quick Picks")
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(quickPicks) { song ->
                            MuzoMusicCard(
                                song = song,
                                isPlaying = isPlaying && currentSong?.id == song.id,
                                onClick = { onPlaySong(song, quickPicks) }
                            )
                        }
                    }
                }
            }

            // ── 3. Top on Muzo (Trending) ──
            if (trendingSongs.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(14.dp))
                    MuzoSectionHeader(title = "Top on Muzo")
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(trendingSongs.drop(4).take(10)) { song ->
                            MuzoMusicCard(
                                song = song,
                                isPlaying = isPlaying && currentSong?.id == song.id,
                                onClick = { onPlaySong(song, trendingSongs) }
                            )
                        }
                    }
                }
            }

            // ── 4. Recently Played Section ──
            if (history.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(14.dp))
                    MuzoSectionHeader(title = "Recently Played")
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(history.take(10)) { song ->
                            MuzoMusicCard(
                                song = song,
                                isPlaying = isPlaying && currentSong?.id == song.id,
                                onClick = { onPlaySong(song, history) }
                            )
                        }
                    }
                }
            }

            // ── 5. Favourites Section ──
            if (favorites.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(14.dp))
                    MuzoSectionHeader(title = "Favourites")
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(favorites) { song ->
                            MuzoMusicCard(
                                song = song,
                                isPlaying = isPlaying && currentSong?.id == song.id,
                                onClick = { onPlaySong(song, favorites) }
                            )
                        }
                    }
                }
            }

            // ── 6. Recommended Tracks list ──
            if (trendingSongs.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(18.dp))
                    MuzoSectionHeader(title = "Recommended Tracks")
                }

                items(trendingSongs.take(15)) { song ->
                    SongListItem(
                        song = song,
                        isPlaying = isPlaying && currentSong?.id == song.id,
                        isFavorite = favoriteIds.contains(song.id),
                        onClick = { onPlaySong(song, trendingSongs) },
                        onToggleFavorite = { onToggleFavorite(song) },
                        onAddToPlaylist = { onAddToPlaylist(song) }
                    )
                }
            }
        }
    }
}

@Composable
private fun MuzoSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium.copy(
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.3).sp
        ),
        color = Color.White,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
    )
}

/**
 * Replicates _buildMusicCard from Muzo home_screen.dart:
 * Width: 152dp, borderRadius: 8dp, subtle shadow, 2-line title.
 */
@Composable
fun MuzoMusicCard(
    song: MuzoItem,
    isPlaying: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .width(152.dp)
            .clickable(onClick = onClick)
    ) {
        // Thumbnail with 8dp radius
        Box(
            modifier = Modifier
                .size(152.dp)
                .shadow(
                    elevation = if (isPlaying) 16.dp else 8.dp,
                    shape = RoundedCornerShape(10.dp),
                    ambientColor = if (isPlaying) MuzoCyan.copy(alpha = 0.4f) else Color.Black.copy(alpha = 0.35f),
                    spotColor = MuzoPurple.copy(alpha = 0.3f)
                )
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF1E202E))
                .border(
                    width = if (isPlaying) 1.5.dp else 0.5.dp,
                    color = if (isPlaying) MuzoCyan else Color.White.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(10.dp)
                )
        ) {
            AsyncImage(
                model = song.displayThumbnail,
                contentDescription = song.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            if (isPlaying) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.45f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.MusicNote,
                        contentDescription = "Playing",
                        tint = MuzoCyan,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Title: 2 lines max, 13sp font
        Text(
            text = song.title,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                lineHeight = 17.sp
            ),
            color = if (isPlaying) MuzoCyan else Color.White
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = song.displayArtist,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 11.sp,
                color = Color.White.copy(alpha = 0.60f)
            )
        )
    }
}
