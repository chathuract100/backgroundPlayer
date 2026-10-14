package com.backgroundplayer.data.db

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TrackDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: TrackDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.trackDao()
    }

    @After
    fun teardown() = db.close()

    private fun track(uri: String, title: String = "t", isVideo: Boolean = false) =
        Track(uri = uri, title = title, isVideo = isVideo)

    @Test
    fun insertThenObserveAllReturnsTrack() = runTest {
        dao.insert(track("content://a", "Song A"))
        val all = dao.observeAll().first()
        assertEquals(1, all.size)
        assertEquals("Song A", all[0].title)
    }

    @Test
    fun insertDuplicateUriIsIgnored() = runTest {
        val first = dao.insert(track("content://dup"))
        val second = dao.insert(track("content://dup"))
        assertTrue(first > 0)
        assertEquals(-1L, second)
        assertEquals(1, dao.observeAll().first().size)
    }

    @Test
    fun observeByTypeFiltersVideo() = runTest {
        dao.insert(track("content://audio", isVideo = false))
        dao.insert(track("content://video", isVideo = true))
        val videos = dao.observeByType(isVideo = true).first()
        assertEquals(1, videos.size)
        assertTrue(videos[0].isVideo)
    }

    @Test
    fun setAvailabilityUpdatesFlag() = runTest {
        val id = dao.insert(track("content://x"))
        dao.setAvailability(id, false)
        val stored = dao.findByUri("content://x")
        assertNotNull(stored)
        assertFalse(stored!!.isAvailable)
    }
}
