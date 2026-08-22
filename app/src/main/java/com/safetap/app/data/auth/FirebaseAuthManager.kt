package com.safetap.app.data.auth

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlin.coroutines.resume

class FirebaseAuthManager(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    val currentUser: FirebaseUser?
        get() = auth.currentUser

    /**
     * Waits for Firebase to restore a persisted session after process start.
     * The first AuthStateListener callback is the restored user (or null).
     */
    suspend fun awaitRestoredUser(): FirebaseUser? =
        suspendCancellableCoroutine { continuation ->
            lateinit var listener: FirebaseAuth.AuthStateListener
            listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
                auth.removeAuthStateListener(listener)
                if (continuation.isActive) {
                    continuation.resume(firebaseAuth.currentUser)
                }
            }
            auth.addAuthStateListener(listener)
            continuation.invokeOnCancellation {
                auth.removeAuthStateListener(listener)
            }
        }

    suspend fun signIn(email: String, password: String): FirebaseUser {
        val result = auth.signInWithEmailAndPassword(email, password).await()
        return requireNotNull(result.user) { "Sign-in succeeded without a user." }
    }

    suspend fun signUp(email: String, password: String): FirebaseUser {
        val result = auth.createUserWithEmailAndPassword(email, password).await()
        return requireNotNull(result.user) { "Sign-up succeeded without a user." }
    }

    suspend fun sendPasswordReset(email: String) {
        auth.sendPasswordResetEmail(email).await()
    }

    fun signOut() {
        auth.signOut()
    }
}
