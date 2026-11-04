package com.backgroundplayer.ui.theme

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

private val DeepViolet = Color(0xFF1C1140)
private val DeepBlue = Color(0xFF0C1630)
private val NearBlack = Color(0xFF0B0B0F)

/**
 * A slow-shifting diagonal gradient (violet -> near-black -> deep blue) that sits
 * behind the whole app. Surfaces drawn on top stay translucent so this shows
 * through, giving the futuristic "glass over gradient" feel.
 */
@Composable
fun GradientBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val transition = rememberInfiniteTransition(label = "gradient")
    val shift by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 14000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "shift",
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .drawBehind {
                val brush = Brush.linearGradient(
                    colors = listOf(DeepViolet, NearBlack, DeepBlue),
                    start = Offset(size.width * shift, 0f),
                    end = Offset(size.width * (1f - shift), size.height),
                )
                drawRect(brush)
            },
        content = content,
    )
}
