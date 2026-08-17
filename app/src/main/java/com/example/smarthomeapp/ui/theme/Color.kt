package com.example.smarthomeapp.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Brand palette — a cool slate neutral with a deep indigo accent.
 *
 * Slate rather than a saturated hue because almost every pixel in this app is chrome around
 * status: the four device states have to be the most colourful thing on screen, so the frame they
 * sit in stays desaturated. Indigo is the interactive accent (buttons, switches, selection) and is
 * deliberately *not* green — green means "device is ON" here and nothing else, or the dashboard
 * stops being readable at a glance.
 */

// Neutrals
val Slate950 = Color(0xFF0B1220)
val Slate900 = Color(0xFF0F172A)
val Slate850 = Color(0xFF141D2F)
val Slate800 = Color(0xFF1E293B)
val Slate700 = Color(0xFF334155)
val Slate600 = Color(0xFF475569)
val Slate500 = Color(0xFF64748B)
val Slate400 = Color(0xFF94A3B8)
val Slate300 = Color(0xFFCBD5E1)
val Slate200 = Color(0xFFE2E8F0)
val Slate100 = Color(0xFFEEF2F7)
val Slate50 = Color(0xFFF8FAFC)

// Interactive accent
val Indigo700 = Color(0xFF3949AB)
val Indigo600 = Color(0xFF4355C7)
val Indigo400 = Color(0xFF8F9CF0)
val Indigo200 = Color(0xFFC8D0FA)
val IndigoContainerLight = Color(0xFFE3E7FD)
val IndigoContainerDark = Color(0xFF2A3475)

// Supporting accent, used sparingly for informational emphasis
val Teal600 = Color(0xFF0F766E)
val Teal300 = Color(0xFF5EEAD4)
val TealContainerLight = Color(0xFFD3F3EF)
val TealContainerDark = Color(0xFF113B37)

// Error / destructive
val Red600 = Color(0xFFC62828)
val Red300 = Color(0xFFFFB4AB)
val RedContainerLight = Color(0xFFFCE4E2)
val RedContainerDark = Color(0xFF4A1513)

/**
 * Device-status colours.
 *
 * Held outside the Material colour scheme on purpose: these four have fixed meanings and must not
 * drift with the rest of the palette. Status is always carried by text as well as colour, so the
 * states stay distinguishable for colour-blind users and in a greyscale screen recording.
 */
val StatusOnLight = Color(0xFF1B7F4B)
val StatusOnDark = Color(0xFF5FD99A)
val StatusOnContainerLight = Color(0xFFD7F3E3)
val StatusOnContainerDark = Color(0xFF14352A)

val StatusOffLight = Color(0xFF5F6672)
val StatusOffDark = Color(0xFF9BA3B2)
val StatusOffContainerLight = Color(0xFFE9EBEF)
val StatusOffContainerDark = Color(0xFF262B34)

val StatusErrorLight = Color(0xFFB3261E)
val StatusErrorDark = Color(0xFFFF8A80)
val StatusErrorContainerLight = Color(0xFFFBE0DE)
val StatusErrorContainerDark = Color(0xFF3B1513)

val StatusDisconnectedLight = Color(0xFF9A6300)
val StatusDisconnectedDark = Color(0xFFFFC46B)
val StatusDisconnectedContainerLight = Color(0xFFFDECCF)
val StatusDisconnectedContainerDark = Color(0xFF35270D)
