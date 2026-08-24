package com.safetap.app.domain.sos.model

sealed interface LocationTrackingState {
    object Idle : LocationTrackingState

    object Initializing : LocationTrackingState

    data class Tracking(
        val latitude: Double,
        val longitude: Double,
        val accuracy: Float,
        val timestamp: Long = System.currentTimeMillis(),
        val isStale: Boolean = false,
        val batteryPercentage: Int? = null,
        val updatesCount: Long = 0L
    ) : LocationTrackingState

    data class LocationUnavailable(
        val reason: String = "GPS location is temporarily unavailable.",
        val lastKnownLocation: LocationResult? = null
    ) : LocationTrackingState

    data class Error(
        val message: String,
        val cause: Throwable? = null
    ) : LocationTrackingState

    object Stopped : LocationTrackingState
}
