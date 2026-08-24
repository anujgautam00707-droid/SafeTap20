package com.safetap.app.domain.auth

import com.safetap.app.R
import com.safetap.app.util.UiText

object AuthValidator {
    private val emailRegex = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

    fun emailError(email: String): UiText? {
        val trimmed = email.trim()
        return when {
            trimmed.isEmpty() -> UiText.StringResource(R.string.error_email_required)
            !emailRegex.matches(trimmed) -> UiText.StringResource(R.string.error_invalid_email)
            else -> null
        }
    }

    fun passwordError(password: String, isNewPassword: Boolean = false): UiText? {
        return when {
            password.isEmpty() -> UiText.StringResource(R.string.error_password_required)
            password.length < 6 -> UiText.StringResource(R.string.error_password_short)
            isNewPassword && password.isBlank() -> UiText.StringResource(R.string.error_password_spaces)
            else -> null
        }
    }

    fun confirmPasswordError(password: String, confirmPassword: String): UiText? {
        return when {
            confirmPassword.isEmpty() -> UiText.StringResource(R.string.error_confirm_password_required)
            confirmPassword != password -> UiText.StringResource(R.string.error_passwords_mismatch)
            else -> null
        }
    }
}
