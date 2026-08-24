package com.safetap.app.domain.sos.services

import com.safetap.app.domain.sos.model.LocationTrackingState
import kotlinx.coroutines.flow.StateFlow

interface LocationTrackingManager {
    val trackingState: StateFlow<LocationTrackingState>

    fun startTracking(sosId: String, liveLocationToken: String)
    fun stopTracking()
    fun isTrackingActive(): Boolean
}
