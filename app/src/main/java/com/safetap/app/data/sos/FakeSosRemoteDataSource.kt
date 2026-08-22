package com.safetap.app.data.sos

import com.safetap.app.domain.sos.model.EmergencyData
import com.safetap.app.domain.sos.model.SosStatus
import kotlinx.coroutines.delay
import java.util.concurrent.ConcurrentHashMap

class FakeSosRemoteDataSource : SosRemoteDataSource {

    private val storage = ConcurrentHashMap<String, EmergencyData>()

    override suspend fun createSosEvent(emergencyData: EmergencyData): Result<String> {
        delay(200) // Simulate fast network dispatch
        val active = emergencyData.copy(status = SosStatus.ACTIVE)
        storage[emergencyData.sosId] = active
        return Result.success(active.sosId)
    }

    override suspend fun updateSosLocation(
        sosId: String,
        latitude: Double,
        longitude: Double
    ): Result<Unit> {
        delay(100)
        val existing = storage[sosId] ?: return Result.failure(NoSuchElementException("SOS Event $sosId not found"))
        storage[sosId] = existing.copy(
            latitude = latitude,
            longitude = longitude,
            timestamp = System.currentTimeMillis()
        )
        return Result.success(Unit)
    }

    override suspend fun closeSosEvent(sosId: String): Result<Unit> {
        delay(100)
        val existing = storage[sosId]
        if (existing != null) {
            storage[sosId] = existing.copy(status = SosStatus.RESOLVED)
        }
        return Result.success(Unit)
    }

    override suspend fun getActiveSosEvent(sosId: String): Result<EmergencyData?> {
        return Result.success(storage[sosId])
    }
}
