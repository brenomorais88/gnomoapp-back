package com.dailyback.shared.application.identity

import com.dailyback.shared.domain.identity.LoginIdentifier
import com.dailyback.shared.validation.IdentifierNormalizer

/**
 * Resolves how the user typed their login into [LoginIdentifier] for credential lookup.
 *
 * Heuristic (V2 foundation): `@` → email; otherwise if the input is mostly phone-shaped
 * (optional leading +, parentheses, spaces) and yields 10–15 digits → phone; else document.
 */
class LoginIdentifierParser {
    fun parse(rawLogin: String): LoginIdentifier {
        val trimmed = rawLogin.trim()
        require(trimmed.isNotEmpty()) { "Login must not be blank" }

        if (trimmed.contains('@')) {
            val email = IdentifierNormalizer.normalizeEmail(trimmed)
            require(email.contains('@')) { "Invalid email" }
            return LoginIdentifier.Email(email)
        }

        val digitCount = IdentifierNormalizer.digitsOnly(trimmed).length
        val looksLikePhone =
            digitCount in PHONE_DIGIT_LENGTH_RANGE &&
                trimmed.any { it == '+' || it == '(' || it == ')' || it == '-' || it.isWhitespace() }

        return if (looksLikePhone) {
            val digits = IdentifierNormalizer.digitsOnly(trimmed)
            require(digits.length in PHONE_DIGIT_LENGTH_RANGE) { "Invalid phone" }
            LoginIdentifier.Phone(digits)
        } else {
            val doc = IdentifierNormalizer.normalizeDocumentKey(trimmed)
            require(doc.isNotEmpty()) { "Invalid document" }
            LoginIdentifier.Document(doc)
        }
    }

    companion object {
        private val PHONE_DIGIT_LENGTH_RANGE = 10..15
    }
}
