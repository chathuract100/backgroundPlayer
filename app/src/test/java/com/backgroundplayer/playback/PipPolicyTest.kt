package com.backgroundplayer.playback

import com.backgroundplayer.data.db.Track
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PipPolicyTest {

    private fun state(isVideo: Boolean, isPlaying: Boolean) = PlaybackState(
        currentTrack = Track(id = 1, uri = "content://1", title = "T", isVideo = isVideo),
        isPlaying = isPlaying,
        isVideo = isVideo,
    )

    @Test
    fun entersPipForPlayingVideo() {
        assertTrue(PipPolicy.shouldEnterPip(state(isVideo = true, isPlaying = true)))
    }

    @Test
    fun noPipForAudio() {
        assertFalse(PipPolicy.shouldEnterPip(state(isVideo = false, isPlaying = true)))
    }

    @Test
    fun noPipWhenPaused() {
        assertFalse(PipPolicy.shouldEnterPip(state(isVideo = true, isPlaying = false)))
    }
}
