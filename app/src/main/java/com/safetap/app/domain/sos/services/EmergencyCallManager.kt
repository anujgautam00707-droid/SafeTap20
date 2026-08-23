package com.safetap.app.domain.sos.services

import android.content.Intent

interface EmergencyCallManager {

    fun getEmergencyDialIntent(
        emergencyNumber: String = "112"
    ): Intent

    fun launchEmergencyDialer(
        emergencyNumber: String = "112"
    ): Result<Unit>

    fun getDirectCallIntent(
        phoneNumber: String
    ): Intent

    fun launchDirectCall(
        phoneNumber: String
    ): Result<Unit>
}
