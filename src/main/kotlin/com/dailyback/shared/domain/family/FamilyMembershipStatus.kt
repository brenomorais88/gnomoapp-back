package com.dailyback.shared.domain.family

enum class FamilyMembershipStatus {
    PENDING_REGISTRATION,
    ACTIVE,
    REMOVED,
    ;

    companion object {
        fun fromValue(value: String): FamilyMembershipStatus {
            val normalized = value.trim().uppercase()
            if (normalized == "PENDING") {
                return PENDING_REGISTRATION
            }
            return entries.firstOrNull { it.name == normalized }
                ?: throw IllegalArgumentException("Unsupported family membership status: $value")
        }
    }
}
