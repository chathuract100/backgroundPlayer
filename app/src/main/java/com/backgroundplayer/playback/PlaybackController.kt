package com.backgroundplayer.playback

import com.backgroundplayer.data.db.Track
import kotlinx.coroutines.flow.StateFlow

enum class RepeatMode { OFF, ONE, ALL }

/**
 * A snapshot of what playback is doing, observed by the UI. [EMPTY] is the
 * idle default (nothing loaded).
 */
data class PlaybackState(
    val currentTrack: Track? = null,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0,
    val durationMs: Long = 0,
    val isVideo: Boolean = false,
    val shuffle: Boolean = false,
    val repeat: RepeatMode = RepeatMode.OFF,
) {
    companion object {
        val EMPTY = PlaybackState()
    }
}

/**
 * The only surface the UI talks to for playback. Implemented over ExoPlayer in
 * the service process ([ExoPlaybackController]) and over a bound MediaController
 * in the UI process; ViewModels depend on this interface so they test with a fake.
 */
interface PlaybackController {
    val state: StateFlow<PlaybackState>
    fun play(tracks: List<Track>, startIndex: Int)
    fun pause()
    fun resume()
    fun next()
    fun previous()
    fun seekTo(ms: Long)
    fun setShuffle(shuffle: Boolean)
    fun setRepeat(mode: RepeatMode)
    fun release()
}
