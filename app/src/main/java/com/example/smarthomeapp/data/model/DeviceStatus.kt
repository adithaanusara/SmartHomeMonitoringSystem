package com.example.smarthomeapp.data.model

/** Operational state shared by every device type: the spec's ON / OFF / ERROR / DISCONNECTED. */
enum class DeviceStatus {
    ON,
    OFF,
    ERROR,
    DISCONNECTED;

    companion object {
        /**
         * Statuses are stored as plain strings because the web simulator and the Node worker both
         * write them without Kotlin's type safety, so an unrecognised value is a real possibility
         * rather than a theoretical one. Falling back to [ERROR] surfaces the data fault in the UI
         * instead of throwing, which would tear down whichever screen was observing the device.
         */
        fun from(raw: String?): DeviceStatus =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: ERROR
    }
}
