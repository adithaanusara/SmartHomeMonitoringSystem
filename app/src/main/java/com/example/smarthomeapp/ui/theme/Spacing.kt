package com.example.smarthomeapp.ui.theme

import androidx.compose.ui.unit.dp

/**
 * Spacing rhythm — a 4dp base, used for every gap, inset and pad in the app.
 *
 * Screens previously mixed 5, 6, 12, 14, 18 and 21dp with no system behind the choices, which is
 * the kind of thing nobody consciously notices but everybody feels. Naming the steps makes the
 * odd ones out obvious in review.
 */
object Spacing {
    /** 4dp — between an icon and its label. */
    val xs = 4.dp

    /** 8dp — inside a compact control, between tightly related items. */
    val sm = 8.dp

    /** 12dp — between rows in a list, inside a badge. */
    val md = 12.dp

    /** 16dp — the standard screen gutter and card padding. */
    val lg = 16.dp

    /** 24dp — between sections of a screen. */
    val xl = 24.dp

    /** 32dp — around empty states and other full-screen messages. */
    val xxl = 32.dp
}

/**
 * Icon sizes, so glyphs stop being sized ad hoc (21dp, 24dp and 28dp all appeared).
 */
object IconSize {
    /** 18dp — inline with text. */
    val sm = 18.dp

    /** 22dp — inside a list-row avatar. */
    val md = 22.dp

    /** 28dp — the focus of an empty state or header. */
    val lg = 28.dp
}

/** Minimum Android touch target. Anything tappable gets at least this. */
val MinTouchTarget = 48.dp
