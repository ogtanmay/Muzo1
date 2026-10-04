package com.shashwat.muzo.data.api

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.shashwat.muzo.data.model.AlbumDetails
import com.shashwat.muzo.data.model.MuzoArtist
import com.shashwat.muzo.data.model.MuzoItem
import com.shashwat.muzo.data.model.MuzoThumbnail
import com.shashwat.muzo.data.model.PlaylistDetails
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

class MuzoApiService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    companion object {
        private const val TAG = "MuzoApiService"
        private const val WORKER_BASE = "https://muzo-api.shashwat-coding.workers.dev"
        private const val YTIFY_BASE = "https://veltrixcode-ytify.hf.space/api"
        private const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
    }

    suspend fun search(query: String, filter: String = "songs"): List<MuzoItem> = withContext(Dispatchers.IO) {
        try {
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val endpoint = if (filter == "videos") "/api/yt_search" else "/api/search"
            val url = if (filter == "all") {
                "$WORKER_BASE$endpoint?q=$encodedQuery"
            } else {
                val encodedFilter = URLEncoder.encode(filter, "UTF-8")
                "$WORKER_BASE$endpoint?q=$encodedQuery&filter=$encodedFilter"
            }

            val request = Request.Builder()
                .url(url)
                .header("User-Agent", USER_AGENT)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext emptyList()
                val body = response.body?.string() ?: return@withContext emptyList()
                val json = JsonParser.parseString(body).asJsonObject
                val resultsArray = json.getAsJsonArray("results") ?: return@withContext emptyList()

                val items = mutableListOf<MuzoItem>()
                for (elem in resultsArray) {
                    if (elem.isJsonObject) {
                        items.add(parseMuzoItem(elem.asJsonObject))
                    }
                }
                items
            }
        } catch (e: Exception) {
            Log.e(TAG, "Search error: ${e.message}", e)
            emptyList()
        }
    }

    suspend fun getSearchSuggestions(query: String): List<String> = withContext(Dispatchers.IO) {
        try {
            val encoded = URLEncoder.encode(query, "UTF-8")
            val url = "$WORKER_BASE/api/search/suggestions?q=$encoded&music=1"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", USER_AGENT)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext emptyList()
                val body = response.body?.string() ?: return@withContext emptyList()
                val json = JsonParser.parseString(body).asJsonObject
                val suggestions = json.getAsJsonArray("suggestions") ?: return@withContext emptyList()
                val list = mutableListOf<String>()
                for (s in suggestions) {
                    list.add(s.asString)
                }
                list
            }
        } catch (e: Exception) {
            Log.e(TAG, "Suggestions error: ${e.message}", e)
            emptyList()
        }
    }

    suspend fun getTrendingContent(): Triple<List<MuzoItem>, List<MuzoItem>, List<MuzoItem>> = withContext(Dispatchers.IO) {
        try {
            val url = "$WORKER_BASE/api/trending"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", USER_AGENT)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext Triple(emptyList(), emptyList(), emptyList())
                val body = response.body?.string() ?: return@withContext Triple(emptyList(), emptyList(), emptyList())
                val json = JsonParser.parseString(body).asJsonObject
                if (json.get("success")?.asBoolean != true) return@withContext Triple(emptyList(), emptyList(), emptyList())

                val data = json.getAsJsonObject("data") ?: return@withContext Triple(emptyList(), emptyList(), emptyList())

                val songs = parseList(data.getAsJsonArray("songs"), "song")
                val videos = parseList(data.getAsJsonArray("videos"), "video")
                val playlists = parseList(data.getAsJsonArray("playlists"), "playlist")

                Triple(songs, videos, playlists)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Trending error: ${e.message}", e)
            Triple(emptyList(), emptyList(), emptyList())
        }
    }

    suspend fun getTopOnMuzo(): List<MuzoItem> = withContext(Dispatchers.IO) {
        try {
            val url = "$YTIFY_BASE/trending"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", USER_AGENT)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext emptyList()
                val body = response.body?.string() ?: return@withContext emptyList()
                val json = JsonParser.parseString(body).asJsonObject
                val list = json.getAsJsonArray("trending") ?: return@withContext emptyList()

                val items = mutableListOf<MuzoItem>()
                for (elem in list) {
                    if (elem.isJsonObject) {
                        val obj = elem.asJsonObject
                        val title = obj.get("title")?.asString ?: "Unknown"
                        val videoId = obj.get("videoId")?.asString
                        val channelName = obj.get("channelName")?.asString
                        val thumbUrl = obj.get("thumbnail")?.asString ?: ""
                        val duration = obj.get("duration")?.asString
                        items.add(
                            MuzoItem(
                                videoId = videoId,
                                title = title,
                                channelName = channelName,
                                artists = listOf(MuzoArtist(name = channelName ?: "Artist")),
                                thumbnails = listOf(MuzoThumbnail(url = thumbUrl)),
                                duration = duration,
                                resultType = "song"
                            )
                        )
                    }
                }
                items
            }
        } catch (e: Exception) {
            Log.e(TAG, "Top on Muzo error: ${e.message}", e)
            emptyList()
        }
    }

    suspend fun getUpNext(videoId: String): List<MuzoItem> = withContext(Dispatchers.IO) {
        try {
            val url = "$WORKER_BASE/api/related?videoId=$videoId"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", USER_AGENT)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext emptyList()
                val body = response.body?.string() ?: return@withContext emptyList()
                val json = JsonParser.parseString(body).asJsonObject
                if (json.get("success")?.asBoolean != true) return@withContext emptyList()
                val songs = json.getAsJsonArray("songs") ?: return@withContext emptyList()
                parseList(songs, "song")
            }
        } catch (e: Exception) {
            Log.e(TAG, "UpNext error: ${e.message}", e)
            emptyList()
        }
    }

    suspend fun getAlbumDetails(albumId: String): AlbumDetails? = withContext(Dispatchers.IO) {
        try {
            val url = "$WORKER_BASE/api/album/$albumId"
            val request = Request.Builder().url(url).header("User-Agent", USER_AGENT).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val body = response.body?.string() ?: return@withContext null
                val obj = JsonParser.parseString(body).asJsonObject
                val id = obj.get("id")?.asString ?: albumId
                val title = obj.get("title")?.asString ?: ""
                val artist = obj.get("artist")?.asString ?: ""
                val year = obj.get("year")?.asString
                val thumbnail = obj.get("thumbnail")?.asString
                val playlistId = obj.get("playlistId")?.asString
                val tracks = parseList(obj.getAsJsonArray("tracks"), "song")
                AlbumDetails(id, title, artist, year, thumbnail, playlistId, tracks)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Album details error: ${e.message}", e)
            null
        }
    }

    suspend fun getPlaylistDetails(playlistId: String): PlaylistDetails? = withContext(Dispatchers.IO) {
        try {
            val url = "$WORKER_BASE/api/playlist/$playlistId"
            val request = Request.Builder().url(url).header("User-Agent", USER_AGENT).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val body = response.body?.string() ?: return@withContext null
                val obj = JsonParser.parseString(body).asJsonObject
                val id = obj.get("id")?.asString ?: playlistId
                val title = obj.get("title")?.asString ?: ""
                val author = obj.get("author")?.asString ?: ""
                val thumbnail = obj.get("thumbnail")?.asString
                val tracks = parseList(obj.getAsJsonArray("tracks"), "song")
                PlaylistDetails(id, title, author, thumbnail, tracks.size, tracks)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Playlist details error: ${e.message}", e)
            null
        }
    }

    suspend fun getCommunityFeed(): List<MuzoItem> = withContext(Dispatchers.IO) {
        try {
            val url = "$YTIFY_BASE/community?limit=50&offset=0"
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext emptyList()
                val body = response.body?.string() ?: return@withContext emptyList()
                val json = JsonParser.parseString(body).asJsonObject
                val list = json.getAsJsonArray("tracks") ?: return@withContext emptyList()
                val items = mutableListOf<MuzoItem>()
                for (elem in list) {
                    if (elem.isJsonObject) {
                        val obj = elem.asJsonObject
                        val id = obj.get("id")?.asString ?: ""
                        val title = obj.get("title")?.asString ?: "Community Track"
                        val desc = obj.get("description")?.asString
                        val thumb = obj.get("thumbnail_url")?.asString
                        val audioUrl = obj.get("audio_url")?.asString
                        val uploader = obj.getAsJsonObject("uploader")
                        val uploaderName = uploader?.get("username")?.asString ?: "Community User"
                        items.add(
                            MuzoItem(
                                videoId = "user_track_$id",
                                title = title,
                                description = desc,
                                channelName = uploaderName,
                                artists = listOf(MuzoArtist(name = uploaderName)),
                                thumbnails = if (!thumb.isNullOrEmpty()) listOf(MuzoThumbnail(url = thumb)) else emptyList(),
                                audioUrl = audioUrl,
                                resultType = "user_track"
                            )
                        )
                    }
                }
                items
            }
        } catch (e: Exception) {
            Log.e(TAG, "Community feed error: ${e.message}", e)
            emptyList()
        }
    }

    private fun parseList(array: com.google.gson.JsonArray?, forceType: String? = null): List<MuzoItem> {
        if (array == null) return emptyList()
        val items = mutableListOf<MuzoItem>()
        for (e in array) {
            if (e.isJsonObject) {
                items.add(parseMuzoItem(e.asJsonObject, forceType))
            }
        }
        return items
    }

    private fun parseMuzoItem(obj: JsonObject, forceType: String? = null): MuzoItem {
        val videoId = obj.get("videoId")?.asString
            ?: obj.get("id")?.asString
        val title = obj.get("title")?.asString ?: "Unknown Title"
        val channelName = obj.get("channelName")?.asString
        val duration = obj.get("duration")?.asString
        val durationSecs = obj.get("durationSeconds")?.asInt
        val resType = forceType ?: obj.get("resultType")?.asString ?: "song"
        val audioUrl = obj.get("audioUrl")?.asString

        val artists = mutableListOf<MuzoArtist>()
        val artistsArray = obj.getAsJsonArray("artists")
        if (artistsArray != null) {
            for (a in artistsArray) {
                if (a.isJsonObject) {
                    val name = a.asJsonObject.get("name")?.asString ?: ""
                    val id = a.asJsonObject.get("id")?.asString
                    artists.add(MuzoArtist(name, id))
                }
            }
        } else if (!channelName.isNullOrEmpty()) {
            artists.add(MuzoArtist(channelName))
        }

        val thumbnails = mutableListOf<MuzoThumbnail>()
        val thumbsArray = obj.getAsJsonArray("thumbnails")
        if (thumbsArray != null) {
            for (t in thumbsArray) {
                if (t.isJsonObject) {
                    val u = t.asJsonObject.get("url")?.asString ?: ""
                    val w = t.asJsonObject.get("width")?.asInt ?: 0
                    val h = t.asJsonObject.get("height")?.asInt ?: 0
                    thumbnails.add(MuzoThumbnail(u, w, h))
                }
            }
        } else {
            val singleThumb = obj.get("thumbnail")?.asString
            if (!singleThumb.isNullOrEmpty()) {
                thumbnails.add(MuzoThumbnail(singleThumb))
            }
        }

        return MuzoItem(
            videoId = videoId,
            title = title,
            channelName = channelName,
            artists = artists,
            thumbnails = thumbnails,
            duration = duration,
            durationSeconds = durationSecs,
            resultType = resType,
            audioUrl = audioUrl,
            description = obj.get("description")?.asString
        )
    }
}
