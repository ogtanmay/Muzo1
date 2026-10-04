package com.shashwat.muzo.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.shashwat.muzo.data.api.MuzoApiService
import com.shashwat.muzo.data.model.MuzoItem
import com.shashwat.muzo.ui.components.SongListItem
import com.shashwat.muzo.ui.theme.MuzoCyan
import kotlinx.coroutines.launch

@Composable
fun CommunityScreen(
    currentSong: MuzoItem?,
    isPlaying: Boolean,
    favoriteIds: Set<String>,
    onPlaySong: (MuzoItem, List<MuzoItem>) -> Unit,
    onToggleFavorite: (MuzoItem) -> Unit,
    onAddToPlaylist: (MuzoItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val apiService = remember { MuzoApiService() }
    val scope = rememberCoroutineScope()

    var communityTracks by remember { mutableStateOf<List<MuzoItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    fun loadCommunity() {
        isLoading = true
        scope.launch {
            try {
                communityTracks = apiService.getCommunityFeed()
            } catch (_: Exception) {}
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        loadCommunity()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("community_screen")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Community",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = "Tracks shared by Muzo creators",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(onClick = { loadCommunity() }) {
                Icon(
                    Icons.Default.Refresh,
                    contentDescription = "Refresh",
                    tint = MuzoCyan
                )
            }
        }

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MuzoCyan)
            }
        } else if (communityTracks.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.People,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No community tracks right now",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 140.dp)
            ) {
                items(communityTracks) { track ->
                    SongListItem(
                        song = track,
                        isPlaying = isPlaying && currentSong?.id == track.id,
                        isFavorite = favoriteIds.contains(track.id),
                        onClick = { onPlaySong(track, communityTracks) },
                        onToggleFavorite = { onToggleFavorite(track) },
                        onAddToPlaylist = { onAddToPlaylist(track) }
                    )
                }
            }
        }
    }
}
