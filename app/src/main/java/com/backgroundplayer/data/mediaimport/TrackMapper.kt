package com.backgroundplayer.data.mediaimport

import com.backgroundplayer.data.db.Track

/**
 * Pure mapping from read metadata to a persistable [Track]. The caller stamps
 * [Track.dateAdded]; this object only applies the field rules:
 * - `isVideo` is true when the MIME type starts with `video/`.
 * - a null/blank `title` falls back to the file's display name.
 * - a missing duration becomes 0.
 */
object TrackMapper {
    fun toTrack(raw: RawMedia): Track = Track(
        uri = raw.uri,
        title = raw.title?.takeIf { it.isNotBlank() } ?: raw.displayName,
        artist = raw.artist,
        album = raw.album,
        durationMs = raw.durationMs ?: 0L,
        isVideo = raw.mimeType?.startsWith("video/") == true,
        thumbnailUri = raw.thumbnailUri,
    )
}
