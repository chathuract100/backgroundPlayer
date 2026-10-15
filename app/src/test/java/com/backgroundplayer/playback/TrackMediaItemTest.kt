package com.backgroundplayer.playback

import com.backgroundplayer.data.db.Track
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TrackMediaItemTest {

    @Test
    fun roundTripsTrackIdAndUri() {
        val track = Track(id = 42, uri = "content://media/1", title = "Song")
        val item = TrackMediaItem.from(track)

        assertEquals(42L, TrackMediaItem.trackId(item))
        assertEquals("content://media/1", item.localConfiguration?.uri.toString())
    }
}
