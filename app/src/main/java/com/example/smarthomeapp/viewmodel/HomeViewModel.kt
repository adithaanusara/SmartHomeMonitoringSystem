package com.example.smarthomeapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smarthomeapp.data.model.Alert
import com.example.smarthomeapp.data.model.Device
import com.example.smarthomeapp.data.model.DeviceStatus
import com.example.smarthomeapp.data.model.Floor
import com.example.smarthomeapp.data.model.House
import com.example.smarthomeapp.data.remote.FirebaseAuthService
import com.example.smarthomeapp.data.repository.DeviceRepository
import com.example.smarthomeapp.data.repository.HouseRepository
import com.example.smarthomeapp.data.repository.UserRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FloorSummary(
    val floor: Floor,
    val deviceCount: Int,
    val onCount: Int,
)

data class HouseUiState(
    val isLoading: Boolean = true,
    val houseId: String? = null,
    val house: House? = null,
    val floors: List<FloorSummary> = emptyList(),
    val devices: List<Device> = emptyList(),
    val alerts: List<Alert> = emptyList(),
    val errorMessage: String? = null,
) {
    val unreadAlerts: Int get() = alerts.count { !it.acknowledged }
    val activeDevices: Int get() = devices.count { it.effectiveStatus == DeviceStatus.ON }
    val faultedDevices: Int
        get() = devices.count {
            it.effectiveStatus == DeviceStatus.ERROR ||
                it.effectiveStatus == DeviceStatus.DISCONNECTED
        }

    fun device(deviceId: String?): Device? = devices.firstOrNull { it.id == deviceId }
    fun floor(floorId: String?): Floor? = floors.firstOrNull { it.floor.id == floorId }?.floor
    fun devicesOn(floorId: String): List<Device> = devices.filter { it.floorId == floorId }
}

/**
 * House-scoped state holder, shared by the dashboard, the floor plan and the device detail screen.
 *
 * One ViewModel rather than one per screen: all three render slices of the same `/devices` node, so
 * separate ViewModels would open duplicate listeners against identical paths and could briefly
 * disagree with each other mid-update. Scoped to the navigation graph in AppNavigation, so it
 * survives navigation between those screens and is cleared on sign-out.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(
    private val authService: FirebaseAuthService = FirebaseAuthService(),
    private val userRepository: UserRepository = UserRepository(),
    private val houseRepository: HouseRepository = HouseRepository(),
    private val deviceRepository: DeviceRepository = DeviceRepository(),
) : ViewModel() {

    private val _actionError = MutableStateFlow<String?>(null)
    val actionError: StateFlow<String?> = _actionError.asStateFlow()

    val uiState: StateFlow<HouseUiState> = authService.authState()
        .map { it?.uid }
        .flatMapLatest { uid ->
            if (uid == null) flowOf(HouseUiState(isLoading = false)) else observeHouseFor(uid)
        }
        .catch { error ->
            emit(HouseUiState(isLoading = false, errorMessage = error.readableMessage()))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = HouseUiState(),
        )

    /**
     * Resolves the user's first house, then fans out into the four nodes the dashboard needs.
     *
     * `flatMapLatest` twice so that switching account, or a house being added, tears down the old
     * listeners instead of leaving them attached to a house the user can no longer read.
     */
    private fun observeHouseFor(uid: String) =
        userRepository.observeUser(uid).flatMapLatest { user ->
            val houseId = user?.houseIds?.firstOrNull()
                ?: return@flatMapLatest flowOf(HouseUiState(isLoading = false, houseId = null))

            combine(
                houseRepository.observeHouse(houseId),
                houseRepository.observeFloors(houseId),
                deviceRepository.observeDevices(houseId),
                deviceRepository.observeAlerts(houseId),
            ) { house, floors, devices, alerts ->
                HouseUiState(
                    isLoading = false,
                    houseId = houseId,
                    house = house,
                    floors = floors.map { floor ->
                        val onFloor = devices.filter { it.floorId == floor.id }
                        FloorSummary(
                            floor = floor,
                            deviceCount = onFloor.size,
                            onCount = onFloor.count { it.effectiveStatus == DeviceStatus.ON },
                        )
                    },
                    devices = devices,
                    alerts = alerts,
                )
            }
        }

    // ------------------------------------------------------------------ actions

    fun toggleDevice(device: Device, turnOn: Boolean) {
        val houseId = uiState.value.houseId ?: return
        runAction {
            deviceRepository.setDeviceStatus(
                houseId = houseId,
                device = device,
                newStatus = if (turnOn) DeviceStatus.ON else DeviceStatus.OFF,
                actorUid = authService.currentUid,
            )
        }
    }

    fun toggleChannel(device: Device, channelId: String, turnOn: Boolean) {
        val houseId = uiState.value.houseId ?: return
        runAction {
            deviceRepository.setChannelStatus(
                houseId = houseId,
                device = device,
                channelId = channelId,
                newStatus = if (turnOn) DeviceStatus.ON else DeviceStatus.OFF,
                actorUid = authService.currentUid,
            )
        }
    }

    fun updateMaxOnDuration(deviceId: String, seconds: Int) {
        val houseId = uiState.value.houseId ?: return
        runAction { deviceRepository.updateMaxOnDuration(houseId, deviceId, seconds) }
    }

    fun acknowledgeAlert(alertId: String) {
        val houseId = uiState.value.houseId ?: return
        runAction { deviceRepository.acknowledgeAlert(houseId, alertId) }
    }

    fun createHouse(name: String) {
        val uid = authService.currentUid ?: return
        runAction { houseRepository.createHouse(name.ifBlank { "My Home" }, uid) }
    }

    fun addFloor(floor: Floor) {
        val houseId = uiState.value.houseId ?: return
        runAction { houseRepository.addFloor(houseId, floor) }
    }

    fun deleteFloor(floorId: String) {
        val houseId = uiState.value.houseId ?: return
        val deviceIds = uiState.value.devicesOn(floorId).map { it.id }
        runAction { houseRepository.deleteFloor(houseId, floorId, deviceIds) }
    }

    fun dismissActionError() = _actionError.update { null }

    /**
     * Writes are fire-and-forget as far as the UI is concerned: the dashboard already re-renders
     * from the database listener, so the only thing left to report is a failure.
     */
    private fun runAction(block: suspend () -> Unit) {
        viewModelScope.launch {
            runCatching { block() }
                .onFailure { error -> _actionError.update { error.readableMessage() } }
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}

private fun Throwable.readableMessage(): String = when {
    message?.contains("permission_denied", ignoreCase = true) == true ->
        "You do not have access to this house. Check the database rules and your membership."

    message?.contains("Client is offline", ignoreCase = true) == true ->
        "You are offline. Changes will sync when the connection returns."

    else -> message ?: "Something went wrong."
}
