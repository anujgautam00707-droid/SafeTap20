package com.safetap.app.ui.screens.sos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.safetap.app.data.auth.AuthRepository
import com.safetap.app.data.contacts.TrustedContactsRepository
import com.safetap.app.di.AppContainer
import com.safetap.app.domain.sos.SosCoordinator
import com.safetap.app.domain.sos.model.LocationTrackingState
import com.safetap.app.domain.sos.model.SosError
import com.safetap.app.domain.sos.services.SmsRecipientStatus
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SosViewModel(
    private val sosCoordinator: SosCoordinator,
    private val authRepository: AuthRepository? = null,
    private val trustedContactsRepository: TrustedContactsRepository =
        AppContainer.trustedContactsRepository
) : ViewModel() {

    private val _uiState =
        MutableStateFlow<SosUiState>(SosUiState.Idle)

    val uiState: StateFlow<SosUiState> =
        _uiState.asStateFlow()

    private val _batteryPercentage =
        MutableStateFlow<Int?>(null)

    val batteryPercentage: StateFlow<Int?> =
        _batteryPercentage.asStateFlow()

    val smsDeliveryStatuses: StateFlow<List<SmsRecipientStatus>> =
        sosCoordinator.smsDeliveryStatuses

    val liveTrackingState: StateFlow<LocationTrackingState>?
        get() = sosCoordinator.trackingState

    private val _contactsCount =
        MutableStateFlow(
            trustedContactsRepository
                .getCurrentContacts()
                .size
        )

    val contactsCount: StateFlow<Int> =
        _contactsCount.asStateFlow()

    private var countdownJob: Job? = null
    private var delayedCallJob: Job? = null
    private var sosDispatchJob: Job? = null
    private var cancelSosJob: Job? = null

    init {
        refreshBatteryPercentage()
        recoverActiveSos()
        syncPendingMetadata()
        observeContactsCount()
    }

    private fun recoverActiveSos() {
        val activeSos = sosCoordinator.getActiveSos()

        if (activeSos != null) {
            _batteryPercentage.value =
                activeSos.batteryPercentage

            _uiState.value =
                SosUiState.Active(activeSos)
        }
    }

    private fun syncPendingMetadata() {
        viewModelScope.launch {
            sosCoordinator.syncPendingMetadata()
        }
    }

    private fun observeContactsCount() {
        viewModelScope.launch {
            trustedContactsRepository.contacts.collect { contacts ->
                _contactsCount.value = contacts.size
            }
        }
    }

    fun refreshBatteryPercentage() {
        viewModelScope.launch {
            runCatching {
                sosCoordinator.getBatteryPercentage()
            }
                .getOrNull()
                ?.takeIf { percentage ->
                    percentage in
                            MINIMUM_BATTERY_PERCENTAGE..
                            MAXIMUM_BATTERY_PERCENTAGE
                }
                ?.let { percentage ->
                    _batteryPercentage.value = percentage
                }
        }
    }

    fun startSos(
        bypassPermissionCheck: Boolean = false
    ) {
        if (
            _uiState.value is SosUiState.Countdown ||
            _uiState.value is SosUiState.Active ||
            _uiState.value ==
            SosUiState.CollectingEmergencyData
        ) {
            return
        }

        cancelPendingJobs()
        refreshBatteryPercentage()

        if (
            !bypassPermissionCheck &&
            !sosCoordinator.hasRequiredPermissions()
        ) {
            _uiState.value =
                SosUiState.CheckingPermissions

            val missing =
                sosCoordinator.getMissingPermissions()

            _uiState.value =
                SosUiState.PermissionsRequired(missing)

            return
        }

        countdownJob = viewModelScope.launch {
            _uiState.value =
                SosUiState.Countdown(
                    secondsRemaining =
                        SOS_COUNTDOWN_SECONDS
                )

            val countdownResult =
                sosCoordinator.runCountdown(
                    durationSeconds =
                        SOS_COUNTDOWN_SECONDS
                ) { secondsRemaining ->

                    _uiState.value =
                        SosUiState.Countdown(
                            secondsRemaining =
                                secondsRemaining
                        )
                }

            if (countdownResult.isSuccess) {
                dispatchEmergencySos()
                return@launch
            }

            val error =
                countdownResult.exceptionOrNull()
                        as? SosError

            when (error) {
                is SosError.Cancelled -> {
                    _uiState.value =
                        SosUiState.Cancelled
                }

                null -> Unit

                else -> {
                    showFailure(
                        throwable = error,
                        fallback = error
                    )
                }
            }
        }
    }

    fun triggerImmediately() {
        cancelPendingJobs()
        refreshBatteryPercentage()

        viewModelScope.launch {
            val permissionCheck =
                sosCoordinator.checkPermissions()

            if (permissionCheck.isFailure) {
                showFailure(
                    throwable =
                        permissionCheck.exceptionOrNull(),
                    fallback =
                        SosError.PermissionDenied()
                )
                return@launch
            }

            dispatchEmergencySos()
        }
    }

    fun onPermissionsResult(allGranted: Boolean) {
        if (allGranted) {
            startSos(
                bypassPermissionCheck = true
            )
        } else {
            _uiState.value =
                SosUiState.Error(
                    error =
                        SosError.PermissionDenied(),
                    message =
                        "Required emergency permissions were not granted."
                )
        }
    }

    fun cancelSos() {
        val wasCountdown =
            _uiState.value is SosUiState.Countdown ||
                    _uiState.value ==
                    SosUiState.CheckingPermissions

        cancelPendingJobs()

        if (wasCountdown) {
            _uiState.value =
                SosUiState.Idle
            return
        }

        cancelSosJob =
            viewModelScope.launch {

                val result =
                    sosCoordinator.cancelSos()

                if (result.isSuccess) {
                    _uiState.value =
                        SosUiState.Cancelled

                    refreshBatteryPercentage()
                } else {
                    val ex =
                        result.exceptionOrNull()

                    if (
                        ex !is CancellationException &&
                        ex !is SosError.Cancelled
                    ) {
                        showFailure(
                            throwable = ex
                        )
                    }
                }
            }
    }

    fun resetSos() {
        cancelPendingJobs()

        _uiState.value =
            SosUiState.Idle

        viewModelScope.launch {
            sosCoordinator.cancelSos()
            refreshBatteryPercentage()
        }
    }

    fun openEmergencyDialer(
        emergencyNumber: String =
            DEFAULT_EMERGENCY_NUMBER
    ): Result<Unit> {

        val result =
            sosCoordinator.openEmergencyDialer(
                emergencyNumber =
                    emergencyNumber
            )

        if (result.isFailure) {
            showFailure(
                throwable =
                    result.exceptionOrNull(),
                fallback =
                    SosError.NoDialerApp(
                        "Failed to open emergency dialer."
                    )
            )
        }

        return result
    }

    fun onSosPressed() {
        startSos()
    }

    private fun dispatchEmergencySos() {
        cancelPendingJobs()

        sosDispatchJob =
            viewModelScope.launch {

                try {
                    _uiState.value =
                        SosUiState.CollectingEmergencyData

                    val userId =
                        authRepository
                            ?.currentUser
                            ?.uid
                            ?: "user_${
                                UUID.randomUUID()
                                    .toString()
                                    .take(8)
                            }"

                    val result =
                        sosCoordinator.triggerSos(
                            userId = userId
                        )

                    if (result.isFailure) {
                        val ex =
                            result.exceptionOrNull()

                        if (
                            ex is CancellationException ||
                            ex is SosError.Cancelled
                        ) {
                            _uiState.value =
                                SosUiState.Cancelled
                        } else {
                            showFailure(
                                throwable = ex
                            )
                        }

                        return@launch
                    }

                    val emergencyData =
                        result.getOrThrow()

                    _batteryPercentage.value =
                        emergencyData.batteryPercentage

                    _uiState.value =
                        SosUiState.Active(
                            emergencyData
                        )

                    schedulePrimaryContactCall()

                } catch (e: CancellationException) {

                    _uiState.value =
                        SosUiState.Cancelled
                }
            }
    }

    private fun schedulePrimaryContactCall() {
        delayedCallJob?.cancel()

        delayedCallJob =
            viewModelScope.launch {

                delay(
                    PRIMARY_CONTACT_CALL_DELAY_MILLIS
                )

                if (
                    _uiState.value !is
                            SosUiState.Active
                ) {
                    return@launch
                }

                val callResult =
                    sosCoordinator.callPrimaryTrustedContact()

                if (callResult.isFailure) {
                    val error =
                        callResult.exceptionOrNull()

                    android.util.Log.e(
                        "SosViewModel",
                        "Failed to call primary contact: ${error?.message}"
                    )
                }
            }
    }

    private fun cancelPendingJobs() {
        countdownJob?.cancel()
        countdownJob = null

        sosDispatchJob?.cancel()
        sosDispatchJob = null

        delayedCallJob?.cancel()
        delayedCallJob = null

        cancelSosJob?.cancel()
        cancelSosJob = null
    }

    private fun showFailure(
        throwable: Throwable?,
        fallback: SosError =
            SosError.UnexpectedError(throwable)
    ) {
        val error =
            throwable as? SosError ?: fallback

        _uiState.value =
            SosUiState.Error(
                error = error,
                message = error.message
            )
    }

    override fun onCleared() {
        cancelPendingJobs()
        super.onCleared()
    }

    private companion object {
        const val SOS_COUNTDOWN_SECONDS = 5

        const val PRIMARY_CONTACT_CALL_DELAY_MILLIS =
            30_000L

        const val MINIMUM_BATTERY_PERCENTAGE = 0

        const val MAXIMUM_BATTERY_PERCENTAGE = 100

        const val DEFAULT_EMERGENCY_NUMBER = "112"

        const val FALLBACK_USER_ID =
            "user_placeholder"
    }
}