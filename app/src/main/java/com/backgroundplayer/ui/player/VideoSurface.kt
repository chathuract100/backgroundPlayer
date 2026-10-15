package com.backgroundplayer.ui.player

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.Player
import androidx.media3.ui.PlayerView

/**
 * Renders video for the given [player] using Media3's [PlayerView]. The app
 * draws its own transport controls, so the view's built-in controller is off.
 */
@Composable
fun VideoSurface(player: Player?, modifier: Modifier = Modifier) {
    AndroidView(
        factory = { context ->
            PlayerView(context).apply {
                useController = false
            }
        },
        update = { view -> view.player = player },
        modifier = modifier,
    )
}
