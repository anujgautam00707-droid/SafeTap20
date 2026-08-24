package com.safetap.app.ui.screens.auth

import com.safetap.app.util.UiText

data class AuthUiState(
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val isPasswordVisible: Boolean = false,
    val isConfirmPasswordVisible: Boolean = false,
    val isLoading: Boolean = false,
    val emailError: UiText? = null,
    val passwordError: UiText? = null,
    val confirmPasswordError: UiText? = null
)

sealed interface AuthEvent {
    data class Snackbar(val message: UiText, val isError: Boolean = false) : AuthEvent
    data object NavigateHome : AuthEvent
    data object PasswordResetSent : AuthEvent
}

