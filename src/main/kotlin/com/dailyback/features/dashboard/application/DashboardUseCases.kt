package com.dailyback.features.dashboard.application

import com.dailyback.features.accountoccurrences.application.OccurrenceFilters
import com.dailyback.features.accountoccurrences.application.OccurrenceRepository
import com.dailyback.features.accountoccurrences.domain.OccurrenceStatus
import com.dailyback.shared.time.UtcClock
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

data class DashboardHomeSummary(
    val overdue: List<DashboardOccurrenceItem>,
    val next7Days: List<DashboardOccurrenceItem>,
    val totalPendingInMonth: BigDecimal,
    val totalPaidInMonth: BigDecimal,
    val upcoming: List<DashboardOccurrenceItem>,
    val categorySummary: List<DashboardCategorySummaryItem>,
)

data class DashboardOccurrenceItem(
    val id: UUID,
    val accountId: UUID,
    val title: String,
    val amount: BigDecimal,
    val dueDate: LocalDate,
    val status: OccurrenceStatus,
    val categoryId: UUID,
)

data class DashboardCategorySummaryItem(
    val categoryId: UUID,
    val totalAmount: BigDecimal,
    val count: Int,
)

data class DashboardMonthProjectionItem(
    val month: String,
    val totalAmount: BigDecimal,
    val occurrences: List<DashboardOccurrenceItem>,
)

class GetDashboardHomeSummaryUseCase(
    private val occurrenceRepository: OccurrenceRepository,
    private val utcClock: UtcClock,
) {
    fun execute(month: String?): DashboardHomeSummary {
        val today = utcClock.today()
        val monthRange = parseMonthRange(month, today)

        val pending = occurrenceRepository.findByFilters(
            OccurrenceFilters(
                status = OccurrenceStatus.PENDING,
                startDate = monthRange.first,
                endDate = monthRange.second,
            ),
        )
        val paid = occurrenceRepository.findByFilters(
            OccurrenceFilters(
                status = OccurrenceStatus.PAID,
                startDate = monthRange.first,
                endDate = monthRange.second,
            ),
        )

        val overdue = occurrenceRepository.findByFilters(
            OccurrenceFilters(status = OccurrenceStatus.PENDING, endDate = today.minusDays(1)),
        )
        val next7Days = occurrenceRepository.findByFilters(
            OccurrenceFilters(
                status = OccurrenceStatus.PENDING,
                startDate = today,
                endDate = today.plusDays(7),
            ),
        )
        val upcoming = occurrenceRepository.findByFilters(
            OccurrenceFilters(
                status = OccurrenceStatus.PENDING,
                startDate = today,
            ),
        ).take(20)

        val monthPendingItems = pending.map(::toItem)
        return DashboardHomeSummary(
            overdue = overdue.map(::toItem),
            next7Days = next7Days.map(::toItem),
            totalPendingInMonth = pending.sumOf { it.amountSnapshot },
            totalPaidInMonth = paid.sumOf { it.amountSnapshot },
            upcoming = upcoming.map(::toItem),
            categorySummary = monthPendingItems.groupBy { it.categoryId }.map { (categoryId, items) ->
                DashboardCategorySummaryItem(
                    categoryId = categoryId,
                    totalAmount = items.sumOf { it.amount },
                    count = items.size,
                )
            }.sortedByDescending { it.totalAmount },
        )
    }
}

class GetDashboardDayDetailsUseCase(
    private val occurrenceRepository: OccurrenceRepository,
) {
    fun execute(date: LocalDate): List<DashboardOccurrenceItem> =
        occurrenceRepository.findByFilters(
            OccurrenceFilters(startDate = date, endDate = date),
        ).map(::toItem)
}

class GetDashboardCategorySummaryUseCase(
    private val occurrenceRepository: OccurrenceRepository,
    private val utcClock: UtcClock,
) {
    fun execute(month: String?): List<DashboardCategorySummaryItem> {
        val today = utcClock.today()
        val range = parseMonthRange(month, today)
        val monthOccurrences = occurrenceRepository.findByFilters(
            OccurrenceFilters(startDate = range.first, endDate = range.second),
        )
        return monthOccurrences.groupBy { it.categoryIdSnapshot }
            .map { (categoryId, items) ->
                DashboardCategorySummaryItem(
                    categoryId = categoryId,
                    totalAmount = items.sumOf { it.amountSnapshot },
                    count = items.size,
                )
            }.sortedByDescending { it.totalAmount }
    }
}

class GetDashboardNext12MonthsProjectionUseCase(
    private val occurrenceRepository: OccurrenceRepository,
    private val utcClock: UtcClock,
) {
    fun execute(includeDetails: Boolean): List<DashboardMonthProjectionItem> {
        val today = utcClock.today()
        val startMonth = YearMonth.from(today)
        val endMonth = startMonth.plusMonths(11)
        val all = occurrenceRepository.findByFilters(
            OccurrenceFilters(
                startDate = startMonth.atDay(1),
                endDate = endMonth.atEndOfMonth(),
            ),
        ).map(::toItem)

        return (0L..11L).map { offset ->
            val ym = startMonth.plusMonths(offset)
            val monthItems = all.filter { YearMonth.from(it.dueDate) == ym }
            DashboardMonthProjectionItem(
                month = ym.toString(),
                totalAmount = monthItems.sumOf { it.amount },
                occurrences = if (includeDetails) monthItems else emptyList(),
            )
        }
    }
}

private fun toItem(occurrence: com.dailyback.features.accounts.domain.AccountOccurrence): DashboardOccurrenceItem =
    DashboardOccurrenceItem(
        id = occurrence.id,
        accountId = occurrence.accountId,
        title = occurrence.titleSnapshot,
        amount = occurrence.amountSnapshot,
        dueDate = occurrence.dueDate,
        status = occurrence.status,
        categoryId = occurrence.categoryIdSnapshot,
    )

private fun parseMonthRange(month: String?, today: LocalDate): Pair<LocalDate, LocalDate> {
    val ym = if (month.isNullOrBlank()) YearMonth.from(today) else YearMonth.parse(month.trim())
    return ym.atDay(1) to ym.atEndOfMonth()
}
