package com.backgroundplayer.playback

import androidx.media3.common.PlaybackException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
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

    @Test
    fun fileNotFoundIsUnavailable() {
        assertTrue(PlaybackErrorPolicy.isFileUnavailable(PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND))
    }

    @Test
    fun noReadPermissionIsUnavailable() {
        assertTrue(PlaybackErrorPolicy.isFileUnavailable(PlaybackException.ERROR_CODE_IO_NO_PERMISSION))
    }

    @Test
    fun decodingErrorIsNotUnavailable() {
        assertFalse(PlaybackErrorPolicy.isFileUnavailable(PlaybackException.ERROR_CODE_DECODING_FAILED))
    }
}
