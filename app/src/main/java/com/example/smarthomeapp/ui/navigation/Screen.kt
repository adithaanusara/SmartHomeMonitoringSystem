package com.example.smarthomeapp.ui.navigation

/**
 * Every destination in the authenticated navigation graph.
 *
 * Routes that require arguments provide a route(...) function,
 * so navigation paths do not need to be manually constructed elsewhere.
 */
sealed class Screen(
    val route: String
) {

    // Home dashboard
    data object Home : Screen(
        route = "home"
    )


    // Usage / reports screen
    data object Report : Screen(
        route = "report"
    )


    /**
     * Manual floor plan editor.
     *
     * The floorId is passed so the editor knows exactly
     * which floor is being edited.
     *
     * Example:
     * floorPlanEditor/floor123
     */
    data object FloorPlanEditor :
        Screen(
            route = "floorPlanEditor/{$ARG_FLOOR_ID}"
        ) {

        fun route(
            floorId: String
        ): String {

            return "floorPlanEditor/$floorId"
        }
    }


    /**
     * Existing floor details screen.
     *
     * Example:
     * floor/floor123
     */
    data object Floor :
        Screen(
            route = "floor/{$ARG_FLOOR_ID}"
        ) {

        fun route(
            floorId: String
        ): String {

            return "floor/$floorId"
        }
    }


    /**
     * Device details screen.
     *
     * Example:
     * device/device123
     */
    data object Device :
        Screen(
            route = "device/{$ARG_DEVICE_ID}"
        ) {

        fun route(
            deviceId: String
        ): String {

            return "device/$deviceId"
        }
    }


    /**
     * Device schedule screen.
     *
     * Example:
     * schedule/device123
     */
    data object Schedule :
        Screen(
            route = "schedule/{$ARG_DEVICE_ID}"
        ) {

        fun route(
            deviceId: String
        ): String {

            return "schedule/$deviceId"
        }
    }


    /**
     * Camera screen.
     *
     * Example:
     * camera/device123
     */
    data object Camera :
        Screen(
            route = "camera/{$ARG_DEVICE_ID}"
        ) {

        fun route(
            deviceId: String
        ): String {

            return "camera/$deviceId"
        }
    }


    companion object {

        const val ARG_FLOOR_ID =
            "floorId"

        const val ARG_DEVICE_ID =
            "deviceId"
    }
}