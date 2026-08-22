package com.safetap.app.ui.screens.sos

import com.safetap.app.domain.sos.model.EmergencyData
import com.safetap.app.domain.sos.model.SosError

sealed interface SosUiState {
    object Idle : SosUiState
    object CheckingPermissions : SosUiState
    data class Countdown(val secondsRemaining: Int = 5) : SosUiState
    object CollectingEmergencyData : SosUiState
    data class ReadyToSend(val emergencyData: EmergencyData) : SosUiState
    data class Active(val emergencyData: EmergencyData) : SosUiState
    object Cancelled : SosUiState
    data class Error(val error: SosError, val message: String = error.message) : SosUiState
}
