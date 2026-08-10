package com.example.smarthomeapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smarthomeapp.data.model.Alert
import com.example.smarthomeapp.data.model.Device
import com.example.smarthomeapp.data.model.DeviceEvent
import com.example.smarthomeapp.data.model.DeviceType
import com.example.smarthomeapp.data.remote.FirebaseAuthService
import com.example.smarthomeapp.data.repository.DeviceRepository
import com.example.smarthomeapp.data.repository.UserRepository
import com.example.smarthomeapp.utils.UsageCalculator
import com.example.smarthomeapp.utils.UsageSummary
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.ZoneId

enum class ReportRange(val label: String) {
    TODAY("Today"),
    WEEK("7 days"),
    MONTH("30 days");

    /** Start of the window in epoch millis, relative to `now`. */
    fun startMillis(now: Long): Long = when (this) {
        TODAY -> LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        WEEK -> now - 7L * 24 * 60 * 60 * 1000
        MONTH -> now - 30L * 24 * 60 * 60 * 1000
    }
}

data class DeviceUsage(
    val deviceId: String,
    val name: String,
    val type: DeviceType,
    val summary: UsageSummary,
)

data class ReportUiState(
    val isLoading: Boolean = true,
    val range: ReportRange = ReportRange.TODAY,
    val usage: List<DeviceUsage> = emptyList(),
    val alerts: List<Alert> = emptyList(),
    val errorMessage: String? = null,
) {
    val totalOnMs: Long get() = usage.sumOf { it.summary.onDurationMs }
    val totalSwitches: Int get() = usage.sumOf { it.summary.timesSwitchedOn }
    val automaticOffs: Int get() = usage.sumOf { it.summary.automaticOffCount }
    val busiest: DeviceUsage? get() = usage.maxByOrNull { it.summary.onDurationMs }

    /** Longest on-duration in the set, used to scale the bars. */
    val peakOnMs: Long get() = usage.maxOfOrNull { it.summary.onDurationMs } ?: 0L

    val hasAnyUsage: Boolean get() = usage.any { it.summary.onDurationMs > 0 || it.summary.timesSwitchedOn > 0 }
}

/**
 * Builds the usage report by folding `/events`.
 *
 * Resolves its own house rather than borrowing HomeViewModel's: this is a leaf screen with a
 * different data need (`/events`, which the dashboard never reads). The overlapping `/devices`
 * listener is close to free — the Realtime Database keeps one sync tree per path per process, so a
 * second listener on an already-synced path serves from cache instead of refetching.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ReportViewModel(
    private val authService: FirebaseAuthService = FirebaseAuthService(),
    private val userRepository: UserRepository = UserRepository(),
    private val deviceRepository: DeviceRepository = DeviceRepository(),
) : ViewModel() {

    private val selectedRange = MutableStateFlow(ReportRange.TODAY)

    private val houseId: kotlinx.coroutines.flow.Flow<String?> = authService.authState()
        .map { it?.uid }
        .flatMapLatest { uid ->
            if (uid == null) flowOf(null)
            else userRepository.observeUser(uid).map { it?.houseIds?.firstOrNull() }
        }
        .distinctUntilChanged()

    val uiState: StateFlow<ReportUiState> = houseId
        .flatMapLatest { id ->
            if (id == null) {
                flowOf(ReportUiState(isLoading = false))
            } else {
                combine(
                    deviceRepository.observeDevices(id),
                    deviceRepository.observeAllEvents(id),
                    deviceRepository.observeAlerts(id),
                    selectedRange,
                ) { devices, events, alerts, range ->
                    build(devices, events, alerts, range)
                }
            }
        }
        .catch { error ->
            emit(ReportUiState(isLoading = false, errorMessage = error.message ?: "Report failed"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = ReportUiState(),
        )

    fun selectRange(range: ReportRange) {
        selectedRange.value = range
    }

    private fun build(
        devices: List<Device>,
        eventsByDevice: Map<String, List<DeviceEvent>>,
        alerts: List<Alert>,
        range: ReportRange,
    ): ReportUiState {
        val now = System.currentTimeMillis()
        val windowStart = range.startMillis(now)

        val usage = devices
            .map { device ->
                DeviceUsage(
                    deviceId = device.id,
                    name = device.name.ifBlank { device.id },
                    type = device.deviceType,
                    summary = UsageCalculator.summarise(
                        events = eventsByDevice[device.id].orEmpty(),
                        windowStart = windowStart,
                        windowEnd = now,
                    ),
                )
            }
            // Descending by the measure the bars encode, so the chart reads top-to-bottom.
            .sortedWith(
                compareByDescending<DeviceUsage> { it.summary.onDurationMs }
                    .thenByDescending { it.summary.timesSwitchedOn }
                    .thenBy { it.name },
            )

        return ReportUiState(
            isLoading = false,
            range = range,
            usage = usage,
            alerts = alerts.filter { it.ts >= windowStart },
        )
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
