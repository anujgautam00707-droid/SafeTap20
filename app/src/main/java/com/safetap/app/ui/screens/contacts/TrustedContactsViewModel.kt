package com.safetap.app.ui.screens.contacts

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class TrustedContactsUiState(
    val contacts: List<String> = emptyList()
)

class TrustedContactsViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(TrustedContactsUiState())
    val uiState: StateFlow<TrustedContactsUiState> = _uiState.asStateFlow()

    fun onAddContactClicked() {
        // TODO: Open contact picker and persist trusted contacts locally / remotely.
    }
}
