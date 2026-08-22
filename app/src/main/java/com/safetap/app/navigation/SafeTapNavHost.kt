package com.safetap.app.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.safetap.app.ui.components.SafeTapScaffold
import com.safetap.app.ui.screens.auth.ForgotPasswordScreen
import com.safetap.app.ui.screens.auth.SignUpScreen
import com.safetap.app.ui.screens.contacts.TrustedContactsScreen
import com.safetap.app.ui.screens.home.HomeScreen
import com.safetap.app.ui.screens.login.LoginScreen
import com.safetap.app.ui.screens.settings.SettingsScreen
import com.safetap.app.ui.screens.sos.SosScreen
import com.safetap.app.ui.screens.splash.SplashScreen

@Composable
fun SafeTapNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    SafeTapScaffold(navController = navController) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.Splash,
            modifier = modifier.padding(innerPadding)
        ) {
            composable(Routes.Splash) {
                SplashScreen(
                    onNavigateToHome = {
                        navController.navigate(Routes.Home) {
                            popUpTo(Routes.Splash) { inclusive = true }
                        }
                    },
                    onNavigateToLogin = {
                        navController.navigate(Routes.Login) {
                            popUpTo(Routes.Splash) { inclusive = true }
                        }
                    }
                )
            }
            composable(Routes.Login) {
                LoginScreen(
                    onLoginSuccess = {
                        navController.navigate(Routes.Home) {
                            popUpTo(Routes.Login) { inclusive = true }
                        }
                    },
                    onNavigateToSignUp = {
                        navController.navigate(Routes.SignUp)
                    },
                    onNavigateToForgotPassword = {
                        navController.navigate(Routes.ForgotPassword)
                    }
                )
            }
            composable(Routes.SignUp) {
                SignUpScreen(
                    onSignUpSuccess = {
                        navController.navigate(Routes.Home) {
                            popUpTo(Routes.Login) { inclusive = true }
                        }
                    },
                    onNavigateToLogin = {
                        navController.popBackStack()
                    }
                )
            }
            composable(Routes.ForgotPassword) {
                ForgotPasswordScreen(
                    onNavigateBackToLogin = {
                        navController.popBackStack()
                    }
                )
            }
            composable(Routes.Home) {
                HomeScreen(
                    onOpenSos = { navController.navigate(Routes.Sos) },
                    onNavigateToContacts = { navController.navigate(Routes.Contacts) }
                )
            }
            composable(Routes.Sos) {
                SosScreen()
            }
            composable(Routes.Contacts) {
                TrustedContactsScreen()
            }
            composable(Routes.Settings) {
                SettingsScreen(
                    onLoggedOut = {
                        navController.navigate(Routes.Login) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }
        }
    }
}

