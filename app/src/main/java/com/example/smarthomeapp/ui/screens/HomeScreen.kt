package com.example.smarthomeapp.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.DeviceHub
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.smarthomeapp.data.model.DeviceStatus
import com.example.smarthomeapp.data.model.Floor
import com.example.smarthomeapp.ui.components.AlertBanner
import com.example.smarthomeapp.ui.components.FloorCard
import com.example.smarthomeapp.ui.components.statusColors
import com.example.smarthomeapp.ui.theme.IconSize
import com.example.smarthomeapp.ui.theme.Spacing
import com.example.smarthomeapp.viewmodel.HomeViewModel
import com.example.smarthomeapp.viewmodel.HouseUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onOpenFloor: (String) -> Unit,
    onOpenFloorEditor: (String) -> Unit,
    onOpenReport: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier,
){
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val actionError by viewModel.actionError.collectAsStateWithLifecycle()
    var showAddFloor by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(state.house?.name?.ifBlank { null } ?: "Smart Home") },
                actions = {
                    if (state.houseId != null) {
                        // BarChart rather than Assessment: Assessment draws its bars inside a
                        // filled rounded square, so next to the plain-stroke + and sign-out icons
                        // it read as a selected chip rather than as a peer action.
                        IconButton(onClick = onOpenReport) {
                            Icon(Icons.Filled.BarChart, contentDescription = "Usage report")
                        }
                        IconButton(onClick = { showAddFloor = true }) {
                            Icon(Icons.Filled.Add, contentDescription = "Add floor")
                        }
                    }
                    IconButton(onClick = onSignOut) {
                        Icon(
                            Icons.AutoMirrored.Filled.Logout,
                            contentDescription = "Sign out",
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when {
                state.isLoading -> LoadingBox()
                state.errorMessage != null -> MessageBox(state.errorMessage!!)
                state.houseId == null -> NoHouseBox(onCreate = viewModel::createHouse)
                else -> HouseContent(
                    state = state,
                    actionError = actionError,
                    onDismissError = viewModel::dismissActionError,
                    onOpenFloor = onOpenFloor,
                    onOpenFloorEditor = onOpenFloorEditor,
                    onAcknowledgeAlert = viewModel::acknowledgeAlert,
                )
            }
        }
    }

    if (showAddFloor) {
        AddFloorDialog(
            existingLevels = state.floors.map { it.floor.level },

            onDismiss = {
                showAddFloor = false
            },

            onConfirm = { floor ->

                viewModel.addFloor(floor)

                showAddFloor = false

            }
        )
    }
}

@Composable
private fun HouseContent(
    state: HouseUiState,
    actionError: String?,
    onDismissError: () -> Unit,
    onOpenFloor: (String) -> Unit,
    onOpenFloorEditor: (String) -> Unit,
    onAcknowledgeAlert: (String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        actionError?.let { message ->
            item {
                AlertBanner(message = message, onDismiss = onDismissError)
            }
        }

        item {
            SummaryRow(
                active = state.activeDevices,
                total = state.devices.size,
                faults = state.faultedDevices,
            )
        }

        val unread = state.alerts.filter { !it.acknowledged }
        if (unread.isNotEmpty()) {
            item { SectionHeader("Alerts") }
            items(unread, key = { it.id }) { alert ->
                AlertBanner(
                    message = alert.message.ifBlank { alert.kind },
                    onDismiss = { onAcknowledgeAlert(alert.id) },
                    dismissLabel = "Acknowledge",
                )
            }
        }

        item { SectionHeader("Floors") }

        if (state.floors.isEmpty()) {
            item {
                Text(
                    text = "No floors yet. Use + to add one.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        items(state.floors, key = { it.floor.id }) { summary ->
            FloorCard(
                floor = summary.floor,
                deviceCount = summary.deviceCount,
                onCount = summary.onCount,
                onClick = { onOpenFloor(summary.floor.id) },
                onEditPlan = { onOpenFloorEditor(summary.floor.id) },
            )
        }
    }
}

/**
 * The three numbers that answer "is my house OK?" before the user reads anything else.
 *
 * Each tile carries its own icon and colour so the row can be parsed by shape, not just by
 * reading three similar digits. Faults is the only one that changes colour with its value —
 * zero faults is unremarkable and stays neutral; anything above zero turns error-red, so the
 * one number that needs attention is the one that visually shouts.
 */
@Composable
private fun SummaryRow(active: Int, total: Int, faults: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
        SummaryTile(
            label = "Active",
            value = active,
            icon = Icons.Filled.Bolt,
            accent = statusColors(DeviceStatus.ON).content,
            modifier = Modifier.weight(1f),
        )
        SummaryTile(
            label = "Devices",
            value = total,
            icon = Icons.Filled.DeviceHub,
            accent = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f),
        )
        SummaryTile(
            label = "Faults",
            value = faults,
            icon = Icons.Filled.ErrorOutline,
            accent = if (faults > 0) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun SummaryTile(
    label: String,
    value: Int,
    icon: ImageVector,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = Spacing.lg, horizontal = Spacing.md),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(IconSize.sm),
            )

            // Counts change when the worker or the simulator writes, not because the user did
            // anything — the number slides so that movement is noticed rather than blinked past.
            AnimatedContent(
                targetState = value,
                transitionSpec = {
                    if (targetState > initialState) {
                        slideInVertically { it } + fadeIn() togetherWith
                            slideOutVertically { -it } + fadeOut()
                    } else {
                        slideInVertically { -it } + fadeIn() togetherWith
                            slideOutVertically { it } + fadeOut()
                    }.using(SizeTransform(clip = false))
                },
                label = "summaryValue",
            ) { shown ->
                Text(
                    text = "$shown",
                    style = MaterialTheme.typography.headlineSmall,
                    color = accent,
                )
            }

            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** One consistent treatment for the "Alerts" / "Floors" dividers between dashboard sections. */
@Composable
private fun SectionHeader(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(top = Spacing.sm, bottom = Spacing.xs),
    )
}

@Composable
private fun LoadingBox() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun MessageBox(message: String) {
    Box(
        Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * Shown to a user with no house — a fresh sign-up, or one whose seed data was never imported.
 * Without this the dashboard would be a permanently empty screen with no way forward.
 */
@Composable
private fun NoHouseBox(onCreate: (String) -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "No house yet",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "Create one to start adding floors and devices.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 6.dp, bottom = 18.dp),
            )
            androidx.compose.material3.Button(onClick = { onCreate("My Home") }) {
                Text("Create house")
            }
        }
    }
}

@Composable
private fun AddFloorDialog(
    existingLevels: List<Int>,
    onDismiss: () -> Unit,
    onConfirm: (Floor) -> Unit,
) {

    var name by remember {
        mutableStateOf("")
    }

    /** Null means "no bundled plan" — the floor starts blank and is drawn in the editor. */
    var selectedPlan by remember {
        mutableStateOf<String?>(null)
    }

    val nextLevel =
        (existingLevels.maxOrNull() ?: -1) + 1


    androidx.compose.material3.AlertDialog(

        onDismissRequest = onDismiss,


        title = {
            Text("Add floor")
        },


        text = {

            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {


                androidx.compose.material3.OutlinedTextField(

                    value = name,

                    onValueChange = {
                        name = it
                    },

                    label = {
                        Text("Floor name")
                    },

                    singleLine = true,

                    modifier = Modifier.fillMaxWidth()

                )


                Text(
                    text = "Plan",
                    style = MaterialTheme.typography.labelLarge,
                )


                /*
                 * Pick a bundled plan, or none. "Draw it" is first because a hand-drawn floor is
                 * now the more flexible option — the sample plans stay for floors that match one.
                 */
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {

                    androidx.compose.material3.FilterChip(
                        selected = selectedPlan == null,
                        onClick = { selectedPlan = null },
                        label = { Text("Draw it") },
                    )

                    AVAILABLE_FLOOR_PLANS.forEach { plan ->

                        androidx.compose.material3.FilterChip(
                            selected = selectedPlan == plan,
                            onClick = { selectedPlan = plan },
                            label = { Text(floorPlanLabel(plan)) },
                        )

                    }

                }


                Text(
                    text = "Rooms can be drawn manually after creating the floor.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

            }

        },


        confirmButton = {


            androidx.compose.material3.TextButton(

                enabled = name.isNotBlank(),


                onClick = {


                    onConfirm(

                        Floor(

                            name = name.trim(),

                            level = nextLevel,

                            planImageAsset = selectedPlan.orEmpty(),

                            rooms = emptyMap()

                        )

                    )

                }

            ) {

                Text("Add")

            }

        },


        dismissButton = {


            androidx.compose.material3.TextButton(

                onClick = onDismiss

            ) {

                Text("Cancel")

            }

        }

    )
}
