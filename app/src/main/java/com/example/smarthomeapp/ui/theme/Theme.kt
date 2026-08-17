package com.example.smarthomeapp.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Light scheme.
 *
 * Surfaces step through four tints rather than relying on shadows: a card at
 * `surfaceContainerLow` reads as raised against `background` without an elevation overlay, which
 * keeps the dashboard flat and calm while still separating its sections.
 */
private val LightColors = lightColorScheme(
    primary = Indigo700,
    onPrimary = Color.White,
    primaryContainer = IndigoContainerLight,
    onPrimaryContainer = Color(0xFF141A4D),

    secondary = Slate600,
    onSecondary = Color.White,
    secondaryContainer = Slate100,
    onSecondaryContainer = Slate800,

    tertiary = Teal600,
    onTertiary = Color.White,
    tertiaryContainer = TealContainerLight,
    onTertiaryContainer = Color(0xFF06302C),

    background = Slate50,
    onBackground = Slate900,

    surface = Color.White,
    onSurface = Slate900,
    surfaceVariant = Slate100,
    onSurfaceVariant = Slate600,

    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFFBFCFE),
    surfaceContainer = Slate50,
    surfaceContainerHigh = Slate100,
    surfaceContainerHighest = Slate200,

    outline = Slate300,
    outlineVariant = Slate200,

    error = Red600,
    onError = Color.White,
    errorContainer = RedContainerLight,
    onErrorContainer = Color(0xFF5C1512),

    scrim = Color(0xFF000000),
)

/**
 * Dark scheme.
 *
 * Not a mechanical inversion: the darkest tone is reserved for the background so elevated
 * surfaces can step *up* toward the light, which is how depth reads on a dark UI. Text tones are
 * pulled from the same slate ramp to keep secondary text at 3:1 or better instead of fading out.
 */
private val DarkColors = darkColorScheme(
    primary = Indigo400,
    onPrimary = Color(0xFF10173F),
    primaryContainer = IndigoContainerDark,
    onPrimaryContainer = Indigo200,

    secondary = Slate400,
    onSecondary = Slate900,
    secondaryContainer = Slate800,
    onSecondaryContainer = Slate200,

    tertiary = Teal300,
    onTertiary = Color(0xFF04322E),
    tertiaryContainer = TealContainerDark,
    onTertiaryContainer = Teal300,

    background = Slate950,
    onBackground = Slate100,

    surface = Slate950,
    onSurface = Slate100,
    surfaceVariant = Slate800,
    onSurfaceVariant = Slate400,

    surfaceContainerLowest = Color(0xFF080D18),
    surfaceContainerLow = Slate900,
    surfaceContainer = Slate850,
    surfaceContainerHigh = Slate800,
    surfaceContainerHighest = Slate700,

    outline = Slate600,
    outlineVariant = Slate800,

    error = Red300,
    onError = Color(0xFF690005),
    errorContainer = RedContainerDark,
    onErrorContainer = Color(0xFFFFDAD6),

    scrim = Color(0xFF000000),
)

/**
 * App theme.
 *
 * **Dynamic colour is deliberately off.** It was on by default from the project template, which
 * meant `primary` and `error` were whatever the user's wallpaper produced — the app had no visual
 * identity of its own and looked different on every device. For a product whose whole job is
 * making status legible at a glance, and for a demo that has to look the same on every machine it
 * is recorded on, a fixed palette is worth more than wallpaper matching.
 */
@Composable
fun SmartHomeAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = Typography,
        shapes = Shapes,
        content = content,
    )
}
