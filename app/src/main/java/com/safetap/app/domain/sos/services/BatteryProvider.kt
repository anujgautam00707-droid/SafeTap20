package com.safetap.app.domain.sos.services

interface BatteryProvider {
    fun getBatteryPercentage(): Int
}
