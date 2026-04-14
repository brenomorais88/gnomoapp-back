package com.dailyback.features.accounts.domain

enum class RecurrenceType {
    UNIQUE,
    DAILY,
    WEEKLY,
    MONTHLY,
    ;

    companion object {
        fun fromValue(value: String): RecurrenceType =
            entries.firstOrNull { it.name == value.trim().uppercase() }
                ?: throw IllegalArgumentException("Unsupported recurrence type: $value")
    }
}
