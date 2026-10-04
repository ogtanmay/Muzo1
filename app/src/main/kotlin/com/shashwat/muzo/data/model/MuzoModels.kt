package com.shashwat.muzo.data.model

import kotlinx.serialization.Serializable

@Serializable
data class MuzoItem(
    val videoId: String? = null,
    val title: String = "",
    val channelName: String? = null,
    val artists: List<MuzoArtist> = emptyList(),
    val thumbnails: List<MuzoThumbnail> = emptyList(),
    val duration: String? = null,
    val durationSeconds: Int? = null,
    val resultType: String = "song",
    val audioUrl: String? = null,
    val description: String? = null
) {
    val displayArtist: String
        get() = artists.firstOrNull()?.name
            ?: channelName
            ?: "Unknown Artist"

    val displayThumbnail: String
        get() = thumbnails.lastOrNull()?.url
            ?: (if (!videoId.isNullOrEmpty() && !videoId.startsWith("user_track_")) {
                "https://i.ytimg.com/vi/$videoId/hqdefault.jpg"
            } else {
                ""
            })

    val id: String
        get() = videoId ?: title.hashCode().toString()
}

@Serializable
data class MuzoArtist(
    val name: String = "",
    val id: String? = null
)

@Serializable
data class MuzoThumbnail(
    val url: String = "",
    val width: Int = 0,
    val height: Int = 0
)

@Serializable
data class AlbumDetails(
    val id: String = "",
    val title: String = "",
    val artist: String = "",
    val year: String? = null,
    val thumbnail: String? = null,
    val playlistId: String? = null,
    val tracks: List<MuzoItem> = emptyList(),
    val type: String? = null
)

@Serializable
data class PlaylistDetails(
    val id: String = "",
    val title: String = "",
    val author: String = "",
    val thumbnail: String? = null,
    val trackCount: Int = 0,
    val tracks: List<MuzoItem> = emptyList()
)

data class SyncedLine(
    val timestampMs: Long,
    val text: String
)

data class Lyrics(
    val trackName: String = "",
    val artistName: String = "",
    val plainLyrics: String = "",
    val syncedLines: List<SyncedLine> = emptyList(),
    val isSynced: Boolean = syncedLines.isNotEmpty()
)

data class UserPlaylist(
    val id: String,
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
    val songs: List<MuzoItem> = emptyList()
)

enum class AppThemeMode {
    SYSTEM,
    DARK,
    LIGHT,
    AMOLED
}

enum class AudioQuality(val title: String, val bitrate: String) {
    HIGH("High Quality", "320 kbps"),
    MEDIUM("Standard Quality", "160 kbps"),
    LOW("Data Saver", "96 kbps")
}
