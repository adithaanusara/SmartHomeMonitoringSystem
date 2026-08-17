package com.example.smarthomeapp.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Edit
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

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale

import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource

import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.smarthomeapp.data.model.Device
import com.example.smarthomeapp.data.model.DeviceStatus
import com.example.smarthomeapp.data.model.Floor
import com.example.smarthomeapp.ui.components.DeviceCard
import com.example.smarthomeapp.ui.components.icon
import com.example.smarthomeapp.ui.components.label
import com.example.smarthomeapp.ui.components.statusColors
import com.example.smarthomeapp.ui.theme.Spacing
import com.example.smarthomeapp.viewmodel.HomeViewModel
import androidx.compose.ui.Alignment


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FloorScreen(
    floorId: String,
    viewModel: HomeViewModel,
    onBack: () -> Unit,
    onOpenDevice: (String) -> Unit,

    // NEW:
    // Opens the manual floor/room editor.
    onEditFloor: () -> Unit,

    modifier: Modifier = Modifier,
) {

    val state by viewModel.uiState.collectAsStateWithLifecycle()

    val floor = state.floor(floorId)

    val devices = state.devicesOn(floorId)

    var confirmDelete by remember {
        mutableStateOf(false)
    }


    Scaffold(
        modifier = modifier.fillMaxSize(),

        topBar = {

            TopAppBar(

                title = {
                    Text(floor?.name ?: "Floor")
                },

                navigationIcon = {

                    IconButton(
                        onClick = onBack
                    ) {

                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )

                    }
                },

                actions = {

                    /*
                     * NEW EDIT BUTTON
                     *
                     * User clicks this to open FloorPlanEditorScreen,
                     * where rooms can be drawn manually.
                     */
                    IconButton(
                        onClick = onEditFloor
                    ) {

                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = "Edit floor plan"
                        )

                    }


                    // Existing delete button
                    IconButton(
                        onClick = {
                            confirmDelete = true
                        }
                    ) {

                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = "Delete floor"
                        )

                    }
                }
            )
        }

    ) { innerPadding ->


        /*
         * Floor might disappear while this screen is open
         * if it was deleted from Firebase.
         */
        if (floor == null) {

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),

                contentAlignment = Alignment.Center
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

            verticalArrangement =
                Arrangement.spacedBy(12.dp)

        ) {


            /*
             * Current floor plan + grid + device markers.
             */
            item {

                FloorPlanGrid(
                    floor = floor,
                    devices = devices,
                    onDeviceTap = onOpenDevice
                )

            }


            item {

                // Same treatment as the dashboard's section dividers.
                Text(
                    text = "DEVICES",

                    style =
                        MaterialTheme.typography.labelMedium,

                    color =
                        MaterialTheme.colorScheme.onSurfaceVariant,

                    modifier =
                        Modifier.padding(top = Spacing.sm, bottom = Spacing.xs)
                )

            }


            if (devices.isEmpty()) {

                item {

                    Text(
                        text = "No devices on this floor yet.",

                        style =
                            MaterialTheme.typography.bodyMedium,

                        color =
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )

                }

            }


            items(
                items = devices,
                key = { it.id }
            ) { device ->


                DeviceCard(

                    device = device,

                    onOpen = {
                        onOpenDevice(device.id)
                    },

                    onToggle = { on ->
                        viewModel.toggleDevice(
                            device,
                            on
                        )
                    }
                )

            }
        }
    }


    /*
     * Delete confirmation dialog
     */
    if (confirmDelete) {

        AlertDialog(

            onDismissRequest = {
                confirmDelete = false
            },

            title = {

                Text(
                    "Delete ${floor?.name.orEmpty()}?"
                )

            },

            text = {

                Text(
                    "This also removes ${devices.size} device(s) on this floor. " +
                            "This cannot be undone."
                )

            },

            confirmButton = {

                TextButton(

                    onClick = {

                        confirmDelete = false

                        viewModel.deleteFloor(
                            floorId
                        )

                        onBack()

                    }

                ) {

                    Text("Delete")

                }

            },

            dismissButton = {

                TextButton(

                    onClick = {
                        confirmDelete = false
                    }

                ) {

                    Text("Cancel")

                }

            }
        )
    }
}


/**
 * Shows:
 *
 * 1. Floor plan image
 * 2. Grid
 * 3. Device markers
 *
 * Room drawing itself is handled by FloorPlanEditorScreen.
 */
@Composable
private fun FloorPlanGrid(
    floor: Floor,
    devices: List<Device>,
    onDeviceTap: (String) -> Unit,
    modifier: Modifier = Modifier,
) {

    val cols =
        floor.gridCols.coerceAtLeast(1)

    val rows =
        floor.gridRows.coerceAtLeast(1)


    val gridLineColor =
        MaterialTheme.colorScheme.outline
            .copy(alpha = 0.35f)


    val roomFillColor =
        MaterialTheme.colorScheme.secondaryContainer
            .copy(alpha = 0.45f)


    val roomBorderColor =
        MaterialTheme.colorScheme.outline


    Card(

        modifier =
            modifier.fillMaxWidth(),

        colors =
            CardDefaults.cardColors(
                containerColor =
                    MaterialTheme.colorScheme.surfaceContainerLow
            )

    ) {


        BoxWithConstraints(

            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(
                    cols.toFloat() /
                            rows.toFloat()
                )

        ) {


            val cellWidth =
                maxWidth / cols

            val cellHeight =
                maxHeight / rows


            /*
             * Layer 1: the bundled plan drawable, when this floor has one. Absent for a floor
             * the user drew from scratch, which is why the lookup is nullable.
             */
            floorPlanResource(floor.planImageAsset)?.let { planRes ->

                Image(
                    painter = painterResource(planRes),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    alpha = floorPlanAlpha(),
                    modifier = Modifier.fillMaxSize(),
                )

            }


            /*
             * Layer 2: the rooms the user drew in the editor.
             *
             * Geometry is stored as fractions of the plan, so it scales onto whatever size this
             * card happens to be — the same numbers drive the editor canvas, which is a
             * different size again. Drawn under the grid lines and device markers so a marker is
             * never hidden behind a room fill.
             */
            floor.rooms.values.forEach { room ->

                Box(
                    modifier = Modifier
                        .offset(
                            x = maxWidth * room.x,
                            y = maxHeight * room.y
                        )
                        .size(
                            width = maxWidth * room.width,
                            height = maxHeight * room.height
                        )
                        .background(roomFillColor)
                        .border(1.dp, roomBorderColor),

                    contentAlignment = Alignment.Center
                ) {

                    Text(
                        text = room.name,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        maxLines = 1
                    )

                }

            }


            if (floor.rooms.isEmpty() && floor.planImageAsset.isBlank()) {

                Text(
                    text = "No rooms drawn yet — use the edit button to draw this floor.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,

                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(horizontal = 24.dp)
                )

            }


            /*
             * Grid lines
             */
            val strokePx =
                with(LocalDensity.current) {
                    1.dp.toPx()
                }


            Canvas(
                modifier =
                    Modifier.fillMaxSize()
            ) {


                val stepX =
                    size.width / cols

                val stepY =
                    size.height / rows


                for (c in 1 until cols) {

                    drawLine(

                        color =
                            gridLineColor,

                        start =
                            androidx.compose.ui.geometry.Offset(
                                c * stepX,
                                0f
                            ),

                        end =
                            androidx.compose.ui.geometry.Offset(
                                c * stepX,
                                size.height
                            ),

                        strokeWidth =
                            strokePx
                    )

                }


                for (r in 1 until rows) {

                    drawLine(

                        color =
                            gridLineColor,

                        start =
                            androidx.compose.ui.geometry.Offset(
                                0f,
                                r * stepY
                            ),

                        end =
                            androidx.compose.ui.geometry.Offset(
                                size.width,
                                r * stepY
                            ),

                        strokeWidth =
                            strokePx
                    )

                }
            }


            /*
             * Device markers
             */
            devices.forEach { device ->


                val x =
                    device.gridX
                        .coerceIn(
                            0,
                            cols - 1
                        )


                val y =
                    device.gridY
                        .coerceIn(
                            0,
                            rows - 1
                        )


                val markerSize =
                    minOf(
                        cellWidth,
                        cellHeight
                    )


                DeviceMarker(

                    device = device,

                    cellSize =
                        markerSize,

                    onTap = {
                        onDeviceTap(
                            device.id
                        )
                    },

                    modifier =
                        Modifier.offset(

                            x =
                                cellWidth * x +
                                        (
                                                cellWidth -
                                                        markerSize
                                                ) / 2,

                            y =
                                cellHeight * y +
                                        (
                                                cellHeight -
                                                        markerSize
                                                ) / 2
                        )
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

    val status =
        device.effectiveStatus

    val colors =
        statusColors(status)

    val description =
        "${device.name}, " +
                "${device.deviceType.label()}, " +
                status.label()


    Box(

        modifier =
            modifier
                .size(cellSize)
                .padding(
                    cellSize * 0.14f
                )
                .clip(CircleShape)
                .background(
                    colors.container
                )
                .semantics {
                    contentDescription =
                        description
                },

        contentAlignment =
            Alignment.Center

    ) {


        IconButton(

            onClick = onTap,

            modifier =
                Modifier.fillMaxSize()

        ) {


            Icon(

                imageVector =
                    device.deviceType.icon(),

                contentDescription =
                    null,

                tint =
                    colors.content,

                modifier =
                    Modifier.size(
                        cellSize * 0.42f
                    )
            )

        }


        /*
         * ON state ring
         */
        if (
            status ==
            DeviceStatus.ON
        ) {

            Canvas(
                Modifier.fillMaxSize()
            ) {

                drawCircle(

                    color =
                        colors.content,

                    radius =
                        size.minDimension /
                                2 -
                                1.dp.toPx(),

                    style =
                        androidx.compose.ui.graphics.drawscope.Stroke(
                            width =
                                2.dp.toPx()
                        )
                )
            }
        }
    }
}