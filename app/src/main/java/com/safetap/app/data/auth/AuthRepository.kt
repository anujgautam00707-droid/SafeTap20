package com.safetap.app.data.auth

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import com.safetap.app.R
import com.safetap.app.util.UiText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Repository handling authentication operations in SafeTap.
 * Converts lower-level Firebase SDK operations into domain-friendly AuthOutcome results.
 */
class AuthRepository(
    private val authManager: FirebaseAuthManager
) {
    val currentUser: FirebaseUser?
        get() = authManager.currentUser

    val isLoggedIn: Boolean
        get() = currentUser != null

    suspend fun awaitSession(): FirebaseUser? = withContext(Dispatchers.IO) {
        authManager.awaitRestoredUser()
    }

    suspend fun signIn(email: String, password: String): AuthOutcome =
        runAuth { authManager.signIn(email.trim(), password) }

    suspend fun signUp(email: String, password: String): AuthOutcome =
        runAuth { authManager.signUp(email.trim(), password) }

    suspend fun sendPasswordReset(email: String): AuthOutcome =
        runAuth { authManager.sendPasswordReset(email.trim()) }

    fun signOut() {
        authManager.signOut()
    }

    private suspend fun runAuth(block: suspend () -> Unit): AuthOutcome =
        withContext(Dispatchers.IO) {
            try {
                block()
                AuthOutcome.Success
            } catch (error: Exception) {
                AuthOutcome.Failure(error.toUserMessage())
            }
        }

    private fun Exception.toUserMessage(): UiText {
        val msg = localizedMessage.orEmpty()
        if (msg.contains("API key not valid", ignoreCase = true) || msg.contains("API_KEY_INVALID", ignoreCase = true)) {
            return UiText.StringResource(R.string.error_invalid_api_key)
        }
        return when (this) {
            is FirebaseAuthWeakPasswordException ->
                UiText.StringResource(R.string.error_weak_password)
            is FirebaseAuthInvalidCredentialsException ->
                if (errorCode == "ERROR_INVALID_EMAIL") {
                    UiText.StringResource(R.string.error_invalid_email)
                } else {
                    UiText.StringResource(R.string.error_invalid_credentials)
                }
            is FirebaseAuthInvalidUserException ->
                UiText.StringResource(R.string.error_no_account_found)
            is FirebaseAuthUserCollisionException ->
                UiText.StringResource(R.string.error_email_already_exists)
            is FirebaseNetworkException ->
                UiText.StringResource(R.string.error_network)
            is FirebaseAuthException -> mapAuthErrorCode(errorCode)
            else -> localizedMessage?.takeIf { it.isNotBlank() }?.let { UiText.DynamicString(it) }
                ?: UiText.StringResource(R.string.error_unexpected)
        }
    }


    private fun mapAuthErrorCode(code: String?): UiText = when (code) {
        "ERROR_INVALID_EMAIL" -> UiText.StringResource(R.string.error_invalid_email)
        "ERROR_WRONG_PASSWORD", "ERROR_INVALID_CREDENTIAL" ->
            UiText.StringResource(R.string.error_invalid_credentials)
        "ERROR_USER_NOT_FOUND" -> UiText.StringResource(R.string.error_no_account_found)
        "ERROR_USER_DISABLED" -> UiText.StringResource(R.string.error_user_disabled)
        "ERROR_EMAIL_ALREADY_IN_USE" -> UiText.StringResource(R.string.error_email_already_exists)
        "ERROR_WEAK_PASSWORD" -> UiText.StringResource(R.string.error_weak_password)
        "ERROR_TOO_MANY_REQUESTS" -> UiText.StringResource(R.string.error_too_many_requests)
        "ERROR_OPERATION_NOT_ALLOWED" ->
            UiText.StringResource(R.string.error_operation_not_allowed)
        else -> UiText.StringResource(R.string.error_auth_failed)
    }
}

