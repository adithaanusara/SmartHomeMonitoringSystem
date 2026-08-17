package com.example.smarthomeapp.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Corner radii, one step softer than the Material defaults.
 *
 * Cards land on `medium` (16dp) rather than 12dp — at the card sizes this app uses, the extra
 * radius is what separates "boxy default template" from something that looks designed. Badges and
 * chips stay on `small` so they read as controls rather than as miniature cards.
 */
val Shapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)
