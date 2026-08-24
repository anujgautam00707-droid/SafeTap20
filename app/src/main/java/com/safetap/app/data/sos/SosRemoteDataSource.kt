package com.safetap.app.data.sos

import com.safetap.app.domain.sos.model.EmergencyData

import kotlinx.coroutines.flow.Flow

interface SosRemoteDataSource {
    suspend fun createSosEvent(emergencyData: EmergencyData): Result<String>
    suspend fun updateSosLocation(
        sosId: String,
        latitude: Double,
        longitude: Double,
        accuracy: Float = 0.0f,
        battery: Int? = null
    ): Result<Unit>
    suspend fun closeSosEvent(sosId: String): Result<Unit>
    suspend fun getActiveSosEvent(sosId: String): Result<EmergencyData?>
    fun observeSosEvent(sosId: String): Flow<EmergencyData?>
}
