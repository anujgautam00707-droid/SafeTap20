package com.safetap.app.data.contacts

import android.content.Context
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

data class TrustedContact(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val relationship: String,
    val phone: String,
    val isPrimary: Boolean = false
)

class TrustedContactsRepository(
    private val preferences: android.content.SharedPreferences? = null,
    initialContacts: List<TrustedContact> = emptyList()
) {

    constructor(context: Context) : this(
        preferences = context.applicationContext.getSharedPreferences(
            PREFERENCES_NAME,
            Context.MODE_PRIVATE
        )
    )

    private val _contacts = MutableStateFlow(
        preferences?.let { loadContacts() } ?: initialContacts
    )

    val contacts: StateFlow<List<TrustedContact>> =
        _contacts.asStateFlow()

    fun getCurrentContacts(): List<TrustedContact> =
        _contacts.value

    @Synchronized
    fun addContact(
        name: String,
        relationship: String,
        phone: String
    ): Result<TrustedContact> {
        return runCatching {
            val trimmedName = name.trim()
            val normalizedPhone = normalizePhoneNumber(phone)

            require(trimmedName.isNotEmpty()) {
                "Contact name is required."
            }

            require(
                _contacts.value.none { contact ->
                    contact.phone == normalizedPhone
                }
            ) {
                "A contact with this phone number already exists."
            }

            val trustedContact = TrustedContact(
                name = trimmedName,
                relationship = relationship.trim().ifBlank {
                    DEFAULT_RELATIONSHIP
                },
                phone = normalizedPhone,
                isPrimary = _contacts.value.isEmpty()
            )

            updateContacts(
                contacts = _contacts.value + trustedContact
            )

            trustedContact
        }
    }

    @Synchronized
    fun removeContact(
        contactId: String
    ): Result<Unit> {
        return runCatching {
            val existingContacts = _contacts.value

            require(
                existingContacts.any { contact ->
                    contact.id == contactId
                }
            ) {
                "Trusted contact was not found."
            }

            val remainingContacts = existingContacts.filterNot { contact ->
                contact.id == contactId
            }

            updateContacts(
                contacts = ensurePrimaryContact(remainingContacts)
            )
        }
    }

    private fun updateContacts(
        contacts: List<TrustedContact>
    ) {
        val normalizedContacts = ensurePrimaryContact(contacts)

        _contacts.value = normalizedContacts
        persistContacts(normalizedContacts)
    }

    private fun ensurePrimaryContact(
        contacts: List<TrustedContact>
    ): List<TrustedContact> {
        if (contacts.isEmpty()) {
            return emptyList()
        }

        val primaryContactId = contacts
            .firstOrNull { contact -> contact.isPrimary }
            ?.id
            ?: contacts.first().id

        return contacts.map { contact ->
            contact.copy(
                isPrimary = contact.id == primaryContactId
            )
        }
    }

    private fun loadContacts(): List<TrustedContact> {
        val storedContacts = preferences?.getString(
            CONTACTS_KEY,
            null
        ) ?: return emptyList()

        return runCatching {
            val contactsArray = JSONArray(storedContacts)
            val contacts = mutableListOf<TrustedContact>()

            for (index in 0 until contactsArray.length()) {
                val contactObject =
                    contactsArray.optJSONObject(index) ?: continue

                val name = contactObject
                    .optString(NAME_KEY)
                    .trim()

                val phone = contactObject
                    .optString(PHONE_KEY)
                    .trim()

                if (name.isEmpty() || phone.isEmpty()) {
                    continue
                }

                val normalizedPhone = runCatching {
                    normalizePhoneNumber(phone)
                }.getOrNull() ?: continue

                contacts += TrustedContact(
                    id = contactObject
                        .optString(ID_KEY)
                        .ifBlank {
                            UUID.randomUUID().toString()
                        },
                    name = name,
                    relationship = contactObject
                        .optString(
                            RELATIONSHIP_KEY,
                            DEFAULT_RELATIONSHIP
                        )
                        .ifBlank {
                            DEFAULT_RELATIONSHIP
                        },
                    phone = normalizedPhone,
                    isPrimary = contactObject.optBoolean(
                        PRIMARY_KEY,
                        false
                    )
                )
            }

            ensurePrimaryContact(contacts)
        }.getOrElse {
            emptyList()
        }
    }

    private fun persistContacts(
        contacts: List<TrustedContact>
    ) {
        val contactsArray = JSONArray()

        contacts.forEach { contact ->
            val contactObject = JSONObject().apply {
                put(ID_KEY, contact.id)
                put(NAME_KEY, contact.name)
                put(RELATIONSHIP_KEY, contact.relationship)
                put(PHONE_KEY, contact.phone)
                put(PRIMARY_KEY, contact.isPrimary)
            }

            contactsArray.put(contactObject)
        }

        preferences
            ?.edit()
            ?.putString(
                CONTACTS_KEY,
                contactsArray.toString()
            )
            ?.apply()
    }

    private fun normalizePhoneNumber(
        phone: String
    ): String {
        val trimmedPhone = phone.trim()
        val digits = trimmedPhone.filter(Char::isDigit)

        require(
            digits.length in MINIMUM_PHONE_DIGITS..MAXIMUM_PHONE_DIGITS
        ) {
            "Enter a valid phone number."
        }

        return if (trimmedPhone.startsWith("+")) {
            "+$digits"
        } else {
            digits
        }
    }

    private companion object {
        const val PREFERENCES_NAME =
            "safetap_trusted_contacts"

        const val CONTACTS_KEY =
            "trusted_contacts"

        const val ID_KEY = "id"
        const val NAME_KEY = "name"
        const val RELATIONSHIP_KEY = "relationship"
        const val PHONE_KEY = "phone"
        const val PRIMARY_KEY = "is_primary"

        const val DEFAULT_RELATIONSHIP =
            "Emergency Contact"

        const val MINIMUM_PHONE_DIGITS = 7
        const val MAXIMUM_PHONE_DIGITS = 15
    }
}