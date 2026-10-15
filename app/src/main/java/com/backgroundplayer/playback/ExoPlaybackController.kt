package com.backgroundplayer.playback

import androidx.media3.common.Player
import com.backgroundplayer.data.db.Track
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Drives a Media3 [Player] and projects its events into a [PlaybackState]
 * stream. The queue of [Track]s is kept alongside the player so the current
 * media item can be mapped back to its originating track.
 */
class ExoPlaybackController(private val player: Player) : PlaybackController {

    private val _state = MutableStateFlow(PlaybackState.EMPTY)
    override val state: StateFlow<PlaybackState> = _state.asStateFlow()

    private var queue: List<Track> = emptyList()

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) = updateState()
    }

    init {
        player.addListener(listener)
        updateState()
    }

    override fun play(tracks: List<Track>, startIndex: Int) {
        queue = tracks
        player.setMediaItems(tracks.map(TrackMediaItem::from), startIndex, 0L)
        player.prepare()
        player.play()
        updateState()
    }

    override fun pause() = player.pause()

    override fun resume() = player.play()

    override fun next() = player.seekToNextMediaItem()

    override fun previous() = player.seekToPreviousMediaItem()

    override fun seekTo(ms: Long) = player.seekTo(ms)

    override fun setShuffle(shuffle: Boolean) {
        player.shuffleModeEnabled = shuffle
        updateState()
    }

    override fun setRepeat(mode: RepeatMode) {
        player.repeatMode = mode.toPlayerRepeatMode()
        updateState()
    }

    override fun release() {
        player.removeListener(listener)
        player.release()
    }

    private fun updateState() {
        _state.value = PlaybackStateReducer.reduce(player, queue)
    }
}
