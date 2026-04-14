package com.dailyback.features.accountoccurrences.application

import com.dailyback.features.accounts.domain.AccountOccurrence
import com.dailyback.features.accountoccurrences.domain.OccurrenceStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

class OccurrenceUseCasesTest {

    @Test
    fun `mark as paid`() {
        val repository = fakeRepository()
        val useCase = MarkOccurrencePaidUseCase(repository)

        val updated = useCase.execute(repository.occurrenceA.id)

        assertEquals(OccurrenceStatus.PAID, updated.status)
        assertTrue(updated.paidAt != null)
    }

    @Test
    fun `unmark as paid`() {
        val repository = fakeRepository().apply {
            markPaid(occurrenceA.id)
        }
        val useCase = UnmarkOccurrencePaidUseCase(repository)

        val updated = useCase.execute(repository.occurrenceA.id)

        assertEquals(OccurrenceStatus.PENDING, updated.status)
        assertEquals(null, updated.paidAt)
    }

    @Test
    fun `override amount changes only one occurrence`() {
        val repository = fakeRepository()
        val useCase = OverrideOccurrenceAmountUseCase(repository)

        useCase.execute(repository.occurrenceA.id, BigDecimal("555.55"))

        val first = repository.findById(repository.occurrenceA.id)!!
        val second = repository.findById(repository.occurrenceB.id)!!
        assertEquals(BigDecimal("555.55"), first.amountSnapshot)
        assertEquals(BigDecimal("20.00"), second.amountSnapshot)
    }

    @Test
    fun `list filtering by status`() {
        val repository = fakeRepository().apply {
            markPaid(occurrenceA.id)
        }
        val useCase = ListOccurrencesUseCase(repository)

        val result = useCase.execute(OccurrenceFilters(status = OccurrenceStatus.PAID))

        assertEquals(1, result.size)
        assertEquals(OccurrenceStatus.PAID, result.first().status)
    }

    @Test
    fun `list filtering by category`() {
        val repository = fakeRepository()
        val useCase = ListOccurrencesUseCase(repository)

        val result = useCase.execute(OccurrenceFilters(categoryId = repository.categoryB))

        assertEquals(1, result.size)
        assertEquals(repository.categoryB, result.first().categoryIdSnapshot)
    }

    @Test
    fun `list filtering by text`() {
        val repository = fakeRepository()
        val useCase = ListOccurrencesUseCase(repository)

        val result = useCase.execute(OccurrenceFilters(text = "market"))

        assertEquals(1, result.size)
        assertTrue(result.first().titleSnapshot.contains("Market"))
    }

    @Test
    fun `list filtering by date range`() {
        val repository = fakeRepository()
        val useCase = ListOccurrencesUseCase(repository)

        val result = useCase.execute(
            OccurrenceFilters(
                startDate = LocalDate.parse("2026-04-10"),
                endDate = LocalDate.parse("2026-04-20"),
            ),
        )

        assertEquals(1, result.size)
        assertEquals(LocalDate.parse("2026-04-15"), result.first().dueDate)
    }

    @Test
    fun `default sorting by due date ascending`() {
        val repository = fakeRepository()
        val useCase = ListOccurrencesUseCase(repository)

        val result = useCase.execute(OccurrenceFilters())

        assertTrue(result[0].dueDate <= result[1].dueDate)
        assertTrue(result[1].dueDate <= result[2].dueDate)
    }
}

private class FakeOccurrenceRepository(
    val occurrenceA: AccountOccurrence,
    val occurrenceB: AccountOccurrence,
    val occurrenceC: AccountOccurrence,
    val categoryB: UUID,
) : OccurrenceRepository {
    private val items = linkedMapOf<UUID, AccountOccurrence>(
        occurrenceA.id to occurrenceA,
        occurrenceB.id to occurrenceB,
        occurrenceC.id to occurrenceC,
    )

    override fun findByFilters(filters: OccurrenceFilters): List<AccountOccurrence> {
        var data = items.values.toList()
        filters.status?.let { status -> data = data.filter { it.status == status } }
        filters.categoryId?.let { categoryId -> data = data.filter { it.categoryIdSnapshot == categoryId } }
        filters.text?.trim()?.takeIf { it.isNotBlank() }?.let { text ->
            val q = text.lowercase()
            data = data.filter {
                it.titleSnapshot.lowercase().contains(q) || (it.notesSnapshot?.lowercase()?.contains(q) == true)
            }
        }
        val monthRange = filters.month?.let { java.time.YearMonth.parse(it) }
        val start = monthRange?.atDay(1) ?: filters.startDate
        val end = monthRange?.atEndOfMonth() ?: filters.endDate
        if (start != null) data = data.filter { it.dueDate >= start }
        if (end != null) data = data.filter { it.dueDate <= end }

        return data.sortedBy { it.dueDate }
    }

    override fun findById(id: UUID): AccountOccurrence? = items[id]

    override fun markPaid(id: UUID): AccountOccurrence {
        val current = items.getValue(id).copy(status = OccurrenceStatus.PAID, paidAt = Instant.now(), updatedAt = Instant.now())
        items[id] = current
        return current
    }

    override fun unmarkPaid(id: UUID): AccountOccurrence {
        val current = items.getValue(id).copy(status = OccurrenceStatus.PENDING, paidAt = null, updatedAt = Instant.now())
        items[id] = current
        return current
    }

    override fun overrideAmount(id: UUID, amount: BigDecimal): AccountOccurrence {
        val current = items.getValue(id).copy(amountSnapshot = amount, updatedAt = Instant.now())
        items[id] = current
        return current
    }
}

private fun fakeRepository(): FakeOccurrenceRepository {
    val accountId = UUID.randomUUID()
    val categoryA = UUID.randomUUID()
    val categoryB = UUID.randomUUID()
    val now = Instant.parse("2026-04-01T00:00:00Z")
    return FakeOccurrenceRepository(
        occurrenceA = AccountOccurrence(
            id = UUID.randomUUID(),
            accountId = accountId,
            titleSnapshot = "Super Market",
            amountSnapshot = BigDecimal("10.00"),
            dueDate = LocalDate.parse("2026-04-05"),
            status = OccurrenceStatus.PENDING,
            paidAt = null,
            notesSnapshot = "weekly groceries",
            categoryIdSnapshot = categoryA,
            createdAt = now,
            updatedAt = now,
        ),
        occurrenceB = AccountOccurrence(
            id = UUID.randomUUID(),
            accountId = accountId,
            titleSnapshot = "Internet",
            amountSnapshot = BigDecimal("20.00"),
            dueDate = LocalDate.parse("2026-04-15"),
            status = OccurrenceStatus.PENDING,
            paidAt = null,
            notesSnapshot = "home",
            categoryIdSnapshot = categoryB,
            createdAt = now,
            updatedAt = now,
        ),
        occurrenceC = AccountOccurrence(
            id = UUID.randomUUID(),
            accountId = accountId,
            titleSnapshot = "Gym",
            amountSnapshot = BigDecimal("30.00"),
            dueDate = LocalDate.parse("2026-04-25"),
            status = OccurrenceStatus.PENDING,
            paidAt = null,
            notesSnapshot = "health",
            categoryIdSnapshot = categoryA,
            createdAt = now,
            updatedAt = now,
        ),
        categoryB = categoryB,
    )
}
