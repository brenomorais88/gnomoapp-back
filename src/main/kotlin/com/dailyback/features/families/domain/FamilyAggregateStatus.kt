package com.dailyback.features.families.domain

enum class FamilyAggregateStatus {
    ACTIVE,
    ARCHIVED,
    ;

    companion object {
        fun fromValue(value: String): FamilyAggregateStatus =
            entries.firstOrNull { it.name == value.trim().uppercase() }
                ?: throw IllegalArgumentException("Unsupported family status: $value")
    }
}
