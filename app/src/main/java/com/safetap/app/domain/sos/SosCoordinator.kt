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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.util.UUID

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
     * Checks if runtime permissions required for SOS operation are granted.
     */
    fun checkPermissions(): Result<Unit> {
        return if (permissionChecker.hasLocationPermission()) {
            Result.success(Unit)
        } else {
            Result.failure(SosError.PermissionDenied())
        }
    }

    /**
     * Runs a cancellable 5-second countdown emitting ticks every second.
     */
    suspend fun runCountdown(
        durationSeconds: Int = 5,
        onTick: suspend (Int) -> Unit
    ): Result<Unit> = withContext(ioDispatcher) {
        try {
            for (sec in durationSeconds downTo 1) {
                onTick(sec)
                delay(1000L)
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            Result.failure(SosError.Cancelled())
        } catch (e: Exception) {
            Result.failure(SosError.UnexpectedError(e))
        }
    }

    /**
     * Executes the local SOS collection and dispatch flow.
     */
    suspend fun triggerSos(
        userId: String = "user_placeholder",
        emergencyMessage: String? = null
    ): Result<EmergencyData> = withContext(ioDispatcher) {
        try {
            // 1. Permission check
            if (!permissionChecker.hasLocationPermission()) {
                return@withContext Result.failure(SosError.PermissionDenied())
            }

            // 2. Check GPS status
            if (!locationProvider.isGpsEnabled()) {
                // If GPS is disabled, we still attempt last-known location before failing
                val lastKnown = locationProvider.getLastKnownLocation()
                if (lastKnown == null) {
                    return@withContext Result.failure(SosError.GpsDisabled())
                }
            }

            // 3. Obtain location with fallback
            val locationResult = locationProvider.getBestAvailableLocation()
                ?: locationProvider.getLastKnownLocation()

            val (latitude, longitude, accuracy, isLastKnown) = if (locationResult != null) {
                listOf(
                    locationResult.latitude,
                    locationResult.longitude,
                    locationResult.accuracy,
                    locationResult.isLastKnownLocation
                )
            } else {
                listOf(0.0, 0.0, 0.0f, false)
            }

            // 4. Query battery level
            val batteryLevel = batteryProvider.getBatteryPercentage()

            // 5. Build emergency payload
            val sosId = UUID.randomUUID().toString()
            currentActiveSosId = sosId

            val emergencyData = EmergencyData(
                sosId = sosId,
                userId = userId,
                latitude = latitude as Double,
                longitude = longitude as Double,
                locationAccuracy = accuracy as Float,
                batteryPercentage = batteryLevel,
                timestamp = System.currentTimeMillis(),
                status = SosStatus.ACTIVE,
                isLastKnownLocation = isLastKnown as Boolean,
                emergencyMessage = emergencyMessage ?: "EMERGENCY: SafeTap user triggered an SOS alert. Immediate assistance required!"
            )

            // 6. Show high-priority ongoing notification
            notificationManager.showActiveSosNotification(emergencyData)

            // 7. Dispatch to remote backend placeholder
            remoteDataSource.createSosEvent(emergencyData)

            Result.success(emergencyData)
        } catch (e: CancellationException) {
            Result.failure(SosError.Cancelled())
        } catch (e: Exception) {
            Result.failure(SosError.UnexpectedError(e))
        }
    }

    /**
     * Cancels active SOS broadcast and dismisses ongoing notifications.
     */
    suspend fun cancelSos(sosId: String? = null): Result<Unit> = withContext(ioDispatcher) {
        try {
            val targetId = sosId ?: currentActiveSosId
            notificationManager.cancelSosNotification()
            if (targetId != null) {
                remoteDataSource.closeSosEvent(targetId)
            }
            currentActiveSosId = null
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(SosError.UnexpectedError(e))
        }
    }

    /**
     * Prepares and launches the device dialer.
     */
    fun openEmergencyDialer(emergencyNumber: String = "911"): Result<Unit> {
        return callManager.launchEmergencyDialer(emergencyNumber)
    }

    fun getActiveSosId(): String? = currentActiveSosId
}
