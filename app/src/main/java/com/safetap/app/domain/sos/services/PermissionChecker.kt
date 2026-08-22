package com.safetap.app.domain.sos.services

interface PermissionChecker {
    fun hasLocationPermission(): Boolean
    fun hasNotificationPermission(): Boolean
    fun hasRequiredPermissions(): Boolean
}
