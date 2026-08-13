package com.example.smarthomeapp.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assessment
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.smarthomeapp.data.model.Floor
import com.example.smarthomeapp.ui.components.AlertBanner
import com.example.smarthomeapp.ui.components.FloorCard
import com.example.smarthomeapp.viewmodel.HomeViewModel
import com.example.smarthomeapp.viewmodel.HouseUiState
import com.example.smarthomeapp.ui.navigation.Screen

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
                        IconButton(onClick = onOpenReport) {
                            Icon(Icons.Filled.Assessment, contentDescription = "Usage report")
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
            item {
                Text(
                    text = "Alerts",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            items(unread, key = { it.id }) { alert ->
                AlertBanner(
                    message = alert.message.ifBlank { alert.kind },
                    onDismiss = { onAcknowledgeAlert(alert.id) },
                    dismissLabel = "Acknowledge",
                )
            }
        }

        item {
            Text(
                text = "Floors",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 4.dp),
            )
        }

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
            )
        }
    }
}

@Composable
private fun SummaryRow(active: Int, total: Int, faults: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        SummaryTile("Active", "$active", Modifier.weight(1f))
        SummaryTile("Devices", "$total", Modifier.weight(1f))
        SummaryTile("Faults", "$faults", Modifier.weight(1f))
    }
}

@Composable
private fun SummaryTile(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
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

                            rooms = emptyList()

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