package com.safetap.app.data.sos.services

import android.Manifest
import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.core.content.ContextCompat
import com.safetap.app.domain.sos.model.SosError
import com.safetap.app.domain.sos.services.EmergencyCallManager

class DefaultEmergencyCallManager(
    context: Context
) : EmergencyCallManager {

    private val appContext = context.applicationContext

    override fun getEmergencyDialIntent(
        emergencyNumber: String
    ): Intent {
        val normalizedNumber = normalizePhoneNumber(
            phoneNumber = emergencyNumber,
            minimumDigits = MINIMUM_EMERGENCY_NUMBER_DIGITS,
            fallbackNumber = DEFAULT_EMERGENCY_NUMBER
        )

        return createPhoneIntent(
            action = Intent.ACTION_DIAL,
            phoneNumber = normalizedNumber
        )
    }

    override fun launchEmergencyDialer(
        emergencyNumber: String
    ): Result<Unit> {
        return try {
            appContext.startActivity(
                getEmergencyDialIntent(emergencyNumber)
            )

            Result.success(Unit)
        } catch (exception: ActivityNotFoundException) {
            Result.failure(
                SosError.NoDialerApp(
                    "No phone application is available."
                )
            )
        } catch (exception: Exception) {
            Result.failure(
                SosError.UnexpectedError(exception)
            )
        }
    }

    override fun getDirectCallIntent(
        phoneNumber: String
    ): Intent {
        val normalizedNumber = normalizePhoneNumber(
            phoneNumber = phoneNumber,
            minimumDigits = MINIMUM_CONTACT_NUMBER_DIGITS
        )

        return createPhoneIntent(
            action = Intent.ACTION_CALL,
            phoneNumber = normalizedNumber
        )
    }

    @SuppressLint("MissingPermission")
    override fun launchDirectCall(
        phoneNumber: String
    ): Result<Unit> {
        if (!hasCallPermission()) {
            return Result.failure(
                SosError.PermissionDenied(
                    "Phone permission is required to call the primary contact."
                )
            )
        }

        if (!supportsVoiceCalling()) {
            return Result.failure(
                SosError.NoDialerApp(
                    "This device does not support phone calls."
                )
            )
        }

        return try {
            appContext.startActivity(
                getDirectCallIntent(phoneNumber)
            )

            Result.success(Unit)
        } catch (exception: SecurityException) {
            Result.failure(
                SosError.PermissionDenied(
                    "SafeTap does not have permission to place phone calls."
                )
            )
        } catch (exception: ActivityNotFoundException) {
            Result.failure(
                SosError.NoDialerApp(
                    "No phone application is available to place the call."
                )
            )
        } catch (exception: Exception) {
            Result.failure(
                SosError.UnexpectedError(exception)
            )
        }
    }

    private fun createPhoneIntent(
        action: String,
        phoneNumber: String
    ): Intent {
        return Intent(action).apply {
            data = Uri.fromParts(
                TELEPHONE_SCHEME,
                phoneNumber,
                null
            )

            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    private fun hasCallPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.CALL_PHONE
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun supportsVoiceCalling(): Boolean {
        return appContext.packageManager.hasSystemFeature(
            PackageManager.FEATURE_TELEPHONY
        )
    }

    private fun normalizePhoneNumber(
        phoneNumber: String,
        minimumDigits: Int,
        fallbackNumber: String? = null
    ): String {
        val sourceNumber = phoneNumber
            .trim()
            .ifBlank {
                fallbackNumber.orEmpty()
            }

        val digits = sourceNumber.filter(Char::isDigit)

        require(
            digits.length in minimumDigits..MAXIMUM_PHONE_NUMBER_DIGITS
        ) {
            "Enter a valid phone number."
        }

        return if (sourceNumber.startsWith("+")) {
            "+$digits"
        } else {
            digits
        }
    }

    private companion object {
        const val TELEPHONE_SCHEME = "tel"
        const val DEFAULT_EMERGENCY_NUMBER = "112"
        const val MINIMUM_EMERGENCY_NUMBER_DIGITS = 3
        const val MINIMUM_CONTACT_NUMBER_DIGITS = 7
        const val MAXIMUM_PHONE_NUMBER_DIGITS = 15
    }
}