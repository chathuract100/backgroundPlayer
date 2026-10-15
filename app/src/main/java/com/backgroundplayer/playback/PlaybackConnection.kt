package com.backgroundplayer.playback

import android.content.ComponentName
import android.content.Context
import androidx.core.content.ContextCompat
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.backgroundplayer.data.LibraryRepository
import com.backgroundplayer.data.db.Track
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * UI-side [PlaybackController] implemented over a Media3 [MediaController] bound
 * to [PlaybackService]. Commands are forwarded to the controller once it has
 * connected; player events are projected to [PlaybackState] through the shared
 * [PlaybackStateReducer]. ViewModels depend on the [PlaybackController]
 * interface, never on this class.
 */
class PlaybackConnection(
    context: Context,
    private val repository: LibraryRepository,
) : PlaybackController {

    private val _state = MutableStateFlow(PlaybackState.EMPTY)
    override val state: StateFlow<PlaybackState> = _state.asStateFlow()

    private var controller: MediaController? = null
    private var queue: List<Track> = emptyList()

    /** The bound controller as a [Player], for attaching to a video surface. */
    val mediaPlayer: Player?
        get() = controller

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            rehydrateQueueIfNeeded()
            updateState()
        }
    }

    private val scope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())

    init {
        val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val future = MediaController.Builder(context, token).buildAsync()
        future.addListener(
            {
                controller = future.get().also { it.addListener(listener) }
                rehydrateQueueIfNeeded()
                updateState()
            },
            ContextCompat.getMainExecutor(context),
        )
        // ExoPlayer emits no periodic position event, so poll while playing to
        // keep the scrubber/position moving between discrete events.
        scope.launch {
            while (isActive) {
                delay(POSITION_POLL_MS)
                if (controller?.isPlaying == true) updateState()
            }
        }
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
        scope.cancel()
        controller?.removeListener(listener)
        controller?.release()
        controller = null
    }

    /**
     * After a cold reconnect (e.g. the app was reopened while the service kept
     * playing) the local [queue] is empty, so the current track can't be mapped.
     * Rebuild it from the session's own media items — each carries its track id
     * in [androidx.media3.common.MediaItem.mediaId] — via a repository lookup.
     */
    private fun rehydrateQueueIfNeeded() {
        val c = controller ?: return
        if (queue.isNotEmpty() || c.mediaItemCount == 0) return
        val ids = (0 until c.mediaItemCount).map { TrackMediaItem.trackId(c.getMediaItemAt(it)) }
        scope.launch {
            val rebuilt = ids.map { id -> repository.findById(id) }
            if (rebuilt.all { it != null }) {
                queue = rebuilt.filterNotNull()
                updateState()
            }
        }
    }

    private fun updateState() {
        val c = controller ?: return
        _state.value = PlaybackStateReducer.reduce(c, queue)
    }

    private companion object {
        const val POSITION_POLL_MS = 500L
    }
}
