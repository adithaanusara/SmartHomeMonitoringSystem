package com.example.smarthomeapp.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.smarthomeapp.data.model.Device
import com.example.smarthomeapp.data.model.DeviceStatus
import com.example.smarthomeapp.data.model.Floor
import com.example.smarthomeapp.ui.components.DeviceCard
import com.example.smarthomeapp.ui.components.icon
import com.example.smarthomeapp.ui.components.label
import com.example.smarthomeapp.ui.components.statusColors
import com.example.smarthomeapp.viewmodel.HomeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FloorScreen(
    floorId: String,
    viewModel: HomeViewModel,
    onBack: () -> Unit,
    onOpenDevice: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val floor = state.floor(floorId)
    val devices = state.devicesOn(floorId)
    var confirmDelete by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(floor?.name ?: "Floor") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { confirmDelete = true }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete floor")
                    }
                },
            )
        },
    ) { innerPadding ->
        if (floor == null) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                Text("This floor no longer exists.")
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                FloorPlanGrid(
                    floor = floor,
                    devices = devices,
                    onDeviceTap = onOpenDevice,
                )
            }

            item {
                Text(
                    text = "Devices",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }

            if (devices.isEmpty()) {
                item {
                    Text(
                        text = "No devices on this floor yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            items(devices, key = { it.id }) { device ->
                DeviceCard(
                    device = device,
                    onOpen = { onOpenDevice(device.id) },
                    onToggle = { on -> viewModel.toggleDevice(device, on) },
                )
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete ${floor?.name.orEmpty()}?") },
            text = {
                Text(
                    "This also removes ${devices.size} device(s) on this floor. " +
                        "This cannot be undone."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    viewModel.deleteFloor(floorId)
                    onBack()
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("Cancel") }
            },
        )
    }
}

/**
 * The spec's abstract grid mapping overlaid onto a floor layout.
 *
 * The plan is drawn at a fixed aspect ratio derived from the floor's own `gridCols`/`gridRows`, so
 * a cell is always square and a device's `gridX`/`gridY` lands in the same place at every screen
 * size. Sizing the grid from the image instead would make placement depend on the drawable's
 * intrinsic dimensions, and markers would drift when a plan was swapped.
 */
@Composable
private fun FloorPlanGrid(
    floor: Floor,
    devices: List<Device>,
    onDeviceTap: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val cols = floor.gridCols.coerceAtLeast(1)
    val rows = floor.gridRows.coerceAtLeast(1)
    val gridLineColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(cols.toFloat() / rows.toFloat()),
        ) {
            val cellWidth = maxWidth / cols
            val cellHeight = maxHeight / rows

            Image(
                painter = painterResource(floorPlanResource(floor.planImageAsset)),
                contentDescription = "${floor.name} plan",
                contentScale = ContentScale.FillBounds,
                modifier = Modifier.fillMaxSize(),
            )

            val strokePx = with(LocalDensity.current) { 1.dp.toPx() }
            Canvas(modifier = Modifier.fillMaxSize()) {
                val stepX = size.width / cols
                val stepY = size.height / rows
                for (c in 1 until cols) {
                    drawLine(
                        color = gridLineColor,
                        start = androidx.compose.ui.geometry.Offset(c * stepX, 0f),
                        end = androidx.compose.ui.geometry.Offset(c * stepX, size.height),
                        strokeWidth = strokePx,
                    )
                }
                for (r in 1 until rows) {
                    drawLine(
                        color = gridLineColor,
                        start = androidx.compose.ui.geometry.Offset(0f, r * stepY),
                        end = androidx.compose.ui.geometry.Offset(size.width, r * stepY),
                        strokeWidth = strokePx,
                    )
                }
            }

            devices.forEach { device ->
                val x = device.gridX.coerceIn(0, cols - 1)
                val y = device.gridY.coerceIn(0, rows - 1)
                DeviceMarker(
                    device = device,
                    cellSize = minOf(cellWidth, cellHeight),
                    onTap = { onDeviceTap(device.id) },
                    modifier = Modifier.offset(
                        x = cellWidth * x + (cellWidth - minOf(cellWidth, cellHeight)) / 2,
                        y = cellHeight * y + (cellHeight - minOf(cellWidth, cellHeight)) / 2,
                    ),
                )
            }
        }
    }
}

@Composable
private fun DeviceMarker(
    device: Device,
    cellSize: androidx.compose.ui.unit.Dp,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val status = device.effectiveStatus
    val colors = statusColors(status)
    val description = "${device.name}, ${device.deviceType.label()}, ${status.label()}"

    Box(
        modifier = modifier
            .size(cellSize)
            .padding(cellSize * 0.14f)
            .clip(CircleShape)
            .background(colors.container)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        IconButton(onClick = onTap, modifier = Modifier.fillMaxSize()) {
            Icon(
                imageVector = device.deviceType.icon(),
                contentDescription = null,
                tint = colors.content,
                modifier = Modifier.size(cellSize * 0.42f),
            )
        }
        // A ring makes ON readable at marker size, where a fill alone is easy to miss.
        if (status == DeviceStatus.ON) {
            Canvas(Modifier.fillMaxSize()) {
                drawCircle(
                    color = colors.content,
                    radius = size.minDimension / 2 - 1.dp.toPx(),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx()),
                )
            }
        }
    }
}
