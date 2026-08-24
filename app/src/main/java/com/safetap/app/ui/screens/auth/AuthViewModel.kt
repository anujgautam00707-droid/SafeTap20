package com.safetap.app.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.safetap.app.data.auth.AuthOutcome
import com.safetap.app.data.auth.AuthRepository
import com.safetap.app.domain.auth.AuthValidator
import com.safetap.app.util.UiText
import com.safetap.app.R
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AuthViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _events = Channel<AuthEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    fun onEmailChanged(value: String) {
        _uiState.update { it.copy(email = value, emailError = null) }
    }

    fun onPasswordChanged(value: String) {
        _uiState.update { it.copy(password = value, passwordError = null) }
    }

    fun onConfirmPasswordChanged(value: String) {
        _uiState.update { it.copy(confirmPassword = value, confirmPasswordError = null) }
    }

    fun togglePasswordVisibility() {
        _uiState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
    }

    fun toggleConfirmPasswordVisibility() {
        _uiState.update { it.copy(isConfirmPasswordVisible = !it.isConfirmPasswordVisible) }
    }

    fun clearErrors() {
        _uiState.update {
            it.copy(
                emailError = null,
                passwordError = null,
                confirmPasswordError = null
            )
        }
    }

    fun resetForm() {
        _uiState.value = AuthUiState()
    }

    fun signIn() {
        val state = _uiState.value
        val emailError = AuthValidator.emailError(state.email)
        val passwordError = AuthValidator.passwordError(state.password)
        if (emailError != null || passwordError != null) {
            _uiState.update {
                it.copy(emailError = emailError, passwordError = passwordError)
            }
            return
        }
        authenticate(successMessage = UiText.StringResource(R.string.welcome_back)) {
            authRepository.signIn(state.email, state.password)
        }
    }

    fun signUp() {
        val state = _uiState.value
        val emailError = AuthValidator.emailError(state.email)
        val passwordError = AuthValidator.passwordError(state.password, isNewPassword = true)
        val confirmError = AuthValidator.confirmPasswordError(state.password, state.confirmPassword)
        if (emailError != null || passwordError != null || confirmError != null) {
            _uiState.update {
                it.copy(
                    emailError = emailError,
                    passwordError = passwordError,
                    confirmPasswordError = confirmError
                )
            }
            return
        }
        authenticate(successMessage = UiText.StringResource(R.string.account_created)) {
            authRepository.signUp(state.email, state.password)
        }
    }

    fun sendPasswordReset() {
        val state = _uiState.value
        val emailError = AuthValidator.emailError(state.email)
        if (emailError != null) {
            _uiState.update { it.copy(emailError = emailError) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            when (val result = authRepository.sendPasswordReset(state.email)) {
                AuthOutcome.Success -> {
                    _uiState.update { it.copy(isLoading = false) }
                    _events.send(
                        AuthEvent.Snackbar(
                            message = UiText.StringResource(R.string.password_reset_sent),
                            isError = false
                        )
                    )
                    _events.send(AuthEvent.PasswordResetSent)
                }
                is AuthOutcome.Failure -> {
                    _uiState.update { it.copy(isLoading = false) }
                    _events.send(AuthEvent.Snackbar(result.message, isError = true))
                }
            }
        }
    }

    private fun authenticate(
        successMessage: UiText,
        request: suspend () -> AuthOutcome
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            when (val result = request()) {
                AuthOutcome.Success -> {
                    _uiState.update { it.copy(isLoading = false) }
                    _events.send(
                        AuthEvent.Snackbar(
                            message = successMessage,
                            isError = false
                        )
                    )
                    _events.send(AuthEvent.NavigateHome)
                }
                is AuthOutcome.Failure -> {
                    _uiState.update { it.copy(isLoading = false) }
                    _events.send(AuthEvent.Snackbar(result.message, isError = true))
                }
            }
        }
    }
}

