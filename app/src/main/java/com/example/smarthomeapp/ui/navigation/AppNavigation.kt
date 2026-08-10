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
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.smarthomeapp.ui.screens.HomeScreen
import com.example.smarthomeapp.ui.screens.LoginScreen
import com.example.smarthomeapp.viewmodel.AuthStatus
import com.example.smarthomeapp.viewmodel.AuthViewModel

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

    when (val status = authStatus) {
        AuthStatus.Loading -> LoadingScreen(modifier)

        AuthStatus.SignedOut -> LoginScreen(
            viewModel = authViewModel,
            modifier = modifier,
        )

        is AuthStatus.SignedIn -> AuthenticatedNavHost(
            displayName = status.displayName,
            onSignOut = authViewModel::signOut,
            modifier = modifier,
        )
    }
}

@Composable
private fun AuthenticatedNavHost(
    displayName: String,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        modifier = modifier,
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                displayName = displayName,
                onSignOut = onSignOut,
            )
        }

        // Floor, Device, Schedule, Camera and Report are registered as the dashboard work lands.
        // Screen.kt already defines their routes and argument keys.
    }
}

@Composable
private fun LoadingScreen(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}
