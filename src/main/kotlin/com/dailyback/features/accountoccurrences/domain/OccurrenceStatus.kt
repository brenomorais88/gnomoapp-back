package com.dailyback.features.accountoccurrences.domain

enum class OccurrenceStatus {
    PENDING,
    PAID,
    ;

    companion object {
        fun fromValue(value: String): OccurrenceStatus =
            entries.firstOrNull { it.name == value.trim().uppercase() }
                ?: throw IllegalArgumentException("Unsupported occurrence status: $value")
    }
}
