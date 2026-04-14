package com.dailyback.features.categories.domain

import java.time.Instant
import java.util.UUID

data class Category(
    val id: UUID,
    val name: String,
    val color: String?,
    val createdAt: Instant,
    val updatedAt: Instant,
)
