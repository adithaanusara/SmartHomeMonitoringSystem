package com.example.smarthomeapp.data.model

import com.google.firebase.database.Exclude
import com.google.firebase.database.IgnoreExtraProperties

/**
 * A safety notification at `/alerts/{houseId}/{pushId}`, written only by the backend worker.
 * The app observes this node so a server-side cutoff surfaces without a manual refresh.
 */
@IgnoreExtraProperties
data class Alert(
    @get:Exclude var id: String = "",
    val ts: Long = 0L,
    val deviceId: String = "",
    val kind: String = "",
    val message: String = "",
    val acknowledged: Boolean = false,
) {
    @get:Exclude
    val alertKind: AlertKind get() = AlertKind.from(kind)
}

enum class AlertKind {
    /** A hazard device exceeded its configured max_on_duration and was forced off. */
    MAX_DURATION_EXCEEDED,
    DEVICE_ERROR,
    DEVICE_DISCONNECTED;

    companion object {
        fun from(raw: String?): AlertKind =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: DEVICE_ERROR
    }
}
