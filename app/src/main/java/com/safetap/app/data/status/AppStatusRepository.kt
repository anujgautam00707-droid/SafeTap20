package com.safetap.app.data.status

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AppStatusRepository(context: Context) {

    private val preferences = context.applicationContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE
    )

    private val _safeTapProtectedAt = MutableStateFlow(
        getLongOrNull(KEY_SAFETAP_PROTECTED_AT)
    )
    val safeTapProtectedAt: StateFlow<Long?> = _safeTapProtectedAt.asStateFlow()

    private val _locationSynchronizedAt = MutableStateFlow(
        getLongOrNull(KEY_LOCATION_SYNCHRONIZED_AT)
    )
    val locationSynchronizedAt: StateFlow<Long?> = _locationSynchronizedAt.asStateFlow()

    private val _contactsLinkedAt = MutableStateFlow(
        getLongOrNull(KEY_CONTACTS_LINKED_AT)
    )
    val contactsLinkedAt: StateFlow<Long?> = _contactsLinkedAt.asStateFlow()

    fun updateSafeTapProtected() {
        val timestamp = System.currentTimeMillis()
        saveLong(KEY_SAFETAP_PROTECTED_AT, timestamp)
        _safeTapProtectedAt.value = timestamp
    }

    fun updateLocationSynchronized() {
        val timestamp = System.currentTimeMillis()
        saveLong(KEY_LOCATION_SYNCHRONIZED_AT, timestamp)
        _locationSynchronizedAt.value = timestamp
    }

    fun updateContactsLinked() {
        val timestamp = System.currentTimeMillis()
        saveLong(KEY_CONTACTS_LINKED_AT, timestamp)
        _contactsLinkedAt.value = timestamp
    }

    private fun getLongOrNull(key: String): Long? {
        val value = preferences.getLong(key, -1L)
        return if (value == -1L) null else value
    }

    private fun saveLong(key: String, value: Long) {
        preferences.edit().putLong(key, value).apply()
    }

    private companion object {
        const val PREFERENCES_NAME = "safetap_app_status"
        const val KEY_SAFETAP_PROTECTED_AT = "safetap_protected_at"
        const val KEY_LOCATION_SYNCHRONIZED_AT = "location_synchronized_at"
        const val KEY_CONTACTS_LINKED_AT = "contacts_linked_at"
    }
}
