package com.safetap.app.domain.sos.services

import com.safetap.app.domain.sos.model.EmergencyData

interface EmergencyNotificationManager {
    fun showActiveSosNotification(emergencyData: EmergencyData)
    fun cancelSosNotification()
}
