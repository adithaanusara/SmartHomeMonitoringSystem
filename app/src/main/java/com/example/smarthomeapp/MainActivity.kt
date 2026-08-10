package com.example.smarthomeapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import com.example.smarthomeapp.ui.navigation.AppNavigation
import com.example.smarthomeapp.ui.theme.SmartHomeAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SmartHomeApp()
        }
    }
}

@Composable
private fun SmartHomeApp() {
    SmartHomeAppTheme {
        AppNavigation()
    }
}
