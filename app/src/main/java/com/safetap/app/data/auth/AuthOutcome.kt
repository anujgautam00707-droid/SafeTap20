package com.safetap.app.data.auth

sealed interface AuthOutcome {
    data object Success : AuthOutcome
    data class Failure(val message: String) : AuthOutcome
}
