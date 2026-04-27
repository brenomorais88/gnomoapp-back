package com.dailyback.shared.validation

object IdentifierNormalizer {
    fun normalizeEmail(raw: String): String = raw.trim().lowercase()

    fun digitsOnly(raw: String): String = buildString {
        for (ch in raw) {
            if (ch.isDigit()) append(ch)
        }
    }

    /**
     * Document numbers: keep digits and letters (e.g. passport), fold case for letters.
     */
    fun normalizeDocumentKey(raw: String): String =
        buildString {
            for (ch in raw) {
                when {
                    ch.isDigit() -> append(ch)
                    ch.isLetter() -> append(ch.uppercaseChar())
                }
            }
        }
}
