package com.shashwat.muzo.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.shashwat.muzo.data.api.InnerTubeClient
import com.shashwat.muzo.data.model.MuzoItem
import com.shashwat.muzo.ui.components.SongListItem
import com.shashwat.muzo.ui.theme.MuzoCyan
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SearchScreen(
    currentSong: MuzoItem?,
    isPlaying: Boolean,
    favoriteIds: Set<String>,
    onPlaySong: (MuzoItem, List<MuzoItem>) -> Unit,
    onToggleFavorite: (MuzoItem) -> Unit,
    onAddToPlaylist: (MuzoItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val innerTubeClient = remember { InnerTubeClient() }
    val scope = rememberCoroutineScope()

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("songs") }
    val filterOptions = listOf(
        "songs" to "Songs",
        "videos" to "Videos",
        "albums" to "Albums",
        "playlists" to "Playlists"
    )

    var searchResults by remember { mutableStateOf<List<MuzoItem>>(emptyList()) }
    var suggestions by remember { mutableStateOf<List<String>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }

    var debounceJob by remember { mutableStateOf<Job?>(null) }

    fun executeSearch(query: String, filter: String) {
        if (query.isBlank()) return
        isSearching = true
        suggestions = emptyList()
        scope.launch {
            try {
                val results = innerTubeClient.search(query.trim(), filter)
                searchResults = results
            } catch (_: Exception) {
                searchResults = emptyList()
            }
            isSearching = false
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("search_screen")
    ) {
        // Search Input Box
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { query ->
                searchQuery = query
                debounceJob?.cancel()
                if (query.length >= 2) {
                    debounceJob = scope.launch {
                        delay(200)
                        val suggs = innerTubeClient.getSearchSuggestions(query.trim())
                        suggestions = suggs
                    }
                } else {
                    suggestions = emptyList()
                }
            },
            placeholder = { Text("Search YouTube Music songs, artists, albums…") },
            leadingIcon = {
                Icon(
                    Icons.Default.Search,
                    contentDescription = "Search",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(
                        onClick = {
                            searchQuery = ""
                            suggestions = emptyList()
                            searchResults = emptyList()
                        }
                    ) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .testTag("search_input")
        )

        // Filter chips
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filterOptions) { (key, label) ->
                FilterChip(
                    selected = selectedFilter == key,
                    onClick = {
                        selectedFilter = key
                        if (searchQuery.isNotBlank()) {
                            executeSearch(searchQuery, key)
                        }
                    },
                    label = { Text(label) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MuzoCyan.copy(alpha = 0.2f),
                        selectedLabelColor = MuzoCyan
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (isSearching) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MuzoCyan)
            }
        } else if (suggestions.isNotEmpty() && searchResults.isEmpty()) {
            // Display suggestions
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 120.dp)
            ) {
                items(suggestions) { suggestion ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                searchQuery = suggestion
                                executeSearch(suggestion, selectedFilter)
                            }
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.History,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = suggestion,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        } else if (searchResults.isNotEmpty()) {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 140.dp)
            ) {
                item {
                    Text(
                        text = "Results (${searchResults.size})",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                    )
                }

                items(searchResults) { song ->
                    SongListItem(
                        song = song,
                        isPlaying = isPlaying && currentSong?.id == song.id,
                        isFavorite = favoriteIds.contains(song.id),
                        onClick = { onPlaySong(song, searchResults) },
                        onToggleFavorite = { onToggleFavorite(song) },
                        onAddToPlaylist = { onAddToPlaylist(song) }
                    )
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (searchQuery.isNotEmpty()) "No results found for \"$searchQuery\"" else "Direct YouTube Music search (No server required)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}
