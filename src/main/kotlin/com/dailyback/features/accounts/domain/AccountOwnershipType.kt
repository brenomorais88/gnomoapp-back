package com.dailyback.features.accounts.domain

enum class AccountOwnershipType {
    PERSONAL,
    FAMILY,
    ;

    companion object {
        fun fromValue(value: String): AccountOwnershipType =
            entries.firstOrNull { it.name.equals(value.trim(), ignoreCase = true) }
                ?: throw IllegalArgumentException("Unsupported account ownership type: $value")
    }
}
