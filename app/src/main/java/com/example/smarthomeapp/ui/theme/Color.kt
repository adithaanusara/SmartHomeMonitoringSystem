package com.example.smarthomeapp.ui.theme

import androidx.compose.ui.graphics.Color

val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

/**
 * Device-status colours.
 *
 * Held outside the Material colour scheme on purpose. The theme opts into dynamic colour, so
 * `primary` and `error` are whatever the user's wallpaper produced — fine for chrome, but ON and
 * ERROR have to stay green and red on every device or the dashboard becomes unreadable.
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
