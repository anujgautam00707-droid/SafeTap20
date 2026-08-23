package com.safetap.app.domain.sos.services

interface PermissionChecker {

    fun hasLocationPermission(): Boolean

    fun hasSmsPermission(): Boolean

    fun hasCallPermission(): Boolean

    fun hasNotificationPermission(): Boolean

    fun hasRequiredPermissions(): Boolean
}