package com.dailyback.features.accounts.domain

import com.dailyback.features.accountoccurrences.domain.OccurrenceStatus
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

data class AccountOccurrence(
    val id: UUID,
    val accountId: UUID,
    val titleSnapshot: String,
    val amountSnapshot: BigDecimal,
    val dueDate: LocalDate,
    val status: OccurrenceStatus,
    val paidAt: Instant?,
    val notesSnapshot: String?,
    val categoryIdSnapshot: UUID,
    val createdAt: Instant,
    val updatedAt: Instant,
)
