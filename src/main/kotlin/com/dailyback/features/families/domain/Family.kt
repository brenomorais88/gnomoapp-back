package com.dailyback.features.families.domain

import java.time.Instant
import java.util.UUID

data class Family(
    val id: UUID,
    val name: String,
    val createdByUserId: UUID?,
    val status: FamilyAggregateStatus,
    val createdAt: Instant,
    val updatedAt: Instant,
)
