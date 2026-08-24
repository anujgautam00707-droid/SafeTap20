package com.safetap.app.domain.sos

import com.safetap.app.data.contacts.TrustedContactsRepository
import com.safetap.app.data.sos.SosRemoteDataSource
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
    private val remoteDataSource: SosRemoteDataSource,
    private val locationTrackingManager: LocationTrackingManager? = null,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    private var currentActiveSosId: String? = null

    val smsDeliveryStatuses: StateFlow<List<SmsRecipientStatus>> =
        emergencySmsSender.deliveryStatuses

    val trackingState: StateFlow<LocationTrackingState>?
        get() = locationTrackingManager?.trackingState

    fun checkPermissions(): Result<Unit> {
        return if (permissionChecker.hasRequiredPermissions()) {
            Result.success(Unit)
        } else {
            Result.failure(
                SosError.PermissionDenied(
                    "Location, SMS, and Phone permissions are required to activate SOS."
                )
            )
        }
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

            val locationResult =
                locationProvider.getBestAvailableLocation()
                    ?: locationProvider.getLastKnownLocation()

            val latitude = locationResult?.latitude ?: 0.0
            val longitude = locationResult?.longitude ?: 0.0
            val locationAccuracy = locationResult?.accuracy ?: 0.0f
            val isLastKnownLocation =
                locationResult?.isLastKnownLocation ?: false

            val batteryPercentage =
                batteryProvider.getBatteryPercentage()

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

            val recipients = trustedContacts.map { contact ->
                contact.phone
            }

            // Prevent callback results from an older SOS appearing in the new one.
            emergencySmsSender.clearDeliveryStatuses()

            val smsResult = emergencySmsSender.sendEmergencyMessage(
                recipients = recipients,
                message = buildEmergencySmsMessage(emergencyData)
            )

            if (smsResult.isFailure) {
                return@withContext Result.failure(
                    SosError.UnexpectedError(
                        smsResult.exceptionOrNull()
                            ?: IllegalStateException(
                                "The emergency SMS could not be sent."
                            )
                    )
                )
            }

            currentActiveSosId = sosId

            // Start continuous high accuracy foreground location tracking
            locationTrackingManager?.startTracking(
                sosId = sosId,
                liveLocationToken = liveToken
            )

            notificationManager.showActiveSosNotification(
                emergencyData
            )

            // Upload initial SOS session to Firebase Realtime Database (non-blocking for SMS)
            val remoteResult = runCatching {
                remoteDataSource.createSosEvent(emergencyData)
            }.getOrElse { Result.failure(it) }

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

            if (targetSosId != null) {
                runCatching {
                    remoteDataSource.closeSosEvent(targetSosId)
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

    fun buildEmergencySmsMessage(
        emergencyData: EmergencyData
    ): String {
        val mapsLink =
            "https://maps.google.com/?q=" +
                    "${emergencyData.latitude}," +
                    emergencyData.longitude

        val liveLink = emergencyData.getLiveTrackingUrl()

        return buildString {
            appendLine("SafeTap emergency alert")
            appendLine()
            appendLine(emergencyData.emergencyMessage)
            appendLine("Location: $mapsLink")
            appendLine("Live Track: $liveLink")
            append("Battery: ${emergencyData.batteryPercentage}%")
        }
    }
}