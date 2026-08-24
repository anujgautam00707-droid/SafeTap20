package com.safetap.app.data.sos.services

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.CancellationSignal
import com.safetap.app.data.status.AppStatusRepository
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.safetap.app.domain.sos.model.LocationResult
import com.safetap.app.domain.sos.services.LocationProvider
import com.safetap.app.domain.sos.services.PermissionChecker
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

class DefaultLocationProvider(
    private val context: Context,
    private val permissionChecker: PermissionChecker,
    private val appStatusRepository: AppStatusRepository
) : LocationProvider {

    private val fusedLocationClient: FusedLocationProviderClient by lazy {
        LocationServices.getFusedLocationProviderClient(context)
    }

    private val locationManager by lazy {
        context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
    }

    override fun isGpsEnabled(): Boolean {
        val lm = locationManager ?: return false
        return lm.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    @SuppressLint("MissingPermission")
    override suspend fun getCurrentLocation(): LocationResult? = withContext(Dispatchers.IO) {
        if (!permissionChecker.hasLocationPermission()) return@withContext null

        // 1. Try Google Play Services Fused Location Client (Priority High Accuracy)
        val fusedLoc = runCatching {
            val cts = CancellationTokenSource()
            withTimeoutOrNull(3000L) {
                fusedLocationClient.getCurrentLocation(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    cts.token
                ).await()
            }
        }.getOrNull()

        if (fusedLoc != null) {
            return@withContext fusedLoc.toLocationResult(isLastKnown = false)
        }

        // 2. Fallback to LocationManager if Fused Client was unavailable or timed out
        val lm = locationManager ?: return@withContext null
        withTimeoutOrNull(2000L) {
            suspendCancellableCoroutine { continuation ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    val cancellationSignal = CancellationSignal()
                    continuation.invokeOnCancellation {
                        cancellationSignal.cancel()
                    }

                    val provider = when {
                        lm.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
                        lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
                        else -> LocationManager.PASSIVE_PROVIDER
                    }

                    try {
                        lm.getCurrentLocation(
                            provider,
                            cancellationSignal,
                            Executors.newSingleThreadExecutor()
                        ) { location: Location? ->
                            if (continuation.isActive) {
                                continuation.resume(location?.toLocationResult(isLastKnown = false))
                                if (location != null) {
                                    appStatusRepository.updateLocationSynchronized()
                                    continuation.resume(location.toLocationResult(isLastKnown = false))
                                } else {
                                    continuation.resume(null)
                                }
                            }
                        }
                    } catch (e: Exception) {
                        if (continuation.isActive) {
                            continuation.resume(null)
                        }
                    }
                } else {
                    val listener = object : LocationListener {
                        override fun onLocationChanged(location: Location) {
                            lm.removeUpdates(this)
                            if (continuation.isActive) {
                                appStatusRepository.updateLocationSynchronized()
                                continuation.resume(location.toLocationResult(isLastKnown = false))
                            }
                        }

                        @Deprecated("Deprecated in Java")
                        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                        override fun onProviderEnabled(provider: String) {}
                        override fun onProviderDisabled(provider: String) {}
                    }

                    continuation.invokeOnCancellation {
                        try {
                            lm.removeUpdates(listener)
                        } catch (_: Exception) {}
                    }

                    try {
                        val provider = if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                            LocationManager.GPS_PROVIDER
                        } else {
                            LocationManager.NETWORK_PROVIDER
                        }
                        lm.requestSingleUpdate(provider, listener, null)
                    } catch (e: Exception) {
                        if (continuation.isActive) {
                            continuation.resume(null)
                        }
                    }
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    override suspend fun getLastKnownLocation(): LocationResult? = withContext(Dispatchers.IO) {
        if (!permissionChecker.hasLocationPermission()) return@withContext null

        // 1. Try FusedLocationProviderClient first
        val fusedLast = runCatching {
            fusedLocationClient.lastLocation.await()
        }.getOrNull()

        if (fusedLast != null) {
            return@withContext fusedLast.toLocationResult(isLastKnown = true)
        }

        // 2. Fallback to LocationManager
        val lm = locationManager ?: return@withContext null

        val gpsLoc = try {
            if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            } else null
        } catch (_: Exception) { null }

        val netLoc = try {
            if (lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            } else null
        } catch (_: Exception) { null }

        val best = when {
            gpsLoc != null && netLoc != null -> if (gpsLoc.time >= netLoc.time) gpsLoc else netLoc
            gpsLoc != null -> gpsLoc
            else -> netLoc
        }

        best?.toLocationResult(isLastKnown = true)
    }

    override suspend fun getBestAvailableLocation(): LocationResult? {
        val current = getCurrentLocation()
        if (current != null) return current
        return getLastKnownLocation()
    }

    private fun Location.toLocationResult(isLastKnown: Boolean): LocationResult {
        return LocationResult(
            latitude = latitude,
            longitude = longitude,
            accuracy = accuracy,
            isLastKnownLocation = isLastKnown,
            timestamp = time
        )
    }
}
