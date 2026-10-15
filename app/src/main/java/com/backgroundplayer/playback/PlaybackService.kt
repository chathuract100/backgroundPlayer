package com.backgroundplayer.playback

import android.content.Intent
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.backgroundplayer.App
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Foreground [MediaSessionService] that owns the [ExoPlayer]. It plays in the
 * background with a media notification and lock-screen controls (via Media3's
 * default notification provider), auto-pauses on headphone unplug / audio-focus
 * loss, and skips corrupt tracks rather than crashing.
 */
class PlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onCreate() {
        super.onCreate()

        val repository = (application as App).container.repository

        val player = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .setUsage(C.USAGE_MEDIA)
                    .build(),
                /* handleAudioFocus = */ true,
            )
            .setHandleAudioBecomingNoisy(true)
            .build()

        player.addListener(object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                if (PlaybackErrorPolicy.isFileUnavailable(error.errorCode)) {
                    val trackId = player.currentMediaItem?.let(TrackMediaItem::trackId) ?: -1L
                    if (trackId > 0) {
                        serviceScope.launch { repository.markUnavailable(trackId) }
                    }
                }
                when (PlaybackErrorPolicy.onError(player.hasNextMediaItem())) {
                    ErrorAction.SKIP_TO_NEXT -> {
                        player.seekToNextMediaItem()
                        player.prepare()
                    }
                    ErrorAction.STOP -> player.stop()
                }
            }
        })

        mediaSession = MediaSession.Builder(this, player).build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? =
        mediaSession

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player
        if (player == null || (!player.playWhenReady || player.mediaItemCount == 0)) {
            stopSelf()
        }
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        serviceScope.cancel()
        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null
        super.onDestroy()
    }
}
