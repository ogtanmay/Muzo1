package com.shashwat.muzo.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.shashwat.muzo.data.api.InnerTubeClient
import com.shashwat.muzo.data.model.MuzoItem
import com.shashwat.muzo.ui.components.SongCard
import com.shashwat.muzo.ui.components.SongListItem
import com.shashwat.muzo.ui.theme.MuzoCyan
import com.shashwat.muzo.ui.theme.MuzoPink
import com.shashwat.muzo.ui.theme.MuzoPurple
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    currentSong: MuzoItem?,
    isPlaying: Boolean,
    favoriteIds: Set<String>,
    onPlaySong: (MuzoItem, List<MuzoItem>) -> Unit,
    onToggleFavorite: (MuzoItem) -> Unit,
    onAddToPlaylist: (MuzoItem) -> Unit,
    onOpenPlaylist: (String, String) -> Unit,
    onOpenAccount: () -> Unit = {},
    userDisplayName: String = "Guest User",
    modifier: Modifier = Modifier
) {
    val innerTubeClient = remember { InnerTubeClient() }
    val scope = rememberCoroutineScope()

    val moodChips = listOf("All", "Relax", "Workout", "Focus", "Energize", "Commute")
    var selectedMood by remember { mutableStateOf("All") }

    var trendingSongs by remember { mutableStateOf<List<MuzoItem>>(emptyList()) }
    var quickPicks by remember { mutableStateOf<List<MuzoItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    fun loadContent(mood: String) {
        isLoading = true
        scope.launch {
            try {
                val moodParam = if (mood == "All") null else mood
                val songs = innerTubeClient.getHomeFeed(moodParam)
                trendingSongs = songs
                quickPicks = if (songs.size > 8) songs.take(8) else songs
            } catch (_: Exception) {}
            isLoading = false
        }
    }

    LaunchedEffect(selectedMood) {
        loadContent(selectedMood)
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("home_screen"),
        contentPadding = PaddingValues(bottom = 140.dp)
    ) {
        // App Header / Hero Banner
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                MuzoPurple.copy(alpha = 0.5f),
                                MuzoCyan.copy(alpha = 0.3f)
                            )
                        )
                    )
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Muzo",
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                        Text(
                            text = "Hi, $userDisplayName • ViMusic Mode",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(
                        onClick = onOpenAccount,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(MuzoCyan.copy(alpha = 0.2f))
                    ) {
                        Icon(
                            Icons.Default.MusicNote,
                            contentDescription = "Account",
                            tint = MuzoCyan,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }

        // ViMusic Mood Selector Chips
        item {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(moodChips) { mood ->
                    FilterChip(
                        selected = selectedMood == mood,
                        onClick = { selectedMood = mood },
                        label = { Text(mood) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MuzoCyan.copy(alpha = 0.25f),
                            selectedLabelColor = MuzoCyan
                        )
                    )
                }
            }
        }

        if (isLoading) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MuzoCyan)
                }
            }
        } else {
            // Quick Picks Horizontal Carousel
            if (trendingSongs.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.TrendingUp,
                            contentDescription = null,
                            tint = MuzoCyan,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (selectedMood == "All") "Quick Picks" else "$selectedMood Mix",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(trendingSongs.take(12)) { song ->
                            SongCard(
                                song = song,
                                isPlaying = isPlaying && currentSong?.id == song.id,
                                onClick = { onPlaySong(song, trendingSongs) }
                            )
                        }
                    }
                }
            }

            // Songs List Section
            if (trendingSongs.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Whatshot,
                            contentDescription = null,
                            tint = MuzoPink,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Recommended Tracks",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                items(trendingSongs.drop(4)) { song ->
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
