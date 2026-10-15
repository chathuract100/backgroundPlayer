package com.backgroundplayer.playback

import androidx.media3.common.PlaybackException

enum class ErrorAction { SKIP_TO_NEXT, STOP }

/**
 * Decides what to do when a track fails to play (corrupt/unplayable file):
 * skip to the next track when one exists, otherwise stop. Keeps playback from
 * crashing on a bad file.
 */
object PlaybackErrorPolicy {
    fun onError(hasNext: Boolean): ErrorAction =
        if (hasNext) ErrorAction.SKIP_TO_NEXT else ErrorAction.STOP

    /**
     * Whether [errorCode] means the underlying file is gone or unreadable (moved,
     * deleted, or permission revoked), so the track should be marked unavailable
     * rather than left as a dead library entry.
     */
    fun isFileUnavailable(errorCode: Int): Boolean =
        errorCode == PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND ||
            errorCode == PlaybackException.ERROR_CODE_IO_NO_PERMISSION
}
