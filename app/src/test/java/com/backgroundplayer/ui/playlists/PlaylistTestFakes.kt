package com.backgroundplayer.ui.playlists

import android.net.Uri
import com.backgroundplayer.data.LibraryRepository
import com.backgroundplayer.data.MediaFilter
import com.backgroundplayer.data.db.Playlist
import com.backgroundplayer.data.db.Track
import com.backgroundplayer.playback.PlaybackController
import com.backgroundplayer.playback.PlaybackState
import com.backgroundplayer.playback.RepeatMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * In-memory [LibraryRepository] for playlist ViewModel tests. Tracks are kept
 * per playlist with an explicit position map; [reorder] sets a track's absolute
 * position and reads emit sorted by position (matching the Room DAO contract).
 */
class FakePlaylistRepository : LibraryRepository {

    private val playlistsFlow = MutableStateFlow<List<Playlist>>(emptyList())
    private val positions = mutableMapOf<Long, MutableMap<Long, Int>>()
    private val data = mutableMapOf<Long, MutableMap<Long, Track>>()
    private val flows = mutableMapOf<Long, MutableStateFlow<List<Track>>>()
    private var nextId = 1L

    private fun flow(pid: Long) = flows.getOrPut(pid) { MutableStateFlow(emptyList()) }

    private fun recompute(pid: Long) {
        val order = positions[pid] ?: emptyMap()
        val tracks = data[pid] ?: emptyMap()
        flow(pid).value = order.entries.sortedBy { it.value }.mapNotNull { tracks[it.key] }
    }

    /** Seeds a playlist with freshly-created tracks and returns the playlist id. */
    fun seed(name: String, titles: List<String>): Long {
        val pid = nextId++
        playlistsFlow.value = playlistsFlow.value + Playlist(id = pid, name = name, createdAt = pid)
        val tracks = titles.mapIndexed { i, title ->
            Track(id = (pid * 100 + i), uri = "content://$pid/$i", title = title)
        }
        val posMap = positions.getOrPut(pid) { mutableMapOf() }
        val dataMap = data.getOrPut(pid) { mutableMapOf() }
        tracks.forEachIndexed { i, t ->
            dataMap[t.id] = t
            posMap[t.id] = i
        }
        recompute(pid)
        return pid
    }

    override suspend fun createPlaylist(name: String): Long {
        val id = nextId++
        playlistsFlow.value = playlistsFlow.value + Playlist(id = id, name = name, createdAt = id)
        return id
    }

    override suspend fun rename(id: Long, name: String) {
        playlistsFlow.value = playlistsFlow.value.map { if (it.id == id) it.copy(name = name) else it }
    }

    override suspend fun delete(id: Long) {
        playlistsFlow.value = playlistsFlow.value.filterNot { it.id == id }
    }

    override fun playlists(): Flow<List<Playlist>> = playlistsFlow

    override fun playlistTracks(id: Long): Flow<List<Track>> = flow(id)

    override suspend fun reorder(playlistId: Long, trackId: Long, newPosition: Int) {
        positions.getOrPut(playlistId) { mutableMapOf() }[trackId] = newPosition
        recompute(playlistId)
    }

    override suspend fun removeFromPlaylist(playlistId: Long, trackId: Long) {
        positions[playlistId]?.remove(trackId)
        data[playlistId]?.remove(trackId)
        recompute(playlistId)
    }

    override suspend fun addToPlaylist(playlistId: Long, trackId: Long) = Unit

    // Unused by playlist ViewModels.
    override fun library(filter: MediaFilter, query: String): Flow<List<Track>> =
        MutableStateFlow(emptyList())
    override suspend fun addTrack(track: Track): Long = track.id
    override suspend fun markUnavailable(trackId: Long) = Unit
}

/** Records the last playback commands for assertions. */
class RecordingPlaybackController : PlaybackController {
    override val state: StateFlow<PlaybackState> = MutableStateFlow(PlaybackState.EMPTY)
    var lastTracks: List<Track>? = null
    var lastStartIndex: Int = -1
    var lastShuffle: Boolean? = null
    var lastRepeat: RepeatMode? = null

    override fun play(tracks: List<Track>, startIndex: Int) {
        lastTracks = tracks
        lastStartIndex = startIndex
    }

    override fun setShuffle(shuffle: Boolean) {
        lastShuffle = shuffle
    }

    override fun setRepeat(mode: RepeatMode) {
        lastRepeat = mode
    }

    override fun pause() = Unit
    override fun resume() = Unit
    override fun next() = Unit
    override fun previous() = Unit
    override fun seekTo(ms: Long) = Unit
    override fun release() = Unit
}
