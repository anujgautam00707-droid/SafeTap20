package com.safetap.app.data.sos

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.safetap.app.domain.sos.model.EmergencyData
import com.safetap.app.domain.sos.model.SosStatus
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class FirebaseSosRemoteDataSource(
    private val database: FirebaseDatabase = FirebaseDatabase.getInstance(DATABASE_URL),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : SosRemoteDataSource {

    private val sosRef: DatabaseReference
        get() = database.getReference(SOS_NODE)

    private val liveLocationsRef: DatabaseReference
        get() = database.getReference(LIVE_LOCATIONS_NODE)

    private val liveLocationOwnersRef: DatabaseReference
        get() = database.getReference(LIVE_LOCATION_OWNERS_NODE)

    // In-memory cache mapping sosId -> liveLocationToken for fast lookups
    private val tokenCache = ConcurrentHashMap<String, String>()

    private suspend fun ensureAuthenticated(): String? {
        val current = auth.currentUser
        if (current != null) {
            return current.uid
        }
        return runCatching {
            val result = auth.signInAnonymously().await()
            val uid = result.user?.uid
            Log.d(TAG, "Authenticated anonymously with Firebase Auth UID: $uid")
            uid
        }.onFailure {
            Log.e(TAG, "Anonymous auth failed: ${it.localizedMessage}", it)
        }.getOrNull()
    }

    override suspend fun createSosEvent(emergencyData: EmergencyData): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                val currentUid = ensureAuthenticated()
                val userId = currentUid ?: emergencyData.userId
                val sosId = emergencyData.sosId
                val token = emergencyData.liveLocationToken.trim().lowercase()

                val safeTokenPrefix = if (token.length >= 8) token.substring(0, 8) + "..." else "token_present"

                Log.d(TAG, ">>> [SOS_CREATE] SOS_ID: $sosId, LIVE_TOKEN_PREFIX: $safeTokenPrefix, RTDB_URL: $DATABASE_URL, AUTH_UID: $userId, LAT: ${emergencyData.latitude}, LNG: ${emergencyData.longitude}, STATUS: ${emergencyData.status}")

                if (token.isNotBlank()) {
                    tokenCache[sosId] = token
                }

                // 1. Private full SOS session record
                val sessionMap = hashMapOf<String, Any?>(
                    FIELD_SOS_ID to sosId,
                    FIELD_USER_ID to userId,
                    FIELD_STATUS to emergencyData.status.name,
                    FIELD_STARTED_AT to emergencyData.startedAt,
                    FIELD_ENDED_AT to emergencyData.endedAt,
                    FIELD_BATTERY to emergencyData.batteryPercentage,
                    FIELD_LIVE_LOCATION_TOKEN to token,
                    FIELD_EMERGENCY_MESSAGE to emergencyData.emergencyMessage,
                    FIELD_CURRENT_LOCATION to hashMapOf(
                        FIELD_LATITUDE to emergencyData.latitude,
                        FIELD_LONGITUDE to emergencyData.longitude,
                        FIELD_ACCURACY to emergencyData.locationAccuracy,
                        FIELD_TIMESTAMP to emergencyData.timestamp
                    )
                )

                sosRef.child(sosId).setValue(sessionMap).await()
                Log.d(TAG, "Write success: sos/$sosId")

                if (token.isNotBlank()) {
                    // 2. Set token ownership metadata (strictly private mapping)
                    val ownerMap = hashMapOf<String, Any>(
                        FIELD_USER_ID to userId
                    )
                    liveLocationOwnersRef.child(token).setValue(ownerMap).await()
                    Log.d(TAG, "Write success: live_location_owners/$safeTokenPrefix")

                    // 3. Sanitized live stream (contains NO userId, NO phone, NO contacts)
                    val sanitizedLiveMap = hashMapOf<String, Any?>(
                        FIELD_LATITUDE to emergencyData.latitude,
                        FIELD_LONGITUDE to emergencyData.longitude,
                        FIELD_ACCURACY to emergencyData.locationAccuracy,
                        FIELD_BATTERY to emergencyData.batteryPercentage,
                        FIELD_TIMESTAMP to emergencyData.timestamp,
                        FIELD_STATUS to emergencyData.status.name,
                        FIELD_STARTED_AT to emergencyData.startedAt,
                        FIELD_ENDED_AT to null
                    )

                    liveLocationsRef.child(token).setValue(sanitizedLiveMap).await()
                    Log.d(TAG, "Write success: live_locations/$safeTokenPrefix [ACTIVE]")
                }

                sosId
            }.onFailure { error ->
                Log.e(TAG, "Failed creating SOS session in Firebase RTDB: ${error.localizedMessage}", error)
            }
        }

    override suspend fun updateSosLocation(
        sosId: String,
        latitude: Double,
        longitude: Double,
        accuracy: Float,
        battery: Int?
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            ensureAuthenticated()
            val timestamp = System.currentTimeMillis()

            // 1. Update private record
            val locationMap = hashMapOf<String, Any>(
                FIELD_LATITUDE to latitude,
                FIELD_LONGITUDE to longitude,
                FIELD_ACCURACY to accuracy,
                FIELD_TIMESTAMP to timestamp
            )

            val privateUpdates = hashMapOf<String, Any>(
                "$sosId/$FIELD_CURRENT_LOCATION" to locationMap
            )

            if (battery != null) {
                privateUpdates["$sosId/$FIELD_BATTERY"] = battery
            }

            sosRef.updateChildren(privateUpdates).await()

            // 2. Update sanitized live location stream
            var token = tokenCache[sosId]
            if (token.isNullOrBlank()) {
                val snapshot = sosRef.child(sosId).child(FIELD_LIVE_LOCATION_TOKEN).get().await()
                token = snapshot.getValue(String::class.java)?.trim()?.lowercase()
                if (!token.isNullOrBlank()) {
                    tokenCache[sosId] = token
                }
            }

            if (!token.isNullOrBlank()) {
                val liveUpdates = hashMapOf<String, Any>(
                    "$token/$FIELD_LATITUDE" to latitude,
                    "$token/$FIELD_LONGITUDE" to longitude,
                    "$token/$FIELD_ACCURACY" to accuracy,
                    "$token/$FIELD_TIMESTAMP" to timestamp,
                    "$token/$FIELD_STATUS" to SosStatus.ACTIVE.name
                )
                if (battery != null) {
                    liveUpdates["$token/$FIELD_BATTERY"] = battery
                }
                liveLocationsRef.updateChildren(liveUpdates).await()
            }

            Unit
        }.onFailure { error ->
            Log.e(TAG, "Failed updating live coordinates in Firebase RTDB: ${error.localizedMessage}", error)
        }
    }

    override suspend fun closeSosEvent(sosId: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                ensureAuthenticated()
                val endedTimestamp = System.currentTimeMillis()

                Log.d(TAG, ">>> [SOS_CLOSE] Closing SOS: $sosId at $endedTimestamp")

                // 1. Close private record
                val closeUpdates = hashMapOf<String, Any>(
                    "$sosId/$FIELD_STATUS" to SosStatus.ENDED.name,
                    "$sosId/$FIELD_ENDED_AT" to endedTimestamp
                )

                sosRef.updateChildren(closeUpdates).await()

                // 2. Update sanitized live location stream to ENDED
                var token = tokenCache.remove(sosId)
                if (token.isNullOrBlank()) {
                    val tokenSnapshot = sosRef.child(sosId).child(FIELD_LIVE_LOCATION_TOKEN).get().await()
                    token = tokenSnapshot.getValue(String::class.java)?.trim()?.lowercase()
                }

                if (!token.isNullOrBlank()) {
                    val liveCloseUpdates = hashMapOf<String, Any>(
                        "$token/$FIELD_STATUS" to SosStatus.ENDED.name,
                        "$token/$FIELD_ENDED_AT" to endedTimestamp
                    )
                    liveLocationsRef.updateChildren(liveCloseUpdates).await()
                    Log.d(TAG, "Live tracking token status set to ENDED")
                }

                Unit
            }.onFailure { error ->
                Log.e(TAG, "Failed closing SOS session in Firebase RTDB: ${error.localizedMessage}", error)
            }
        }

    override suspend fun getActiveSosEvent(sosId: String): Result<EmergencyData?> =
        withContext(Dispatchers.IO) {
            runCatching {
                val snapshot = sosRef.child(sosId).get().await()
                snapshot.toEmergencyData()
            }
        }

    override fun observeSosEvent(sosId: String): Flow<EmergencyData?> = callbackFlow {
        val targetRef = sosRef.child(sosId)

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                trySend(snapshot.toEmergencyData())
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }

        targetRef.addValueEventListener(listener)

        awaitClose {
            targetRef.removeEventListener(listener)
        }
    }.flowOn(Dispatchers.IO)

    private fun DataSnapshot.toEmergencyData(): EmergencyData? {
        if (!exists()) return null

        val sosId = child(FIELD_SOS_ID).getValue(String::class.java)
            ?: key
            ?: return null

        val userId = child(FIELD_USER_ID).getValue(String::class.java) ?: "anonymous_user"
        val statusString = child(FIELD_STATUS).getValue(String::class.java) ?: SosStatus.ACTIVE.name
        val status = runCatching { SosStatus.valueOf(statusString) }.getOrDefault(SosStatus.ACTIVE)
        val startedAt = child(FIELD_STARTED_AT).getValue(Long::class.java) ?: System.currentTimeMillis()
        val endedAt = child(FIELD_ENDED_AT).getValue(Long::class.java)
        val battery = child(FIELD_BATTERY).getValue(Int::class.java) ?: 100
        val liveToken = child(FIELD_LIVE_LOCATION_TOKEN).getValue(String::class.java) ?: ""
        val message = child(FIELD_EMERGENCY_MESSAGE).getValue(String::class.java)
            ?: "EMERGENCY: SafeTap user triggered an SOS alert. Immediate assistance required!"

        val locSnapshot = child(FIELD_CURRENT_LOCATION)
        val latitude = locSnapshot.child(FIELD_LATITUDE).getValue(Double::class.java) ?: 0.0
        val longitude = locSnapshot.child(FIELD_LONGITUDE).getValue(Double::class.java) ?: 0.0
        val accuracy = locSnapshot.child(FIELD_ACCURACY).getValue(Float::class.java)
            ?: locSnapshot.child(FIELD_ACCURACY).getValue(Double::class.java)?.toFloat()
            ?: 0f
        val timestamp = locSnapshot.child(FIELD_TIMESTAMP).getValue(Long::class.java) ?: startedAt

        return EmergencyData(
            sosId = sosId,
            userId = userId,
            latitude = latitude,
            longitude = longitude,
            locationAccuracy = accuracy,
            batteryPercentage = battery,
            timestamp = timestamp,
            startedAt = startedAt,
            endedAt = endedAt,
            status = status,
            isLastKnownLocation = false,
            emergencyMessage = message,
            liveLocationToken = liveToken
        )
    }

    companion object {
        private const val TAG = "SafeTapRTDB"
        const val DATABASE_URL = "https://safetap-2fb1e-default-rtdb.asia-southeast1.firebasedatabase.app"

        const val SOS_NODE = "sos"
        const val LIVE_LOCATIONS_NODE = "live_locations"
        const val LIVE_LOCATION_OWNERS_NODE = "live_location_owners"

        const val FIELD_SOS_ID = "sosId"
        const val FIELD_USER_ID = "userId"
        const val FIELD_STATUS = "status"
        const val FIELD_STARTED_AT = "startedAt"
        const val FIELD_ENDED_AT = "endedAt"
        const val FIELD_BATTERY = "battery"
        const val FIELD_LIVE_LOCATION_TOKEN = "liveLocationToken"
        const val FIELD_EMERGENCY_MESSAGE = "emergencyMessage"
        const val FIELD_CURRENT_LOCATION = "currentLocation"
        const val FIELD_LATITUDE = "latitude"
        const val FIELD_LONGITUDE = "longitude"
        const val FIELD_ACCURACY = "accuracy"
        const val FIELD_TIMESTAMP = "timestamp"
    }
}
