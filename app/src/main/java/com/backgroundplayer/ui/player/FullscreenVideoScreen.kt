package com.backgroundplayer.ui.player

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.Player
import com.backgroundplayer.playback.PlaybackState
import kotlinx.coroutines.delay

/**
 * Immersive, landscape full-screen video. Hides the system bars and requests a
 * sensor-landscape orientation while shown, restoring both on exit. Tapping the
 * video toggles the controls (play/pause, seek, exit), which auto-hide while
 * playing. The system back button and the exit button both leave full-screen.
 */
@Composable
fun FullscreenVideoScreen(
    state: PlaybackState,
    player: Player?,
    onPlayPause: () -> Unit,
    onSeek: (Long) -> Unit,
    onExit: () -> Unit,
) {
    val activity = LocalContext.current.findActivity()

    DisposableEffect(Unit) {
        // Go immersive in whatever orientation the device is currently in; the
        // activity already follows physical rotation, so no orientation is forced.
        val window = activity?.window
        val controller = window?.let { WindowInsetsControllerCompat(it, it.decorView) }
        controller?.apply {
            systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(WindowInsetsCompat.Type.systemBars())
        }

        onDispose {
            controller?.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    BackHandler { onExit() }

    var controlsVisible by remember { mutableStateOf(true) }
    LaunchedEffect(controlsVisible, state.isPlaying) {
        if (controlsVisible && state.isPlaying) {
            delay(3000)
            controlsVisible = false
        }
    }

    val interaction = remember { MutableInteractionSource() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(interactionSource = interaction, indication = null) {
                controlsVisible = !controlsVisible
            },
    ) {
        VideoSurface(player = player, modifier = Modifier.fillMaxSize())

        if (controlsVisible) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.35f)),
            ) {
                IconButton(
                    onClick = onExit,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .windowInsetsPadding(WindowInsets.systemBars)
                        .padding(8.dp),
                ) {
                    Icon(Icons.Filled.Close, contentDescription = "Exit full screen", tint = Color.White)
                }

                IconButton(
                    onClick = onPlayPause,
                    modifier = Modifier.align(Alignment.Center).size(72.dp),
                ) {
                    Icon(
                        imageVector = if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (state.isPlaying) "Pause" else "Play",
                        tint = Color.White,
                        modifier = Modifier.size(48.dp),
                    )
                }

                val duration = state.durationMs.coerceAtLeast(0L)
                val position = state.positionMs.coerceIn(0L, if (duration == 0L) 0L else duration)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.systemBars)
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                ) {
                    Text(formatTime(position), color = Color.White)
                    GradientSlider(
                        value = position.toFloat(),
                        onValueChange = { onSeek(it.toLong()) },
                        valueRange = 0f..(if (duration == 0L) 1f else duration.toFloat()),
                        modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
                    )
                    Text(formatTime(duration), color = Color.White)
                }
            }
        }
    }
}

private fun Context.findActivity(): Activity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    return null
}

private fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    return "%d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}
