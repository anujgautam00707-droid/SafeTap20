package com.safetap.app.ui.screens.settings

import androidx.lifecycle.ViewModel
import com.safetap.app.data.auth.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class SettingsUiState(
    val userEmail: String = "",
    val notificationsEnabled: Boolean = true,
    val isSigningOut: Boolean = false,
    val showSignOutDialog: Boolean = false
)

class SettingsViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        val email = authRepository.currentUser?.email.orEmpty()
        _uiState.update { it.copy(userEmail = email) }
    }

    fun onShowSignOutDialog(show: Boolean) {
        _uiState.update { it.copy(showSignOutDialog = show) }
    }

    fun onSignOut(onLoggedOut: () -> Unit) {
        _uiState.update { it.copy(isSigningOut = true, showSignOutDialog = false) }
        authRepository.signOut()
        onLoggedOut()
    }
}

