package com.dailyback.features.users.domain

enum class UserStatus {
    ACTIVE,
    DISABLED,
    ;

    companion object {
        fun fromValue(value: String): UserStatus =
            entries.firstOrNull { it.name == value.trim().uppercase() }
                ?: throw IllegalArgumentException("Unsupported user status: $value")
    }
}
