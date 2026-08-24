package com.safetap.app.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sos
import androidx.compose.ui.graphics.vector.ImageVector
import com.safetap.app.R

data class BottomNavItem(
    val route: String,
    @StringRes val labelResId: Int,
    val icon: ImageVector
)

val mainBottomNavItems = listOf(
    BottomNavItem(Routes.Home, R.string.nav_home, Icons.Filled.Home),
    BottomNavItem(Routes.Sos, R.string.nav_sos, Icons.Filled.Sos),
    BottomNavItem(Routes.Contacts, R.string.nav_contacts, Icons.Filled.Contacts),
    BottomNavItem(Routes.Settings, R.string.nav_settings, Icons.Filled.Settings)
)
