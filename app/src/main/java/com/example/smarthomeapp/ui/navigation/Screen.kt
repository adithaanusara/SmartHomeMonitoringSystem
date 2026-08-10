package com.example.smarthomeapp.ui.navigation

/**
 * Every destination in the authenticated graph.
 *
 * Routes with arguments expose a `route(...)` builder so callers never hand-assemble a path and
 * drift out of sync with the pattern.
 */
sealed class Screen(val route: String) {

    data object Home : Screen("home")

    data object Report : Screen("report")

    data object Floor : Screen("floor/{$ARG_FLOOR_ID}") {
        fun route(floorId: String) = "floor/$floorId"
    }

    data object Device : Screen("device/{$ARG_DEVICE_ID}") {
        fun route(deviceId: String) = "device/$deviceId"
    }

    data object Schedule : Screen("schedule/{$ARG_DEVICE_ID}") {
        fun route(deviceId: String) = "schedule/$deviceId"
    }

    data object Camera : Screen("camera/{$ARG_DEVICE_ID}") {
        fun route(deviceId: String) = "camera/$deviceId"
    }

    companion object {
        const val ARG_FLOOR_ID = "floorId"
        const val ARG_DEVICE_ID = "deviceId"
    }
}
