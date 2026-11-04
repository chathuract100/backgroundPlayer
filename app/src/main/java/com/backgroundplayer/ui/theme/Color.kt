package com.backgroundplayer.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * "Sunset glass" palette: a deep navy/purple canvas with soft out-of-focus color
 * blobs (orange -> pink -> magenta, plus a cool accent) glowing behind frosted
 * translucent surfaces. The warm sunset gradient ([SunsetGradient]) is the
 * primary accent used on the play button, scrubber and calls to action.
 */

// --- Accent (sunset) ---
val Accent = Color(0xFFFF2E7E)        // hot pink — primary
val AccentVariant = Color(0xFFFF8A3D) // orange
val AccentMagenta = Color(0xFFB52BE0) // magenta/purple

/** Warm sunset gradient, orange -> pink -> magenta. */
val SunsetGradient = listOf(
    Color(0xFFFF9A3D),
    Color(0xFFFF2E7E),
    Color(0xFFB52BE0),
)

// --- Dark glass surfaces ---
val Background = Color(0xFF0C0A1A)
val Surface = Color(0xFF17132B)
val SurfaceElevated = Color(0xFF201A3A)
val SurfaceVariant = Color(0xFF2A2348)

val OnDark = Color(0xFFF3ECFF)
val OnDarkMuted = Color(0xFFB4A8D6)
val OnAccent = Color(0xFF2A0A17)

/** Visible border/outline on both the dark surfaces and the gradient. */
val OutlineColor = Color(0xFF6E6694)

val ErrorRed = Color(0xFFFF5A6A)

// --- Background gradient base + glow blobs ---
val BgTopRight = Color(0xFF0A0E29)    // deep navy
val BgBottomLeft = Color(0xFF281046)  // deep purple

val BlobOrange = Color(0xFFFF7A2F)
val BlobPink = Color(0xFFFF2E7E)
val BlobCoral = Color(0xFFFF4D6D)
val BlobBlue = Color(0xFF3E6BFF)
