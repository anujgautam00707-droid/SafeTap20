package com.safetap.app.domain.sos.model

data class LocationResult(
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float,
    val isLastKnownLocation: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)
