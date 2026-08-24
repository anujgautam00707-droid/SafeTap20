package com.safetap.app.domain.sos.model

import java.security.SecureRandom
import java.util.UUID

data class EmergencyData(
    val sosId: String = UUID.randomUUID().toString(),
    val userId: String = "anonymous_user",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val locationAccuracy: Float = 0.0f,
    val batteryPercentage: Int = 100,
    val timestamp: Long = System.currentTimeMillis(),
    val startedAt: Long = timestamp,
    val endedAt: Long? = null,
    val status: SosStatus = SosStatus.PENDING,
    val isLastKnownLocation: Boolean = false,
    val emergencyMessage: String = "EMERGENCY: SafeTap user triggered an SOS alert. Immediate assistance required!",
    val liveLocationToken: String = generateCryptographicToken()
) {
    val isLocationAvailable: Boolean
        get() = (latitude != 0.0 || longitude != 0.0) && locationAccuracy > 0f

    val googleMapsUrl: String
        get() = "https://maps.google.com/?q=$latitude,$longitude"

    fun getLiveTrackingUrl(domain: String = "safetap-2fb1e.web.app"): String {
        return "https://$domain/live/$sosId?token=$liveLocationToken"
    }

    companion object {
        fun generateCryptographicToken(): String {
            val randomBytes = ByteArray(32) // 256 bits of entropy
            SecureRandom().nextBytes(randomBytes)
            return randomBytes.joinToString("") { "%02x".format(it) }
        }
    }
}
