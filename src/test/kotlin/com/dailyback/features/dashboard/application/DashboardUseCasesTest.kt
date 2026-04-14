package com.dailyback.features.dashboard.application

import com.dailyback.features.accountoccurrences.application.OccurrenceFilters
import com.dailyback.features.accountoccurrences.application.OccurrenceRepository
import com.dailyback.features.accountoccurrences.domain.OccurrenceStatus
import com.dailyback.features.accounts.domain.AccountOccurrence
import com.dailyback.shared.time.UtcClock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.UUID

class DashboardUseCasesTest {

    @Test
    fun `overdue calculations and next 7 days reminder logic`() {
        val repository = FakeDashboardOccurrenceRepository()
        val clock = UtcClock(Clock.fixed(Instant.parse("2026-04-10T00:00:00Z"), ZoneOffset.UTC))
        val useCase = GetDashboardHomeSummaryUseCase(repository, clock)

        val result = useCase.execute(month = "2026-04")

        assertEquals(1, result.overdue.size)
        assertEquals(LocalDate.parse("2026-04-09"), result.overdue.first().dueDate)
        assertEquals(2, result.next7Days.size)
        assertTrue(result.next7Days.all { it.dueDate in LocalDate.parse("2026-04-10")..LocalDate.parse("2026-04-17") })
    }

    @Test
    fun `month totals and category summary`() {
        val repository = FakeDashboardOccurrenceRepository()
        val clock = UtcClock(Clock.fixed(Instant.parse("2026-04-10T00:00:00Z"), ZoneOffset.UTC))
        val home = GetDashboardHomeSummaryUseCase(repository, clock).execute(month = "2026-04")
        val summary = GetDashboardCategorySummaryUseCase(repository, clock).execute(month = "2026-04")

        assertEquals(BigDecimal("60.00"), home.totalPendingInMonth)
        assertEquals(BigDecimal("40.00"), home.totalPaidInMonth)
        assertTrue(summary.isNotEmpty())
        assertEquals(BigDecimal("70.00"), summary.first().totalAmount)
    }

    @Test
    fun `next 12 months projection grouping`() {
        val repository = FakeDashboardOccurrenceRepository()
        val clock = UtcClock(Clock.fixed(Instant.parse("2026-04-10T00:00:00Z"), ZoneOffset.UTC))
        val useCase = GetDashboardNext12MonthsProjectionUseCase(repository, clock)

        val projection = useCase.execute(includeDetails = true)

        assertEquals(12, projection.size)
        assertEquals("2026-04", projection.first().month)
        assertEquals(BigDecimal("100.00"), projection.first().totalAmount)
        assertTrue(projection.first().occurrences.isNotEmpty())
    }
}

private class FakeDashboardOccurrenceRepository : OccurrenceRepository {
    private val categoryA = UUID.randomUUID()
    private val categoryB = UUID.randomUUID()
    private val occurrences = listOf(
        occurrence("Store", "10.00", "2026-04-09", OccurrenceStatus.PENDING, categoryA),
        occurrence("Internet", "20.00", "2026-04-10", OccurrenceStatus.PENDING, categoryA),
        occurrence("Gym", "30.00", "2026-04-17", OccurrenceStatus.PENDING, categoryB),
        occurrence("Rent", "40.00", "2026-04-15", OccurrenceStatus.PAID, categoryA),
        occurrence("Projection", "50.00", "2026-05-03", OccurrenceStatus.PENDING, categoryA),
    )

    override fun findByFilters(filters: OccurrenceFilters): List<AccountOccurrence> {
        var data = occurrences
        filters.status?.let { s -> data = data.filter { it.status == s } }
        filters.categoryId?.let { c -> data = data.filter { it.categoryIdSnapshot == c } }
        filters.startDate?.let { d -> data = data.filter { it.dueDate >= d } }
        filters.endDate?.let { d -> data = data.filter { it.dueDate <= d } }
        filters.month?.let {
            val ym = java.time.YearMonth.parse(it)
            data = data.filter { occurrence -> java.time.YearMonth.from(occurrence.dueDate) == ym }
        }
        filters.text?.trim()?.takeIf { it.isNotBlank() }?.let { q ->
            val text = q.lowercase()
            data = data.filter { it.titleSnapshot.lowercase().contains(text) }
        }
        return data.sortedBy { it.dueDate }
    }

    override fun findById(id: UUID): AccountOccurrence? = occurrences.firstOrNull { it.id == id }
    override fun markPaid(id: UUID): AccountOccurrence = throw UnsupportedOperationException()
    override fun unmarkPaid(id: UUID): AccountOccurrence = throw UnsupportedOperationException()
    override fun overrideAmount(id: UUID, amount: BigDecimal): AccountOccurrence = throw UnsupportedOperationException()

    private fun occurrence(
        title: String,
        amount: String,
        date: String,
        status: OccurrenceStatus,
        categoryId: UUID,
    ): AccountOccurrence = AccountOccurrence(
        id = UUID.randomUUID(),
        accountId = UUID.randomUUID(),
        titleSnapshot = title,
        amountSnapshot = BigDecimal(amount),
        dueDate = LocalDate.parse(date),
        status = status,
        paidAt = if (status == OccurrenceStatus.PAID) Instant.parse("2026-04-15T10:00:00Z") else null,
        notesSnapshot = null,
        categoryIdSnapshot = categoryId,
        createdAt = Instant.parse("2026-04-01T00:00:00Z"),
        updatedAt = Instant.parse("2026-04-01T00:00:00Z"),
    )
}
