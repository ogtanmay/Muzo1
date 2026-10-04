package com.shashwat.muzo.data.api

import android.util.Log
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.shashwat.muzo.data.model.MuzoArtist
import com.shashwat.muzo.data.model.MuzoItem
import com.shashwat.muzo.data.model.MuzoThumbnail
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * ViMusic-style direct YouTube Music InnerTube client.
 * Connects directly from the device to YouTube Music and Google public endpoints.
 * ZERO self-hosted APIs or servers required!
 */
class InnerTubeClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    companion object {
        private const val TAG = "InnerTubeClient"
        private const val YTM_BASE = "https://music.youtube.com/youtubei/v1"
        private const val YT_BASE = "https://www.youtube.com/youtubei/v1"
        private val streamCache = ConcurrentHashMap<String, String>()

        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

        private fun createWebRemixContext(): JsonObject {
            val context = JsonObject()
            val client = JsonObject().apply {
                addProperty("clientName", "WEB_REMIX")
                addProperty("clientVersion", "1.20240101.01.00")
                addProperty("hl", "en")
                addProperty("gl", "US")
            }
            context.add("client", client)
            return context
        }

        private fun createAndroidVrContext(): JsonObject {
            val context = JsonObject()
            val client = JsonObject().apply {
                addProperty("clientName", "ANDROID_VR")
                addProperty("clientVersion", "1.60.19")
                addProperty("deviceModel", "Quest 3")
                addProperty("deviceMake", "Oculus")
                addProperty("osVersion", "12L")
                addProperty("osName", "Android")
                addProperty("androidSdkVersion", "32")
                addProperty("hl", "en")
                addProperty("gl", "US")
            }
            context.add("client", client)
            return context
        }
    }

    suspend fun getSearchSuggestions(query: String): List<String> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        try {
            val encoded = URLEncoder.encode(query.trim(), "UTF-8")
            val url = "https://suggestqueries.google.com/complete/search?client=youtube&ds=yt&q=$encoded"
            val request = Request.Builder().url(url).build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext emptyList()
                val body = response.body?.string() ?: return@withContext emptyList()
                // Format: window.google.ac.h(["query",[["item1",0],["item2",0]]])
                val startIndex = body.indexOf('[')
                val endIndex = body.lastIndexOf(']')
                if (startIndex != -1 && endIndex != -1 && endIndex > startIndex) {
                    val jsonStr = body.substring(startIndex, endIndex + 1)
                    val array = JsonParser.parseString(jsonStr).asJsonArray
                    if (array.size() > 1 && array[1].isJsonArray) {
                        val items = array[1].asJsonArray
                        val results = mutableListOf<String>()
                        for (item in items) {
                            if (item.isJsonArray && item.asJsonArray.size() > 0) {
                                results.add(item.asJsonArray[0].asString)
                            }
                        }
                        return@withContext results
                    }
                }
            }
        } catch (e: Exception) {
            Log.d(TAG, "Search suggestions error: ${e.message}")
        }
        emptyList()
    }

    suspend fun search(query: String, filter: String = "songs"): List<MuzoItem> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        try {
            val payload = JsonObject().apply {
                add("context", createWebRemixContext())
                addProperty("query", query)
                // Filter params for YouTube Music
                val params = when (filter.lowercase()) {
                    "songs" -> "EgWKAQIIAWoKEAkQChAFEAMQCQ%3D%3D"
                    "videos" -> "EgWKAQIQAWoKEAkQChAFEAMQCQ%3D%3D"
                    "albums" -> "EgWKAQIYAWoKEAkQChAFEAMQCQ%3D%3D"
                    "playlists" -> "EgWKAQIoAWoKEAkQChAFEAMQCQ%3D%3D"
                    else -> null
                }
                if (params != null) {
                    addProperty("params", params)
                }
            }

            val request = Request.Builder()
                .url("$YTM_BASE/search")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .header("Referer", "https://music.youtube.com/")
                .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext emptyList()
                val body = response.body?.string() ?: return@withContext emptyList()
                val root = JsonParser.parseString(body).asJsonObject
                return@withContext parseInnerTubeResults(root)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Direct search failed: ${e.message}", e)
            emptyList()
        }
    }

    suspend fun getHomeFeed(mood: String? = null): List<MuzoItem> = withContext(Dispatchers.IO) {
        val query = when (mood?.lowercase()) {
            "relax" -> "Chill music"
            "workout" -> "Workout energetic songs"
            "focus" -> "Deep focus study music"
            "energize" -> "Upbeat party music"
            "commute" -> "Road trip driving playlist"
            else -> "Top Hits 2025"
        }
        search(query, "songs")
    }

    suspend fun getRelatedTracks(videoId: String): List<MuzoItem> = withContext(Dispatchers.IO) {
        try {
            val payload = JsonObject().apply {
                add("context", createWebRemixContext())
                addProperty("videoId", videoId)
            }

            val request = Request.Builder()
                .url("$YTM_BASE/next")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .header("Referer", "https://music.youtube.com/")
                .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext emptyList()
                val body = response.body?.string() ?: return@withContext emptyList()
                val root = JsonParser.parseString(body).asJsonObject
                return@withContext parseInnerTubeNextResults(root)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Related tracks error: ${e.message}", e)
            emptyList()
        }
    }

    suspend fun getStreamUrl(item: MuzoItem): String? = withContext(Dispatchers.IO) {
        val videoId = item.videoId ?: return@withContext null
        streamCache[videoId]?.let { return@withContext it }

        // Parallel resolution: Saavn 320kbps fast stream + Direct YouTube InnerTube VR player
        coroutineScope {
            val saavnDeferred = async { fetchSaavnStream(item.title, item.displayArtist) }
            val ytDeferred = async { fetchInnerTubePlayerStream(videoId) }

            val saavn = try { saavnDeferred.await() } catch (_: Exception) { null }
            if (!saavn.isNullOrEmpty()) {
                streamCache[videoId] = saavn
                return@coroutineScope saavn
            }

            val yt = try { ytDeferred.await() } catch (_: Exception) { null }
            if (!yt.isNullOrEmpty()) {
                streamCache[videoId] = yt
                return@coroutineScope yt
            }
            null
        }
    }

    private fun fetchSaavnStream(title: String, artist: String): String? {
        try {
            val cleanT = title.replace(Regex("""\s*[(\[][^\])]*[\])]"""), "").trim()
            val cleanA = artist.replace(Regex("""\s*-\s*Topic$""", RegexOption.IGNORE_CASE), "").trim()
            val encT = URLEncoder.encode(cleanT, "UTF-8")
            val encA = URLEncoder.encode(cleanA, "UTF-8")
            val url = "https://fast-saavn.vercel.app/?title=$encT&artist=$encA"
            val request = Request.Builder().url(url).build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val path = response.body?.string()?.trim() ?: ""
                    if (path.isNotEmpty() && !path.contains("error", ignoreCase = true)) {
                        return "https://aac.saavncdn.com/${path}_320.mp4"
                    }
                }
            }
        } catch (_: Exception) {}
        return null
    }

    private fun fetchInnerTubePlayerStream(videoId: String): String? {
        try {
            val payload = JsonObject().apply {
                addProperty("videoId", videoId)
                add("context", createAndroidVrContext())
            }

            val request = Request.Builder()
                .url("$YT_BASE/player?prettyPrint=false")
                .header("User-Agent", "com.google.android.apps.youtube.vr.oculus/1.60.19 (Linux; U; Android 12L; eureka-user Build/SQ3A.220605.009.A1) gzip")
                .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val body = response.body?.string() ?: return null
                val root = JsonParser.parseString(body).asJsonObject
                val streamingData = root.getAsJsonObject("streamingData") ?: return null
                val formats = streamingData.getAsJsonArray("adaptiveFormats") ?: return null

                var bestUrl: String? = null
                var maxBitrate = 0
                for (elem in formats) {
                    if (!elem.isJsonObject) continue
                    val obj = elem.asJsonObject
                    val mime = obj.get("mimeType")?.asString ?: ""
                    if (mime.startsWith("audio/")) {
                        val url = obj.get("url")?.asString
                        val bitrate = obj.get("bitrate")?.asInt ?: 0
                        if (!url.isNullOrEmpty() && bitrate > maxBitrate) {
                            maxBitrate = bitrate
                            bestUrl = url
                        }
                    }
                }
                return bestUrl
            }
        } catch (_: Exception) {}
        return null
    }

    private fun parseInnerTubeResults(root: JsonObject): List<MuzoItem> {
        val items = mutableListOf<MuzoItem>()
        try {
            val contents = root.getAsJsonObject("contents")
                ?.getAsJsonObject("tabbedSearchResultsRenderer")
                ?.getAsJsonArray("tabs")?.get(0)?.asJsonObject
                ?.getAsJsonObject("tabRenderer")
                ?.getAsJsonObject("content")
                ?.getAsJsonObject("sectionListRenderer")
                ?.getAsJsonArray("contents") ?: return emptyList()

            for (section in contents) {
                val shelf = section.asJsonObject.getAsJsonObject("musicShelfRenderer") ?: continue
                val shelfContents = shelf.getAsJsonArray("contents") ?: continue

                for (row in shelfContents) {
                    val itemRenderer = row.asJsonObject.getAsJsonObject("musicResponsiveListItemRenderer") ?: continue
                    val flexColumns = itemRenderer.getAsJsonArray("flexColumns") ?: continue
                    if (flexColumns.size() == 0) continue

                    // Title
                    val titleObj = flexColumns[0].asJsonObject
                        .getAsJsonObject("musicResponsiveListItemFlexColumnRenderer")
                        ?.getAsJsonObject("text")
                    val title = titleObj?.getAsJsonArray("runs")?.get(0)?.asJsonObject?.get("text")?.asString ?: "Unknown"

                    // Artist & metadata
                    var artist = "YouTube Music"
                    var durationStr: String? = null
                    if (flexColumns.size() > 1) {
                        val metaObj = flexColumns[1].asJsonObject
                            .getAsJsonObject("musicResponsiveListItemFlexColumnRenderer")
                            ?.getAsJsonObject("text")
                        val runs = metaObj?.getAsJsonArray("runs")
                        if (runs != null && runs.size() > 0) {
                            artist = runs[0].asJsonObject.get("text")?.asString ?: "Artist"
                            if (runs.size() >= 3) {
                                durationStr = runs[runs.size() - 1].asJsonObject.get("text")?.asString
                            }
                        }
                    }

                    // Video ID
                    val playNav = itemRenderer.getAsJsonObject("playlistItemData")
                    val videoId = playNav?.get("videoId")?.asString
                        ?: itemRenderer.getAsJsonArray("menu")?.get(0)?.asJsonObject
                            ?.getAsJsonObject("menuRenderer")?.getAsJsonArray("items")
                            ?.get(0)?.asJsonObject?.getAsJsonObject("menuNavigationItemRenderer")
                            ?.getAsJsonObject("navigationEndpoint")?.getAsJsonObject("watchEndpoint")
                            ?.get("videoId")?.asString

                    // Thumbnail
                    val thumbObj = itemRenderer.getAsJsonObject("thumbnail")
                        ?.getAsJsonObject("musicThumbnailRenderer")
                        ?.getAsJsonObject("thumbnail")
                    val thumbsArray = thumbObj?.getAsJsonArray("thumbnails")
                    val thumbUrl = thumbsArray?.lastOrNull()?.asJsonObject?.get("url")?.asString
                        ?: (if (!videoId.isNullOrEmpty()) "https://i.ytimg.com/vi/$videoId/hqdefault.jpg" else "")

                    if (!videoId.isNullOrEmpty() || title.isNotEmpty()) {
                        items.add(
                            MuzoItem(
                                videoId = videoId,
                                title = title,
                                channelName = artist,
                                artists = listOf(MuzoArtist(name = artist)),
                                thumbnails = if (thumbUrl.isNotEmpty()) listOf(MuzoThumbnail(thumbUrl)) else emptyList(),
                                duration = durationStr,
                                resultType = "song"
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.d(TAG, "Parsing search error: ${e.message}")
        }
        return items
    }

    private fun parseInnerTubeNextResults(root: JsonObject): List<MuzoItem> {
        val items = mutableListOf<MuzoItem>()
        try {
            val tabs = root.getAsJsonObject("contents")
                ?.getAsJsonObject("singleColumnMusicWatchNextResultsRenderer")
                ?.getAsJsonObject("tabbedRenderer")
                ?.getAsJsonObject("watchNextTabbedResultsRenderer")
                ?.getAsJsonArray("tabs") ?: return emptyList()

            for (tab in tabs) {
                val tabRenderer = tab.asJsonObject.getAsJsonObject("tabRenderer") ?: continue
                val queueRenderer = tabRenderer.getAsJsonObject("content")
                    ?.getAsJsonObject("musicQueueRenderer")
                    ?.getAsJsonObject("content")
                    ?.getAsJsonObject("playlistPanelRenderer") ?: continue

                val contents = queueRenderer.getAsJsonArray("contents") ?: continue
                for (panelItem in contents) {
                    val renderer = panelItem.asJsonObject.getAsJsonObject("playlistPanelVideoRenderer") ?: continue
                    val title = renderer.getAsJsonObject("title")?.getAsJsonArray("runs")?.get(0)?.asJsonObject?.get("text")?.asString ?: ""
                    val videoId = renderer.get("videoId")?.asString
                    val artist = renderer.getAsJsonObject("longBylineText")?.getAsJsonArray("runs")?.get(0)?.asJsonObject?.get("text")?.asString ?: "Artist"
                    val lengthText = renderer.getAsJsonObject("lengthText")?.getAsJsonArray("runs")?.get(0)?.asJsonObject?.get("text")?.asString
                    val thumbUrl = renderer.getAsJsonObject("thumbnail")?.getAsJsonArray("thumbnails")?.lastOrNull()?.asJsonObject?.get("url")?.asString
                        ?: (if (!videoId.isNullOrEmpty()) "https://i.ytimg.com/vi/$videoId/hqdefault.jpg" else "")

                    if (!videoId.isNullOrEmpty() && title.isNotEmpty()) {
                        items.add(
                            MuzoItem(
                                videoId = videoId,
                                title = title,
                                channelName = artist,
                                artists = listOf(MuzoArtist(name = artist)),
                                thumbnails = if (thumbUrl.isNotEmpty()) listOf(MuzoThumbnail(thumbUrl)) else emptyList(),
                                duration = lengthText,
                                resultType = "song"
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.d(TAG, "Parsing next queue error: ${e.message}")
        }
        return items
    }
}
