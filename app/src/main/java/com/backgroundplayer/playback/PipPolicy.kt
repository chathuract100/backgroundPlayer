package com.backgroundplayer.playback

/**
 * Decides when minimizing the app should enter Picture-in-Picture: only for a
 * video that is currently playing. Audio-only playback and paused video do not.
 */
object PipPolicy {
    fun shouldEnterPip(state: PlaybackState): Boolean = state.isVideo && state.isPlaying
}
