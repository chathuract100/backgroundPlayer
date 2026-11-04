package com.backgroundplayer.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val DarkColors = darkColorScheme(
    primary = Accent,
    onPrimary = OnAccent,
    primaryContainer = AccentMagenta,
    onPrimaryContainer = OnDark,
    secondary = AccentVariant,
    onSecondary = OnAccent,
    tertiary = AccentMagenta,
    onTertiary = OnDark,
    background = Background,
    onBackground = OnDark,
    surface = Surface,
    onSurface = OnDark,
    surfaceVariant = SurfaceVariant,
    onSurfaceVariant = OnDarkMuted,
    surfaceContainer = SurfaceElevated,
    surfaceContainerHigh = SurfaceVariant,
    error = ErrorRed,
    onError = OnDark,
    outline = SurfaceVariant,
)

private val AppShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
)

/** App-wide dark theme. The app is always dark by design (a media player). */
@Composable
fun BackgroundPlayerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColors,
        shapes = AppShapes,
        content = content,
    )
}
