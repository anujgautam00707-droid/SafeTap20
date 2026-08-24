package com.safetap.app.domain.sos.services

import com.safetap.app.domain.sos.model.LocationPrecision

interface PermissionChecker {

    fun hasLocationPermission(): Boolean

    fun hasPreciseLocationPermission(): Boolean

    fun hasApproximateLocationPermission(): Boolean

    fun getLocationPrecision(): LocationPrecision

    fun hasSmsPermission(): Boolean

    fun hasCallPermission(): Boolean

    fun hasNotificationPermission(): Boolean

    /**
     * Returns the list of Android manifest permissions required for SOS.
     */
    fun getRequiredPermissions(): List<String>

    /**
     * Returns the list of required permissions that are currently missing.
     */
    fun getMissingPermissions(): List<String>

    /**
     * Checks if all required permissions are granted.
     */
    fun hasRequiredPermissions(): Boolean
}