package com.safetap.app.data.sos.services

import android.Manifest
import android.app.Activity
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.SmsManager
import androidx.core.content.ContextCompat
import com.safetap.app.domain.sos.services.EmergencySmsSender
import com.safetap.app.domain.sos.services.SmsDeliveryState
import com.safetap.app.domain.sos.services.SmsRecipientStatus
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class DefaultEmergencySmsSender(
    context: Context
) : EmergencySmsSender {

    private val appContext = context.applicationContext
    private val statusLock = Any()

    private val _deliveryStatuses =
        MutableStateFlow<List<SmsRecipientStatus>>(emptyList())

    override val deliveryStatuses: StateFlow<List<SmsRecipientStatus>> =
        _deliveryStatuses.asStateFlow()

    private val progressByRecipient =
        mutableMapOf<String, MultipartMessageProgress>()

    private val pendingIntentRequestCode =
        AtomicInteger(INITIAL_REQUEST_CODE)

    private val dispatchIdGenerator =
        AtomicInteger(INITIAL_DISPATCH_ID)

    private var currentDispatchId: Int = 0

    private val statusReceiver = object : BroadcastReceiver() {
        override fun onReceive(
            context: Context?,
            intent: Intent?
        ) {
            val receivedIntent = intent ?: return

            val recipient = receivedIntent.getStringExtra(
                EXTRA_RECIPIENT
            ) ?: return

            val dispatchId = receivedIntent.getIntExtra(
                EXTRA_DISPATCH_ID,
                INVALID_DISPATCH_ID
            )

            when (receivedIntent.action) {
                ACTION_SMS_SENT -> handleSentResult(
                    recipient = recipient,
                    dispatchId = dispatchId,
                    resultCode = resultCode
                )

                ACTION_SMS_DELIVERED -> handleDeliveryResult(
                    recipient = recipient,
                    dispatchId = dispatchId
                )
            }
        }
    }

    init {
        registerStatusReceiver()
    }

    override fun sendEmergencyMessage(
        recipients: List<String>,
        message: String
    ): Result<Int> {
        return runCatching {
            require(message.isNotBlank()) {
                "Emergency SMS message cannot be empty."
            }

            if (!hasSmsPermission()) {
                return Result.failure(
                    SecurityException("SMS permission has not been granted.")
                )
            }

            if (!supportsSmsMessaging()) {
                return Result.failure(
                    IllegalStateException("This device does not support SMS messaging.")
                )
            }

            val validRecipients = recipients
                .map(::normalizePhoneNumber)
                .distinct()

            require(validRecipients.isNotEmpty()) {
                "No valid trusted-contact phone numbers were found."
            }

            val smsManager = appContext.getSystemService(
                SmsManager::class.java
            ) ?: error("Android SMS service is unavailable.")

            val dispatchId = dispatchIdGenerator.getAndIncrement()
            val trimmedMessage = message.trim()

            initializeStatuses(
                recipients = validRecipients,
                dispatchId = dispatchId
            )

            var submittedRecipientCount = 0

            validRecipients.forEach { recipient ->
                val messageParts = smsManager.divideMessage(
                    trimmedMessage
                ).ifEmpty {
                    arrayListOf(trimmedMessage)
                }

                synchronized(statusLock) {
                    progressByRecipient[recipient] =
                        MultipartMessageProgress(
                            totalParts = messageParts.size
                        )
                }

                val submissionResult = runCatching {
                    sendMessage(
                        smsManager = smsManager,
                        recipient = recipient,
                        messageParts = messageParts,
                        dispatchId = dispatchId
                    )
                }

                if (submissionResult.isSuccess) {
                    submittedRecipientCount += 1
                } else {
                    markRecipientFailed(
                        recipient = recipient,
                        dispatchId = dispatchId,
                        errorMessage = submissionResult
                            .exceptionOrNull()
                            ?.message
                            ?: "The SMS could not be submitted."
                    )
                }
            }

            check(submittedRecipientCount > 0) {
                "No emergency SMS messages could be submitted."
            }

            submittedRecipientCount
        }
    }

    override fun clearDeliveryStatuses() {
        synchronized(statusLock) {
            currentDispatchId =
                dispatchIdGenerator.getAndIncrement()

            progressByRecipient.clear()
            _deliveryStatuses.value = emptyList()
        }
    }

    private fun initializeStatuses(
        recipients: List<String>,
        dispatchId: Int
    ) {
        synchronized(statusLock) {
            currentDispatchId = dispatchId
            progressByRecipient.clear()

            _deliveryStatuses.value = recipients.map { recipient ->
                SmsRecipientStatus(
                    recipient = recipient,
                    state = SmsDeliveryState.QUEUED
                )
            }
        }
    }

    private fun sendMessage(
        smsManager: SmsManager,
        recipient: String,
        messageParts: ArrayList<String>,
        dispatchId: Int
    ) {
        if (messageParts.size == 1) {
            smsManager.sendTextMessage(
                recipient,
                null,
                messageParts.first(),
                createStatusPendingIntent(
                    action = ACTION_SMS_SENT,
                    recipient = recipient,
                    dispatchId = dispatchId,
                    partIndex = 0
                ),
                createStatusPendingIntent(
                    action = ACTION_SMS_DELIVERED,
                    recipient = recipient,
                    dispatchId = dispatchId,
                    partIndex = 0
                )
            )

            return
        }

        val sentIntents = ArrayList<PendingIntent>(
            messageParts.size
        )

        val deliveryIntents = ArrayList<PendingIntent>(
            messageParts.size
        )

        messageParts.indices.forEach { partIndex ->
            sentIntents += createStatusPendingIntent(
                action = ACTION_SMS_SENT,
                recipient = recipient,
                dispatchId = dispatchId,
                partIndex = partIndex
            )

            deliveryIntents += createStatusPendingIntent(
                action = ACTION_SMS_DELIVERED,
                recipient = recipient,
                dispatchId = dispatchId,
                partIndex = partIndex
            )
        }

        smsManager.sendMultipartTextMessage(
            recipient,
            null,
            messageParts,
            sentIntents,
            deliveryIntents
        )
    }

    private fun createStatusPendingIntent(
        action: String,
        recipient: String,
        dispatchId: Int,
        partIndex: Int
    ): PendingIntent {
        val callbackIntent = Intent(action).apply {
            setPackage(appContext.packageName)

            putExtra(EXTRA_RECIPIENT, recipient)
            putExtra(EXTRA_DISPATCH_ID, dispatchId)
            putExtra(EXTRA_PART_INDEX, partIndex)
        }

        return PendingIntent.getBroadcast(
            appContext,
            pendingIntentRequestCode.getAndIncrement(),
            callbackIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or
                    PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun handleSentResult(
        recipient: String,
        dispatchId: Int,
        resultCode: Int
    ) {
        synchronized(statusLock) {
            if (dispatchId != currentDispatchId) {
                return
            }

            val progress = progressByRecipient[recipient]
                ?: return

            if (progress.hasFailed) {
                return
            }

            if (resultCode != Activity.RESULT_OK) {
                progress.hasFailed = true

                updateRecipientStatusLocked(
                    recipient = recipient,
                    state = SmsDeliveryState.FAILED,
                    errorMessage = sentFailureMessage(resultCode)
                )

                return
            }

            progress.sentParts += 1

            if (progress.sentParts >= progress.totalParts) {
                updateRecipientStatusLocked(
                    recipient = recipient,
                    state = SmsDeliveryState.SENT
                )
            }
        }
    }

    private fun handleDeliveryResult(
        recipient: String,
        dispatchId: Int
    ) {
        synchronized(statusLock) {
            if (dispatchId != currentDispatchId) {
                return
            }

            val progress = progressByRecipient[recipient]
                ?: return

            if (progress.hasFailed) {
                return
            }

            progress.deliveredParts += 1

            if (
                progress.deliveredParts >= progress.totalParts
            ) {
                updateRecipientStatusLocked(
                    recipient = recipient,
                    state = SmsDeliveryState.DELIVERED
                )
            }
        }
    }

    private fun markRecipientFailed(
        recipient: String,
        dispatchId: Int,
        errorMessage: String
    ) {
        synchronized(statusLock) {
            if (dispatchId != currentDispatchId) {
                return
            }

            progressByRecipient[recipient]?.hasFailed = true

            updateRecipientStatusLocked(
                recipient = recipient,
                state = SmsDeliveryState.FAILED,
                errorMessage = errorMessage
            )
        }
    }

    private fun updateRecipientStatusLocked(
        recipient: String,
        state: SmsDeliveryState,
        errorMessage: String? = null
    ) {
        _deliveryStatuses.value =
            _deliveryStatuses.value.map { currentStatus ->
                if (currentStatus.recipient != recipient) {
                    return@map currentStatus
                }

                if (
                    currentStatus.state ==
                    SmsDeliveryState.DELIVERED
                ) {
                    return@map currentStatus
                }

                if (
                    currentStatus.state ==
                    SmsDeliveryState.FAILED
                ) {
                    return@map currentStatus
                }

                currentStatus.copy(
                    state = state,
                    errorMessage = errorMessage
                )
            }
    }

    private fun registerStatusReceiver() {
        val intentFilter = IntentFilter().apply {
            addAction(ACTION_SMS_SENT)
            addAction(ACTION_SMS_DELIVERED)
        }

        ContextCompat.registerReceiver(
            appContext,
            statusReceiver,
            intentFilter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    private fun hasSmsPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.SEND_SMS
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun supportsSmsMessaging(): Boolean {
        val packageManager = appContext.packageManager

        return if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.TIRAMISU
        ) {
            packageManager.hasSystemFeature(
                PackageManager.FEATURE_TELEPHONY_MESSAGING
            )
        } else {
            packageManager.hasSystemFeature(
                PackageManager.FEATURE_TELEPHONY
            )
        }
    }

    private fun normalizePhoneNumber(
        phoneNumber: String
    ): String {
        val trimmedNumber = phoneNumber.trim()
        val digits = trimmedNumber.filter(Char::isDigit)

        require(
            digits.length in
                    MINIMUM_PHONE_DIGITS..MAXIMUM_PHONE_DIGITS
        ) {
            "Invalid trusted-contact phone number: $phoneNumber"
        }

        return if (trimmedNumber.startsWith("+")) {
            "+$digits"
        } else {
            digits
        }
    }

    private fun sentFailureMessage(
        resultCode: Int
    ): String {
        return when (resultCode) {
            SmsManager.RESULT_ERROR_GENERIC_FAILURE ->
                "The mobile network could not send the SMS."

            SmsManager.RESULT_ERROR_RADIO_OFF ->
                "The phone radio is turned off."

            SmsManager.RESULT_ERROR_NULL_PDU ->
                "Android could not create the SMS message."

            SmsManager.RESULT_ERROR_NO_SERVICE ->
                "No mobile network service is available."

            SmsManager.RESULT_ERROR_LIMIT_EXCEEDED ->
                "The device SMS sending limit was exceeded."

            SmsManager.RESULT_ERROR_FDN_CHECK_FAILURE ->
                "The phone's fixed-dialing settings blocked the SMS."

            else ->
                "SMS sending failed with error code $resultCode."
        }
    }

    private data class MultipartMessageProgress(
        val totalParts: Int,
        var sentParts: Int = 0,
        var deliveredParts: Int = 0,
        var hasFailed: Boolean = false
    )

    private companion object {
        const val ACTION_SMS_SENT =
            "com.safetap.app.action.SMS_SENT"

        const val ACTION_SMS_DELIVERED =
            "com.safetap.app.action.SMS_DELIVERED"

        const val EXTRA_RECIPIENT =
            "com.safetap.app.extra.SMS_RECIPIENT"

        const val EXTRA_DISPATCH_ID =
            "com.safetap.app.extra.SMS_DISPATCH_ID"

        const val EXTRA_PART_INDEX =
            "com.safetap.app.extra.SMS_PART_INDEX"

        const val INVALID_DISPATCH_ID = -1

        const val INITIAL_REQUEST_CODE = 10_000
        const val INITIAL_DISPATCH_ID = 1

        const val MINIMUM_PHONE_DIGITS = 7
        const val MAXIMUM_PHONE_DIGITS = 15
    }
}