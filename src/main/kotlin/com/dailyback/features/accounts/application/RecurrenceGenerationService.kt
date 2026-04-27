package com.dailyback.features.accounts.application

import com.dailyback.features.accountoccurrences.domain.OccurrenceStatus
import com.dailyback.features.accounts.domain.Account
import com.dailyback.features.accounts.domain.AccountOwnershipType
import com.dailyback.features.accounts.domain.RecurrenceType
import java.time.LocalDate
import java.time.YearMonth

/**
 * Builds pending occurrence snapshots for an account window. User-facing visibility and mutations
 * are enforced when listing or changing occurrences; this service only requires a consistent account ownership context.
 */
class RecurrenceGenerationService {
    fun generateSnapshots(
        account: Account,
        fromDate: LocalDate,
        horizonEndDate: LocalDate,
    ): List<OccurrenceSnapshot> {
        requireOwnershipContext(account)
        if (!account.active) {
            return emptyList()
        }
        val effectiveEnd = minDate(
            horizonEndDate,
            account.endDate ?: horizonEndDate,
        )
        if (effectiveEnd < fromDate) {
            return emptyList()
        }

        val dueDates = when (account.recurrenceType) {
            RecurrenceType.UNIQUE -> generateUnique(account.startDate, fromDate, effectiveEnd)
            RecurrenceType.DAILY -> generateDaily(account.startDate, fromDate, effectiveEnd)
            RecurrenceType.WEEKLY -> generateWeekly(account.startDate, fromDate, effectiveEnd)
            RecurrenceType.MONTHLY -> generateMonthly(account.startDate, fromDate, effectiveEnd)
        }

        return dueDates.distinct().map { dueDate ->
            OccurrenceSnapshot(
                accountId = account.id,
                titleSnapshot = account.title,
                amountSnapshot = account.baseAmount,
                dueDate = dueDate,
                status = OccurrenceStatus.PENDING,
                notesSnapshot = account.notes,
                categoryIdSnapshot = account.categoryId,
            )
        }
    }

    private fun generateUnique(startDate: LocalDate, fromDate: LocalDate, effectiveEnd: LocalDate): List<LocalDate> =
        if (startDate in fromDate..effectiveEnd) listOf(startDate) else emptyList()

    private fun generateDaily(startDate: LocalDate, fromDate: LocalDate, effectiveEnd: LocalDate): List<LocalDate> {
        val first = if (startDate > fromDate) startDate else fromDate
        if (first > effectiveEnd) {
            return emptyList()
        }
        return generateSequence(first) { it.plusDays(1) }
            .takeWhile { !it.isAfter(effectiveEnd) }
            .toList()
    }

    private fun generateWeekly(startDate: LocalDate, fromDate: LocalDate, effectiveEnd: LocalDate): List<LocalDate> {
        val first = if (startDate > fromDate) startDate else fromDate
        val offset = (startDate.dayOfWeek.value - first.dayOfWeek.value + 7) % 7
        val alignedStart = first.plusDays(offset.toLong())
        if (alignedStart > effectiveEnd) {
            return emptyList()
        }
        return generateSequence(alignedStart) { it.plusWeeks(1) }
            .takeWhile { !it.isAfter(effectiveEnd) }
            .toList()
    }

    private fun generateMonthly(startDate: LocalDate, fromDate: LocalDate, effectiveEnd: LocalDate): List<LocalDate> {
        val startMonth = YearMonth.from(maxDate(startDate, fromDate))
        val endMonth = YearMonth.from(effectiveEnd)
        val startDay = startDate.dayOfMonth

        val dates = mutableListOf<LocalDate>()
        var current = startMonth
        while (!current.isAfter(endMonth)) {
            val day = minOf(startDay, current.lengthOfMonth())
            val candidate = current.atDay(day)
            if (candidate in fromDate..effectiveEnd && !candidate.isBefore(startDate)) {
                dates += candidate
            }
            current = current.plusMonths(1)
        }
        return dates
    }

    private fun minDate(a: LocalDate, b: LocalDate): LocalDate = if (a <= b) a else b
    private fun maxDate(a: LocalDate, b: LocalDate): LocalDate = if (a >= b) a else b

    private fun requireOwnershipContext(account: Account) {
        when (account.ownershipType) {
            AccountOwnershipType.PERSONAL ->
                require(account.ownerUserId != null) {
                    "PERSONAL account must have ownerUserId to generate occurrences"
                }

            AccountOwnershipType.FAMILY ->
                require(account.familyId != null) {
                    "FAMILY account must have familyId to generate occurrences"
                }
        }
    }
}

