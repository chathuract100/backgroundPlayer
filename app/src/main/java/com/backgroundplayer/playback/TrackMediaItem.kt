package com.backgroundplayer.playback

import androidx.media3.common.MediaItem
import com.backgroundplayer.data.db.Track

/**
 * Converts between a library [Track] and a Media3 [MediaItem], carrying the
 * track's id in [MediaItem.mediaId] so playback events can be mapped back to
 * the originating track.
 */
object TrackMediaItem {
    fun from(track: Track): MediaItem =
        MediaItem.Builder()
            .setMediaId(track.id.toString())
            .setUri(track.uri)
            .build()

    fun trackId(item: MediaItem): Long = item.mediaId.toLongOrNull() ?: -1L
}
