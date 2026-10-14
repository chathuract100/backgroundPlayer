package com.backgroundplayer.playback

import android.content.Context
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.test.utils.FakeMediaSourceFactory
import androidx.media3.test.utils.TestExoPlayerBuilder
import androidx.media3.test.utils.robolectric.TestPlayerRunHelper
import androidx.test.core.app.ApplicationProvider
import com.backgroundplayer.data.db.Track
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowLooper

@RunWith(RobolectricTestRunner::class)
class ExoPlaybackControllerTest {

    private lateinit var player: ExoPlayer
    private lateinit var controller: ExoPlaybackController

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        player = TestExoPlayerBuilder(context)
            .setMediaSourceFactory(FakeMediaSourceFactory())
            .build()
        controller = ExoPlaybackController(player)
    }

    @After
    fun teardown() {
        controller.release()
    }

    private fun track(id: Long, isVideo: Boolean = false) =
        Track(id = id, uri = "content://media/$id", title = "Track $id", isVideo = isVideo)

    @Test
    fun playEmitsCurrentTrackAndPlaying() {
        val t = track(1)
        controller.play(listOf(t), startIndex = 0)
        TestPlayerRunHelper.runUntilPlaybackState(player, Player.STATE_READY)
        ShadowLooper.idleMainLooper()

        assertEquals(1L, controller.state.value.currentTrack?.id)
        assertTrue(controller.state.value.isPlaying)
    }

    @Test
    fun setRepeatOneMapsToPlayerRepeatOne() {
        controller.setRepeat(RepeatMode.ONE)
        assertEquals(Player.REPEAT_MODE_ONE, player.repeatMode)
        assertEquals(RepeatMode.ONE, controller.state.value.repeat)
    }

    @Test
    fun setShuffleTogglesPlayerShuffle() {
        controller.setShuffle(true)
        assertTrue(player.shuffleModeEnabled)
        assertTrue(controller.state.value.shuffle)
    }
}
