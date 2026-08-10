package com.example.smarthomeapp.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.smarthomeapp.data.model.DeviceSchedule
import com.example.smarthomeapp.viewmodel.HomeViewModel

private val DAY_LABELS = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
private const val DEFAULT_ON_AT = "18:30"
private const val DEFAULT_OFF_AT = "23:00"

/** Local edit buffer. Kept out of the database until Save so a half-typed window is never live. */
private data class ScheduleForm(
    val enabled: Boolean,
    val onAt: String,
    val offAt: String,
    val days: Set<Int>,
) {
    fun toSchedule() = DeviceSchedule(
        enabled = enabled,
        onAt = onAt,
        offAt = offAt,
        days = days.sorted(),
    )

    companion object {
        fun from(schedule: DeviceSchedule?) = ScheduleForm(
            enabled = schedule?.enabled ?: false,
            onAt = schedule?.onAt?.takeIf { it.isNotBlank() } ?: DEFAULT_ON_AT,
            offAt = schedule?.offAt?.takeIf { it.isNotBlank() } ?: DEFAULT_OFF_AT,
            days = schedule?.days?.toSet().orEmpty(),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleScreen(
    deviceId: String,
    viewModel: HomeViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val device = state.device(deviceId)

    var form by remember { mutableStateOf<ScheduleForm?>(null) }
    var editing by remember { mutableStateOf<TimeField?>(null) }

    // Seed once the device first arrives, keyed on its id rather than on the schedule object —
    // re-seeding on every database emission would wipe edits in progress.
    LaunchedEffect(device?.id) {
        device?.let { form = ScheduleForm.from(it.schedule) }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Schedule") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { innerPadding ->
        val current = form
        if (device == null || current == null) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) { Text(if (device == null) "This device no longer exists." else "Loading…") }
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
            Text(
                text = device.name,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )

            SectionCard("Automatic operation") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = if (current.enabled) "Enabled" else "Disabled",
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Text(
                            text = "Runs on the server, so it works with the app closed.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = current.enabled,
                        onCheckedChange = { form = current.copy(enabled = it) },
                    )
                }
            }

            SectionCard("Times") {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TimeButton(
                        label = "Turns on",
                        value = current.onAt,
                        onClick = { editing = TimeField.ON },
                        modifier = Modifier.weight(1f),
                    )
                    TimeButton(
                        label = "Turns off",
                        value = current.offAt,
                        onClick = { editing = TimeField.OFF },
                        modifier = Modifier.weight(1f),
                    )
                }
                if (crossesMidnight(current.onAt, current.offAt)) {
                    Text(
                        text = "This window runs overnight and switches off the next morning.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            SectionCard("Days") {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DAY_LABELS.forEachIndexed { index, label ->
                        val day = index + 1
                        FilterChip(
                            selected = current.days.isEmpty() || day in current.days,
                            onClick = {
                                val base = if (current.days.isEmpty()) DAY_LABELS.indices.map { it + 1 }.toSet()
                                else current.days
                                val next = if (day in base) base - day else base + day
                                form = current.copy(days = next)
                            },
                            label = { Text(label) },
                        )
                    }
                }
                Text(
                    text = if (current.days.isEmpty()) {
                        "Every day"
                    } else {
                        current.days.sorted().mapNotNull { DAY_LABELS.getOrNull(it - 1) }
                            .joinToString(", ")
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            SectionCard("Summary") {
                Text(
                    text = if (!current.enabled) {
                        "No automatic switching while this is disabled."
                    } else {
                        "Switches on at ${current.onAt} and off at ${current.offAt}, " +
                            if (current.days.isEmpty()) "every day." else
                                current.days.sorted().mapNotNull { DAY_LABELS.getOrNull(it - 1) }
                                    .joinToString(", ") + "."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = "The schedule acts only at those two moments, so switching the device " +
                        "by hand in between keeps working until the next one.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Button(
                onClick = {
                    viewModel.updateSchedule(deviceId, current.toSchedule())
                    onBack()
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Save schedule") }
        }
    }

    editing?.let { field ->
        val initial = when (field) {
            TimeField.ON -> form?.onAt
            TimeField.OFF -> form?.offAt
        }
        TimePickerDialog(
            title = if (field == TimeField.ON) "Turns on at" else "Turns off at",
            initial = initial ?: DEFAULT_ON_AT,
            onDismiss = { editing = null },
            onConfirm = { value ->
                form = when (field) {
                    TimeField.ON -> form?.copy(onAt = value)
                    TimeField.OFF -> form?.copy(offAt = value)
                }
                editing = null
            },
        )
    }
}

private enum class TimeField { ON, OFF }

@Composable
private fun TimeButton(
    label: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedButton(onClick = onClick, modifier = modifier) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = label, style = MaterialTheme.typography.labelSmall)
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerDialog(
    title: String,
    initial: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    val (initialHour, initialMinute) = parseHhMm(initial)
    val pickerState = rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute,
        is24Hour = true,
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                TimePicker(state = pickerState)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onConfirm(
                    "%02d:%02d".format(pickerState.hour, pickerState.minute)
                )
            }) { Text("Set") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
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

/** Returns hour and minute, falling back to a sane default rather than throwing on bad data. */
private fun parseHhMm(value: String): Pair<Int, Int> {
    val match = Regex("^(\\d{1,2}):(\\d{2})$").find(value.trim()) ?: return 18 to 30
    val hour = match.groupValues[1].toIntOrNull()?.takeIf { it in 0..23 } ?: 18
    val minute = match.groupValues[2].toIntOrNull()?.takeIf { it in 0..59 } ?: 30
    return hour to minute
}

private fun crossesMidnight(onAt: String, offAt: String): Boolean {
    val (onH, onM) = parseHhMm(onAt)
    val (offH, offM) = parseHhMm(offAt)
    return (offH * 60 + offM) <= (onH * 60 + onM)
}
