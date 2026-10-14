package com.backgroundplayer.playback

enum class ErrorAction { SKIP_TO_NEXT, STOP }

/**
 * Decides what to do when a track fails to play (corrupt/unplayable file):
 * skip to the next track when one exists, otherwise stop. Keeps playback from
 * crashing on a bad file.
 */
object PlaybackErrorPolicy {
    fun onError(hasNext: Boolean): ErrorAction =
        if (hasNext) ErrorAction.SKIP_TO_NEXT else ErrorAction.STOP
}
