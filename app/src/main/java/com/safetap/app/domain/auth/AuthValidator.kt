package com.safetap.app.domain.auth

object AuthValidator {
    private val emailRegex = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

    fun emailError(email: String): String? {
        val trimmed = email.trim()
        return when {
            trimmed.isEmpty() -> "Email is required"
            !emailRegex.matches(trimmed) -> "Enter a valid email address"
            else -> null
        }
    }

    fun passwordError(password: String, isNewPassword: Boolean = false): String? {
        return when {
            password.isEmpty() -> "Password is required"
            password.length < 6 -> "Password must be at least 6 characters"
            isNewPassword && password.isBlank() -> "Password cannot be only spaces"
            else -> null
        }
    }

    fun confirmPasswordError(password: String, confirmPassword: String): String? {
        return when {
            confirmPassword.isEmpty() -> "Confirm your password"
            confirmPassword != password -> "Passwords do not match"
            else -> null
        }
    }
}
