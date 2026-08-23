package com.safetap.app.domain.sos.services

interface EmergencySmsSender {

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
}