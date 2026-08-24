package com.safetap.app.data.auth

import com.safetap.app.util.UiText

sealed interface AuthOutcome {
    data object Success : AuthOutcome
    data class Failure(val message: UiText) : AuthOutcome
}
