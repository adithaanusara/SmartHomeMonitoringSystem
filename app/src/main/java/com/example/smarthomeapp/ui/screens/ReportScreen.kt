package com.example.smarthomeapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.smarthomeapp.ui.components.icon
import com.example.smarthomeapp.utils.formatOnDuration
import com.example.smarthomeapp.viewmodel.DeviceUsage
import com.example.smarthomeapp.viewmodel.ReportRange
import com.example.smarthomeapp.viewmodel.ReportUiState
import com.example.smarthomeapp.viewmodel.ReportViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Usage report.
 *
 * Totals are stat tiles rather than a chart — a single number is read faster as a number. The
 * per-device comparison is one single-series magnitude chart, so it needs no legend (the section
 * title names the measure) and no second hue; each bar is directly labelled with its value because
 * there is no axis to read it against.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ReportViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Usage report") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { innerPadding ->
        when {
            state.isLoading -> Box(
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }

            state.errorMessage != null -> Box(
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(32.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = state.errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                )
            }

            else -> ReportContent(
                state = state,
                onRangeChange = viewModel::selectRange,
                modifier = Modifier.padding(innerPadding),
            )
        }
    }
}

@Composable
private fun ReportContent(
    state: ReportUiState,
    onRangeChange: (ReportRange) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Filters sit in one row above everything they affect.
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ReportRange.entries.forEach { range ->
                    FilterChip(
                        selected = state.range == range,
                        onClick = { onRangeChange(range) },
                        label = { Text(range.label) },
                    )
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatTile("Total on-time", formatOnDuration(state.totalOnMs), Modifier.weight(1f))
                StatTile("Switch-ons", "${state.totalSwitches}", Modifier.weight(1f))
                StatTile("Auto cut-offs", "${state.automaticOffs}", Modifier.weight(1f))
            }
        }

        state.busiest?.takeIf { it.summary.onDurationMs > 0 }?.let { busiest ->
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    ),
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            text = "Most used",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                        Text(
                            text = busiest.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                        Text(
                            text = "${formatOnDuration(busiest.summary.onDurationMs)} on, " +
                                "${busiest.summary.timesSwitchedOn} switch-ons",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                    }
                }
            }
        }

        item {
            SectionTitle("On-time by device")
        }

        if (!state.hasAnyUsage) {
            item { EmptyUsageNote() }
        } else {
            items(state.usage, key = { it.deviceId }) { usage ->
                UsageBarRow(usage = usage, peakOnMs = state.peakOnMs)
            }
        }

        if (state.alerts.isNotEmpty()) {
            item { SectionTitle("Alerts in this period") }
            items(state.alerts, key = { it.id }) { alert ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    ),
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Text(
                            text = alert.message.ifBlank { alert.kind },
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text(
                            text = formatTimestamp(alert.ts) +
                                if (alert.acknowledged) " · acknowledged" else "",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

/**
 * One bar in the magnitude chart.
 *
 * The track shows the full scale so a short bar still reads as "small share of the peak" rather
 * than as a rendering glitch, and the value is labelled directly since the chart has no axis.
 */
@Composable
private fun UsageBarRow(usage: DeviceUsage, peakOnMs: Long) {
    val fraction = if (peakOnMs <= 0) 0f else (usage.summary.onDurationMs.toFloat() / peakOnMs)
    val label = formatOnDuration(usage.summary.onDurationMs)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clearAndSetSemantics {
                contentDescription = "${usage.name}, $label on, " +
                    "${usage.summary.timesSwitchedOn} switch-ons"
            },
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = usage.type.icon(),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp),
            )
            Text(
                text = usage.name,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 8.dp),
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        ) {
            if (fraction > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction.coerceIn(0.02f, 1f))
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.primary),
                )
            }
        }

        if (usage.summary.timesSwitchedOn > 0 || usage.summary.automaticOffCount > 0) {
            Text(
                text = buildString {
                    append("${usage.summary.timesSwitchedOn} switch-ons")
                    if (usage.summary.automaticOffCount > 0) {
                        append(" · ${usage.summary.automaticOffCount} automatic off")
                    }
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainerHighest)
    }
}

@Composable
private fun StatTile(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = 4.dp),
    )
}

@Composable
private fun EmptyUsageNote() {
    Text(
        text = "No activity recorded in this period. Usage is derived from state changes, so a " +
            "device only appears here once it has been switched at least once.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/**
 * Built per call rather than held in a top-level val: a cached formatter captures the locale at
 * class-init, so it would keep formatting in the old locale after the user changes it.
 */
private fun formatTimestamp(millis: Long): String =
    SimpleDateFormat("d MMM, HH:mm", Locale.getDefault()).format(Date(millis))
