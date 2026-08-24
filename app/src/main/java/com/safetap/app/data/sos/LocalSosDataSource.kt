package com.safetap.app.data.sos

import android.content.Context
import com.safetap.app.domain.sos.model.EmergencyData
import com.safetap.app.domain.sos.model.SosStatus
import org.json.JSONObject

class LocalSosDataSource(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE
    )

    fun saveActiveSos(data: EmergencyData, isSynced: Boolean) {
        val json = JSONObject().apply {
            put(KEY_SOS_ID, data.sosId)
            put(KEY_USER_ID, data.userId)
            put(KEY_LATITUDE, data.latitude)
            put(KEY_LONGITUDE, data.longitude)
            put(KEY_ACCURACY, data.locationAccuracy.toDouble())
            put(KEY_BATTERY, data.batteryPercentage)
            put(KEY_TIMESTAMP, data.timestamp)
            put(KEY_STATUS, data.status.name)
            put(KEY_IS_LAST_KNOWN, data.isLastKnownLocation)
            put(KEY_MESSAGE, data.emergencyMessage)
        }
        prefs.edit()
            .putString(KEY_ACTIVE_SOS_JSON, json.toString())
            .putBoolean(KEY_IS_SYNCED, isSynced)
            .putBoolean(KEY_IS_PENDING_CLOSURE, false)
            .apply()
    }

    fun getActiveSos(): EmergencyData? {
        val jsonString = prefs.getString(KEY_ACTIVE_SOS_JSON, null) ?: return null
        return try {
            val json = JSONObject(jsonString)
            EmergencyData(
                sosId = json.getString(KEY_SOS_ID),
                userId = json.getString(KEY_USER_ID),
                latitude = json.getDouble(KEY_LATITUDE),
                longitude = json.getDouble(KEY_LONGITUDE),
                locationAccuracy = json.getDouble(KEY_ACCURACY).toFloat(),
                batteryPercentage = json.getInt(KEY_BATTERY),
                timestamp = json.getLong(KEY_TIMESTAMP),
                status = SosStatus.valueOf(json.getString(KEY_STATUS)),
                isLastKnownLocation = json.getBoolean(KEY_IS_LAST_KNOWN),
                emergencyMessage = json.getString(KEY_MESSAGE)
            )
        } catch (e: Exception) {
            null
        }
    }

    fun isSynced(): Boolean = prefs.getBoolean(KEY_IS_SYNCED, true)

    fun markSynced() {
        prefs.edit().putBoolean(KEY_IS_SYNCED, true).apply()
    }

    fun markPendingClosure(sosId: String) {
        prefs.edit()
            .putBoolean(KEY_IS_PENDING_CLOSURE, true)
            .putString(KEY_PENDING_CLOSURE_ID, sosId)
            .apply()
    }

    fun isPendingClosure(): Boolean = prefs.getBoolean(KEY_IS_PENDING_CLOSURE, false)

    fun getPendingClosureId(): String? = prefs.getString(KEY_PENDING_CLOSURE_ID, null)

    fun clearPendingClosure() {
        prefs.edit()
            .putBoolean(KEY_IS_PENDING_CLOSURE, false)
            .remove(KEY_PENDING_CLOSURE_ID)
            .apply()
    }

    fun clearActiveSos() {
        prefs.edit()
            .remove(KEY_ACTIVE_SOS_JSON)
            .remove(KEY_IS_SYNCED)
            .apply()
    }

    private companion object {
        const val PREFERENCES_NAME = "safetap_local_sos"
        
        const val KEY_ACTIVE_SOS_JSON = "active_sos_json"
        const val KEY_IS_SYNCED = "is_synced"
        const val KEY_IS_PENDING_CLOSURE = "is_pending_closure"
        const val KEY_PENDING_CLOSURE_ID = "pending_closure_id"

        const val KEY_SOS_ID = "sosId"
        const val KEY_USER_ID = "userId"
        const val KEY_LATITUDE = "latitude"
        const val KEY_LONGITUDE = "longitude"
        const val KEY_ACCURACY = "locationAccuracy"
        const val KEY_BATTERY = "batteryPercentage"
        const val KEY_TIMESTAMP = "timestamp"
        const val KEY_STATUS = "status"
        const val KEY_IS_LAST_KNOWN = "isLastKnownLocation"
        const val KEY_MESSAGE = "emergencyMessage"
    }
}
