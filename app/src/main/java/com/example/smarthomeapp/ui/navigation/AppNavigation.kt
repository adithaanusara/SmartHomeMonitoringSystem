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
import com.example.smarthomeapp.ui.screens.DeviceScreen
import com.example.smarthomeapp.ui.screens.FloorScreen
import com.example.smarthomeapp.ui.screens.HomeScreen
import com.example.smarthomeapp.ui.screens.LoginScreen
import com.example.smarthomeapp.viewmodel.AuthStatus
import com.example.smarthomeapp.viewmodel.AuthViewModel
import com.example.smarthomeapp.viewmodel.HomeViewModel

/**
 * Root of the UI.
 *
 * Login and the authenticated graph are separate trees rather than two destinations in one
 * `NavHost`. Swapping the whole tree on sign-out discards the authenticated back stack outright,
 * so there is no way to press Back into a dashboard belonging to a user who has just signed out.
 */
@Composable
fun AppNavigation(
    modifier: Modifier = Modifier,
    authViewModel: AuthViewModel = viewModel(),
) {
    val authStatus by authViewModel.authStatus.collectAsStateWithLifecycle()

    when (authStatus) {
        AuthStatus.Loading -> LoadingScreen(modifier)

        AuthStatus.SignedOut -> LoginScreen(
            viewModel = authViewModel,
            modifier = modifier,
        )

        is AuthStatus.SignedIn -> AuthenticatedNavHost(
            onSignOut = authViewModel::signOut,
            modifier = modifier,
        )
    }
}

@Composable
private fun AuthenticatedNavHost(
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val navController = rememberNavController()

    // Hoisted above the NavHost so the dashboard, the floor plan and the device screen share one
    // set of database listeners instead of opening three against the same paths.
    val homeViewModel: HomeViewModel = viewModel()

    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        modifier = modifier,
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                viewModel = homeViewModel,
                onOpenFloor = { floorId -> navController.navigate(Screen.Floor.route(floorId)) },
                onOpenReport = { navController.navigate(Screen.Report.route) },
                onSignOut = onSignOut,
            )
        }

        composable(
            route = Screen.Floor.route,
            arguments = listOf(navArgument(Screen.ARG_FLOOR_ID) { type = NavType.StringType }),
        ) { entry ->
            FloorScreen(
                floorId = entry.requireArg(Screen.ARG_FLOOR_ID),
                viewModel = homeViewModel,
                onBack = navController::popBackStack,
                onOpenDevice = { id -> navController.navigate(Screen.Device.route(id)) },
            )
        }

        composable(
            route = Screen.Device.route,
            arguments = listOf(navArgument(Screen.ARG_DEVICE_ID) { type = NavType.StringType }),
        ) { entry ->
            DeviceScreen(
                deviceId = entry.requireArg(Screen.ARG_DEVICE_ID),
                viewModel = homeViewModel,
                onBack = navController::popBackStack,
                onOpenSchedule = { id -> navController.navigate(Screen.Schedule.route(id)) },
            )
        }

        // Schedule and Report land with the next workstream; their routes already exist in Screen.
    }
}

/** Route arguments are declared non-null in the graph, so a missing one is a wiring bug. */
private fun NavBackStackEntry.requireArg(key: String): String =
    checkNotNull(arguments?.getString(key)) { "Missing route argument '$key'" }

@Composable
private fun LoadingScreen(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}
