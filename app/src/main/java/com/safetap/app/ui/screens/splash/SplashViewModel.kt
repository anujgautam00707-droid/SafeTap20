package com.safetap.app.ui.screens.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.safetap.app.data.auth.AuthRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface SplashNavigationState {
    data object Loading : SplashNavigationState
    data object NavigateToHome : SplashNavigationState
    data object NavigateToLogin : SplashNavigationState
}

class SplashViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _navigationState = MutableStateFlow<SplashNavigationState>(SplashNavigationState.Loading)
    val navigationState: StateFlow<SplashNavigationState> = _navigationState.asStateFlow()

    init {
        checkAuthSession()
    }

    private fun checkAuthSession() {
        viewModelScope.launch {
            val startTime = System.currentTimeMillis()
            val user = authRepository.awaitSession()
            val elapsed = System.currentTimeMillis() - startTime
            val remainingDelay = (MIN_SPLASH_DELAY_MS - elapsed).coerceAtLeast(0L)
            if (remainingDelay > 0) {
                delay(remainingDelay)
            }

            if (user != null) {
                _navigationState.value = SplashNavigationState.NavigateToHome
            } else {
                _navigationState.value = SplashNavigationState.NavigateToLogin
            }
        }
    }

    companion object {
        private const val MIN_SPLASH_DELAY_MS = 1_000L
    }
}

