package com.safetap.app.data.sos.services

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.safetap.app.domain.sos.model.LocationPrecision
import com.safetap.app.domain.sos.services.PermissionChecker

class DefaultPermissionChecker(
    context: Context
) : PermissionChecker {

    private val appContext = context.applicationContext

    override fun hasLocationPermission(): Boolean {
        return hasPreciseLocationPermission() ||
                hasApproximateLocationPermission()
    }

    override fun hasPreciseLocationPermission(): Boolean {
        return isGranted(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    override fun hasApproximateLocationPermission(): Boolean {
        return isGranted(Manifest.permission.ACCESS_COARSE_LOCATION)
    }

    override fun getLocationPrecision(): LocationPrecision {
        return when {
            hasPreciseLocationPermission() -> LocationPrecision.HIGH_PRECISION
            hasApproximateLocationPermission() -> LocationPrecision.APPROXIMATE
            else -> LocationPrecision.UNAVAILABLE
        }
    }

    override fun hasSmsPermission(): Boolean {
        return isGranted(Manifest.permission.SEND_SMS)
    }

    override fun hasCallPermission(): Boolean {
        return isGranted(Manifest.permission.CALL_PHONE)
    }

    override fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            isGranted(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            true
        }
    }

    override fun getRequiredPermissions(): List<String> {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.SEND_SMS,
            Manifest.permission.CALL_PHONE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        return permissions
    }

    override fun getMissingPermissions(): List<String> {
        return getRequiredPermissions().filter { permission ->
            !isGranted(permission)
        }
    }

    override fun hasRequiredPermissions(): Boolean {
        return getMissingPermissions().isEmpty()
    }

    private fun isGranted(permission: String): Boolean {
        return try {
            ContextCompat.checkSelfPermission(
                appContext,
                permission
            ) == PackageManager.PERMISSION_GRANTED
        } catch (_: Exception) {
            false
        }
    }
}