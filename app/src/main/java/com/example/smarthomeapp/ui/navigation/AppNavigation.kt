package com.example.smarthomeapp.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.smarthomeapp.ui.floorplan.FloorPlanEditorScreen
import com.example.smarthomeapp.ui.screens.DeviceScreen
import com.example.smarthomeapp.ui.screens.FloorScreen
import com.example.smarthomeapp.ui.screens.HomeScreen
import com.example.smarthomeapp.ui.screens.LoginScreen
import com.example.smarthomeapp.ui.screens.ReportScreen
import com.example.smarthomeapp.ui.screens.ScheduleScreen
import com.example.smarthomeapp.viewmodel.AuthStatus
import com.example.smarthomeapp.viewmodel.AuthViewModel
import com.example.smarthomeapp.viewmodel.HomeViewModel


@Composable
fun AppNavigation(
    modifier: Modifier = Modifier,
    authViewModel: AuthViewModel = viewModel(),
) {

    val authStatus by authViewModel.authStatus
        .collectAsStateWithLifecycle()


    when (authStatus) {

        AuthStatus.Loading -> {
            LoadingScreen(modifier)
        }


        AuthStatus.SignedOut -> {

            LoginScreen(
                viewModel = authViewModel,
                modifier = modifier
            )

        }


        is AuthStatus.SignedIn -> {

            AuthenticatedNavHost(
                onSignOut = authViewModel::signOut,
                modifier = modifier
            )

        }
    }
}



@Composable
private fun AuthenticatedNavHost(
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier,
) {

    val navController = rememberNavController()

    val homeViewModel: HomeViewModel = viewModel()


    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        modifier = modifier
    ) {


        // HOME
        composable(Screen.Home.route) {


            HomeScreen(

                viewModel = homeViewModel,


                onOpenFloor = { floorId ->

                    navController.navigate(
                        Screen.Floor.route(floorId)
                    )

                },


                onOpenFloorEditor = { floorId ->

                    navController.navigate(
                        Screen.FloorPlanEditor.route(floorId)
                    )

                },


                onOpenReport = {

                    navController.navigate(
                        Screen.Report.route
                    )

                },


                onSignOut = onSignOut

            )

        }




        // FLOOR PLAN EDITOR
        composable(

            route = Screen.FloorPlanEditor.route,

            arguments = listOf(

                navArgument(Screen.ARG_FLOOR_ID) {

                    type = NavType.StringType

                }

            )

        ) { entry ->


            val floorId =
                entry.requireArg(
                    Screen.ARG_FLOOR_ID
                )


            FloorPlanEditorScreen(

                floorId = floorId,

                onBack = {
                    navController.popBackStack()
                },

                onSave = { rooms ->

                    // next step Firebase save
                    navController.popBackStack()

                }

            )

        }





        // FLOOR SCREEN
        composable(

            route = Screen.Floor.route,

            arguments = listOf(

                navArgument(Screen.ARG_FLOOR_ID) {

                    type = NavType.StringType

                }

            )

        ) { entry ->


            val floorId =
                entry.requireArg(
                    Screen.ARG_FLOOR_ID
                )


            FloorScreen(

                floorId = floorId,

                viewModel = homeViewModel,


                onBack = {

                    navController.popBackStack()

                },


                onOpenDevice = { deviceId ->


                    navController.navigate(

                        Screen.Device.route(deviceId)

                    )

                },


                onEditFloor = {


                    navController.navigate(

                        Screen.FloorPlanEditor.route(floorId)

                    )

                }

            )

        }





        // DEVICE
        composable(

            route = Screen.Device.route,

            arguments = listOf(

                navArgument(Screen.ARG_DEVICE_ID) {

                    type = NavType.StringType

                }

            )

        ) { entry ->


            DeviceScreen(

                deviceId =
                    entry.requireArg(
                        Screen.ARG_DEVICE_ID
                    ),


                viewModel = homeViewModel,


                onBack = {

                    navController.popBackStack()

                },


                onOpenSchedule = { deviceId ->


                    navController.navigate(

                        Screen.Schedule.route(deviceId)

                    )

                }

            )

        }





        // SCHEDULE
        composable(

            route = Screen.Schedule.route,

            arguments = listOf(

                navArgument(Screen.ARG_DEVICE_ID) {

                    type = NavType.StringType

                }

            )

        ) { entry ->


            ScheduleScreen(

                deviceId =
                    entry.requireArg(
                        Screen.ARG_DEVICE_ID
                    ),


                viewModel = homeViewModel,


                onBack = {

                    navController.popBackStack()

                }

            )

        }





        // REPORT
        composable(Screen.Report.route) {


            ReportScreen(

                onBack = {

                    navController.popBackStack()

                }

            )

        }

    }

}




private fun NavBackStackEntry.requireArg(
    key: String
): String {

    return checkNotNull(
        arguments?.getString(key)
    ) {

        "Missing route argument '$key'"

    }

}





@Composable
private fun LoadingScreen(
    modifier: Modifier = Modifier
) {


    Box(

        modifier = modifier.fillMaxSize(),

        contentAlignment = Alignment.Center

    ) {


        CircularProgressIndicator()

    }

}