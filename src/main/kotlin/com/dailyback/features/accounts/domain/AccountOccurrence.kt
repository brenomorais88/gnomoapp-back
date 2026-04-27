package com.dailyback.features.accounts.domain

import com.dailyback.features.accountoccurrences.domain.OccurrenceStatus
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

/**
 * A persisted occurrence row. PERSONAL vs FAMILY context is always resolved via [accountId] on the
 * parent account; snapshot fields preserve historical values for that occurrence.
 */
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
