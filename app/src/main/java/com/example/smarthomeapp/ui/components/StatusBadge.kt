package com.example.smarthomeapp.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwitchColors
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import com.example.smarthomeapp.data.model.DeviceStatus
import com.example.smarthomeapp.ui.theme.Spacing
import com.example.smarthomeapp.ui.theme.StatusDisconnectedContainerDark
import com.example.smarthomeapp.ui.theme.StatusDisconnectedContainerLight
import com.example.smarthomeapp.ui.theme.StatusDisconnectedDark
import com.example.smarthomeapp.ui.theme.StatusDisconnectedLight
import com.example.smarthomeapp.ui.theme.StatusErrorContainerDark
import com.example.smarthomeapp.ui.theme.StatusErrorContainerLight
import com.example.smarthomeapp.ui.theme.StatusErrorDark
import com.example.smarthomeapp.ui.theme.StatusErrorLight
import com.example.smarthomeapp.ui.theme.StatusOffContainerDark
import com.example.smarthomeapp.ui.theme.StatusOffContainerLight
import com.example.smarthomeapp.ui.theme.StatusOffDark
import com.example.smarthomeapp.ui.theme.StatusOffLight
import com.example.smarthomeapp.ui.theme.StatusOnContainerDark
import com.example.smarthomeapp.ui.theme.StatusOnContainerLight
import com.example.smarthomeapp.ui.theme.StatusOnDark
import com.example.smarthomeapp.ui.theme.StatusOnLight

@Immutable
data class StatusColors(val content: Color, val container: Color)

/**
 * Resolves the palette for a status against the current light/dark mode.
 *
 * Status is also carried by the badge's text, never by colour alone — the four states have to stay
 * distinguishable for colour-blind users and in a greyscale screen recording.
 */
@Composable
@ReadOnlyComposable
fun statusColors(status: DeviceStatus): StatusColors {
    val dark = isSystemInDarkTheme()
    return when (status) {
        DeviceStatus.ON -> StatusColors(
            content = if (dark) StatusOnDark else StatusOnLight,
            container = if (dark) StatusOnContainerDark else StatusOnContainerLight,
        )

        DeviceStatus.OFF -> StatusColors(
            content = if (dark) StatusOffDark else StatusOffLight,
            container = if (dark) StatusOffContainerDark else StatusOffContainerLight,
        )

        DeviceStatus.ERROR -> StatusColors(
            content = if (dark) StatusErrorDark else StatusErrorLight,
            container = if (dark) StatusErrorContainerDark else StatusErrorContainerLight,
        )

        DeviceStatus.DISCONNECTED -> StatusColors(
            content = if (dark) StatusDisconnectedDark else StatusDisconnectedLight,
            container = if (dark) StatusDisconnectedContainerDark else StatusDisconnectedContainerLight,
        )
    }
}

/**
 * Status pill.
 *
 * Colours cross-fade rather than cut when a device changes state. A device flipping to
 * DISCONNECTED because the worker noticed a missed heartbeat is not something the user initiated,
 * so an instant colour swap reads as a glitch; a 220ms fade reads as the system telling them
 * something. The dot and the word change together, so the meaning never rests on colour alone.
 */
@Composable
fun StatusBadge(
    status: DeviceStatus,
    modifier: Modifier = Modifier,
) {
    val target = statusColors(status)
    val content by animateColorAsState(
        targetValue = target.content,
        animationSpec = tween(STATUS_FADE_MS),
        label = "statusContent",
    )
    val container by animateColorAsState(
        targetValue = target.container,
        animationSpec = tween(STATUS_FADE_MS),
        label = "statusContainer",
    )

    Row(
        modifier = modifier
            .background(container, MaterialTheme.shapes.small)
            .padding(horizontal = Spacing.sm, vertical = 5.dp)
            .clearAndSetSemantics { contentDescription = "Status ${status.label()}" },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .background(content, CircleShape)
        )
        Text(
            text = status.label(),
            color = content,
            style = MaterialTheme.typography.labelSmall,
        )
    }
}

/** Shared by every status-coloured surface so they all cross-fade in step. */
const val STATUS_FADE_MS = 220

/**
 * Switch colours for anything that turns a device on.
 *
 * Checked uses the ON status green rather than the theme's indigo. A switch and the status dot
 * next to it are reporting the same fact, and two different "active" colours on one row read as
 * two unrelated signals. Indigo stays for navigation and confirmation — things the user drives —
 * while green means, everywhere without exception, that current is flowing.
 */
@Composable
fun statusSwitchColors(): SwitchColors = SwitchDefaults.colors(
    checkedThumbColor = MaterialTheme.colorScheme.surface,
    checkedTrackColor = statusColors(DeviceStatus.ON).content,
    checkedBorderColor = statusColors(DeviceStatus.ON).content,
)

fun DeviceStatus.label(): String = when (this) {
    DeviceStatus.ON -> "On"
    DeviceStatus.OFF -> "Off"
    DeviceStatus.ERROR -> "Error"
    DeviceStatus.DISCONNECTED -> "Offline"
}
