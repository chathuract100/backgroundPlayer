package com.backgroundplayer.playback

import android.content.ComponentName
import android.content.Context
import androidx.core.content.ContextCompat
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.backgroundplayer.data.db.Track
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * UI-side [PlaybackController] implemented over a Media3 [MediaController] bound
 * to [PlaybackService]. Commands are forwarded to the controller once it has
 * connected; player events are projected to [PlaybackState] through the shared
 * [PlaybackStateReducer]. ViewModels depend on the [PlaybackController]
 * interface, never on this class.
 */
class PlaybackConnection(context: Context) : PlaybackController {

    private val _state = MutableStateFlow(PlaybackState.EMPTY)
    override val state: StateFlow<PlaybackState> = _state.asStateFlow()

    private var controller: MediaController? = null
    private var queue: List<Track> = emptyList()

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) = updateState()
    }

    init {
        val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val future = MediaController.Builder(context, token).buildAsync()
        future.addListener(
            {
                controller = future.get().also { it.addListener(listener) }
                updateState()
            },
            ContextCompat.getMainExecutor(context),
        )
    }

    override fun play(tracks: List<Track>, startIndex: Int) {
        queue = tracks
        val c = controller ?: return
        c.setMediaItems(tracks.map(TrackMediaItem::from), startIndex, 0L)
        c.prepare()
        c.play()
    }

    override fun pause() {
        controller?.pause()
    }

    override fun resume() {
        controller?.play()
    }

    override fun next() {
        controller?.seekToNextMediaItem()
    }

    override fun previous() {
        controller?.seekToPreviousMediaItem()
    }

    override fun seekTo(ms: Long) {
        controller?.seekTo(ms)
    }

    override fun setShuffle(shuffle: Boolean) {
        controller?.shuffleModeEnabled = shuffle
    }

    override fun setRepeat(mode: RepeatMode) {
        controller?.repeatMode = mode.toPlayerRepeatMode()
    }

    override fun release() {
        controller?.removeListener(listener)
        controller?.release()
        controller = null
    }

    private fun updateState() {
        val c = controller ?: return
        _state.value = PlaybackStateReducer.reduce(c, queue)
    }
}
