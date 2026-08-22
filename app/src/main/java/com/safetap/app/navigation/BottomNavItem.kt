package com.safetap.app.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sos
import androidx.compose.ui.graphics.vector.ImageVector

data class BottomNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector
)

val mainBottomNavItems = listOf(
    BottomNavItem(Routes.Home, "Home", Icons.Filled.Home),
    BottomNavItem(Routes.Sos, "SOS", Icons.Filled.Sos),
    BottomNavItem(Routes.Contacts, "Contacts", Icons.Filled.Contacts),
    BottomNavItem(Routes.Settings, "Settings", Icons.Filled.Settings)
)
