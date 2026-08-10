package com.example.smarthomeapp.data.repository

import com.example.smarthomeapp.data.model.Alert
import com.example.smarthomeapp.data.model.Device
import com.example.smarthomeapp.data.model.DeviceEvent
import com.example.smarthomeapp.data.model.DeviceSchedule
import com.example.smarthomeapp.data.model.DeviceStatus
import com.example.smarthomeapp.data.model.DeviceType
import com.example.smarthomeapp.data.model.EventSource
import com.example.smarthomeapp.data.remote.FirebaseDatabaseService
import com.example.smarthomeapp.data.remote.observeChildren
import com.example.smarthomeapp.data.remote.observeObject
import com.example.smarthomeapp.data.remote.setValueSuspend
import com.example.smarthomeapp.data.remote.valueEvents
import com.example.smarthomeapp.utils.Constants
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Reads and writes `/devices`, `/events` and `/alerts`.
 *
 * Every state change goes through an atomic multi-path update so the device status, its safety
 * bookkeeping and the audit event land together. A partial write here would leave a hazard device
 * ON with no `onSince`, which is exactly the state the worker cannot protect against.
 */
class DeviceRepository(
    private val db: FirebaseDatabaseService = FirebaseDatabaseService(),
) {
    fun observeDevices(houseId: String): Flow<List<Device>> =
        db.devices(houseId)
            .observeChildren(Device::class.java) { device, key -> device.id = key }
            .map { devices -> devices.sortedBy { it.name } }

    fun observeDevicesOnFloor(houseId: String, floorId: String): Flow<List<Device>> =
        observeDevices(houseId).map { devices -> devices.filter { it.floorId == floorId } }

    fun observeDevice(houseId: String, deviceId: String): Flow<Device?> =
        db.device(houseId, deviceId)
            .observeObject(Device::class.java) { device, key -> device.id = key }

    /**
     * Every device's event log for the house, keyed by device id.
     *
     * One listener on `/events/{houseId}` rather than one per device: the reporting screen needs
     * all of them at once, and N listeners would fan out N round trips and emit N times per render.
     */
    fun observeAllEvents(houseId: String): Flow<Map<String, List<DeviceEvent>>> =
        db.events(houseId).valueEvents().map { snapshot ->
            snapshot.children.associate { deviceNode ->
                val deviceId = deviceNode.key.orEmpty()
                deviceId to deviceNode.children.mapNotNull { eventNode ->
                    runCatching { eventNode.getValue(DeviceEvent::class.java) }
                        .getOrNull()
                        ?.also { it.id = eventNode.key.orEmpty() }
                }
            }
        }

    fun observeEvents(houseId: String, deviceId: String): Flow<List<DeviceEvent>> =
        db.deviceEvents(houseId, deviceId)
            .observeChildren(DeviceEvent::class.java) { event, key -> event.id = key }
            .map { events -> events.sortedByDescending { it.ts } }

    fun observeAlerts(houseId: String): Flow<List<Alert>> =
        db.alerts(houseId)
            .observeChildren(Alert::class.java) { alert, key -> alert.id = key }
            .map { alerts -> alerts.sortedByDescending { it.ts } }

    /**
     * Toggles a whole device.
     *
     * For a hazard device this also maintains `safety/onSince`: set when switching on so the
     * worker can arm its cutoff, cleared when switching off so a stale value cannot trip a timer
     * against a device that is already off.
     */
    suspend fun setDeviceStatus(
        houseId: String,
        device: Device,
        newStatus: DeviceStatus,
        actorUid: String?,
    ) {
        val basePath = "/${Constants.PATH_DEVICES}/$houseId/${device.id}"
        val updates = mutableMapOf<String, Any?>(
            "$basePath/status" to newStatus.name,
        )

        if (device.deviceType == DeviceType.HAZARD) {
            updates["$basePath/safety/onSince"] =
                if (newStatus == DeviceStatus.ON) db.serverTimestamp() else null
        }

        // A multi-switch's channels follow the unit, so the roll-up cannot contradict the unit.
        if (device.deviceType == DeviceType.MULTI_SWITCH && newStatus != DeviceStatus.ON) {
            device.channels.keys.forEach { channelId ->
                updates["$basePath/channels/$channelId/status"] = newStatus.name
            }
        }

        updates += eventUpdate(
            houseId = houseId,
            deviceId = device.id,
            from = device.effectiveStatus,
            to = newStatus,
            actorUid = actorUid,
        )

        db.applyAtomicUpdate(updates)
    }

    /** Toggles one switch inside a gang box. The unit's own status is left to the roll-up. */
    suspend fun setChannelStatus(
        houseId: String,
        device: Device,
        channelId: String,
        newStatus: DeviceStatus,
        actorUid: String?,
    ) {
        val basePath = "/${Constants.PATH_DEVICES}/$houseId/${device.id}"
        val previous = device.channels[channelId]?.channelStatus ?: DeviceStatus.OFF

        val updates = mutableMapOf<String, Any?>(
            "$basePath/channels/$channelId/status" to newStatus.name,
        )

        // Keep the unit's stored status consistent with its channels after this change.
        val othersOn = device.channels
            .filterKeys { it != channelId }
            .values
            .any { it.channelStatus == DeviceStatus.ON }
        val unitStatus =
            if (newStatus == DeviceStatus.ON || othersOn) DeviceStatus.ON else DeviceStatus.OFF
        updates["$basePath/status"] = unitStatus.name

        updates += eventUpdate(
            houseId = houseId,
            deviceId = device.id,
            from = previous,
            to = newStatus,
            actorUid = actorUid,
        )

        db.applyAtomicUpdate(updates)
    }

    suspend fun updateSchedule(houseId: String, deviceId: String, schedule: DeviceSchedule) {
        db.device(houseId, deviceId).child("schedule").setValueSuspend(schedule)
    }

    suspend fun updateMaxOnDuration(houseId: String, deviceId: String, seconds: Int) {
        db.device(houseId, deviceId).child("safety/maxOnDurationSec").setValueSuspend(seconds)
    }

    suspend fun acknowledgeAlert(houseId: String, alertId: String) {
        db.alerts(houseId).child(alertId).child("acknowledged").setValueSuspend(true)
    }

    /**
     * Builds the `/events` half of an atomic update. Returned as a path map rather than written
     * separately so the audit entry cannot survive a failed status write, or vice versa.
     */
    private fun eventUpdate(
        houseId: String,
        deviceId: String,
        from: DeviceStatus,
        to: DeviceStatus,
        actorUid: String?,
    ): Map<String, Any?> {
        val key = db.newEventKey(houseId, deviceId)
        val event = mapOf(
            "ts" to db.serverTimestamp(),
            "from" to from.name,
            "to" to to.name,
            "source" to EventSource.APP.name,
            "actorUid" to actorUid,
        )
        return mapOf("/${Constants.PATH_EVENTS}/$houseId/$deviceId/$key" to event)
    }
}
