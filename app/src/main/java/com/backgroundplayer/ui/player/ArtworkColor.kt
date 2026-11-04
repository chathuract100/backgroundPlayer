package com.backgroundplayer.ui.player

import android.graphics.drawable.BitmapDrawable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.palette.graphics.Palette
import coil.ImageLoader
import coil.request.ImageRequest

/**
 * Extracts a representative accent color from the current track's artwork so the
 * now-playing screen can glow in the album's tones. Falls back to [fallback]
 * when there's no artwork (e.g. audio with no embedded art, or video).
 */
@Composable
fun rememberArtworkColor(thumbnailUri: String?, fallback: Color): Color {
    val context = LocalContext.current
    var color by remember(thumbnailUri) { mutableStateOf(fallback) }

    LaunchedEffect(thumbnailUri, fallback) {
        color = fallback
        val uri = thumbnailUri ?: return@LaunchedEffect
        val request = ImageRequest.Builder(context)
            .data(uri)
            .allowHardware(false) // Palette needs a readable (software) bitmap
            .size(144)
            .build()
        val drawable = ImageLoader(context).execute(request).drawable
        val bitmap = (drawable as? BitmapDrawable)?.bitmap ?: return@LaunchedEffect
        val palette = Palette.from(bitmap).generate()
        val swatch = palette.vibrantSwatch
            ?: palette.mutedSwatch
            ?: palette.dominantSwatch
        if (swatch != null) color = Color(swatch.rgb)
    }

    return color
}
