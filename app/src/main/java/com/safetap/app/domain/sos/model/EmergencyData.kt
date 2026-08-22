package com.safetap.app.domain.sos.model

import java.util.UUID

data class EmergencyData(
    val sosId: String = UUID.randomUUID().toString(),
    val userId: String = "anonymous_user",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val locationAccuracy: Float = 0.0f,
    val batteryPercentage: Int = 100,
    val timestamp: Long = System.currentTimeMillis(),
    val status: SosStatus = SosStatus.PENDING,
    val isLastKnownLocation: Boolean = false,
    val emergencyMessage: String = "EMERGENCY: SafeTap user triggered an SOS alert. Immediate assistance required!"
)
