package com.example.smarthomeapp.data.model

import com.example.smarthomeapp.utils.Constants
import com.google.firebase.database.Exclude
import com.google.firebase.database.IgnoreExtraProperties

/**
 * A device at `/devices/{houseId}/{deviceId}`.
 *
 * Enum-valued fields are held as [String] rather than as Kotlin enums: the Firebase mapper throws
 * on an unrecognised enum constant, and two of the three writers to this node are untyped
 * JavaScript. Use [deviceType] and [effectiveStatus] to read them safely.
 *
 * The `safety`, `schedule`, `channels` and `camera` sub-objects are populated only for the device
 * types that use them, so all four are nullable or empty by default.
 */
@IgnoreExtraProperties
data class Device(
    @get:Exclude var id: String = "",
    val floorId: String = "",
    val name: String = "",
    val type: String = DeviceType.OUTLET.name,
    val gridX: Int = 0,
    val gridY: Int = 0,
    val status: String = DeviceStatus.OFF.name,
    val lastSeen: Long = 0L,
    val channels: Map<String, DeviceChannel> = emptyMap(),
    val safety: DeviceSafety? = null,
    val schedule: DeviceSchedule? = null,
    val camera: CameraConfig? = null,
) {
    @get:Exclude
    val deviceType: DeviceType get() = DeviceType.from(type)

    /**
     * Status as the UI should render it. Implements schema invariant 1: a multi-switch unit rolls
     * its channels up, so any channel ON makes the unit ON.
     *
     * A fault on the unit itself wins over the roll-up — a gang box that is unreachable is
     * DISCONNECTED regardless of the last known channel states.
     */
    @get:Exclude
    val effectiveStatus: DeviceStatus
        get() {
            val base = DeviceStatus.from(status)
            if (base == DeviceStatus.ERROR || base == DeviceStatus.DISCONNECTED) return base
            if (deviceType != DeviceType.MULTI_SWITCH) return base
            return if (channels.values.any { DeviceStatus.from(it.status) == DeviceStatus.ON }) {
                DeviceStatus.ON
            } else {
                DeviceStatus.OFF
            }
        }

    @get:Exclude
    val isOn: Boolean get() = effectiveStatus == DeviceStatus.ON

    /** True once the simulator's heartbeat has gone stale; the worker acts on the same threshold. */
    @Exclude
    fun isHeartbeatStale(nowMillis: Long): Boolean =
        lastSeen > 0L && nowMillis - lastSeen > Constants.HEARTBEAT_TIMEOUT_MS

    /**
     * Seconds remaining before the server-side cutoff trips, or null when no cutoff is armed.
     * Display only — the authoritative timer lives in the worker, because the phone may be
     * offline or killed when the limit is reached.
     */
    @Exclude
    fun secondsUntilCutoff(nowMillis: Long): Long? {
        val safety = safety ?: return null
        val onSince = safety.onSince ?: return null
        if (effectiveStatus != DeviceStatus.ON) return null
        val elapsedSec = (nowMillis - onSince) / 1000
        return (safety.maxOnDurationSec - elapsedSec).coerceAtLeast(0)
    }
}

/** One individually addressable switch within a [DeviceType.MULTI_SWITCH] gang box. */
@IgnoreExtraProperties
data class DeviceChannel(
    @get:Exclude var id: String = "",
    val label: String = "",
    val status: String = DeviceStatus.OFF.name,
) {
    @get:Exclude
    val channelStatus: DeviceStatus get() = DeviceStatus.from(status)
}

/**
 * Max-on-duration configuration for [DeviceType.HAZARD] devices.
 *
 * [onSince] is written by whoever turns the device on and cleared on off. The worker reads it to
 * arm the cutoff; nothing should compute elapsed time client-side.
 */
@IgnoreExtraProperties
data class DeviceSafety(
    val maxOnDurationSec: Int = Constants.DEFAULT_MAX_ON_DURATION_SEC,
    val onSince: Long? = null,
)

/** Automatic on/off window for lights and hazard devices. Enforced by the worker, not the app. */
@IgnoreExtraProperties
data class DeviceSchedule(
    val enabled: Boolean = false,
    val onAt: String = "",
    val offAt: String = "",
    /** ISO day numbers, 1 = Monday .. 7 = Sunday. Empty means every day. */
    val days: List<Int> = emptyList(),
)

/** Mock snapshot / stream URIs for [DeviceType.CAMERA]. */
@IgnoreExtraProperties
data class CameraConfig(
    val snapshotUrl: String = "",
    val streamUrl: String = "",
)
