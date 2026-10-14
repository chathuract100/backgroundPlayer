package com.backgroundplayer.playback

import org.junit.Assert.assertEquals
import org.junit.Test

class PlaybackErrorPolicyTest {

    @Test
    fun skipsWhenNextExists() {
        assertEquals(ErrorAction.SKIP_TO_NEXT, PlaybackErrorPolicy.onError(hasNext = true))
    }

    @Test
    fun stopsWhenNoNext() {
        assertEquals(ErrorAction.STOP, PlaybackErrorPolicy.onError(hasNext = false))
    }
}
