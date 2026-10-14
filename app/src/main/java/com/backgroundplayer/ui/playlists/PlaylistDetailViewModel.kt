package com.backgroundplayer.ui.playlists

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.backgroundplayer.data.LibraryRepository
import com.backgroundplayer.data.db.Track
import com.backgroundplayer.playback.PlaybackController
import com.backgroundplayer.playback.RepeatMode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Backs one playlist's detail screen: its ordered tracks, drag-to-reorder,
 * remove, and play-all with shuffle/repeat. Reordering renumbers every track to
 * its new index so positions stay unique.
 */
class PlaylistDetailViewModel(
    private val repository: LibraryRepository,
    private val playback: PlaybackController,
    private val playlistId: Long,
) : ViewModel() {

    val tracks: StateFlow<List<Track>> =
        repository.playlistTracks(playlistId)
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun reorder(from: Int, to: Int) {
        viewModelScope.launch {
            val current = tracks.value.toMutableList()
            if (from !in current.indices || to !in current.indices) return@launch
            val moved = current.removeAt(from)
            current.add(to, moved)
            current.forEachIndexed { index, track ->
                repository.reorder(playlistId, track.id, index)
            }
        }
    }

    fun remove(trackId: Long) {
        viewModelScope.launch { repository.removeFromPlaylist(playlistId, trackId) }
    }

    /** Plays the whole playlist in order; a no-op when the playlist is empty. */
    fun playAll(shuffle: Boolean, repeat: RepeatMode) {
        val queue = tracks.value
        if (queue.isEmpty()) return
        playback.setShuffle(shuffle)
        playback.setRepeat(repeat)
        playback.play(queue, startIndex = 0)
    }
}
