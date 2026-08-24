package com.safetap.app.data.contacts

import android.content.Context
import com.safetap.app.data.status.AppStatusRepository
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
    context: Context,
    private val appStatusRepository: AppStatusRepository
) {

    private val preferences = context.applicationContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE
    )

    private val _contacts = MutableStateFlow(loadContacts())

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

            validateContactData(trimmedName, normalizedPhone)

            if (_contacts.value.any { it.phone == normalizedPhone }) {
                throw IllegalArgumentException("A contact with this phone number already exists.")
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

            appStatusRepository.updateContactsLinked()

            trustedContact
        }
    }

    @Synchronized
    fun updateContact(
        updatedContact: TrustedContact
    ): Result<TrustedContact> {
        return runCatching {
            val trimmedName = updatedContact.name.trim()
            val normalizedPhone = normalizePhoneNumber(updatedContact.phone)

            validateContactData(trimmedName, normalizedPhone)

            val existingContacts = _contacts.value
            
            if (existingContacts.none { it.id == updatedContact.id }) {
                throw NoSuchElementException("Trusted contact was not found.")
            }

            if (existingContacts.any { it.id != updatedContact.id && it.phone == normalizedPhone }) {
                throw IllegalArgumentException("Another contact with this phone number already exists.")
            }

            val contactToUpdate = updatedContact.copy(
                name = trimmedName,
                relationship = updatedContact.relationship.trim().ifBlank {
                    DEFAULT_RELATIONSHIP
                },
                phone = normalizedPhone
            )

            updateContacts(
                contacts = existingContacts.map { contact ->
                    if (contact.id == updatedContact.id) contactToUpdate else contact
                }
            )

            contactToUpdate
        }
    }

    @Synchronized
    fun setPrimaryContact(
        contactId: String
    ): Result<Unit> {
        return runCatching {
            val existingContacts = _contacts.value

            if (existingContacts.none { it.id == contactId }) {
                throw NoSuchElementException("Trusted contact was not found.")
            }

            updateContacts(
                contacts = existingContacts.map { contact ->
                    contact.copy(isPrimary = contact.id == contactId)
                }
            )
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

            appStatusRepository.updateContactsLinked()
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
        val storedContacts = preferences.getString(
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
            .edit()
            .putString(
                CONTACTS_KEY,
                contactsArray.toString()
            )
            .apply()
    }

    private fun normalizePhoneNumber(
        phone: String
    ): String {
        val trimmedPhone = phone.trim()
        val digits = trimmedPhone.filter(Char::isDigit)

        require(
            digits.length in MINIMUM_PHONE_DIGITS..MAXIMUM_PHONE_DIGITS
        ) {
            "Enter a valid phone number (7-15 digits)."
        }

        return if (trimmedPhone.startsWith("+")) {
            "+$digits"
        } else {
            digits
        }
    }

    private fun validateContactData(name: String, phone: String) {
        require(name.isNotEmpty()) {
            "Contact name is required."
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