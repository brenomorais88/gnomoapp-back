package com.dailyback.shared.application.maintenance

import com.dailyback.features.accounts.application.AccountRepository
import com.dailyback.features.accounts.application.OccurrenceSnapshot
import com.dailyback.features.accounts.application.RecurrenceGenerationService
import com.dailyback.features.accounts.application.SaveAccountCommand
import com.dailyback.features.accounts.application.AccountViewerQuery
import com.dailyback.features.accounts.domain.Account
import com.dailyback.features.accounts.domain.AccountOccurrence
import com.dailyback.features.accounts.domain.AccountOwnershipType
import com.dailyback.features.accounts.domain.RecurrenceType
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

class RecurrenceMaintenanceServiceTest {

    @Test
    fun `scheduler idempotency`() {
        val repository = FakeMaintenanceAccountRepository()
        val clock = UtcClock(Clock.fixed(Instant.parse("2026-04-10T00:00:00Z"), ZoneOffset.UTC))
        val service = RecurrenceMaintenanceService(repository, RecurrenceGenerationService(), clock)

        service.execute()
        val firstRunCount = repository.occurrences.size
        service.execute()

        assertEquals(firstRunCount, repository.occurrences.size)
    }

    @Test
    fun `scheduler fills missing future occurrences`() {
        val repository = FakeMaintenanceAccountRepository()
        val clock = UtcClock(Clock.fixed(Instant.parse("2026-04-10T00:00:00Z"), ZoneOffset.UTC))
        val service = RecurrenceMaintenanceService(repository, RecurrenceGenerationService(), clock)

        service.execute()

        assertTrue(repository.occurrences.any { it.dueDate == LocalDate.parse("2026-04-10") })
        assertTrue(repository.occurrences.any { it.dueDate == LocalDate.parse("2026-04-11") })
    }
}

private class FakeMaintenanceAccountRepository : AccountRepository {
    private val recurring = Account(
        id = UUID.randomUUID(),
        title = "Water",
        baseAmount = BigDecimal("10.00"),
        startDate = LocalDate.parse("2026-04-01"),
        endDate = LocalDate.parse("2026-04-20"),
        recurrenceType = RecurrenceType.DAILY,
        categoryId = UUID.randomUUID(),
        notes = null,
        active = true,
        ownershipType = AccountOwnershipType.PERSONAL,
        ownerUserId = UUID.fromString("11111111-1111-1111-1111-111111111111"),
        familyId = null,
        createdByUserId = UUID.fromString("11111111-1111-1111-1111-111111111111"),
        responsibleMemberId = null,
        createdAt = Instant.parse("2026-04-01T00:00:00Z"),
        updatedAt = Instant.parse("2026-04-01T00:00:00Z"),
    )

    val occurrences = mutableListOf<AccountOccurrence>()

    override fun findVisibleForUser(query: AccountViewerQuery): List<Account> = emptyList()

    override fun findVisibleAccountIds(query: AccountViewerQuery): Set<UUID> = emptySet()

    override fun findActiveRecurringAccounts(): List<Account> = listOf(recurring)
    override fun findById(id: UUID): Account? = if (id == recurring.id) recurring else null
    override fun create(command: SaveAccountCommand): Account = recurring
    override fun update(id: UUID, command: SaveAccountCommand): Account = recurring
    override fun updateAndRefreshFuturePendingOccurrences(
        id: UUID,
        command: SaveAccountCommand,
        fromDate: LocalDate,
        futurePendingSnapshots: List<OccurrenceSnapshot>,
    ): Account = recurring
    override fun setActive(id: UUID, active: Boolean): Account = recurring.copy(active = active)
    override fun delete(id: UUID) {}

    override fun upsertOccurrences(occurrences: List<OccurrenceSnapshot>) {
        occurrences.forEach { snapshot ->
            val exists = this.occurrences.any { it.accountId == snapshot.accountId && it.dueDate == snapshot.dueDate }
            if (!exists) {
                this.occurrences += AccountOccurrence(
                    id = UUID.randomUUID(),
                    accountId = snapshot.accountId,
                    titleSnapshot = snapshot.titleSnapshot,
                    amountSnapshot = snapshot.amountSnapshot,
                    dueDate = snapshot.dueDate,
                    status = snapshot.status,
                    paidAt = null,
                    notesSnapshot = snapshot.notesSnapshot,
                    categoryIdSnapshot = snapshot.categoryIdSnapshot,
                    createdAt = Instant.now(),
                    updatedAt = Instant.now(),
                )
            }
        }
    }

    override fun deleteFuturePendingOccurrences(accountId: UUID, fromDate: LocalDate) {}
    override fun findOccurrencesByAccountId(accountId: UUID): List<AccountOccurrence> = occurrences.filter { it.accountId == accountId }
    override fun hasRelevantHistory(accountId: UUID, today: LocalDate): Boolean = false
}
