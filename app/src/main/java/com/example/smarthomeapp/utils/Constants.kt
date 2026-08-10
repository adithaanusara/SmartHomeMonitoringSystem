package com.example.smarthomeapp.utils

/**
 * Values shared with the web simulator and the Node worker. Anything changed here must be changed
 * in those two projects too — see docs/SCHEMA.md.
 */
object Constants {

    // Realtime Database top-level paths
    const val PATH_USERS = "users"
    const val PATH_HOUSES = "houses"
    const val PATH_FLOORS = "floors"
    const val PATH_DEVICES = "devices"
    const val PATH_EVENTS = "events"
    const val PATH_ALERTS = "alerts"

    /**
     * How long the simulator's heartbeat may go stale before the worker marks a device
     * DISCONNECTED. The simulator writes `lastSeen` roughly every 10s, so this allows two
     * missed beats before reacting.
     */
    const val HEARTBEAT_TIMEOUT_MS = 30_000L

    /** Default cutoff for a newly created hazard device: 30 minutes. */
    const val DEFAULT_MAX_ON_DURATION_SEC = 1_800

    // Default floor grid, overridable per floor
    const val DEFAULT_GRID_COLS = 8
    const val DEFAULT_GRID_ROWS = 6

    /** ISO day numbers, matching [java.time.DayOfWeek.getValue]. */
    const val DAY_MONDAY = 1
    const val DAY_SUNDAY = 7
}
