package com.dailyback.features.accounts.application

import com.dailyback.features.accounts.domain.Account
import com.dailyback.features.accounts.domain.RecurrenceType
import kotlin.test.Test
import kotlin.test.assertEquals
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

class RecurrenceGenerationServiceTest {
    private val service = RecurrenceGenerationService()

    @Test
    fun `create unique account generates one occurrence`() {
        val account = accountOf(
            recurrenceType = RecurrenceType.UNIQUE,
            startDate = LocalDate.parse("2026-01-10"),
        )

        val snapshots = service.generateSnapshots(
            account = account,
            fromDate = LocalDate.parse("2026-01-01"),
            horizonEndDate = LocalDate.parse("2026-12-31"),
        )

        assertEquals(1, snapshots.size)
        assertEquals(LocalDate.parse("2026-01-10"), snapshots.first().dueDate)
    }

    @Test
    fun `create daily account generates future occurrences correctly`() {
        val account = accountOf(
            recurrenceType = RecurrenceType.DAILY,
            startDate = LocalDate.parse("2026-01-10"),
        )

        val snapshots = service.generateSnapshots(
            account = account,
            fromDate = LocalDate.parse("2026-01-10"),
            horizonEndDate = LocalDate.parse("2026-01-13"),
        )

        assertEquals(
            listOf("2026-01-10", "2026-01-11", "2026-01-12", "2026-01-13"),
            snapshots.map { it.dueDate.toString() },
        )
    }

    @Test
    fun `create weekly account keeps same weekday`() {
        val account = accountOf(
            recurrenceType = RecurrenceType.WEEKLY,
            startDate = LocalDate.parse("2026-01-07"),
        )

        val snapshots = service.generateSnapshots(
            account = account,
            fromDate = LocalDate.parse("2026-01-01"),
            horizonEndDate = LocalDate.parse("2026-01-31"),
        )

        assertEquals(listOf(3, 3, 3, 3), snapshots.map { it.dueDate.dayOfWeek.value })
    }

    @Test
    fun `create monthly account keeps same day when possible`() {
        val account = accountOf(
            recurrenceType = RecurrenceType.MONTHLY,
            startDate = LocalDate.parse("2026-01-15"),
        )

        val snapshots = service.generateSnapshots(
            account = account,
            fromDate = LocalDate.parse("2026-01-01"),
            horizonEndDate = LocalDate.parse("2026-04-30"),
        )

        assertEquals(
            listOf("2026-01-15", "2026-02-15", "2026-03-15", "2026-04-15"),
            snapshots.map { it.dueDate.toString() },
        )
    }

    @Test
    fun `monthly account starting on day 31 shifts to last day in smaller months`() {
        val account = accountOf(
            recurrenceType = RecurrenceType.MONTHLY,
            startDate = LocalDate.parse("2026-01-31"),
        )

        val snapshots = service.generateSnapshots(
            account = account,
            fromDate = LocalDate.parse("2026-01-01"),
            horizonEndDate = LocalDate.parse("2026-04-30"),
        )

        assertEquals(
            listOf("2026-01-31", "2026-02-28", "2026-03-31", "2026-04-30"),
            snapshots.map { it.dueDate.toString() },
        )
    }

    @Test
    fun `generation respects endDate`() {
        val account = accountOf(
            recurrenceType = RecurrenceType.DAILY,
            startDate = LocalDate.parse("2026-01-10"),
            endDate = LocalDate.parse("2026-01-12"),
        )

        val snapshots = service.generateSnapshots(
            account = account,
            fromDate = LocalDate.parse("2026-01-01"),
            horizonEndDate = LocalDate.parse("2026-12-31"),
        )

        assertEquals(
            listOf("2026-01-10", "2026-01-11", "2026-01-12"),
            snapshots.map { it.dueDate.toString() },
        )
    }
}

private fun accountOf(
    recurrenceType: RecurrenceType,
    startDate: LocalDate,
    endDate: LocalDate? = null,
) = Account(
    id = UUID.randomUUID(),
    title = "Internet",
    baseAmount = BigDecimal("0.00"),
    startDate = startDate,
    endDate = endDate,
    recurrenceType = recurrenceType,
    categoryId = UUID.randomUUID(),
    notes = null,
    active = true,
    createdAt = Instant.parse("2026-01-01T00:00:00Z"),
    updatedAt = Instant.parse("2026-01-01T00:00:00Z"),
)
