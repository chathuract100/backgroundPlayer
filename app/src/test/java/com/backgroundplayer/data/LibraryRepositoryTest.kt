package com.backgroundplayer.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.backgroundplayer.data.db.AppDatabase
import com.backgroundplayer.data.db.Track
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LibraryRepositoryTest {

    private lateinit var db: AppDatabase
    private lateinit var repo: LibraryRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repo = LibraryRepository(db.trackDao(), db.playlistDao())
    }

    @After
    fun teardown() = db.close()

    private fun track(
        uri: String,
        title: String = "t",
        artist: String? = null,
        isVideo: Boolean = false,
    ) = Track(uri = uri, title = title, artist = artist, isVideo = isVideo)

    @Test
    fun searchFiltersByTitleAndArtist() = runTest {
        repo.addTrack(track("content://1", title = "Bohemian Rhapsody", artist = "Queen"))
        repo.addTrack(track("content://2", title = "Imagine", artist = "Lennon"))

        val byTitle = repo.library(MediaFilter.ALL, "bohemian").first()
        assertEquals(listOf("Bohemian Rhapsody"), byTitle.map { it.title })

        val byArtist = repo.library(MediaFilter.ALL, "queen").first()
        assertEquals(listOf("Bohemian Rhapsody"), byArtist.map { it.title })
    }

    @Test
    fun videoFilterExcludesAudio() = runTest {
        repo.addTrack(track("content://audio", title = "A", isVideo = false))
        repo.addTrack(track("content://video", title = "V", isVideo = true))

        val videos = repo.library(MediaFilter.VIDEO, "").first()
        assertEquals(listOf("V"), videos.map { it.title })
    }

    @Test
    fun addDuplicateUriDoesNotCreateSecondRow() = runTest {
        val first = repo.addTrack(track("content://dup", title = "First"))
        val second = repo.addTrack(track("content://dup", title = "Second"))

        assertEquals(first, second)
        assertEquals(1, repo.library(MediaFilter.ALL, "").first().size)
    }

    @Test
    fun markUnavailableHidesOrFlagsTrack() = runTest {
        val id = repo.addTrack(track("content://gone", title = "Gone"))
        repo.markUnavailable(id)

        val visible = repo.library(MediaFilter.ALL, "").first()
        assertEquals(emptyList<String>(), visible.map { it.title })
    }

    @Test
    fun addToPlaylistAppendsAtEnd() = runTest {
        val a = repo.addTrack(track("content://a", title = "A"))
        val b = repo.addTrack(track("content://b", title = "B"))
        val pid = repo.createPlaylist("list")
        repo.addToPlaylist(pid, a)
        repo.addToPlaylist(pid, b)

        val ordered = repo.playlistTracks(pid).first()
        assertEquals(listOf("A", "B"), ordered.map { it.title })
    }

    @Test
    fun reorderMovesTrack() = runTest {
        val a = repo.addTrack(track("content://a", title = "A"))
        val b = repo.addTrack(track("content://b", title = "B"))
        val pid = repo.createPlaylist("list")
        repo.addToPlaylist(pid, a)
        repo.addToPlaylist(pid, b)

        // Move A to the end so order becomes B, A.
        repo.reorder(pid, a, 2)

        val ordered = repo.playlistTracks(pid).first()
        assertEquals(listOf("B", "A"), ordered.map { it.title })
    }
}
