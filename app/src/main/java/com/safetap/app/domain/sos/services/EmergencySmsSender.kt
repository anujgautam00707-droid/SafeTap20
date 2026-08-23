package com.safetap.app.domain.sos.services

import kotlinx.coroutines.flow.StateFlow

enum class SmsDeliveryState {
    QUEUED,
    SENT,
    DELIVERED,
    FAILED
}

data class SmsRecipientStatus(
    val recipient: String,
    val state: SmsDeliveryState,
    val errorMessage: String? = null
)

interface EmergencySmsSender {

    val deliveryStatuses: StateFlow<List<SmsRecipientStatus>>

    /**
     * Submits an emergency SMS to every supplied recipient.
     *
     * A successful result contains the number of recipients whose messages
     * were submitted to Android's SMS service.
     */
    fun sendEmergencyMessage(
        recipients: List<String>,
        message: String
    ): Result<Int>

    /**
     * Clears statuses from the previous SOS dispatch.
     */
    fun clearDeliveryStatuses()
}