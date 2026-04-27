package com.dailyback.shared.domain.family

enum class FamilyMemberRole {
    ADMIN,
    MEMBER,
    ;

    companion object {
        fun fromValue(value: String): FamilyMemberRole =
            entries.firstOrNull { it.name == value.trim().uppercase() }
                ?: throw IllegalArgumentException("Unsupported family member role: $value")
    }
}
