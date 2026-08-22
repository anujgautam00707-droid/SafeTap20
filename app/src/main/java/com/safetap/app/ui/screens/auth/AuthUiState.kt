package com.safetap.app.ui.screens.auth

data class AuthUiState(
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val isPasswordVisible: Boolean = false,
    val isConfirmPasswordVisible: Boolean = false,
    val isLoading: Boolean = false,
    val emailError: String? = null,
    val passwordError: String? = null,
    val confirmPasswordError: String? = null
)

sealed interface AuthEvent {
    data class Snackbar(val message: String, val isError: Boolean = false) : AuthEvent
    data object NavigateHome : AuthEvent
    data object PasswordResetSent : AuthEvent
}

