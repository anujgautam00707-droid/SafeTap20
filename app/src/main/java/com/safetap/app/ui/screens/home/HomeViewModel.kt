package com.safetap.app.ui.screens.home

import androidx.lifecycle.ViewModel
import com.safetap.app.data.auth.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class HomeUiState(
    val userDisplayName: String = "there",
    val userEmail: String = ""
)

class HomeViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadUserProfile()
    }

    private fun loadUserProfile() {
        val user = authRepository.currentUser
        val email = user?.email.orEmpty()
        val displayName = user?.displayName?.takeIf { it.isNotBlank() }
            ?: email.substringBefore("@").takeIf { it.isNotBlank() }
            ?: "there"
        _uiState.value = HomeUiState(
            userDisplayName = displayName,
            userEmail = email
        )
    }
}

