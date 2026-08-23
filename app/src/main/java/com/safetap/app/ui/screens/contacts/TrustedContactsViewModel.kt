package com.safetap.app.ui.screens.contacts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.safetap.app.data.contacts.TrustedContact
import com.safetap.app.data.contacts.TrustedContactsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TrustedContactsUiState(
    val contacts: List<TrustedContact> = emptyList(),
    val errorMessage: String? = null
)

class TrustedContactsViewModel(
    private val trustedContactsRepository: TrustedContactsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        TrustedContactsUiState(
            contacts = trustedContactsRepository.getCurrentContacts()
        )
    )

    val uiState: StateFlow<TrustedContactsUiState> =
        _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            trustedContactsRepository.contacts.collect { contacts ->
                _uiState.update { currentState ->
                    currentState.copy(contacts = contacts)
                }
            }
        }
    }

    fun addContact(
        name: String,
        relationship: String,
        phone: String
    ): Boolean {
        val result = trustedContactsRepository.addContact(
            name = name,
            relationship = relationship,
            phone = phone
        )

        _uiState.update { currentState ->
            currentState.copy(
                errorMessage = result.exceptionOrNull()?.message
            )
        }

        return result.isSuccess
    }

    fun removeContact(contactId: String): Boolean {
        val result = trustedContactsRepository.removeContact(contactId)

        _uiState.update { currentState ->
            currentState.copy(
                errorMessage = result.exceptionOrNull()?.message
            )
        }

        return result.isSuccess
    }

    fun clearError() {
        _uiState.update { currentState ->
            currentState.copy(errorMessage = null)
        }
    }

    fun onAddContactClicked() = Unit
}
