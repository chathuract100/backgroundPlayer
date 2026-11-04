package com.backgroundplayer.playback

/**
 * Decides when minimizing the app should show the floating overlay tile: only
 * for audio (video uses Picture-in-Picture instead) when a track is loaded and
 * the "display over other apps" permission has been granted.
 */
object OverlayPolicy {
    fun shouldShow(state: PlaybackState, hasPermission: Boolean): Boolean =
        hasPermission && state.currentTrack != null && !state.isVideo
}
