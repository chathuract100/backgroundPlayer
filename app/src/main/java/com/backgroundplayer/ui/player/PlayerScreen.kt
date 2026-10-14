package com.backgroundplayer.ui.player

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import coil.compose.AsyncImage
import com.backgroundplayer.playback.PlaybackState
import com.backgroundplayer.playback.RepeatMode

/**
 * Full now-playing screen: video surface (or album art), a scrubber bound to
 * position/duration, transport, shuffle/repeat toggles, and a minimize-to-PiP
 * action shown only for video.
 */
@Composable
fun PlayerScreen(
    state: PlaybackState,
    player: Player?,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeek: (Long) -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onMinimizeToPip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f),
        ) {
            if (state.isVideo) {
                VideoSurface(player = player, modifier = Modifier.fillMaxSize())
            } else {
                AsyncImage(
                    model = state.currentTrack?.thumbnailUri,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }

        Text(
            text = state.currentTrack?.title ?: "Nothing playing",
            style = MaterialTheme.typography.titleLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
        )
        state.currentTrack?.artist?.let {
            Text(it, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }

        val duration = state.durationMs.coerceAtLeast(0L)
        val position = state.positionMs.coerceIn(0L, if (duration == 0L) 0L else duration)
        Slider(
            value = position.toFloat(),
            onValueChange = { onSeek(it.toLong()) },
            valueRange = 0f..(if (duration == 0L) 1f else duration.toFloat()),
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(formatMs(position), style = MaterialTheme.typography.labelSmall)
            Text(formatMs(duration), style = MaterialTheme.typography.labelSmall)
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(top = 8.dp),
        ) {
            ToggleIcon(active = state.shuffle, onClick = onToggleShuffle) {
                Icon(Icons.Filled.Shuffle, contentDescription = "Shuffle")
            }
            IconButton(onClick = onPrevious) {
                Icon(Icons.Filled.SkipPrevious, contentDescription = "Previous")
            }
            IconButton(onClick = onPlayPause) {
                if (state.isPlaying) {
                    Icon(Icons.Filled.Pause, contentDescription = "Pause")
                } else {
                    Icon(Icons.Filled.PlayArrow, contentDescription = "Play")
                }
            }
            IconButton(onClick = onNext) {
                Icon(Icons.Filled.SkipNext, contentDescription = "Next")
            }
            ToggleIcon(active = state.repeat != RepeatMode.OFF, onClick = onCycleRepeat) {
                if (state.repeat == RepeatMode.ONE) {
                    Icon(Icons.Filled.RepeatOne, contentDescription = "Repeat one")
                } else {
                    Icon(Icons.Filled.Repeat, contentDescription = "Repeat")
                }
            }
        }

        if (state.isVideo) {
            IconButton(onClick = onMinimizeToPip, modifier = Modifier.padding(top = 8.dp)) {
                Icon(Icons.Filled.PictureInPictureAlt, contentDescription = "Minimize to floating window")
            }
        }
    }
}

@Composable
private fun ToggleIcon(active: Boolean, onClick: () -> Unit, content: @Composable () -> Unit) {
    val tint = if (active) MaterialTheme.colorScheme.primary else LocalContentColor.current
    IconButton(onClick = onClick) {
        androidx.compose.runtime.CompositionLocalProvider(LocalContentColor provides tint) {
            content()
        }
    }
}

private fun formatMs(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
