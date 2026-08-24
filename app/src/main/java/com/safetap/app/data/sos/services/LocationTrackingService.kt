package com.safetap.app.data.sos.services

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.safetap.app.MainActivity
import com.safetap.app.R
import com.safetap.app.di.AppContainer
import com.safetap.app.domain.sos.model.LocationTrackingState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class LocationTrackingService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private var activeSosId: String? = null
    private var liveLocationToken: String? = null
    private var updatesCount = 0L
    @Volatile
    private var isTrackingStopped = false

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            if (isTrackingStopped || activeSosId == null) return
            val location = result.lastLocation ?: return
            handleLocationUpdate(location)
        }

        override fun onLocationAvailability(availability: com.google.android.gms.location.LocationAvailability) {
            if (isTrackingStopped || activeSosId == null) return
            if (!availability.isLocationAvailable && updatesCount == 0L) {
                DefaultLocationTrackingManager.postTrackingState(
                    LocationTrackingState.LocationUnavailable(
                        reason = "GPS signal temporarily lost. Waiting for satellite lock..."
                    )
                )
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action

        if (action == ACTION_STOP_TRACKING) {
            stopTrackingAndSelf()
            return START_NOT_STICKY
        }

        if (action == ACTION_START_TRACKING) {
            val sosId = intent.getStringExtra(EXTRA_SOS_ID)
            val token = intent.getStringExtra(EXTRA_TOKEN)

            if (!sosId.isNullOrBlank()) {
                isTrackingStopped = false
                activeSosId = sosId
                liveLocationToken = token
                updatesCount = 0L
                startForegroundWithNotification()
                requestContinuousLocationUpdates()
            } else {
                stopTrackingAndSelf()
            }
        }

        return START_STICKY
    }

    private fun startForegroundWithNotification() {
        val notification = buildNotification(
            statusText = "Broadcasting live GPS coordinates to trusted contacts"
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        DefaultLocationTrackingManager.postTrackingState(LocationTrackingState.Initializing)
    }

    @SuppressLint("MissingPermission")
    private fun requestContinuousLocationUpdates() {
        try {
            val locationRequest = LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY,
                LOCATION_UPDATE_INTERVAL_MS
            ).apply {
                setMinUpdateIntervalMillis(FASTEST_LOCATION_INTERVAL_MS)
                setMinUpdateDistanceMeters(0f)
                setWaitForAccurateLocation(false)
            }.build()

            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )

            // Prime coordinates immediately from lastLocation while waiting for first continuous update
            fusedLocationClient.lastLocation.addOnSuccessListener { lastLoc ->
                if (!isTrackingStopped && lastLoc != null && activeSosId != null && updatesCount == 0L) {
                    handleLocationUpdate(lastLoc)
                }
            }
        } catch (e: Exception) {
            DefaultLocationTrackingManager.postTrackingState(
                LocationTrackingState.Error("Failed to initiate location tracking: ${e.localizedMessage}", e)
            )
        }
    }

    private fun handleLocationUpdate(location: Location) {
        if (isTrackingStopped) return
        val sosId = activeSosId ?: return
        updatesCount++

        serviceScope.launch {
            if (isTrackingStopped) return@launch

            val battery = runCatching {
                if (AppContainer.isInitialized) {
                    AppContainer.batteryProvider.getBatteryPercentage()
                } else 100
            }.getOrDefault(100)

            DefaultLocationTrackingManager.postTrackingState(
                LocationTrackingState.Tracking(
                    latitude = location.latitude,
                    longitude = location.longitude,
                    accuracy = location.accuracy,
                    timestamp = location.time,
                    batteryPercentage = battery,
                    updatesCount = updatesCount
                )
            )

            // Update persistent ongoing notification with real coordinates
            updateNotificationWithLocation(location.latitude, location.longitude, location.accuracy)

            // Upload directly to Firebase Realtime Database
            if (!isTrackingStopped && AppContainer.isInitialized) {
                runCatching {
                    AppContainer.sosRemoteDataSource.updateSosLocation(
                        sosId = sosId,
                        latitude = location.latitude,
                        longitude = location.longitude,
                        accuracy = location.accuracy,
                        battery = battery
                    )
                }
            }
        }
    }

    private fun updateNotificationWithLocation(lat: Double, lng: Double, accuracy: Float) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        val locText = "GPS: %.4f, %.4f (±%.1fm)".format(lat, lng, accuracy)
        val notification = buildNotification(statusText = locText)
        notificationManager?.notify(NOTIFICATION_ID, notification)
    }

    private fun buildNotification(statusText: String): Notification {
        val contentIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("🚨 SafeTap SOS Live Location Active")
            .setContentText(statusText)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("SafeTap is continuously synchronizing your live GPS location with your emergency contacts.\n$statusText")
            )
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setOngoing(true)
            .setAutoCancel(false)
            .setContentIntent(pendingIntent)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notification for active SafeTap SOS live location tracking"
                enableLights(true)
                enableVibration(false)
                setShowBadge(true)
            }

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            notificationManager?.createNotificationChannel(channel)
        }
    }

    private fun stopTrackingAndSelf() {
        if (isTrackingStopped) return
        isTrackingStopped = true
        activeSosId = null
        liveLocationToken = null

        try {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        } catch (_: Exception) {}

        serviceScope.cancel()

        DefaultLocationTrackingManager.postTrackingState(LocationTrackingState.Stopped)

        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        stopTrackingAndSelf()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_START_TRACKING = "com.safetap.app.action.START_TRACKING"
        const val ACTION_STOP_TRACKING = "com.safetap.app.action.STOP_TRACKING"

        const val EXTRA_SOS_ID = "com.safetap.app.extra.SOS_ID"
        const val EXTRA_TOKEN = "com.safetap.app.extra.TOKEN"

        const val CHANNEL_ID = "safetap_location_tracking_channel"
        const val CHANNEL_NAME = "SafeTap Live Tracking"
        const val NOTIFICATION_ID = 9120

        const val LOCATION_UPDATE_INTERVAL_MS = 5000L
        const val FASTEST_LOCATION_INTERVAL_MS = 3000L
    }
}
