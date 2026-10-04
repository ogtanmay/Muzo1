package com.shashwat.muzo.data.api

import com.shashwat.muzo.data.model.MuzoItem

/**
 * Direct audio stream extractor using client-side InnerTube and Saavn APIs.
 * Requires NO external hosted servers or middleware!
 */
class StreamExtractor {
    private val innerTubeClient = InnerTubeClient()

    suspend fun getStreamUrl(item: MuzoItem): String? {
        if (!item.audioUrl.isNullOrEmpty()) {
            return item.audioUrl
        }
        return innerTubeClient.getStreamUrl(item)
    }
}
