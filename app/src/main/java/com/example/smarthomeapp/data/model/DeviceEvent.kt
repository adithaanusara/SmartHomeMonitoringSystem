package com.example.smarthomeapp.data.model

import com.google.firebase.database.Exclude
import com.google.firebase.database.IgnoreExtraProperties

/**
 * An append-only state transition at `/events/{houseId}/{deviceId}/{pushId}`.
 *
 * This log is the sole source for the reporting screen. Usage is derived by folding transitions
 * rather than kept as a running counter, which would drift whenever a client died mid-session.
 */
@IgnoreExtraProperties
data class DeviceEvent(
    @get:Exclude var id: String = "",
    val ts: Long = 0L,
    val from: String = "",
    val to: String = "",
    val source: String = EventSource.APP.name,
    /** Present only when [source] is [EventSource.APP]. */
    val actorUid: String? = null,
) {
    @get:Exclude
    val eventSource: EventSource get() = EventSource.from(source)

    @get:Exclude
    val fromStatus: DeviceStatus get() = DeviceStatus.from(from)

    @get:Exclude
    val toStatus: DeviceStatus get() = DeviceStatus.from(to)
}

/** Who drove a transition. Lets the report distinguish a user toggle from a safety cutoff. */
enum class EventSource {
    APP,
    SIMULATOR,
    WORKER,
    SCHEDULE;

    companion object {
        fun from(raw: String?): EventSource =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: APP
    }
}
