package com.dailyback.features.accountoccurrences.application

import com.dailyback.features.accounts.domain.AccountOccurrence
import com.dailyback.features.accountoccurrences.domain.OccurrenceStatus
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

data class OccurrenceFilters(
    val status: OccurrenceStatus? = null,
    val categoryId: UUID? = null,
    val text: String? = null,
    val startDate: LocalDate? = null,
    val endDate: LocalDate? = null,
    val month: String? = null,
)

interface OccurrenceRepository {
    fun findByFilters(filters: OccurrenceFilters): List<AccountOccurrence>
    fun findById(id: UUID): AccountOccurrence?
    fun markPaid(id: UUID): AccountOccurrence
    fun unmarkPaid(id: UUID): AccountOccurrence
    fun overrideAmount(id: UUID, amount: BigDecimal): AccountOccurrence
}
