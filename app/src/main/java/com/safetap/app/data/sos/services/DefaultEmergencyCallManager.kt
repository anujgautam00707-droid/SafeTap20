package com.safetap.app.data.sos.services

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.safetap.app.domain.sos.model.SosError
import com.safetap.app.domain.sos.services.EmergencyCallManager

class DefaultEmergencyCallManager(
    private val context: Context
) : EmergencyCallManager {

    override fun getEmergencyDialIntent(emergencyNumber: String): Intent {
        val sanitized = emergencyNumber.replace("[^0-9+]".toRegex(), "").ifBlank { "911" }
        return Intent(Intent.ACTION_DIAL).apply {
            data = Uri.parse("tel:$sanitized")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    }

    override fun launchEmergencyDialer(emergencyNumber: String): Result<Unit> {
        val intent = getEmergencyDialIntent(emergencyNumber)
        return try {
            context.startActivity(intent)
            Result.success(Unit)
        } catch (e: ActivityNotFoundException) {
            Result.failure(SosError.NoDialerApp("No application available on device to dial $emergencyNumber"))
        } catch (e: Exception) {
            Result.failure(SosError.UnexpectedError(e, "Failed to launch emergency dialer: ${e.localizedMessage}"))
        }
    }
}
