package com.safetap.app.di

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.safetap.app.ui.screens.auth.AuthViewModel
import com.safetap.app.ui.screens.home.HomeViewModel
import com.safetap.app.ui.screens.settings.SettingsViewModel
import com.safetap.app.ui.screens.sos.SosViewModel
import com.safetap.app.ui.screens.splash.SplashViewModel

object SafeTapViewModelFactory : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val authRepo = AppContainer.authRepository
        return when {
            modelClass.isAssignableFrom(AuthViewModel::class.java) ->
                AuthViewModel(authRepo) as T
            modelClass.isAssignableFrom(SplashViewModel::class.java) ->
                SplashViewModel(authRepo) as T
            modelClass.isAssignableFrom(SettingsViewModel::class.java) ->
                SettingsViewModel(authRepo) as T
            modelClass.isAssignableFrom(HomeViewModel::class.java) ->
                HomeViewModel(authRepo) as T
            modelClass.isAssignableFrom(SosViewModel::class.java) ->
                SosViewModel(AppContainer.sosCoordinator, authRepo) as T
            else -> throw IllegalArgumentException("Unknown ViewModel: ${modelClass.name}")
        }
    }
}
