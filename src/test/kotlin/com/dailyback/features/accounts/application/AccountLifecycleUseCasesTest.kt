package com.dailyback.features.accounts.application

import com.dailyback.features.accountoccurrences.domain.OccurrenceStatus
import com.dailyback.features.accounts.domain.Account
import com.dailyback.features.accounts.domain.AccountOccurrence
import com.dailyback.features.accounts.domain.RecurrenceType
import com.dailyback.shared.time.UtcClock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.UUID

class AccountLifecycleUseCasesTest {

    @Test
    fun `editing account updates only future pending occurrences`() {
        val repository = FakeAccountRepository()
        val clock = UtcClock(Clock.fixed(Instant.parse("2026-01-15T00:00:00Z"), ZoneOffset.UTC))
        val createUseCase = CreateAccountUseCase(repository, RecurrenceGenerationService(), clock)
        val updateUseCase = UpdateAccountUseCase(repository, RecurrenceGenerationService(), clock)

        val created = createUseCase.execute(
            UpsertAccountInput(
                title = "Gym",
                baseAmount = BigDecimal("100.00"),
                startDate = LocalDate.parse("2026-01-10"),
                endDate = LocalDate.parse("2026-01-20"),
                recurrenceType = RecurrenceType.DAILY,
                categoryId = repository.defaultCategoryId,
                notes = null,
            ),
        )

        repository.markOccurrencePaid(created.id, LocalDate.parse("2026-01-16"))

        updateUseCase.execute(
            created.id,
            UpsertAccountInput(
                title = "Gym Premium",
                baseAmount = BigDecimal("120.00"),
                startDate = LocalDate.parse("2026-01-10"),
                endDate = LocalDate.parse("2026-01-20"),
                recurrenceType = RecurrenceType.DAILY,
                categoryId = repository.defaultCategoryId,
                notes = "Updated",
                active = true,
            ),
        )

        val all = repository.findOccurrencesByAccountId(created.id)
        val paid = all.first { it.dueDate == LocalDate.parse("2026-01-16") }
        assertEquals(OccurrenceStatus.PAID, paid.status)
        assertEquals("Gym", paid.titleSnapshot)

        val futureOpen = all.filter { it.dueDate >= LocalDate.parse("2026-01-15") && it.status == OccurrenceStatus.PENDING }
        assertTrue(futureOpen.all { it.titleSnapshot == "Gym Premium" })
    }

    @Test
    fun `paid and past occurrences are preserved on edit`() {
        val repository = FakeAccountRepository()
        val clock = UtcClock(Clock.fixed(Instant.parse("2026-03-10T00:00:00Z"), ZoneOffset.UTC))
        val createUseCase = CreateAccountUseCase(repository, RecurrenceGenerationService(), clock)
        val updateUseCase = UpdateAccountUseCase(repository, RecurrenceGenerationService(), clock)

        val created = createUseCase.execute(
            UpsertAccountInput(
                title = "Rent",
                baseAmount = BigDecimal("800.00"),
                startDate = LocalDate.parse("2026-03-01"),
                endDate = LocalDate.parse("2026-03-20"),
                recurrenceType = RecurrenceType.DAILY,
                categoryId = repository.defaultCategoryId,
                notes = null,
            ),
        )

        repository.markOccurrencePaid(created.id, LocalDate.parse("2026-03-12"))

        updateUseCase.execute(
            created.id,
            UpsertAccountInput(
                title = "Rent Updated",
                baseAmount = BigDecimal("850.00"),
                startDate = LocalDate.parse("2026-03-01"),
                endDate = LocalDate.parse("2026-03-20"),
                recurrenceType = RecurrenceType.DAILY,
                categoryId = repository.defaultCategoryId,
                notes = null,
                active = true,
            ),
        )

        val occurrences = repository.findOccurrencesByAccountId(created.id)
        val past = occurrences.filter { it.dueDate < LocalDate.parse("2026-03-10") }
        assertTrue(past.all { it.titleSnapshot == "Rent" })
        assertTrue(occurrences.any { it.dueDate == LocalDate.parse("2026-03-12") && it.status == OccurrenceStatus.PAID })
    }

    @Test
    fun `deactivation removes future pending occurrences`() {
        val repository = FakeAccountRepository()
        val clock = UtcClock(Clock.fixed(Instant.parse("2026-02-10T00:00:00Z"), ZoneOffset.UTC))
        val createUseCase = CreateAccountUseCase(repository, RecurrenceGenerationService(), clock)
        val deactivateUseCase = DeactivateAccountUseCase(repository, clock)

        val account = createUseCase.execute(
            UpsertAccountInput(
                title = "Streaming",
                baseAmount = BigDecimal("30.00"),
                startDate = LocalDate.parse("2026-02-01"),
                endDate = LocalDate.parse("2026-02-20"),
                recurrenceType = RecurrenceType.DAILY,
                categoryId = repository.defaultCategoryId,
                notes = null,
            ),
        )

        deactivateUseCase.execute(account.id)

        assertFalse(repository.findById(account.id)!!.active)
        assertTrue(
            repository.findOccurrencesByAccountId(account.id)
                .none { it.status == OccurrenceStatus.PENDING && it.dueDate >= LocalDate.parse("2026-02-10") },
        )
    }

    @Test
    fun `activation regenerates occurrences`() {
        val repository = FakeAccountRepository()
        val clock = UtcClock(Clock.fixed(Instant.parse("2026-02-10T00:00:00Z"), ZoneOffset.UTC))
        val createUseCase = CreateAccountUseCase(repository, RecurrenceGenerationService(), clock)
        val deactivateUseCase = DeactivateAccountUseCase(repository, clock)
        val activateUseCase = ActivateAccountUseCase(repository, RecurrenceGenerationService(), clock)

        val account = createUseCase.execute(
            UpsertAccountInput(
                title = "Phone",
                baseAmount = BigDecimal("50.00"),
                startDate = LocalDate.parse("2026-02-01"),
                endDate = LocalDate.parse("2026-02-20"),
                recurrenceType = RecurrenceType.DAILY,
                categoryId = repository.defaultCategoryId,
                notes = null,
            ),
        )
        deactivateUseCase.execute(account.id)
        activateUseCase.execute(account.id)

        val openFuture = repository.findOccurrencesByAccountId(account.id)
            .filter { it.status == OccurrenceStatus.PENDING && it.dueDate >= LocalDate.parse("2026-02-10") }
        assertTrue(openFuture.isNotEmpty())
    }

    @Test
    fun `deletion only works when allowed`() {
        val repository = FakeAccountRepository()
        val clock = UtcClock(Clock.fixed(Instant.parse("2026-02-10T00:00:00Z"), ZoneOffset.UTC))
        val createUseCase = CreateAccountUseCase(repository, RecurrenceGenerationService(), clock)
        val deleteUseCase = DeleteAccountUseCase(repository, clock)

        val deletable = createUseCase.execute(
            UpsertAccountInput(
                title = "One time",
                baseAmount = BigDecimal("0.00"),
                startDate = LocalDate.parse("2026-03-01"),
                endDate = null,
                recurrenceType = RecurrenceType.UNIQUE,
                categoryId = repository.defaultCategoryId,
                notes = null,
            ),
        )
        deleteUseCase.execute(deletable.id)
        assertEquals(null, repository.findById(deletable.id))

        val withHistory = createUseCase.execute(
            UpsertAccountInput(
                title = "Water",
                baseAmount = BigDecimal("10.00"),
                startDate = LocalDate.parse("2026-01-01"),
                endDate = LocalDate.parse("2026-03-01"),
                recurrenceType = RecurrenceType.DAILY,
                categoryId = repository.defaultCategoryId,
                notes = null,
            ),
        )
        repository.markOccurrencePaid(withHistory.id, LocalDate.parse("2026-02-10"))
        deleteUseCase.execute(withHistory.id)
        assertFalse(repository.findById(withHistory.id)!!.active)
    }
}

private class FakeAccountRepository : AccountRepository {
    private val accounts = linkedMapOf<UUID, Account>()
    private val occurrences = mutableListOf<AccountOccurrence>()
    val defaultCategoryId: UUID = UUID.randomUUID()

    override fun findAll(): List<Account> = accounts.values.toList()

    override fun findActiveRecurringAccounts(): List<Account> =
        accounts.values.filter { it.active && it.recurrenceType != RecurrenceType.UNIQUE }

    override fun findById(id: UUID): Account? = accounts[id]

    override fun categoryExists(categoryId: UUID): Boolean = categoryId == defaultCategoryId

    override fun create(command: SaveAccountCommand): Account {
        val now = Instant.parse("2026-01-01T00:00:00Z")
        val account = Account(
            id = UUID.randomUUID(),
            title = command.title,
            baseAmount = command.baseAmount,
            startDate = command.startDate,
            endDate = command.endDate,
            recurrenceType = command.recurrenceType,
            categoryId = command.categoryId,
            notes = command.notes,
            active = command.active,
            createdAt = now,
            updatedAt = now,
        )
        accounts[account.id] = account
        return account
    }

    override fun update(id: UUID, command: SaveAccountCommand): Account {
        val existing = accounts.getValue(id)
        val updated = existing.copy(
            title = command.title,
            baseAmount = command.baseAmount,
            startDate = command.startDate,
            endDate = command.endDate,
            recurrenceType = command.recurrenceType,
            categoryId = command.categoryId,
            notes = command.notes,
            active = command.active,
            updatedAt = Instant.now(),
        )
        accounts[id] = updated
        return updated
    }

    override fun setActive(id: UUID, active: Boolean): Account {
        val existing = accounts.getValue(id)
        val updated = existing.copy(active = active, updatedAt = Instant.now())
        accounts[id] = updated
        return updated
    }

    override fun delete(id: UUID) {
        accounts.remove(id)
        occurrences.removeIf { it.accountId == id }
    }

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

    override fun deleteFuturePendingOccurrences(accountId: UUID, fromDate: LocalDate) {
        occurrences.removeIf {
            it.accountId == accountId &&
                it.status == OccurrenceStatus.PENDING &&
                it.dueDate >= fromDate
        }
    }

    override fun findOccurrencesByAccountId(accountId: UUID): List<AccountOccurrence> =
        occurrences.filter { it.accountId == accountId }.sortedBy { it.dueDate }

    override fun hasRelevantHistory(accountId: UUID, today: LocalDate): Boolean =
        occurrences.any {
            it.accountId == accountId &&
                (it.status == OccurrenceStatus.PAID || it.dueDate < today)
        }

    fun markOccurrencePaid(accountId: UUID, dueDate: LocalDate) {
        val index = occurrences.indexOfFirst { it.accountId == accountId && it.dueDate == dueDate }
        if (index >= 0) {
            occurrences[index] = occurrences[index].copy(
                status = OccurrenceStatus.PAID,
                paidAt = Instant.now(),
            )
        }
    }
}
