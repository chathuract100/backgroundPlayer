package com.backgroundplayer.ui.playlists

import com.backgroundplayer.playback.RepeatMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PlaylistsViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val repo = FakePlaylistRepository()

    @Before
    fun setup() = Dispatchers.setMain(dispatcher)

    @After
    fun teardown() = Dispatchers.resetMain()

    @Test
    fun createThenRenameThenDelete() = runTest(dispatcher) {
        val vm = PlaylistsViewModel(repo)
        testScheduler.advanceUntilIdle()

        vm.create("Workout")
        testScheduler.advanceUntilIdle()
        assertEquals(listOf("Workout"), vm.playlists.value.map { it.name })

        val id = vm.playlists.value.first().id
        vm.rename(id, "Gym")
        testScheduler.advanceUntilIdle()
        assertEquals(listOf("Gym"), vm.playlists.value.map { it.name })

        vm.delete(id)
        testScheduler.advanceUntilIdle()
        assertEquals(emptyList<String>(), vm.playlists.value.map { it.name })
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class PlaylistDetailViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val repo = FakePlaylistRepository()
    private val playback = RecordingPlaybackController()

    @Before
    fun setup() = Dispatchers.setMain(dispatcher)

    @After
    fun teardown() = Dispatchers.resetMain()

    private fun vmFor(playlistId: Long) = PlaylistDetailViewModel(repo, playback, playlistId)

    @Test
    fun reorderPersistsNewOrder() = runTest(dispatcher) {
        val pid = repo.seed("list", listOf("A", "B", "C"))
        val vm = vmFor(pid)
        testScheduler.advanceUntilIdle()

        vm.reorder(from = 0, to = 2)
        testScheduler.advanceUntilIdle()

        assertEquals(listOf("B", "C", "A"), vm.tracks.value.map { it.title })
    }

    @Test
    fun playAllBuildsQueueInOrder() = runTest(dispatcher) {
        val pid = repo.seed("list", listOf("A", "B", "C"))
        val vm = vmFor(pid)
        testScheduler.advanceUntilIdle()

        vm.playAll(shuffle = false, repeat = RepeatMode.OFF)

        assertEquals(listOf("A", "B", "C"), playback.lastTracks?.map { it.title })
        assertEquals(0, playback.lastStartIndex)
    }

    @Test
    fun playAllOnEmptyPlaylistIsNoOp() = runTest(dispatcher) {
        val pid = repo.seed("empty", emptyList())
        val vm = vmFor(pid)
        testScheduler.advanceUntilIdle()

        vm.playAll(shuffle = false, repeat = RepeatMode.OFF)

        assertEquals(null, playback.lastTracks)
    }

    @Test
    fun playAllWithShuffleEnablesShuffle() = runTest(dispatcher) {
        val pid = repo.seed("list", listOf("A", "B"))
        val vm = vmFor(pid)
        testScheduler.advanceUntilIdle()

        vm.playAll(shuffle = true, repeat = RepeatMode.ALL)

        assertEquals(true, playback.lastShuffle)
        assertEquals(RepeatMode.ALL, playback.lastRepeat)
    }
}
