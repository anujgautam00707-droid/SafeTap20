package com.safetap.app.domain.sos.model

sealed class SosError(
    override val message: String,
    override val cause: Throwable? = null
) : Exception(message, cause) {

    data class PermissionDenied(
        override val message: String = "Required permissions (Location / Notification) were denied."
    ) : SosError(message)

    data class GpsDisabled(
        override val message: String = "GPS location services are disabled on this device."
    ) : SosError(message)

    data class LocationUnavailable(
        override val message: String = "Unable to acquire current or cached GPS location."
    ) : SosError(message)

    data class NoDialerApp(
        override val message: String = "No compatible phone dialer application was found on device."
    ) : SosError(message)

    data class Cancelled(
        override val message: String = "SOS trigger was cancelled by the user."
    ) : SosError(message)

    data class UnexpectedError(
        override val cause: Throwable? = null,
        override val message: String = cause?.localizedMessage ?: "An unexpected error occurred during SOS processing."
    ) : SosError(message, cause)
}
