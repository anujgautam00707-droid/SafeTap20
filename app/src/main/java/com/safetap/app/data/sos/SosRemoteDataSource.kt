package com.safetap.app.data.sos

import com.safetap.app.domain.sos.model.EmergencyData

interface SosRemoteDataSource {
    suspend fun createSosEvent(emergencyData: EmergencyData): Result<String>
    suspend fun updateSosLocation(sosId: String, latitude: Double, longitude: Double): Result<Unit>
    suspend fun closeSosEvent(sosId: String): Result<Unit>
    suspend fun getActiveSosEvent(sosId: String): Result<EmergencyData?>
}
