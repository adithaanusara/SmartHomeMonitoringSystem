package com.example.smarthomeapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smarthomeapp.data.model.DeviceStatus
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

@Composable
fun StatusBadge(
    status: DeviceStatus,
    modifier: Modifier = Modifier,
) {
    val colors = statusColors(status)

    Row(
        modifier = modifier
            .background(colors.container, MaterialTheme.shapes.small)
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .clearAndSetSemantics { contentDescription = "Status ${status.label()}" },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .background(colors.content, CircleShape)
        )
        Text(
            text = status.label(),
            color = colors.content,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.labelSmall,
        )
    }
}

fun DeviceStatus.label(): String = when (this) {
    DeviceStatus.ON -> "On"
    DeviceStatus.OFF -> "Off"
    DeviceStatus.ERROR -> "Error"
    DeviceStatus.DISCONNECTED -> "Offline"
}
