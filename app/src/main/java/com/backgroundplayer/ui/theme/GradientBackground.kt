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

/**
 * The "sunset glass" backdrop: a deep purple->navy base with several large, soft,
 * out-of-focus color blobs (orange, pink, coral, blue) that drift slowly. Frosted
 * translucent surfaces drawn on top let these glow through, as in the reference.
 */
@Composable
fun GradientBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val transition = rememberInfiniteTransition(label = "backdrop")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 16000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "drift",
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .drawBehind {
                // Deep base, purple (bottom-left) into navy (top-right).
                drawRect(
                    Brush.linearGradient(
                        colors = listOf(BgBottomLeft, BgTopRight),
                        start = Offset(0f, size.height),
                        end = Offset(size.width, 0f),
                    ),
                )

                val w = size.width
                val h = size.height
                fun blob(color: Color, cx: Float, cy: Float, radius: Float, alpha: Float) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(color.copy(alpha = alpha), Color.Transparent),
                            center = Offset(cx, cy),
                            radius = radius,
                        ),
                        radius = radius,
                        center = Offset(cx, cy),
                    )
                }

                // Blobs drift a little with t so the backdrop feels alive.
                blob(BlobOrange, w * (0.12f + 0.05f * t), h * (0.08f + 0.04f * t), w * 0.62f, 0.68f)
                blob(BlobPink, w * (0.86f - 0.06f * t), h * (0.28f + 0.05f * t), w * 0.60f, 0.60f)
                blob(BlobCoral, w * (0.30f + 0.06f * t), h * (0.94f - 0.05f * t), w * 0.66f, 0.62f)
                blob(BlobBlue, w * (0.80f - 0.04f * t), h * (0.90f - 0.03f * t), w * 0.46f, 0.42f)
            },
        content = content,
    )
}
