package com.shashwat.muzo.data.local

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.shashwat.muzo.data.model.AppThemeMode
import com.shashwat.muzo.data.model.AudioQuality
import com.shashwat.muzo.data.model.MuzoItem
import com.shashwat.muzo.data.model.UserPlaylist
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

class LocalMusicStore(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("muzo_local_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    private val _favorites = MutableStateFlow<List<MuzoItem>>(emptyList())
    val favorites: StateFlow<List<MuzoItem>> = _favorites.asStateFlow()

    private val _history = MutableStateFlow<List<MuzoItem>>(emptyList())
    val history: StateFlow<List<MuzoItem>> = _history.asStateFlow()

    private val _playlists = MutableStateFlow<List<UserPlaylist>>(emptyList())
    val playlists: StateFlow<List<UserPlaylist>> = _playlists.asStateFlow()

    private val _themeMode = MutableStateFlow(AppThemeMode.AMOLED)
    val themeMode: StateFlow<AppThemeMode> = _themeMode.asStateFlow()

    private val _audioQuality = MutableStateFlow(AudioQuality.HIGH)
    val audioQuality: StateFlow<AudioQuality> = _audioQuality.asStateFlow()

    private val _username = MutableStateFlow("Music Lover")
    val username: StateFlow<String> = _username.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        // Favorites
        val favJson = prefs.getString("favorites_json", null)
        if (!favJson.isNullOrEmpty()) {
            val type = object : TypeToken<List<MuzoItem>>() {}.type
            _favorites.value = gson.fromJson(favJson, type) ?: emptyList()
        }

        // History
        val histJson = prefs.getString("history_json", null)
        if (!histJson.isNullOrEmpty()) {
            val type = object : TypeToken<List<MuzoItem>>() {}.type
            _history.value = gson.fromJson(histJson, type) ?: emptyList()
        }

        // Playlists
        val plJson = prefs.getString("playlists_json", null)
        if (!plJson.isNullOrEmpty()) {
            val type = object : TypeToken<List<UserPlaylist>>() {}.type
            _playlists.value = gson.fromJson(plJson, type) ?: emptyList()
        } else {
            // Seed a starter playlist if empty
            val defaultList = listOf(
                UserPlaylist(
                    id = "pl_favorites",
                    name = "My Favorites",
                    songs = emptyList()
                )
            )
            _playlists.value = defaultList
            savePlaylists(defaultList)
        }

        // Theme
        val themeStr = prefs.getString("theme_mode", AppThemeMode.AMOLED.name)
        _themeMode.value = try {
            AppThemeMode.valueOf(themeStr ?: AppThemeMode.AMOLED.name)
        } catch (_: Exception) {
            AppThemeMode.AMOLED
        }

        // Quality
        val qualityStr = prefs.getString("audio_quality", AudioQuality.HIGH.name)
        _audioQuality.value = try {
            AudioQuality.valueOf(qualityStr ?: AudioQuality.HIGH.name)
        } catch (_: Exception) {
            AudioQuality.HIGH
        }

        // Username
        _username.value = prefs.getString("username", "Music Lover") ?: "Music Lover"
    }

    // --- Favorites ---

    fun toggleFavorite(item: MuzoItem): Boolean {
        val current = _favorites.value.toMutableList()
        val exists = current.any { it.id == item.id }
        if (exists) {
            current.removeAll { it.id == item.id }
        } else {
            current.add(0, item)
        }
        _favorites.value = current
        prefs.edit().putString("favorites_json", gson.toJson(current)).apply()
        return !exists
    }

    fun isFavorite(item: MuzoItem): Boolean {
        return _favorites.value.any { it.id == item.id }
    }

    // --- History ---

    fun addToHistory(item: MuzoItem) {
        val current = _history.value.toMutableList()
        current.removeAll { it.id == item.id }
        current.add(0, item)
        val trimmed = if (current.size > 100) current.take(100) else current
        _history.value = trimmed
        prefs.edit().putString("history_json", gson.toJson(trimmed)).apply()
    }

    fun clearHistory() {
        _history.value = emptyList()
        prefs.edit().remove("history_json").apply()
    }

    // --- Playlists ---

    fun createPlaylist(name: String, initialSongs: List<MuzoItem> = emptyList()): UserPlaylist {
        val newPlaylist = UserPlaylist(
            id = UUID.randomUUID().toString(),
            name = name.trim().ifEmpty { "New Playlist" },
            songs = initialSongs
        )
        val current = _playlists.value.toMutableList()
        current.add(0, newPlaylist)
        _playlists.value = current
        savePlaylists(current)
        return newPlaylist
    }

    fun addToPlaylist(playlistId: String, song: MuzoItem) {
        val current = _playlists.value.map { pl ->
            if (pl.id == playlistId) {
                if (pl.songs.none { it.id == song.id }) {
                    pl.copy(songs = pl.songs + song)
                } else pl
            } else pl
        }
        _playlists.value = current
        savePlaylists(current)
    }

    fun removeSongFromPlaylist(playlistId: String, songId: String) {
        val current = _playlists.value.map { pl ->
            if (pl.id == playlistId) {
                pl.copy(songs = pl.songs.filterNot { it.id == songId })
            } else pl
        }
        _playlists.value = current
        savePlaylists(current)
    }

    fun deletePlaylist(playlistId: String) {
        val current = _playlists.value.filterNot { it.id == playlistId }
        _playlists.value = current
        savePlaylists(current)
    }

    private fun savePlaylists(list: List<UserPlaylist>) {
        prefs.edit().putString("playlists_json", gson.toJson(list)).apply()
    }

    // --- Settings ---

    fun setThemeMode(mode: AppThemeMode) {
        _themeMode.value = mode
        prefs.edit().putString("theme_mode", mode.name).apply()
    }

    fun setAudioQuality(quality: AudioQuality) {
        _audioQuality.value = quality
        prefs.edit().putString("audio_quality", quality.name).apply()
    }

    fun setUsername(name: String) {
        _username.value = name.trim().ifEmpty { "Music Lover" }
        prefs.edit().putString("username", _username.value).apply()
    }
}
