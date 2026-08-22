package com.safetap.app.ui.screens.sos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.safetap.app.data.auth.AuthRepository
import com.safetap.app.domain.sos.SosCoordinator
import com.safetap.app.domain.sos.model.SosError
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SosViewModel(
    private val sosCoordinator: SosCoordinator,
    private val authRepository: AuthRepository? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow<SosUiState>(SosUiState.Idle)
    val uiState: StateFlow<SosUiState> = _uiState.asStateFlow()

    private var countdownJob: Job? = null

    /**
     * Starts the SOS flow: checks permissions, runs 5-second countdown, and dispatches SOS.
     */
    fun startSos() {
        if (_uiState.value is SosUiState.Countdown || _uiState.value is SosUiState.Active) {
            return
        }

        _uiState.value = SosUiState.CheckingPermissions

        val permCheck = sosCoordinator.checkPermissions()
        if (permCheck.isFailure) {
            val error = permCheck.exceptionOrNull() as? SosError ?: SosError.PermissionDenied()
            _uiState.value = SosUiState.Error(error, error.message)
            return
        }

        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            _uiState.value = SosUiState.Countdown(5)

            val countdownResult = sosCoordinator.runCountdown(5) { sec ->
                _uiState.value = SosUiState.Countdown(sec)
            }

            if (countdownResult.isSuccess) {
                dispatchEmergencySos()
            } else {
                val error = countdownResult.exceptionOrNull() as? SosError
                if (error is SosError.Cancelled) {
                    _uiState.value = SosUiState.Cancelled
                } else if (error != null) {
                    _uiState.value = SosUiState.Error(error, error.message)
                }
            }
        }
    }

    /**
     * Cancels any active countdown or armed SOS alert.
     */
    fun cancelSos() {
        countdownJob?.cancel()
        countdownJob = null

        viewModelScope.launch {
            sosCoordinator.cancelSos()
            _uiState.value = SosUiState.Cancelled
        }
    }

    /**
     * Skips countdown and immediately dispatches the emergency SOS alert.
     */
    fun triggerImmediately() {
        countdownJob?.cancel()
        countdownJob = null

        viewModelScope.launch {
            val permCheck = sosCoordinator.checkPermissions()
            if (permCheck.isFailure) {
                val error = permCheck.exceptionOrNull() as? SosError ?: SosError.PermissionDenied()
                _uiState.value = SosUiState.Error(error, error.message)
                return@launch
            }

            dispatchEmergencySos()
        }
    }

    /**
     * Resets the SOS state back to Idle.
     */
    fun resetSos() {
        countdownJob?.cancel()
        countdownJob = null

        viewModelScope.launch {
            sosCoordinator.cancelSos()
            _uiState.value = SosUiState.Idle
        }
    }

    /**
     * Opens the device phone dialer with the designated emergency number.
     */
    fun openEmergencyDialer(emergencyNumber: String = "911"): Result<Unit> {
        val result = sosCoordinator.openEmergencyDialer(emergencyNumber)
        if (result.isFailure) {
            val error = result.exceptionOrNull() as? SosError
                ?: SosError.NoDialerApp("Failed to open emergency dialer.")
            _uiState.value = SosUiState.Error(error, error.message)
        }
        return result
    }

    /**
     * Compatibility hook for existing callers.
     */
    fun onSosPressed() {
        triggerImmediately()
    }

    private suspend fun dispatchEmergencySos() {
        _uiState.value = SosUiState.CollectingEmergencyData

        val userId = authRepository?.currentUser?.uid ?: "user_placeholder"
        val result = sosCoordinator.triggerSos(userId = userId)

        if (result.isSuccess) {
            val emergencyData = result.getOrThrow()
            _uiState.value = SosUiState.Active(emergencyData)
        } else {
            val error = result.exceptionOrNull() as? SosError
                ?: SosError.UnexpectedError(result.exceptionOrNull())
            _uiState.value = SosUiState.Error(error, error.message)
        }
    }

    override fun onCleared() {
        super.onCleared()
        countdownJob?.cancel()
    }
}
