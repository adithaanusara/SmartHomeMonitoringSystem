package com.example.smarthomeapp.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Power
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.smarthomeapp.data.model.DeviceType

/** One glyph per device profile, so a device is recognisable before its label is read. */
fun DeviceType.icon(): ImageVector = when (this) {
    DeviceType.OUTLET -> Icons.Filled.Power
    DeviceType.MULTI_SWITCH -> Icons.Filled.Tune
    DeviceType.LIGHT -> Icons.Outlined.Lightbulb
    DeviceType.HAZARD -> Icons.Filled.LocalFireDepartment
    DeviceType.CAMERA -> Icons.Filled.Videocam
}

fun DeviceType.label(): String = when (this) {
    DeviceType.OUTLET -> "Outlet"
    DeviceType.MULTI_SWITCH -> "Multi-switch"
    DeviceType.LIGHT -> "Light"
    DeviceType.HAZARD -> "Hazard appliance"
    DeviceType.CAMERA -> "Camera"
}
