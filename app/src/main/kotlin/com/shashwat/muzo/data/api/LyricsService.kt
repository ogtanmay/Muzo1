package com.shashwat.muzo.data.api

import android.util.Log
import com.google.gson.JsonParser
import com.shashwat.muzo.data.model.Lyrics
import com.shashwat.muzo.data.model.SyncedLine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

class LyricsService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    companion object {
        private const val TAG = "LyricsService"
    }

    suspend fun getLyrics(trackName: String, artistName: String, durationSeconds: Int?): Lyrics? = withContext(Dispatchers.IO) {
        val cleanTrack = cleanTitle(trackName)
        val cleanArtist = cleanTitle(artistName)
        val dur = durationSeconds ?: 0

        // 1. Try Atomix lyrics API
        try {
            val encodedTrack = URLEncoder.encode(cleanTrack, "UTF-8")
            val encodedArtist = URLEncoder.encode(cleanArtist, "UTF-8")
            val url = "https://lyrics.geeked.wtf/v2/lyrics/get?title=$encodedTrack&artist=$encodedArtist&duration=$dur"

            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: ""
                    val root = JsonParser.parseString(body).asJsonObject
                    val lyricsArray = root.getAsJsonArray("lyrics")
                    if (lyricsArray != null && lyricsArray.size() > 0) {
                        val syncedLines = mutableListOf<SyncedLine>()
                        val plainLines = StringBuilder()

                        for (elem in lyricsArray) {
                            if (!elem.isJsonObject) continue
                            val obj = elem.asJsonObject
                            val timeMs = obj.get("time")?.asLong ?: 0L
                            val text = obj.get("text")?.asString?.trim() ?: ""
                            if (text.isNotEmpty()) {
                                syncedLines.add(SyncedLine(timeMs, text))
                                plainLines.appendLine(text)
                            }
                        }

                        if (syncedLines.isNotEmpty()) {
                            return@withContext Lyrics(
                                trackName = cleanTrack,
                                artistName = cleanArtist,
                                plainLyrics = plainLines.toString(),
                                syncedLines = syncedLines
                            )
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.d(TAG, "Atomix lyrics error: ${e.message}")
        }

        // 2. Try LRCLIB exact match
        try {
            val encodedTrack = URLEncoder.encode(cleanTrack, "UTF-8")
            val encodedArtist = URLEncoder.encode(cleanArtist, "UTF-8")
            val url = "https://lrclib.net/api/get?track_name=$encodedTrack&artist_name=$encodedArtist"

            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: ""
                    val root = JsonParser.parseString(body).asJsonObject
                    val syncedText = root.get("syncedLyrics")?.asString
                    val plainText = root.get("plainLyrics")?.asString ?: ""

                    val lines = if (!syncedText.isNullOrEmpty()) parseLrc(syncedText) else emptyList()
                    if (lines.isNotEmpty() || plainText.isNotEmpty()) {
                        return@withContext Lyrics(
                            trackName = cleanTrack,
                            artistName = cleanArtist,
                            plainLyrics = plainText.ifEmpty { lines.joinToString("\n") { it.text } },
                            syncedLines = lines
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.d(TAG, "LRCLIB exact error: ${e.message}")
        }

        // 3. Fallback: search LRCLIB
        try {
            val encodedTrack = URLEncoder.encode(cleanTrack, "UTF-8")
            val encodedArtist = URLEncoder.encode(cleanArtist, "UTF-8")
            val url = "https://lrclib.net/api/search?track_name=$encodedTrack&artist_name=$encodedArtist"
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: ""
                    val array = JsonParser.parseString(body).asJsonArray
                    if (array.size() > 0) {
                        val first = array[0].asJsonObject
                        val syncedText = first.get("syncedLyrics")?.asString
                        val plainText = first.get("plainLyrics")?.asString ?: ""
                        val lines = if (!syncedText.isNullOrEmpty()) parseLrc(syncedText) else emptyList()
                        if (lines.isNotEmpty() || plainText.isNotEmpty()) {
                            return@withContext Lyrics(
                                trackName = cleanTrack,
                                artistName = cleanArtist,
                                plainLyrics = plainText.ifEmpty { lines.joinToString("\n") { it.text } },
                                syncedLines = lines
                            )
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.d(TAG, "LRCLIB search error: ${e.message}")
        }

        null
    }

    private fun parseLrc(lrcContent: String): List<SyncedLine> {
        val lines = mutableListOf<SyncedLine>()
        val regex = Regex("""\[(\d{2}):(\d{2})\.(\d{2,3})\](.*)""")
        for (rawLine in lrcContent.lines()) {
            val match = regex.find(rawLine)
            if (match != null) {
                val min = match.groupValues[1].toLongOrNull() ?: 0L
                val sec = match.groupValues[2].toLongOrNull() ?: 0L
                val frac = match.groupValues[3]
                val millis = if (frac.length == 2) (frac.toLongOrNull() ?: 0L) * 10 else (frac.toLongOrNull() ?: 0L)
                val totalMs = min * 60000 + sec * 1000 + millis
                val text = match.groupValues[4].trim()
                if (text.isNotEmpty()) {
                    lines.add(SyncedLine(totalMs, text))
                }
            }
        }
        return lines.sortedBy { it.timestampMs }
    }

    private fun cleanTitle(text: String): String {
        var clean = text
        clean = clean.replace(Regex("""\s*[(\[][^\])]*(?:official|video|audio|lyrics|lyric|hd|hq|4k|mv|music video|full audio)[\])]""", RegexOption.IGNORE_CASE), "")
        clean = clean.replace(Regex("""\s+(ft\.|feat\.|featuring)\s+.*""", RegexOption.IGNORE_CASE), "")
        clean = clean.replace(" - Topic", "", ignoreCase = true)
        return clean.trim()
    }
}
