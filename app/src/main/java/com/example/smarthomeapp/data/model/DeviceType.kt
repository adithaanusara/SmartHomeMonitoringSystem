package com.example.smarthomeapp.data.model

/**
 * The five heterogeneous device profiles from the spec. The type drives which detail UI is shown
 * and which optional sub-objects on [Device] are populated.
 */
enum class DeviceType {
    /** Single-node binary: a continuous power supply outlet. */
    OUTLET,

    /** Gang-box unit managing 2..5 individually addressable switches. */
    MULTI_SWITCH,

    /** Bulb; may carry a [Schedule] for automatic on/off over a preset period. */
    LIGHT,

    /** Fire-hazard-prone appliance such as an iron; carries a [Safety] max-on-duration. */
    HAZARD,

    /** Monitoring space fed by mock snapshot / stream URIs. */
    CAMERA;

    companion object {
        /** Unknown types fall back to [OUTLET], whose single-toggle UI is safe for anything. */
        fun from(raw: String?): DeviceType =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: OUTLET
    }
}
