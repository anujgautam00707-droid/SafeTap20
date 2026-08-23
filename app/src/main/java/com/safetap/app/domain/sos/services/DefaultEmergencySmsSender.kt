package com.safetap.app.data.sos.services

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.SmsManager
import androidx.core.content.ContextCompat
import com.safetap.app.domain.sos.services.EmergencySmsSender

class DefaultEmergencySmsSender(
    context: Context
) : EmergencySmsSender {

    private val appContext = context.applicationContext

    override fun sendEmergencyMessage(
        recipients: List<String>,
        message: String
    ): Result<Int> {
        return runCatching {
            require(message.isNotBlank()) {
                "Emergency SMS message cannot be empty."
            }

            check(hasSmsPermission()) {
                "SMS permission has not been granted."
            }

            check(supportsSmsMessaging()) {
                "This device does not support SMS messaging."
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

            val trimmedMessage = message.trim()

            validRecipients.forEach { recipient ->
                sendMessage(
                    smsManager = smsManager,
                    recipient = recipient,
                    message = trimmedMessage
                )
            }

            validRecipients.size
        }
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
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
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

    private fun sendMessage(
        smsManager: SmsManager,
        recipient: String,
        message: String
    ) {
        val messageParts = smsManager.divideMessage(message)

        if (messageParts.size == 1) {
            smsManager.sendTextMessage(
                recipient,
                null,
                message,
                null,
                null
            )
        } else {
            smsManager.sendMultipartTextMessage(
                recipient,
                null,
                ArrayList(messageParts),
                null,
                null
            )
        }
    }

    private fun normalizePhoneNumber(
        phoneNumber: String
    ): String {
        val trimmedNumber = phoneNumber.trim()
        val digits = trimmedNumber.filter(Char::isDigit)

        require(
            digits.length in MINIMUM_PHONE_DIGITS..MAXIMUM_PHONE_DIGITS
        ) {
            "Invalid trusted-contact phone number: $phoneNumber"
        }

        return if (trimmedNumber.startsWith("+")) {
            "+$digits"
        } else {
            digits
        }
    }

    private companion object {
        const val MINIMUM_PHONE_DIGITS = 7
        const val MAXIMUM_PHONE_DIGITS = 15
    }
}