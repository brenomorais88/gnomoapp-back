package com.dailyback.shared.domain.access

enum class ResourceScope {
    PERSONAL,
    FAMILY,
    ;

    companion object {
        fun fromValue(value: String): ResourceScope =
            entries.firstOrNull { it.name == value.trim().uppercase() }
                ?: throw IllegalArgumentException("Unsupported resource scope: $value")
    }
}
