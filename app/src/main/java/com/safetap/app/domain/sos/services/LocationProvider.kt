package com.safetap.app.domain.sos.services

import com.safetap.app.domain.sos.model.LocationResult

interface LocationProvider {
    fun isGpsEnabled(): Boolean
    suspend fun getCurrentLocation(): LocationResult?
    suspend fun getLastKnownLocation(): LocationResult?
    suspend fun getBestAvailableLocation(): LocationResult?
}
