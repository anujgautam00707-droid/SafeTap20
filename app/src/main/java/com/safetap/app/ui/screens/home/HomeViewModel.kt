package com.safetap.app.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.safetap.app.data.auth.AuthRepository
import com.safetap.app.data.contacts.TrustedContactsRepository
import com.safetap.app.data.status.AppStatusRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeUiState(
    val userDisplayName: String = "there",
    val userEmail: String = "",
    val contactsCount: Int = 0,
    val safeTapProtectedAt: Long? = null,
    val locationSynchronizedAt: Long? = null,
    val contactsLinkedAt: Long? = null,
    val currentTime: Long = System.currentTimeMillis()
)

class HomeViewModel(
    private val authRepository: AuthRepository,
    private val trustedContactsRepository: TrustedContactsRepository,
    private val appStatusRepository: AppStatusRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadUserProfile()
        observeContactsCount()
        observeAppStatus()
        startClock()
    }

    private fun loadUserProfile() {
        val user = authRepository.currentUser
        val email = user?.email.orEmpty()
        val displayName = user?.displayName?.takeIf { it.isNotBlank() }
            ?: email.substringBefore("@").takeIf { it.isNotBlank() }
            ?: "there"
        _uiState.update { currentState ->
            currentState.copy(
                userDisplayName = displayName,
                userEmail = email
            )
        }
    }

    private fun observeContactsCount() {
        viewModelScope.launch {
            trustedContactsRepository.contacts.collect { contacts ->
                _uiState.update { currentState ->
                    currentState.copy(contactsCount = contacts.size)
                }
            }
        }
    }

    private fun observeAppStatus() {
        viewModelScope.launch {
            combine(
                appStatusRepository.safeTapProtectedAt,
                appStatusRepository.locationSynchronizedAt,
                appStatusRepository.contactsLinkedAt
            ) { protectedAt, locationAt, contactsAt ->
                Triple(protectedAt, locationAt, contactsAt)
            }.collect { (protectedAt, locationAt, contactsAt) ->
                _uiState.update { currentState ->
                    currentState.copy(
                        safeTapProtectedAt = protectedAt,
                        locationSynchronizedAt = locationAt,
                        contactsLinkedAt = contactsAt
                    )
                }
            }
        }
    }

    private fun startClock() {
        viewModelScope.launch {
            while (true) {
                delay(30_000L) // Refresh every 30 seconds for better relative time precision
                _uiState.update { currentState ->
                    currentState.copy(currentTime = System.currentTimeMillis())
                }
            }
        }
    }
}
