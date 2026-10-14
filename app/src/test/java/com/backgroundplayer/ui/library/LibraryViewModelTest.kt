package com.backgroundplayer.ui.library

import android.net.Uri
import com.backgroundplayer.data.LibraryRepository
import com.backgroundplayer.data.MediaFilter
import com.backgroundplayer.data.db.Playlist
import com.backgroundplayer.data.db.Track
import com.backgroundplayer.data.mediaimport.MediaImporter
import com.backgroundplayer.playback.PlaybackController
import com.backgroundplayer.playback.PlaybackState
import com.backgroundplayer.playback.RepeatMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LibraryViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val repo = FakeLibraryRepository()
    private val playback = FakePlaybackController()
    private val importer = FakeMediaImporter()

    private fun viewModel() = LibraryViewModel(repo, playback, importer)

    @Before
    fun setup() = Dispatchers.setMain(dispatcher)

    @After
    fun teardown() = Dispatchers.resetMain()

    private fun track(id: Long, title: String, artist: String? = null, isVideo: Boolean = false) =
        Track(id = id, uri = "content://$id", title = title, artist = artist, isVideo = isVideo)

    @Test
    fun setQueryFiltersList() = runTest(dispatcher) {
        repo.tracks.value = listOf(track(1, "Bohemian Rhapsody"), track(2, "Imagine"))
        val vm = viewModel()
        testScheduler.advanceUntilIdle()

        vm.setQuery("imagine")
        testScheduler.advanceUntilIdle()

        assertEquals(listOf("Imagine"), vm.uiState.value.tracks.map { it.title })
    }

    @Test
    fun setFilterToVideoShowsOnlyVideo() = runTest(dispatcher) {
        repo.tracks.value = listOf(track(1, "A", isVideo = false), track(2, "V", isVideo = true))
        val vm = viewModel()
        testScheduler.advanceUntilIdle()

        vm.setFilter(MediaFilter.VIDEO)
        testScheduler.advanceUntilIdle()

        assertEquals(listOf("V"), vm.uiState.value.tracks.map { it.title })
    }

    @Test
    fun emptyLibraryReportsIsEmpty() = runTest(dispatcher) {
        val vm = viewModel()
        testScheduler.advanceUntilIdle()

        assertTrue(vm.uiState.value.tracks.isEmpty())
        assertTrue(vm.uiState.value.isEmpty)
    }

    @Test
    fun playTrackStartsAtSelectedIndex() = runTest(dispatcher) {
        repo.tracks.value = listOf(track(1, "A"), track(2, "B"), track(3, "C"))
        val vm = viewModel()
        testScheduler.advanceUntilIdle()

        vm.playTrack(track(2, "B"))

        assertEquals(listOf("A", "B", "C"), playback.lastTracks?.map { it.title })
        assertEquals(1, playback.lastStartIndex)
        assertFalse(vm.uiState.value.isEmpty)
    }
}

private class FakeLibraryRepository : LibraryRepository {
    val tracks = MutableStateFlow<List<Track>>(emptyList())

    override fun library(filter: MediaFilter, query: String): Flow<List<Track>> =
        tracks.map { list ->
            list.filter {
                when (filter) {
                    MediaFilter.ALL -> true
                    MediaFilter.AUDIO -> !it.isVideo
                    MediaFilter.VIDEO -> it.isVideo
                }
            }.filter { t ->
                query.isBlank() ||
                    t.title.contains(query, ignoreCase = true) ||
                    t.artist?.contains(query, ignoreCase = true) == true
            }
        }

    override suspend fun addTrack(track: Track): Long = track.id
    override suspend fun markUnavailable(trackId: Long) = Unit
    override suspend fun findById(id: Long): Track? = tracks.value.firstOrNull { it.id == id }
    override suspend fun createPlaylist(name: String): Long = 0
    override suspend fun rename(id: Long, name: String) = Unit
    override suspend fun delete(id: Long) = Unit
    override fun playlists(): Flow<List<Playlist>> = MutableStateFlow(emptyList())
    override fun playlistTracks(id: Long): Flow<List<Track>> = MutableStateFlow(emptyList())
    override suspend fun addToPlaylist(playlistId: Long, trackId: Long) = Unit
    override suspend fun reorder(playlistId: Long, trackId: Long, newPosition: Int) = Unit
    override suspend fun removeFromPlaylist(playlistId: Long, trackId: Long) = Unit
}

private class FakePlaybackController : PlaybackController {
    override val state: StateFlow<PlaybackState> = MutableStateFlow(PlaybackState.EMPTY)
    var lastTracks: List<Track>? = null
    var lastStartIndex: Int = -1

    override fun play(tracks: List<Track>, startIndex: Int) {
        lastTracks = tracks
        lastStartIndex = startIndex
    }

    override fun pause() = Unit
    override fun resume() = Unit
    override fun next() = Unit
    override fun previous() = Unit
    override fun seekTo(ms: Long) = Unit
    override fun setShuffle(shuffle: Boolean) = Unit
    override fun setRepeat(mode: RepeatMode) = Unit
    override fun release() = Unit
}

private class FakeMediaImporter : MediaImporter {
    override suspend fun scan(): Int = 0
    override suspend fun import(uris: List<Uri>): Int = 0
}
