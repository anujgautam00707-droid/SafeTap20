package com.safetap.app.data.sos

import com.safetap.app.domain.sos.model.EmergencyData
import com.safetap.app.domain.sos.model.SosStatus
import kotlinx.coroutines.delay
import java.util.concurrent.ConcurrentHashMap

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeSosRemoteDataSource : SosRemoteDataSource {

    private val storage = ConcurrentHashMap<String, EmergencyData>()
    private val storageFlow = MutableStateFlow<Map<String, EmergencyData>>(emptyMap())

    override suspend fun createSosEvent(emergencyData: EmergencyData): Result<String> {
        delay(50)
        val active = emergencyData.copy(status = SosStatus.ACTIVE)
        storage[emergencyData.sosId] = active
        storageFlow.value = storage.toMap()
        return Result.success(active.sosId)
    }

    override suspend fun updateSosLocation(
        sosId: String,
        latitude: Double,
        longitude: Double,
        accuracy: Float,
        battery: Int?
    ): Result<Unit> {
        delay(20)
        val existing = storage[sosId] ?: return Result.failure(NoSuchElementException("SOS Event $sosId not found"))
        val updated = existing.copy(
            latitude = latitude,
            longitude = longitude,
            locationAccuracy = accuracy,
            batteryPercentage = battery ?: existing.batteryPercentage,
            timestamp = System.currentTimeMillis()
        )
        storage[sosId] = updated
        storageFlow.value = storage.toMap()
        return Result.success(Unit)
    }

    override suspend fun closeSosEvent(sosId: String): Result<Unit> {
        delay(20)
        val existing = storage[sosId]
        if (existing != null) {
            val ended = existing.copy(
                status = SosStatus.ENDED,
                endedAt = System.currentTimeMillis()
            )
            storage[sosId] = ended
            storageFlow.value = storage.toMap()
        }
        return Result.success(Unit)
    }

    override suspend fun getActiveSosEvent(sosId: String): Result<EmergencyData?> {
        return Result.success(storage[sosId])
    }

    override fun observeSosEvent(sosId: String): Flow<EmergencyData?> {
        return storageFlow.map { it[sosId] }
    }
}
