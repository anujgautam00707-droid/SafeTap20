package com.safetap.app.di

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.safetap.app.ui.screens.auth.AuthViewModel
import com.safetap.app.ui.screens.contacts.TrustedContactsViewModel
import com.safetap.app.ui.screens.home.HomeViewModel
import com.safetap.app.ui.screens.settings.SettingsViewModel
import com.safetap.app.ui.screens.sos.SosViewModel
import com.safetap.app.ui.screens.splash.SplashViewModel

object SafeTapViewModelFactory : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val authRepository = AppContainer.authRepository

        return when {
            modelClass.isAssignableFrom(AuthViewModel::class.java) ->
                AuthViewModel(authRepository) as T

            modelClass.isAssignableFrom(SplashViewModel::class.java) ->
                SplashViewModel(authRepository) as T

            modelClass.isAssignableFrom(SettingsViewModel::class.java) ->
                SettingsViewModel(authRepository) as T

            modelClass.isAssignableFrom(HomeViewModel::class.java) ->
                HomeViewModel(
                    authRepository,
                    AppContainer.trustedContactsRepository,
                    AppContainer.appStatusRepository
                ) as T

            modelClass.isAssignableFrom(TrustedContactsViewModel::class.java) ->
                TrustedContactsViewModel(
                    AppContainer.trustedContactsRepository
                ) as T

            modelClass.isAssignableFrom(SosViewModel::class.java) ->
                SosViewModel(
                    AppContainer.sosCoordinator,
                    authRepository
                ) as T

            else -> throw IllegalArgumentException(
                "Unknown ViewModel: ${modelClass.name}"
            )
        }
    }
}