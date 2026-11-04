package com.backgroundplayer.playback

import com.backgroundplayer.data.db.Track
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OverlayPolicyTest {

    private fun state(track: Track? = null, isVideo: Boolean = false) = PlaybackState(
        currentTrack = track,
        isPlaying = true,
        isVideo = isVideo,
    )

    private val audio = Track(id = 1, uri = "content://1", title = "T", isVideo = false)
    private val video = Track(id = 2, uri = "content://2", title = "V", isVideo = true)

    @Test
    fun showsOverlayForBackgroundedAudioWithPermission() {
        assertTrue(OverlayPolicy.shouldShow(state(track = audio), hasPermission = true))
    }

    @Test
    fun noOverlayForVideo() {
        // Video minimizes to Picture-in-Picture instead.
        assertFalse(OverlayPolicy.shouldShow(state(track = video, isVideo = true), hasPermission = true))
    }

    @Test
    fun noOverlayWithoutPermission() {
        assertFalse(OverlayPolicy.shouldShow(state(track = audio), hasPermission = false))
    }

    @Test
    fun noOverlayWhenNothingLoaded() {
        assertFalse(OverlayPolicy.shouldShow(state(track = null), hasPermission = true))
    }
}
