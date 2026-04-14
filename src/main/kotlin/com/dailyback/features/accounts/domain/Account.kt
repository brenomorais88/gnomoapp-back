package com.dailyback.features.accounts.domain

import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

data class Account(
    val id: UUID,
    val title: String,
    val baseAmount: BigDecimal,
    val startDate: LocalDate,
    val endDate: LocalDate?,
    val recurrenceType: RecurrenceType,
    val categoryId: UUID,
    val notes: String?,
    val active: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant,
)
