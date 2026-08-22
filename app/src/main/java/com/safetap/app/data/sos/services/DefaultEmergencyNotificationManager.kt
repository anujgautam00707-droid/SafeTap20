package com.safetap.app.data.sos.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.safetap.app.MainActivity
import com.safetap.app.R
import com.safetap.app.domain.sos.model.EmergencyData
import com.safetap.app.domain.sos.services.EmergencyNotificationManager
import com.safetap.app.domain.sos.services.PermissionChecker

class DefaultEmergencyNotificationManager(
    private val context: Context,
    private val permissionChecker: PermissionChecker
) : EmergencyNotificationManager {

    companion object {
        const val SOS_NOTIFICATION_ID = 9119
        const val CHANNEL_ID = "safetap_emergency_channel"
        const val CHANNEL_NAME = "SafeTap Emergency Alerts"
    }

    private val notificationManager by lazy {
        context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "High priority notifications triggered during active SafeTap SOS emergencies."
                enableLights(true)
                enableVibration(true)
            }
            notificationManager?.createNotificationChannel(channel)
        }
    }

    override fun showActiveSosNotification(emergencyData: EmergencyData) {
        if (!permissionChecker.hasNotificationPermission()) return

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        val locationText = if (emergencyData.latitude != 0.0 && emergencyData.longitude != 0.0) {
            "Location: %.4f, %.4f (±%.0fm)".format(
                emergencyData.latitude,
                emergencyData.longitude,
                emergencyData.locationAccuracy
            )
        } else {
            "Location lock in progress..."
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("🚨 SafeTap SOS Alert ACTIVE")
            .setContentText("Emergency broadcast in progress. $locationText")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("SafeTap is actively broadcasting your emergency status and GPS location to your trusted contacts.\n$locationText\nBattery: ${emergencyData.batteryPercentage}%")
            )
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setOngoing(true)
            .setAutoCancel(false)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager?.notify(SOS_NOTIFICATION_ID, notification)
    }

    override fun cancelSosNotification() {
        notificationManager?.cancel(SOS_NOTIFICATION_ID)
    }
}
