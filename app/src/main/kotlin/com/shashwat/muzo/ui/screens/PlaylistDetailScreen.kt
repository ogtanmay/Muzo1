package com.shashwat.muzo.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
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
import com.shashwat.muzo.data.model.MuzoItem
import com.shashwat.muzo.ui.components.SongListItem
import com.shashwat.muzo.ui.theme.MuzoCyan
import com.shashwat.muzo.ui.theme.MuzoPurple

@Composable
fun PlaylistDetailScreen(
    title: String,
    songs: List<MuzoItem>,
    playlistId: String?,
    currentSong: MuzoItem?,
    isPlaying: Boolean,
    favoriteIds: Set<String>,
    onBack: () -> Unit,
    onPlaySong: (MuzoItem, List<MuzoItem>) -> Unit,
    onPlayAll: (List<MuzoItem>, Boolean) -> Unit,
    onToggleFavorite: (MuzoItem) -> Unit,
    onAddToPlaylist: (MuzoItem) -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("playlist_detail_screen")
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${songs.size} tracks",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Action Buttons: Play All & Shuffle
        if (songs.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { onPlayAll(songs, false) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MuzoCyan)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Play All", color = Color.Black, fontWeight = FontWeight.Bold)
                }

                FilledTonalButton(
                    onClick = { onPlayAll(songs, true) },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Shuffle, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Shuffle", fontWeight = FontWeight.Bold)
                }
            }
        }

        if (songs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No tracks in this playlist",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 140.dp)
            ) {
                items(songs) { song ->
                    SongListItem(
                        song = song,
                        isPlaying = isPlaying && currentSong?.id == song.id,
                        isFavorite = favoriteIds.contains(song.id),
                        onClick = { onPlaySong(song, songs) },
                        onToggleFavorite = { onToggleFavorite(song) },
                        onAddToPlaylist = { onAddToPlaylist(song) }
                    )
                }
            }
        }
    }
}
