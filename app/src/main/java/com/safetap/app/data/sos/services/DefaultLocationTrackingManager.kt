package com.safetap.app.data.sos.services

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.safetap.app.domain.sos.model.LocationTrackingState
import com.safetap.app.domain.sos.services.LocationTrackingManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class DefaultLocationTrackingManager(
    private val context: Context
) : LocationTrackingManager {

    private val appContext = context.applicationContext
    private var currentSosId: String? = null

    override val trackingState: StateFlow<LocationTrackingState>
        get() = _trackingState.asStateFlow()

    override fun startTracking(sosId: String, liveLocationToken: String) {
        if (isTrackingActive() && currentSosId == sosId) {
            // Tracking is already active for this SOS session, do not start duplicates
            return
        }

        currentSosId = sosId
        _trackingState.value = LocationTrackingState.Initializing

        val intent = Intent(appContext, LocationTrackingService::class.java).apply {
            action = LocationTrackingService.ACTION_START_TRACKING
            putExtra(LocationTrackingService.EXTRA_SOS_ID, sosId)
            putExtra(LocationTrackingService.EXTRA_TOKEN, liveLocationToken)
        }

        try {
            ContextCompat.startForegroundService(appContext, intent)
        } catch (e: Exception) {
            _trackingState.value = LocationTrackingState.Error(
                "Unable to start foreground location service: ${e.localizedMessage}",
                e
            )
        }
    }

    override fun stopTracking() {
        currentSosId = null

        val intent = Intent(appContext, LocationTrackingService::class.java).apply {
            action = LocationTrackingService.ACTION_STOP_TRACKING
        }

        try {
            appContext.startService(intent)
        } catch (_: Exception) {
            try {
                appContext.stopService(intent)
            } catch (_: Exception) {}
        }

        _trackingState.value = LocationTrackingState.Stopped
    }

    override fun isTrackingActive(): Boolean {
        val state = _trackingState.value
        return currentSosId != null && (state is LocationTrackingState.Initializing || state is LocationTrackingState.Tracking)
    }

    companion object {
        private val _trackingState =
            MutableStateFlow<LocationTrackingState>(LocationTrackingState.Idle)

        fun postTrackingState(state: LocationTrackingState) {
            _trackingState.value = state
        }
    }
}
