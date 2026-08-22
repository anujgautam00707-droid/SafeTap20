package com.safetap.app.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.safetap.app.navigation.Routes

@Composable
fun SafeTapScaffold(
    navController: NavHostController,
    content: @Composable (PaddingValues) -> Unit
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val showBottomBar = currentRoute in setOf(
        Routes.Home,
        Routes.Sos,
        Routes.Contacts,
        Routes.Settings
    )

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                SafeTapBottomBar(
                    navController = navController,
                    currentDestination = navBackStackEntry?.destination
                )
            }
        },
        content = content
    )
}
