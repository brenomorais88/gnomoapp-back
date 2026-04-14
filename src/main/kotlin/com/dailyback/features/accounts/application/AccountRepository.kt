package com.dailyback.features.accounts.application

import com.dailyback.features.accounts.domain.Account
import com.dailyback.features.accounts.domain.AccountOccurrence
import java.time.LocalDate
import java.util.UUID

interface AccountRepository {
    fun findAll(): List<Account>
    fun findActiveRecurringAccounts(): List<Account>
    fun findById(id: UUID): Account?
    fun categoryExists(categoryId: UUID): Boolean
    fun create(command: SaveAccountCommand): Account
    fun update(id: UUID, command: SaveAccountCommand): Account
    fun setActive(id: UUID, active: Boolean): Account
    fun delete(id: UUID)

    fun upsertOccurrences(occurrences: List<OccurrenceSnapshot>)
    fun deleteFuturePendingOccurrences(accountId: UUID, fromDate: LocalDate)
    fun findOccurrencesByAccountId(accountId: UUID): List<AccountOccurrence>
    fun hasRelevantHistory(accountId: UUID, today: LocalDate): Boolean
}

data class SaveAccountCommand(
    val title: String,
    val baseAmount: java.math.BigDecimal,
    val startDate: LocalDate,
    val endDate: LocalDate?,
    val recurrenceType: com.dailyback.features.accounts.domain.RecurrenceType,
    val categoryId: UUID,
    val notes: String?,
    val active: Boolean,
)

data class OccurrenceSnapshot(
    val accountId: UUID,
    val titleSnapshot: String,
    val amountSnapshot: java.math.BigDecimal,
    val dueDate: LocalDate,
    val status: com.dailyback.features.accountoccurrences.domain.OccurrenceStatus,
    val notesSnapshot: String?,
    val categoryIdSnapshot: UUID,
)
