package com.backgroundplayer.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import coil.compose.AsyncImage
import com.backgroundplayer.playback.PlaybackState
import com.backgroundplayer.playback.RepeatMode
import com.backgroundplayer.ui.theme.Accent
import com.backgroundplayer.ui.theme.BlobBlue
import com.backgroundplayer.ui.theme.BlobOrange
import com.backgroundplayer.ui.theme.SunsetGradient

/**
 * Full now-playing screen: a collapse control to return to the browse view, a
 * video surface (or album art) tinting the background in the artwork's tones, a
 * scrubber bound to position/duration, transport, shuffle/repeat toggles, and a
 * minimize-to-PiP action shown only for video.
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
    onMinimizeToFloat: () -> Unit,
    onEnterFullscreen: () -> Unit,
    onCollapse: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val artworkColor = rememberArtworkColor(
        thumbnailUri = state.currentTrack?.thumbnailUri,
        fallback = MaterialTheme.colorScheme.primary,
    )
    // Opaque, sunset-glass gradient: a glow blended from the artwork at the top,
    // falling through deep purple into near-black navy. Fully opaque so the
    // animated app background never shows through and clashes.
    val topGlow = lerp(artworkColor, Color(0xFF2A1248), 0.35f)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    0f to topGlow,
                    0.5f to Color(0xFF18122E),
                    1f to Color(0xFF0B0A18),
                ),
            ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars)
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Top bar: collapse back to the browse view, plus PiP for video.
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            ) {
                IconButton(onClick = onCollapse) {
                    Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Close player")
                }
                Text(
                    text = "Now playing",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                )
                if (state.isVideo) {
                    IconButton(onClick = onMinimizeToPip) {
                        Icon(
                            Icons.Filled.PictureInPictureAlt,
                            contentDescription = "Minimize to floating window",
                        )
                    }
                } else {
                    IconButton(onClick = onMinimizeToFloat) {
                        Icon(
                            Icons.Filled.PictureInPictureAlt,
                            contentDescription = "Pop out to floating tile",
                        )
                    }
                }
            }

            Spacer(Modifier.size(16.dp))

            if (state.isVideo) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .clip(RoundedCornerShape(24.dp))
                        .background(MaterialTheme.colorScheme.surface),
                ) {
                    VideoSurface(player = player, modifier = Modifier.fillMaxSize())
                    IconButton(
                        onClick = onEnterFullscreen,
                        modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp),
                    ) {
                        Icon(
                            Icons.Filled.Fullscreen,
                            contentDescription = "Full screen",
                            tint = Color.White,
                        )
                    }
                }
            } else {
                // Circular album disc with an artwork-tinted glow ring.
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxWidth().aspectRatio(1f),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.radialGradient(
                                    listOf(artworkColor.copy(alpha = 0.55f), Color.Transparent),
                                ),
                                CircleShape,
                            ),
                    )
                    val icon = rememberVectorPainter(Icons.Filled.MusicNote)
                    AsyncImage(
                        model = state.currentTrack?.thumbnailUri,
                        contentDescription = null,
                        placeholder = icon,
                        error = icon,
                        fallback = icon,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize(0.78f)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surface)
                            .border(3.dp, Brush.linearGradient(SunsetGradient), CircleShape),
                    )
                }
            }

            Text(
                text = state.currentTrack?.title ?: "Nothing playing",
                style = MaterialTheme.typography.headlineSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 28.dp),
            )
            state.currentTrack?.artist?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }

            val duration = state.durationMs.coerceAtLeast(0L)
            val position = state.positionMs.coerceIn(0L, if (duration == 0L) 0L else duration)
            GradientSlider(
                value = position.toFloat(),
                onValueChange = { onSeek(it.toLong()) },
                valueRange = 0f..(if (duration == 0L) 1f else duration.toFloat()),
                modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(formatMs(position), style = MaterialTheme.typography.labelMedium)
                Text(formatMs(duration), style = MaterialTheme.typography.labelMedium)
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.padding(top = 20.dp),
            ) {
                GlassCircle(active = state.shuffle, dotColor = BlobBlue) {
                    IconButton(onClick = onToggleShuffle) {
                        Icon(Icons.Filled.Shuffle, contentDescription = "Shuffle", tint = toggleTint(state.shuffle))
                    }
                }
                GlassCircle {
                    IconButton(onClick = onPrevious) {
                        Icon(Icons.Filled.SkipPrevious, contentDescription = "Previous", tint = Color.White)
                    }
                }
                PlayPauseButton(isPlaying = state.isPlaying, onClick = onPlayPause)
                GlassCircle {
                    IconButton(onClick = onNext) {
                        Icon(Icons.Filled.SkipNext, contentDescription = "Next", tint = Color.White)
                    }
                }
                GlassCircle(active = state.repeat != RepeatMode.OFF, dotColor = BlobOrange) {
                    IconButton(onClick = onCycleRepeat) {
                        val tint = toggleTint(state.repeat != RepeatMode.OFF)
                        if (state.repeat == RepeatMode.ONE) {
                            Icon(Icons.Filled.RepeatOne, contentDescription = "Repeat one", tint = tint)
                        } else {
                            Icon(Icons.Filled.Repeat, contentDescription = "Repeat", tint = tint)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlayPauseButton(isPlaying: Boolean, onClick: () -> Unit) {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(92.dp)) {
        // Soft pink glow halo behind the button.
        Box(
            modifier = Modifier
                .size(92.dp)
                .background(
                    Brush.radialGradient(listOf(Accent.copy(alpha = 0.45f), Color.Transparent)),
                    CircleShape,
                ),
        )
        // Sunset gradient circle.
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(SunsetGradient)),
        ) {
            IconButton(onClick = onClick) {
                Icon(
                    imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = Color.White,
                )
            }
        }
    }
}

/** A translucent "glass" circle that houses a transport control, with an
 *  optional accent dot and a primary-tinted fill when [active]. */
@Composable
private fun GlassCircle(
    active: Boolean = false,
    dotColor: Color? = null,
    content: @Composable () -> Unit,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(54.dp)
            .clip(CircleShape)
            .background(
                if (active) MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
                else Color.White.copy(alpha = 0.07f),
            )
            .border(1.dp, Color.White.copy(alpha = 0.16f), CircleShape),
    ) {
        content()
        if (dotColor != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(7.dp)
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(dotColor),
            )
        }
    }
}

@Composable
private fun toggleTint(active: Boolean): Color =
    if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant

private fun formatMs(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
