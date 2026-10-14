package com.backgroundplayer.playback

import androidx.media3.common.C
import androidx.media3.common.Player
import com.backgroundplayer.data.db.Track

/**
 * Projects a Media3 [Player]'s current state onto a [PlaybackState]. Shared by
 * the in-service [ExoPlaybackController] and the UI-side [PlaybackConnection] so
 * both report state identically. The current track is resolved by the player's
 * media-item index into [queue] (stable under shuffle).
 */
object PlaybackStateReducer {
    fun reduce(player: Player, queue: List<Track>): PlaybackState {
        val track = if (player.mediaItemCount == 0) null else queue.getOrNull(player.currentMediaItemIndex)
        val rawDuration = player.duration
        return PlaybackState(
            currentTrack = track,
            isPlaying = player.isPlaying,
            positionMs = player.currentPosition.coerceAtLeast(0L),
            durationMs = if (rawDuration == C.TIME_UNSET) 0L else rawDuration,
            isVideo = track?.isVideo ?: false,
            shuffle = player.shuffleModeEnabled,
            repeat = player.repeatMode.toRepeatMode(),
        )
    }
}

/** Maps this [RepeatMode] to the matching Media3 `Player.REPEAT_MODE_*` constant. */
fun RepeatMode.toPlayerRepeatMode(): Int = when (this) {
    RepeatMode.OFF -> Player.REPEAT_MODE_OFF
    RepeatMode.ONE -> Player.REPEAT_MODE_ONE
    RepeatMode.ALL -> Player.REPEAT_MODE_ALL
}

/** Maps a Media3 `Player.REPEAT_MODE_*` constant to [RepeatMode]. */
fun Int.toRepeatMode(): RepeatMode = when (this) {
    Player.REPEAT_MODE_ONE -> RepeatMode.ONE
    Player.REPEAT_MODE_ALL -> RepeatMode.ALL
    else -> RepeatMode.OFF
}
