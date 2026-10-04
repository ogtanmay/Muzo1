package com.shashwat.muzo.data.api

import android.util.Log
import com.google.gson.JsonParser
import com.shashwat.muzo.data.model.MuzoArtist
import com.shashwat.muzo.data.model.MuzoItem
import com.shashwat.muzo.data.model.MuzoThumbnail
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

data class ImportProgress(
    val total: Int = 0,
    val current: Int = 0,
    val status: String = "",
    val playlistName: String = "",
    val importedSongs: List<MuzoItem> = emptyList(),
    val isComplete: Boolean = false,
    val errorMessage: String? = null
)

class SpotifyImportService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    companion object {
        private const val TAG = "SpotifyImportService"
        private const val BASE_URL = "https://dawn-violet-2368.shashwat-coding.workers.dev"
    }

    private fun extractPlaylistId(input: String): String {
        val trimmed = input.trim()
        if (trimmed.contains("spotify.com/playlist/")) {
            val parts = trimmed.split("spotify.com/playlist/")
            if (parts.size > 1) {
                return parts[1].split("?").first().trim()
            }
        }
        return trimmed
    }

    fun importSpotifyPlaylist(urlOrId: String): Flow<ImportProgress> = flow {
        val playlistId = extractPlaylistId(urlOrId)
        if (playlistId.isEmpty()) {
            emit(ImportProgress(isComplete = true, errorMessage = "Please enter a valid Spotify playlist URL or ID"))
            return@flow
        }

        emit(ImportProgress(status = "Fetching Spotify playlist metadata..."))

        try {
            val url = "$BASE_URL/api/data/spotify/playlist/$playlistId"
            val request = Request.Builder().url(url).build()

            val responseData = withContext(Dispatchers.IO) {
                client.newCall(request).execute().use { resp ->
                    if (!resp.isSuccessful) return@use null
                    resp.body?.string()
                }
            }

            if (responseData.isNullOrEmpty()) {
                emit(ImportProgress(isComplete = true, errorMessage = "Could not fetch Spotify playlist. Check the link or privacy."))
                return@flow
            }

            val root = JsonParser.parseString(responseData).asJsonObject
            val playlistInfo = root.getAsJsonObject("playlist")
            val playlistName = playlistInfo?.get("name")?.asString ?: "Imported Spotify Playlist"
            val tracksArray = root.getAsJsonArray("tracks")

            if (tracksArray == null || tracksArray.size() == 0) {
                emit(ImportProgress(isComplete = true, errorMessage = "The Spotify playlist has no tracks."))
                return@flow
            }

            val total = tracksArray.size()
            emit(ImportProgress(total = total, current = 0, status = "Found $total tracks in \"$playlistName\"", playlistName = playlistName))

            val imported = mutableListOf<MuzoItem>()
            var count = 0

            for (elem in tracksArray) {
                if (!elem.isJsonObject) continue
                val trackObj = elem.asJsonObject
                val title = trackObj.get("title")?.asString ?: ""
                val artists = trackObj.get("artists")?.asString ?: ""

                emit(ImportProgress(
                    total = total,
                    current = count,
                    status = "Matching \"$title\" ($count/$total)...",
                    playlistName = playlistName,
                    importedSongs = imported
                ))

                val resolved = resolveToYtm(title, artists)
                if (resolved != null) {
                    imported.add(resolved)
                }
                count++
            }

            emit(ImportProgress(
                total = total,
                current = total,
                status = "Successfully imported ${imported.size} tracks!",
                playlistName = playlistName,
                importedSongs = imported,
                isComplete = true
            ))

        } catch (e: Exception) {
            Log.e(TAG, "Import failed: ${e.message}", e)
            emit(ImportProgress(isComplete = true, errorMessage = "Import failed: ${e.message}"))
        }
    }

    private suspend fun resolveToYtm(name: String, artist: String): MuzoItem? = withContext(Dispatchers.IO) {
        try {
            val encodedName = URLEncoder.encode(name, "UTF-8")
            val encodedArtist = URLEncoder.encode(artist, "UTF-8")
            val url = "$BASE_URL/api/find/track?name=$encodedName&artist=$encodedArtist"
            val request = Request.Builder().url(url).build()

            client.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext null
                val body = resp.body?.string() ?: return@withContext null
                val obj = JsonParser.parseString(body).asJsonObject
                val videoId = obj.get("videoId")?.asString ?: return@withContext null
                val title = obj.get("title")?.asString ?: name
                val channel = obj.get("channelName")?.asString ?: artist
                val thumb = obj.get("thumbnail")?.asString ?: ""

                MuzoItem(
                    videoId = videoId,
                    title = title,
                    channelName = channel,
                    artists = listOf(MuzoArtist(name = channel)),
                    thumbnails = if (thumb.isNotEmpty()) listOf(MuzoThumbnail(thumb)) else emptyList(),
                    resultType = "song"
                )
            }
        } catch (e: Exception) {
            Log.d(TAG, "Resolve track error: ${e.message}")
            null
        }
    }
}
