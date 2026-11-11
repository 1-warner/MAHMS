package com.doodoobops.mahms.utils

import java.security.MessageDigest

/** SHA-256 password hashing. Adequate for a local mock; not production auth. */
fun hashPassword(password: String): String =
    MessageDigest.getInstance("SHA-256")
        .digest(password.toByteArray())
        .joinToString("") { "%02x".format(it) }

object ValidationUtils {

    private val emailRegex = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
    // Any international phone: optional +, then 7-15 digits (spaces/dashes/parens stripped)
    private val phoneRegex = Regex("^\\+?\\d{7,15}$")

    fun emailError(value: String): String? = when {
        value.isBlank() -> "Email is required"
        !emailRegex.matches(value.trim()) -> "Enter a valid email address"
        else -> null
    }

    fun phoneError(value: String): String? {
        val stripped = value.trim().replace(Regex("[\\s\\-().]+"), "")
        return when {
            stripped.isBlank() -> "Phone number is required"
            !phoneRegex.matches(stripped) -> "Enter a valid phone number (7-15 digits)"
            else -> null
        }
    }

    fun passwordError(value: String): String? = when {
        value.length < 8 -> "At least 8 characters"
        !value.any { it.isUpperCase() } -> "Add an uppercase letter"
        !value.any { it.isDigit() } -> "Add a number"
        else -> null
    }

    fun confirmError(password: String, confirm: String): String? =
        if (password != confirm) "Passwords do not match" else null

    fun requiredError(value: String, field: String): String? =
        if (value.isBlank()) "$field is required" else null

    fun normalizePhone(value: String): String = value.trim().replace(" ", "")
}
