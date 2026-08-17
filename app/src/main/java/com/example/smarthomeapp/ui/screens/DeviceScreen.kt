package com.example.smarthomeapp.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.example.smarthomeapp.data.model.Device
import com.example.smarthomeapp.data.model.DeviceStatus
import com.example.smarthomeapp.data.model.DeviceType
import com.example.smarthomeapp.ui.components.StatusBadge
import com.example.smarthomeapp.ui.components.formatDuration
import com.example.smarthomeapp.ui.components.icon
import com.example.smarthomeapp.ui.components.label
import com.example.smarthomeapp.ui.components.statusColors
import com.example.smarthomeapp.ui.components.statusSwitchColors
import com.example.smarthomeapp.viewmodel.HomeViewModel
import kotlinx.coroutines.delay

/**
 * Device detail. Dispatches on [DeviceType] so each of the spec's five profiles gets a distinct
 * control surface rather than one generic toggle.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceScreen(
    deviceId: String,
    viewModel: HomeViewModel,
    onBack: () -> Unit,
    onOpenSchedule: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val device = state.device(deviceId)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(device?.name ?: "Device") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (device?.deviceType == DeviceType.LIGHT ||
                        device?.deviceType == DeviceType.HAZARD
                    ) {
                        IconButton(onClick = { onOpenSchedule(deviceId) }) {
                            Icon(Icons.Filled.Schedule, contentDescription = "Schedule")
                        }
                    }
                },
            )
        },
    ) { innerPadding ->
        if (device == null) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                Text("This device no longer exists.")
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            DeviceHeader(device)

            when (device.deviceType) {
                DeviceType.OUTLET, DeviceType.LIGHT -> SimpleToggleCard(
                    device = device,
                    onToggle = { on -> viewModel.toggleDevice(device, on) },
                )

                DeviceType.MULTI_SWITCH -> MultiSwitchCard(
                    device = device,
                    onToggleChannel = { id, on -> viewModel.toggleChannel(device, id, on) },
                    onToggleAll = { on -> viewModel.toggleDevice(device, on) },
                )

                DeviceType.HAZARD -> HazardCard(
                    device = device,
                    onToggle = { on -> viewModel.toggleDevice(device, on) },
                    onLimitChange = { seconds ->
                        viewModel.updateMaxOnDuration(device.id, seconds)
                    },
                )

                DeviceType.CAMERA -> CameraCard(device)
            }

            if (device.schedule?.enabled == true) {
                ScheduleSummaryCard(device, onOpen = { onOpenSchedule(deviceId) })
            }
        }
    }
}

@Composable
private fun DeviceHeader(device: Device) {
    val status = device.effectiveStatus
    val colors = statusColors(status)

    Card(
        colors = CardDefaults.cardColors(containerColor = colors.container),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Icon(
                imageVector = device.deviceType.icon(),
                contentDescription = null,
                tint = colors.content,
                modifier = Modifier.size(34.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = device.deviceType.label(),
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.content,
                )
                Text(
                    text = device.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.content,
                )
            }
            StatusBadge(status)
        }
    }
}

/** Shown for every profile whose fault state blocks control. */
@Composable
private fun FaultNotice(device: Device) {
    val status = device.effectiveStatus
    if (status != DeviceStatus.ERROR && status != DeviceStatus.DISCONNECTED) return

    Text(
        text = when (status) {
            DeviceStatus.DISCONNECTED ->
                "This device stopped responding. Controls are disabled until it reconnects."

            else -> "This device reported a fault."
        },
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun SimpleToggleCard(device: Device, onToggle: (Boolean) -> Unit) {
    val enabled = device.effectiveStatus != DeviceStatus.DISCONNECTED

    SectionCard(title = "Power") {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = if (device.isOn) "On" else "Off",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
            )
            Switch(
                checked = device.isOn,
                onCheckedChange = onToggle,
                enabled = enabled,
                colors = statusSwitchColors(),
            )
        }
        FaultNotice(device)
    }
}

/**
 * Gang box: one entity in the database, N individually addressable switches in the UI.
 * The unit row toggles every channel at once; each row addresses its own channel.
 */
@Composable
private fun MultiSwitchCard(
    device: Device,
    onToggleChannel: (String, Boolean) -> Unit,
    onToggleAll: (Boolean) -> Unit,
) {
    val enabled = device.effectiveStatus != DeviceStatus.DISCONNECTED
    val channels = device.channels.entries.sortedBy { it.key }

    SectionCard(title = "${channels.size}-gang switch unit") {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "All switches",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f),
            )
            Switch(
                checked = device.isOn,
                onCheckedChange = onToggleAll,
                enabled = enabled,
                colors = statusSwitchColors(),
            )
        }

        HorizontalDivider(Modifier.padding(vertical = 4.dp))

        channels.forEach { (channelId, channel) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = channel.label.ifBlank { channelId },
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        text = channelId,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = channel.channelStatus == DeviceStatus.ON,
                    onCheckedChange = { on -> onToggleChannel(channelId, on) },
                    enabled = enabled,
                    colors = statusSwitchColors(),
                )
            }
        }
        FaultNotice(device)
    }
}

/**
 * Fire-hazard appliance.
 *
 * The countdown is display only. The authoritative timer runs in the backend worker, because the
 * phone may be offline or killed when the limit is reached — which is exactly when the cutoff
 * matters most.
 */
@Composable
private fun HazardCard(
    device: Device,
    onToggle: (Boolean) -> Unit,
    onLimitChange: (Int) -> Unit,
) {
    val enabled = device.effectiveStatus != DeviceStatus.DISCONNECTED
    val limitSec = device.safety?.maxOnDurationSec ?: 0
    var sliderMinutes by remember(limitSec) { mutableFloatStateOf(limitSec / 60f) }

    // Re-reads the clock every second so the countdown ticks without a database round trip.
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(device.id, device.status) {
        while (true) {
            now = System.currentTimeMillis()
            delay(1_000)
        }
    }

    SectionCard(title = "Power") {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = if (device.isOn) "On" else "Off",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
            )
            Switch(
                checked = device.isOn,
                onCheckedChange = onToggle,
                enabled = enabled,
                colors = statusSwitchColors(),
            )
        }
        FaultNotice(device)
    }

    val remaining = device.secondsUntilCutoff(now)
    if (remaining != null && limitSec > 0) {
        SectionCard(title = "Safety cutoff") {
            Text(
                text = "Switches off automatically in ${formatDuration(remaining.toInt())}",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
            )
            LinearProgressIndicator(
                progress = { (remaining.toFloat() / limitSec).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                text = "Enforced by the backend, so it still applies if this phone is offline.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    SectionCard(title = "Maximum on-duration") {
        Text(
            text = formatDuration((sliderMinutes * 60).toInt()),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Slider(
            value = sliderMinutes,
            onValueChange = { sliderMinutes = it },
            onValueChangeFinished = { onLimitChange((sliderMinutes * 60).toInt()) },
            valueRange = 1f..60f,
            steps = 58,
        )
        Text(
            text = "Between 1 and 60 minutes.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * Camera. The "stream" is a mock snapshot URL re-requested on a timer — enough to read as a live
 * feed in the demo without any streaming infrastructure.
 */
@Composable
private fun CameraCard(device: Device) {
    val snapshotUrl = device.camera?.snapshotUrl.orEmpty()
    val streamUrl = device.camera?.streamUrl.orEmpty()
    var live by remember { mutableStateOf(false) }
    var frame by remember { mutableIntStateOf(0) }

    LaunchedEffect(live) {
        while (live) {
            delay(3_000)
            frame++
        }
    }

    SectionCard(title = if (live) "Live view" else "Latest snapshot") {
        val url = if (live && streamUrl.isNotBlank()) streamUrl else snapshotUrl
        if (url.isBlank()) {
            Text(
                text = "No camera URL configured for this device.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            AsyncImage(
                // The cache-busting suffix is what makes successive frames actually re-fetch.
                model = if (live) "$url?frame=$frame" else url,
                contentDescription = "${device.name} view",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f),
            )
            OutlinedButton(
                onClick = { live = !live },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (live) "Stop live view" else "Start live view")
            }
        }
        FaultNotice(device)
    }
}

@Composable
private fun ScheduleSummaryCard(device: Device, onOpen: () -> Unit) {
    val schedule = device.schedule ?: return

    SectionCard(title = "Schedule") {
        Text(
            text = "On at ${schedule.onAt}, off at ${schedule.offAt}",
            style = MaterialTheme.typography.bodyLarge,
        )
        Text(
            text = if (schedule.days.isEmpty()) "Every day" else schedule.days.dayNames(),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedButton(onClick = onOpen, modifier = Modifier.fillMaxWidth()) {
            Text("Edit schedule")
        }
    }
}

private val DAY_LABELS = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

fun List<Int>.dayNames(): String =
    sorted().mapNotNull { DAY_LABELS.getOrNull(it - 1) }.joinToString(", ")

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        // Same hairline border as the cards on every other screen — without it these were the
        // only surfaces in the app defined purely by a fill, and they floated.
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            content()
        }
    }
}
