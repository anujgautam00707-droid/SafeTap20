package com.safetap.app.data.auth

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
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

    private fun Exception.toUserMessage(): String {
        val msg = localizedMessage.orEmpty()
        if (msg.contains("API key not valid", ignoreCase = true) || msg.contains("API_KEY_INVALID", ignoreCase = true)) {
            return "Invalid Firebase API key. Please update google-services.json with your actual Firebase project configuration."
        }
        return when (this) {
            is FirebaseAuthWeakPasswordException ->
                "Password is too weak. Please use at least 6 characters."
            is FirebaseAuthInvalidCredentialsException ->
                if (errorCode == "ERROR_INVALID_EMAIL") {
                    "Please enter a valid email address."
                } else {
                    "Incorrect email or password. Please try again."
                }
            is FirebaseAuthInvalidUserException ->
                "No account found with this email. Please sign up first."
            is FirebaseAuthUserCollisionException ->
                "An account with this email already exists."
            is FirebaseNetworkException ->
                "Network error. Please check your internet connection and try again."
            is FirebaseAuthException -> mapAuthErrorCode(errorCode)
            else -> localizedMessage?.takeIf { it.isNotBlank() }
                ?: "An unexpected error occurred. Please try again."
        }
    }


    private fun mapAuthErrorCode(code: String?): String = when (code) {
        "ERROR_INVALID_EMAIL" -> "Please enter a valid email address."
        "ERROR_WRONG_PASSWORD", "ERROR_INVALID_CREDENTIAL" ->
            "Incorrect email or password. Please try again."
        "ERROR_USER_NOT_FOUND" -> "No account found with this email."
        "ERROR_USER_DISABLED" -> "This account has been disabled. Contact support."
        "ERROR_EMAIL_ALREADY_IN_USE" -> "An account with this email already exists."
        "ERROR_WEAK_PASSWORD" -> "Password is too weak. Please use at least 6 characters."
        "ERROR_TOO_MANY_REQUESTS" -> "Too many unsuccessful attempts. Please try again later."
        "ERROR_OPERATION_NOT_ALLOWED" ->
            "Email/password sign-in is not enabled in Firebase Console."
        else -> "Authentication failed. Please try again."
    }
}

