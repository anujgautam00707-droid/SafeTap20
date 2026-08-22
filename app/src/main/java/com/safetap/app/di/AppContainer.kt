package com.safetap.app.di

import android.content.Context
import com.safetap.app.data.auth.AuthRepository
import com.safetap.app.data.auth.FirebaseAuthManager
import com.safetap.app.data.sos.FakeSosRemoteDataSource
import com.safetap.app.data.sos.SosRemoteDataSource
import com.safetap.app.data.sos.services.DefaultBatteryProvider
import com.safetap.app.data.sos.services.DefaultEmergencyCallManager
import com.safetap.app.data.sos.services.DefaultEmergencyNotificationManager
import com.safetap.app.data.sos.services.DefaultLocationProvider
import com.safetap.app.data.sos.services.DefaultPermissionChecker
import com.safetap.app.domain.sos.SosCoordinator
import com.safetap.app.domain.sos.services.BatteryProvider
import com.safetap.app.domain.sos.services.EmergencyCallManager
import com.safetap.app.domain.sos.services.EmergencyNotificationManager
import com.safetap.app.domain.sos.services.LocationProvider
import com.safetap.app.domain.sos.services.PermissionChecker

object AppContainer {
    lateinit var authRepository: AuthRepository
        private set

    lateinit var permissionChecker: PermissionChecker
        private set

    lateinit var locationProvider: LocationProvider
        private set

    lateinit var batteryProvider: BatteryProvider
        private set

    lateinit var emergencyNotificationManager: EmergencyNotificationManager
        private set

    lateinit var emergencyCallManager: EmergencyCallManager
        private set

    lateinit var sosRemoteDataSource: SosRemoteDataSource
        private set

    lateinit var sosCoordinator: SosCoordinator
        private set

    val isInitialized: Boolean
        get() = ::authRepository.isInitialized && ::sosCoordinator.isInitialized

    fun init(context: Context? = null) {
        if (isInitialized) return

        authRepository = AuthRepository(FirebaseAuthManager())

        if (context != null) {
            val appContext = context.applicationContext
            permissionChecker = DefaultPermissionChecker(appContext)
            locationProvider = DefaultLocationProvider(appContext, permissionChecker)
            batteryProvider = DefaultBatteryProvider(appContext)
            emergencyNotificationManager = DefaultEmergencyNotificationManager(appContext, permissionChecker)
            emergencyCallManager = DefaultEmergencyCallManager(appContext)
            sosRemoteDataSource = FakeSosRemoteDataSource()

            sosCoordinator = SosCoordinator(
                permissionChecker = permissionChecker,
                locationProvider = locationProvider,
                batteryProvider = batteryProvider,
                notificationManager = emergencyNotificationManager,
                callManager = emergencyCallManager,
                remoteDataSource = sosRemoteDataSource
            )
        }
    }
}
