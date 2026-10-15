package com.backgroundplayer.data.mediaimport

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TrackMapperTest {

    private fun raw(
        mimeType: String? = "audio/mpeg",
        displayName: String = "file.mp3",
        title: String? = "Title",
        durationMs: Long? = 1000L,
    ) = RawMedia(
        uri = "content://x",
        mimeType = mimeType,
        displayName = displayName,
        title = title,
        artist = null,
        album = null,
        durationMs = durationMs,
        thumbnailUri = null,
    )

    @Test
    fun videoMimeMarksIsVideo() {
        assertTrue(TrackMapper.toTrack(raw(mimeType = "video/mp4")).isVideo)
    }

    @Test
    fun audioMimeIsNotVideo() {
        assertFalse(TrackMapper.toTrack(raw(mimeType = "audio/mpeg")).isVideo)
    }

    @Test
    fun missingTitleFallsBackToDisplayName() {
        assertEquals("song.mp3", TrackMapper.toTrack(raw(title = null, displayName = "song.mp3")).title)
        assertEquals("song.mp3", TrackMapper.toTrack(raw(title = "   ", displayName = "song.mp3")).title)
    }

    @Test
    fun nullDurationBecomesZero() {
        assertEquals(0L, TrackMapper.toTrack(raw(durationMs = null)).durationMs)
    }
}
