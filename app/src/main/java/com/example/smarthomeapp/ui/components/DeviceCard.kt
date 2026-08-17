package com.example.smarthomeapp.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.smarthomeapp.data.model.Device
import com.example.smarthomeapp.data.model.DeviceStatus
import com.example.smarthomeapp.data.model.DeviceType
import com.example.smarthomeapp.ui.theme.IconSize
import com.example.smarthomeapp.ui.theme.Spacing

/**
 * One device in a list.
 *
 * The whole card opens the detail screen; only the switch toggles. Cameras get no switch — their
 * ON state means "streaming", which is not something the user drives from a list.
 */
@Composable
fun DeviceCard(
    device: Device,
    onOpen: () -> Unit,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val status = device.effectiveStatus
    val colors = statusColors(status)
    val controllable = device.deviceType != DeviceType.CAMERA &&
        status != DeviceStatus.DISCONNECTED

    // The avatar tint follows the device's state, so a glance down the list reads as a column of
    // status colour rather than as ten identical grey rows.
    val avatarContainer by animateColorAsState(
        targetValue = colors.container,
        animationSpec = tween(STATUS_FADE_MS),
        label = "deviceAvatarContainer",
    )
    val avatarContent by animateColorAsState(
        targetValue = colors.content,
        animationSpec = tween(STATUS_FADE_MS),
        label = "deviceAvatarContent",
    )

    Card(
        onClick = onOpen,
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier.padding(
                start = Spacing.md,
                end = Spacing.md,
                top = Spacing.md,
                bottom = Spacing.md,
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(avatarContainer, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = device.deviceType.icon(),
                    contentDescription = null,
                    tint = avatarContent,
                    modifier = Modifier.size(IconSize.md),
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = device.name.ifBlank { "Unnamed device" },
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = device.subtitle(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            if (controllable) {
                Switch(
                    checked = status == DeviceStatus.ON,
                    onCheckedChange = onToggle,
                    colors = statusSwitchColors(),
                )
            } else {
                StatusBadge(status)
            }
        }
    }
}

/** Secondary line: the most useful fact about this particular profile. */
private fun Device.subtitle(): String = when (deviceType) {
    DeviceType.MULTI_SWITCH -> {
        val on = channels.values.count { it.channelStatus == DeviceStatus.ON }
        "${channels.size} switches · $on on"
    }

    DeviceType.HAZARD -> {
        val limit = safety?.maxOnDurationSec
        if (limit != null) "Auto-off after ${formatDuration(limit)}" else deviceType.label()
    }

    DeviceType.LIGHT -> {
        val schedule = schedule
        if (schedule?.enabled == true) {
            "Scheduled ${schedule.onAt}–${schedule.offAt}"
        } else {
            deviceType.label()
        }
    }

    else -> deviceType.label()
}

fun formatDuration(totalSeconds: Int): String {
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return when {
        minutes == 0 -> "${seconds}s"
        seconds == 0 -> "${minutes}m"
        else -> "${minutes}m ${seconds}s"
    }
}
