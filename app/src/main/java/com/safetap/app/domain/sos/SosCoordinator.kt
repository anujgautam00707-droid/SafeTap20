package com.safetap.app.domain.sos

import com.safetap.app.data.sos.SosRemoteDataSource
import com.safetap.app.domain.sos.model.EmergencyData
import com.safetap.app.domain.sos.model.SosError
import com.safetap.app.domain.sos.model.SosStatus
import com.safetap.app.domain.sos.services.BatteryProvider
import com.safetap.app.domain.sos.services.EmergencyCallManager
import com.safetap.app.domain.sos.services.EmergencyNotificationManager
import com.safetap.app.domain.sos.services.LocationProvider
import com.safetap.app.domain.sos.services.PermissionChecker
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

class SosCoordinator(
    private val permissionChecker: PermissionChecker,
    private val locationProvider: LocationProvider,
    private val batteryProvider: BatteryProvider,
    private val notificationManager: EmergencyNotificationManager,
    private val callManager: EmergencyCallManager,
    private val remoteDataSource: SosRemoteDataSource,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    private var currentActiveSosId: String? = null

    /**
     * Checks whether the runtime permissions required for SOS are granted.
     */
    fun checkPermissions(): Result<Unit> {
        return if (permissionChecker.hasLocationPermission()) {
            Result.success(Unit)
        } else {
            Result.failure(SosError.PermissionDenied())
        }
    }

    /**
     * Reads the current device battery percentage.
     */
    suspend fun getBatteryPercentage(): Int = withContext(ioDispatcher) {
        batteryProvider.getBatteryPercentage()
    }

    /**
     * Runs a cancellable countdown and emits one tick per second.
     */
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

    /**
     * Collects emergency data and dispatches the SOS event.
     */
    suspend fun triggerSos(
        userId: String = "user_placeholder",
        emergencyMessage: String? = null
    ): Result<EmergencyData> = withContext(ioDispatcher) {
        try {
            if (!permissionChecker.hasLocationPermission()) {
                return@withContext Result.failure(
                    SosError.PermissionDenied()
                )
            }

            if (!locationProvider.isGpsEnabled()) {
                val lastKnownLocation =
                    locationProvider.getLastKnownLocation()

                if (lastKnownLocation == null) {
                    return@withContext Result.failure(
                        SosError.GpsDisabled()
                    )
                }
            }

            val locationResult =
                locationProvider.getBestAvailableLocation()
                    ?: locationProvider.getLastKnownLocation()

            val latitude = locationResult?.latitude ?: 0.0
            val longitude = locationResult?.longitude ?: 0.0
            val accuracy = locationResult?.accuracy ?: 0.0f
            val isLastKnownLocation =
                locationResult?.isLastKnownLocation ?: false

            val batteryPercentage =
                batteryProvider.getBatteryPercentage()

            val sosId = UUID.randomUUID().toString()
            currentActiveSosId = sosId

            val emergencyData = EmergencyData(
                sosId = sosId,
                userId = userId,
                latitude = latitude,
                longitude = longitude,
                locationAccuracy = accuracy,
                batteryPercentage = batteryPercentage,
                timestamp = System.currentTimeMillis(),
                status = SosStatus.ACTIVE,
                isLastKnownLocation = isLastKnownLocation,
                emergencyMessage = emergencyMessage
                    ?: "EMERGENCY: SafeTap user triggered an SOS alert. Immediate assistance required!"
            )

            notificationManager.showActiveSosNotification(
                emergencyData
            )

            remoteDataSource.createSosEvent(emergencyData)

            Result.success(emergencyData)
        } catch (exception: CancellationException) {
            Result.failure(SosError.Cancelled())
        } catch (exception: Exception) {
            Result.failure(SosError.UnexpectedError(exception))
        }
    }

    /**
     * Cancels the active SOS event and dismisses its notification.
     */
    suspend fun cancelSos(
        sosId: String? = null
    ): Result<Unit> = withContext(ioDispatcher) {
        try {
            val targetSosId = sosId ?: currentActiveSosId

            notificationManager.cancelSosNotification()

            if (targetSosId != null) {
                remoteDataSource.closeSosEvent(targetSosId)
            }

            currentActiveSosId = null
            Result.success(Unit)
        } catch (exception: Exception) {
            Result.failure(SosError.UnexpectedError(exception))
        }
    }

    /**
     * Opens the device dialer with an emergency number.
     */
    fun openEmergencyDialer(
        emergencyNumber: String = "911"
    ): Result<Unit> {
        return callManager.launchEmergencyDialer(emergencyNumber)
    }

    fun getActiveSosId(): String? = currentActiveSosId
}