package com.safetap.app.domain.sos

import com.safetap.app.data.contacts.TrustedContactsRepository
import com.safetap.app.data.sos.LocalSosDataSource
import com.safetap.app.data.sos.SosRemoteDataSource
import com.safetap.app.data.status.AppStatusRepository
import com.safetap.app.domain.sos.model.EmergencyData
import com.safetap.app.domain.sos.model.LocationTrackingState
import com.safetap.app.domain.sos.model.SosError
import com.safetap.app.domain.sos.model.SosStatus
import com.safetap.app.domain.sos.services.BatteryProvider
import com.safetap.app.domain.sos.services.EmergencyCallManager
import com.safetap.app.domain.sos.services.EmergencyNotificationManager
import com.safetap.app.domain.sos.services.EmergencySmsSender
import com.safetap.app.domain.sos.services.LocationProvider
import com.safetap.app.domain.sos.services.LocationTrackingManager
import com.safetap.app.domain.sos.services.PermissionChecker
import com.safetap.app.domain.sos.services.SmsRecipientStatus
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext

class SosCoordinator(
    private val permissionChecker: PermissionChecker,
    private val locationProvider: LocationProvider,
    private val batteryProvider: BatteryProvider,
    private val notificationManager: EmergencyNotificationManager,
    private val callManager: EmergencyCallManager,
    private val emergencySmsSender: EmergencySmsSender,
    private val trustedContactsRepository: TrustedContactsRepository,
    private val appStatusRepository: AppStatusRepository,
    private val remoteDataSource: SosRemoteDataSource,
    private val locationTrackingManager: LocationTrackingManager? = null,
    private val localSosDataSource: LocalSosDataSource,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    private var currentActiveSosId: String? = null

    init {
        // Recover any active SOS session from local persistence (e.g. after process death)
        currentActiveSosId = localSosDataSource.getActiveSos()?.sosId
    }

    val smsDeliveryStatuses: StateFlow<List<SmsRecipientStatus>> =
        emergencySmsSender.deliveryStatuses

    val trackingState: StateFlow<LocationTrackingState>?
        get() = locationTrackingManager?.trackingState

    fun hasRequiredPermissions(): Boolean = permissionChecker.hasRequiredPermissions()

    fun getMissingPermissions(): List<String> = permissionChecker.getMissingPermissions()

    fun checkPermissions(): Result<Unit> {
        return Result.success(Unit)
    }

    suspend fun getBatteryPercentage(): Int = withContext(ioDispatcher) {
        batteryProvider.getBatteryPercentage()
    }

    suspend fun runCountdown(
        durationSeconds: Int = 5,
        onTick: suspend (Int) -> Unit
    ): Result<Unit> = withContext(ioDispatcher) {
        try {
            for (secondsRemaining in durationSeconds downTo 1) {
                onTick(secondsRemaining)
                delay(1_000L)
            }

            Result.success(Unit)
        } catch (exception: CancellationException) {
            Result.failure(SosError.Cancelled())
        } catch (exception: Exception) {
            Result.failure(SosError.UnexpectedError(exception))
        }
    }

    suspend fun triggerSos(
        userId: String = "anonymous_user",
        emergencyMessage: String? = null
    ): Result<EmergencyData> = withContext(ioDispatcher) {
        var generatedSosId: String? = null
        try {
            val permissionResult = checkPermissions()

            if (permissionResult.isFailure) {
                return@withContext Result.failure(
                    permissionResult.exceptionOrNull()
                        ?: SosError.PermissionDenied()
                )
            }

            val trustedContacts =
                trustedContactsRepository.getCurrentContacts()

            if (trustedContacts.isEmpty()) {
                return@withContext Result.failure(
                    SosError.UnexpectedError(
                        IllegalStateException(
                            "Add at least one trusted contact before sending an SOS."
                        )
                    )
                )
            }

            val locationResult = runCatching {
                locationProvider.getBestAvailableLocation()
                    ?: locationProvider.getLastKnownLocation()
            }.getOrNull()

            if (locationResult != null) {
                appStatusRepository.updateLocationSynchronized()
            }

            val latitude = locationResult?.latitude ?: 0.0
            val longitude = locationResult?.longitude ?: 0.0
            val locationAccuracy = locationResult?.accuracy ?: 0.0f
            val isLastKnownLocation =
                locationResult?.isLastKnownLocation ?: false

            val batteryPercentage = runCatching {
                batteryProvider.getBatteryPercentage()
            }.getOrDefault(100)

            val sosId = UUID.randomUUID().toString()
            generatedSosId = sosId
            val liveToken = EmergencyData.generateCryptographicToken()

            val alertMessage = emergencyMessage
                ?: "EMERGENCY: A SafeTap user triggered an SOS alert and may need assistance."

            val emergencyData = EmergencyData(
                sosId = sosId,
                userId = userId,
                latitude = latitude,
                longitude = longitude,
                locationAccuracy = locationAccuracy,
                batteryPercentage = batteryPercentage,
                timestamp = System.currentTimeMillis(),
                startedAt = System.currentTimeMillis(),
                status = SosStatus.ACTIVE,
                isLastKnownLocation = isLastKnownLocation,
                emergencyMessage = alertMessage,
                liveLocationToken = liveToken
            )

            val recipients = trustedContactsRepository.getCurrentContacts().map { contact ->
                contact.phone
            }

            if (recipients.isNotEmpty()) {
                // Attempt SMS independently. Failure does not terminate SOS.
                emergencySmsSender.clearDeliveryStatuses()
                emergencySmsSender.sendEmergencyMessage(
                    recipients = recipients,
                    message = buildEmergencySmsMessage(emergencyData)
                )
            }

            currentActiveSosId = sosId

            // Start continuous high accuracy foreground location tracking
            locationTrackingManager?.startTracking(
                sosId = sosId,
                liveLocationToken = liveToken
            )

            // PERSIST LOCALLY: authoritatively mark SOS as active on this device.
            localSosDataSource.saveActiveSos(emergencyData, isSynced = false)

            appStatusRepository.updateSafeTapProtected()

            notificationManager.showActiveSosNotification(
                emergencyData
            )

            // Upload initial SOS session to Firebase Realtime Database (non-blocking for SMS)
            val remoteResult = runCatching {
                remoteDataSource.createSosEvent(emergencyData)
            }.getOrElse { Result.failure(it) }
            // ATTEMPT REMOTE SYNC: isolated from the primary local SOS result.
            // We do not wait for the remote response to return Success to the ViewModel.
            // This ensures SOS remains active even if the network is down.
            syncEvent(emergencyData)

            Result.success(emergencyData)
        } catch (exception: CancellationException) {
            locationTrackingManager?.stopTracking()
            notificationManager.cancelSosNotification()
            generatedSosId?.let { id ->
                runCatching { remoteDataSource.closeSosEvent(id) }
            }
            currentActiveSosId = null
            Result.failure(SosError.Cancelled())
        } catch (exception: Exception) {
            locationTrackingManager?.stopTracking()
            notificationManager.cancelSosNotification()
            generatedSosId?.let { id ->
                runCatching { remoteDataSource.closeSosEvent(id) }
            }
            currentActiveSosId = null
            Result.failure(SosError.UnexpectedError(exception))
        }
    }

    fun callPrimaryTrustedContact(): Result<Unit> {
        val contacts =
            trustedContactsRepository.getCurrentContacts()

        val primaryContact =
            contacts.firstOrNull { contact ->
                contact.isPrimary
            } ?: contacts.firstOrNull()

        if (primaryContact == null) {
            return Result.failure(
                SosError.UnexpectedError(
                    IllegalStateException(
                        "No trusted contact is available to call."
                    )
                )
            )
        }

        return callManager.launchDirectCall(
            phoneNumber = primaryContact.phone
        )
    }

    suspend fun cancelSos(
        sosId: String? = null
    ): Result<Unit> = withContext(ioDispatcher) {
        try {
            val targetSosId = sosId ?: currentActiveSosId
            currentActiveSosId = null

            // Stop foreground location tracking
            locationTrackingManager?.stopTracking()

            notificationManager.cancelSosNotification()

            // LOCAL CANCELLATION IS IMMEDIATE
            currentActiveSosId = null
            localSosDataSource.clearActiveSos()

            if (targetSosId != null) {
                // ATTEMPT REMOTE CLOSE: failure will not block local cancellation.
                val closeResult = runCatching {
                    remoteDataSource.closeSosEvent(targetSosId)
                }.getOrElse { Result.failure(it) }

                if (closeResult.isFailure) {
                    localSosDataSource.markPendingClosure(targetSosId)
                    android.util.Log.w(
                        "SosCoordinator",
                        "Remote closure failed; marked as pending."
                    )
                } else {
                    localSosDataSource.clearPendingClosure()
                }
            }

            Result.success(Unit)
        } catch (exception: CancellationException) {
            Result.failure(SosError.Cancelled())
        } catch (exception: Exception) {
            Result.failure(SosError.UnexpectedError(exception))
        }
    }

    fun clearSmsDeliveryStatuses() {
        emergencySmsSender.clearDeliveryStatuses()
    }

    fun openEmergencyDialer(
        emergencyNumber: String = "112"
    ): Result<Unit> {
        return callManager.launchEmergencyDialer(
            emergencyNumber = emergencyNumber
        )
    }

    fun getActiveSosId(): String? = currentActiveSosId

    fun getActiveSos(): EmergencyData? =
        localSosDataSource.getActiveSos()
    /**
     * Retries any pending remote synchronization (creation or closure).
     * Should be called when the app is active and network might be available.
     */
    suspend fun syncPendingMetadata() = withContext(ioDispatcher) {
        // 1. Retry pending creation
        val activeSos = localSosDataSource.getActiveSos()
        if (activeSos != null && !localSosDataSource.isSynced()) {
            val result = remoteDataSource.createSosEvent(activeSos)
            if (result.isSuccess) {
                localSosDataSource.markSynced()
            }
        }

        // 2. Retry pending closure
        if (localSosDataSource.isPendingClosure()) {
            val closureId = localSosDataSource.getPendingClosureId()
            if (closureId != null) {
                val result = remoteDataSource.closeSosEvent(closureId)
                if (result.isSuccess) {
                    localSosDataSource.clearPendingClosure()
                }
            }
        }
    }

    private suspend fun syncEvent(emergencyData: EmergencyData) {
        // Use a non-blocking attempt for the initial sync.
        // If it fails, the event remains in LocalSosDataSource with isSynced=false.
        try {
            val result = remoteDataSource.createSosEvent(emergencyData)
            if (result.isSuccess) {
                localSosDataSource.markSynced()
            }
        } catch (e: Exception) {
            android.util.Log.e("SosCoordinator", "Initial remote sync failed: ${e.message}")
        }
    }

    private fun buildEmergencySmsMessage(
        emergencyData: EmergencyData
    ): String {
        // We avoid labeling 0.0 as a real location if it appears to be a fallback.
        val hasLocation = emergencyData.latitude != 0.0 || emergencyData.longitude != 0.0

        val liveLink = emergencyData.getLiveTrackingUrl()

        return buildString {
            appendLine("SafeTap emergency alert")
            appendLine()
            appendLine(emergencyData.emergencyMessage)
            appendLine("Live Track: $liveLink")
            if (hasLocation) {
                val mapsLink =
                    "https://maps.google.com/?q=" +
                            "${emergencyData.latitude}," +
                            emergencyData.longitude
                appendLine("Location: $mapsLink")
            } else {
                appendLine("Location: Unavailable")
            }
            append("Battery: ${emergencyData.batteryPercentage}%")
        }
    }
}
