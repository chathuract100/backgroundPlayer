package com.backgroundplayer.data.db

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PlaylistDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var tracks: TrackDao
    private lateinit var playlists: PlaylistDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        tracks = db.trackDao()
        playlists = db.playlistDao()
    }

    @After
    fun teardown() = db.close()

    @Test
    fun addTracksObservedInPositionOrder() = runTest {
        val t1 = tracks.insert(Track(uri = "content://1", title = "One"))
        val t2 = tracks.insert(Track(uri = "content://2", title = "Two"))
        val pid = playlists.create("My List")
        playlists.addTrack(PlaylistTrack(pid, t2, 0))
        playlists.addTrack(PlaylistTrack(pid, t1, 1))
        val ordered = playlists.observeTracks(pid).first()
        assertEquals(listOf("Two", "One"), ordered.map { it.title })
    }

    @Test
    fun removeTrackDropsIt() = runTest {
        val t1 = tracks.insert(Track(uri = "content://1", title = "One"))
        val pid = playlists.create("L")
        playlists.addTrack(PlaylistTrack(pid, t1, 0))
        playlists.removeTrack(pid, t1)
        assertTrue(playlists.observeTracks(pid).first().isEmpty())
    }

    @Test
    fun renameChangesName() = runTest {
        val pid = playlists.create("Old")
        playlists.rename(pid, "New")
        val all = playlists.observePlaylists().first()
        assertEquals("New", all.first { it.id == pid }.name)
    }
}
