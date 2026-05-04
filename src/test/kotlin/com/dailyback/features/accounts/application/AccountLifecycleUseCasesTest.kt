package com.dailyback.features.accounts.application

import com.dailyback.features.accountoccurrences.domain.OccurrenceStatus
import com.dailyback.features.accounts.domain.Account
import com.dailyback.features.accounts.domain.AccountOccurrence
import com.dailyback.features.accounts.domain.AccountOwnershipType
import com.dailyback.features.accounts.domain.RecurrenceType
import com.dailyback.features.categories.application.CategoryRepository
import com.dailyback.features.categories.domain.Category
import com.dailyback.features.families.application.FamilyMemberPermissionRepository
import com.dailyback.features.families.application.FamilyMemberRepository
import com.dailyback.features.families.domain.FamilyMember
import com.dailyback.shared.domain.family.FamilyMemberPermissionFlags
import com.dailyback.shared.domain.family.FamilyMemberRole
import com.dailyback.shared.domain.family.FamilyMembershipStatus
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

private val accountLifecycleTestUserId: UUID =
    UUID.fromString("22222222-2222-2222-2222-222222222222")

private val lifecycleTestFamilyId: UUID =
    UUID.fromString("33333333-3333-3333-3333-333333333333")

private val lifecycleTestMemberId: UUID =
    UUID.fromString("44444444-4444-4444-4444-444444444444")

private object LifecycleStubPermissionRepository : FamilyMemberPermissionRepository {
    override fun findByMemberId(memberId: UUID): FamilyMemberPermissionFlags? = null

    override fun upsert(memberId: UUID, flags: FamilyMemberPermissionFlags): FamilyMemberPermissionFlags =
        throw UnsupportedOperationException()
}

private object AccountLifecycleFamilyMemberRepository : FamilyMemberRepository {
    override fun findActiveMembershipForUser(userId: UUID): FamilyMember? {
        if (userId != accountLifecycleTestUserId) return null
        val now = Instant.parse("2026-01-01T00:00:00Z")
        return FamilyMember(
            id = lifecycleTestMemberId,
            familyId = lifecycleTestFamilyId,
            userId = userId,
            displayName = "Lifecycle",
            document = null,
            email = null,
            phone = null,
            role = FamilyMemberRole.ADMIN,
            status = FamilyMembershipStatus.ACTIVE,
            invitedByUserId = null,
            joinedAt = now,
            createdAt = now,
            updatedAt = now,
        )
    }

    override fun findActiveMembersByFamily(familyId: UUID): List<FamilyMember> = emptyList()

    override fun findNonRemovedMemberInFamilyByUser(familyId: UUID, userId: UUID): FamilyMember? = null

    override fun findConflictingInviteInFamily(
        familyId: UUID,
        documentNormalized: String?,
        emailLower: String?,
        phoneDigits: String?,
    ): FamilyMember? = null

    override fun insertMember(
        familyId: UUID,
        userId: UUID?,
        displayName: String,
        document: String?,
        email: String?,
        phone: String?,
        role: FamilyMemberRole,
        status: FamilyMembershipStatus,
        invitedByUserId: UUID?,
        joinedAt: Instant?,
    ): FamilyMember = throw UnsupportedOperationException()

    override fun findMemberByIdInFamily(memberId: UUID, familyId: UUID): FamilyMember? = null

    override fun countActiveAdminsInFamily(familyId: UUID): Int = 0

    override fun updateMemberRole(memberId: UUID, familyId: UUID, newRole: FamilyMemberRole): FamilyMember? = null

    override fun markMemberRemoved(memberId: UUID, familyId: UUID): FamilyMember? = null
}

private val lifecycleAccountAccess = AccountAccessContextResolver(
    AccountLifecycleFamilyMemberRepository,
    LifecycleStubPermissionRepository,
)

private fun createInput(
    title: String,
    baseAmount: BigDecimal,
    startDate: LocalDate,
    endDate: LocalDate?,
    recurrenceType: RecurrenceType,
    categoryId: UUID,
    notes: String?,
) = CreateAccountInput(
    ownershipType = AccountOwnershipType.PERSONAL,
    responsibleMemberId = null,
    title = title,
    baseAmount = baseAmount,
    startDate = startDate,
    endDate = endDate,
    recurrenceType = recurrenceType,
    categoryId = categoryId,
    notes = notes,
    active = true,
)

class AccountLifecycleUseCasesTest {
    @Test
    fun `creating recurring account generates occurrences with same base amount`() {
        val repository = FakeAccountRepository()
        val clock = UtcClock(Clock.fixed(Instant.parse("2026-01-15T00:00:00Z"), ZoneOffset.UTC))
        val categoryRepo = AccountLifecycleStubCategoryRepository(repository.defaultCategoryId)
        val createUseCase = CreateAccountUseCase(
            repository,
            categoryRepo,
            AccountLifecycleFamilyMemberRepository,
            lifecycleAccountAccess,
            RecurrenceGenerationService(),
            clock,
        )

        val created = createUseCase.execute(
            accountLifecycleTestUserId,
            createInput(
                title = "School",
                baseAmount = BigDecimal("199.90"),
                startDate = LocalDate.parse("2026-01-15"),
                endDate = LocalDate.parse("2026-01-20"),
                recurrenceType = RecurrenceType.DAILY,
                categoryId = repository.defaultCategoryId,
                notes = null,
            ),
        )

        val generated = repository.findOccurrencesByAccountId(created.id)
        assertTrue(generated.isNotEmpty())
        assertTrue(generated.all { it.amountSnapshot == BigDecimal("199.90") })
    }


    @Test
    fun `editing account updates only future pending occurrences`() {
        val repository = FakeAccountRepository()
        val clock = UtcClock(Clock.fixed(Instant.parse("2026-01-15T00:00:00Z"), ZoneOffset.UTC))
        val categoryRepo = AccountLifecycleStubCategoryRepository(repository.defaultCategoryId)
        val createUseCase = CreateAccountUseCase(
            repository,
            categoryRepo,
            AccountLifecycleFamilyMemberRepository,
            lifecycleAccountAccess,
            RecurrenceGenerationService(),
            clock,
        )
        val updateUseCase = UpdateAccountUseCase(
            repository,
            categoryRepo,
            AccountLifecycleFamilyMemberRepository,
            lifecycleAccountAccess,
            RecurrenceGenerationService(),
            clock,
        )

        val created = createUseCase.execute(
            accountLifecycleTestUserId,
            createInput(
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
            accountLifecycleTestUserId,
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
        assertEquals(BigDecimal("100.00"), paid.amountSnapshot)

        val futureOpen = all.filter { it.dueDate >= LocalDate.parse("2026-01-15") && it.status == OccurrenceStatus.PENDING }
        assertTrue(futureOpen.all { it.titleSnapshot == "Gym Premium" })
        assertTrue(futureOpen.all { it.amountSnapshot == BigDecimal("120.00") })
    }

    @Test
    fun `paid and past occurrences are preserved on edit`() {
        val repository = FakeAccountRepository()
        val clock = UtcClock(Clock.fixed(Instant.parse("2026-03-10T00:00:00Z"), ZoneOffset.UTC))
        val categoryRepo = AccountLifecycleStubCategoryRepository(repository.defaultCategoryId)
        val createUseCase = CreateAccountUseCase(
            repository,
            categoryRepo,
            AccountLifecycleFamilyMemberRepository,
            lifecycleAccountAccess,
            RecurrenceGenerationService(),
            clock,
        )
        val updateUseCase = UpdateAccountUseCase(
            repository,
            categoryRepo,
            AccountLifecycleFamilyMemberRepository,
            lifecycleAccountAccess,
            RecurrenceGenerationService(),
            clock,
        )

        val created = createUseCase.execute(
            accountLifecycleTestUserId,
            createInput(
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
            accountLifecycleTestUserId,
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
        assertTrue(past.all { it.amountSnapshot == BigDecimal("800.00") })
        assertTrue(occurrences.any { it.dueDate == LocalDate.parse("2026-03-12") && it.status == OccurrenceStatus.PAID })
        assertTrue(
            occurrences.any {
                it.dueDate == LocalDate.parse("2026-03-12") &&
                    it.status == OccurrenceStatus.PAID &&
                    it.amountSnapshot == BigDecimal("800.00")
            },
        )
        val futurePending = occurrences.filter {
            it.dueDate >= LocalDate.parse("2026-03-10") && it.status == OccurrenceStatus.PENDING
        }
        assertTrue(futurePending.all { it.amountSnapshot == BigDecimal("850.00") })
    }

    @Test
    fun `deactivation removes future pending occurrences`() {
        val repository = FakeAccountRepository()
        val clock = UtcClock(Clock.fixed(Instant.parse("2026-02-10T00:00:00Z"), ZoneOffset.UTC))
        val categoryRepo = AccountLifecycleStubCategoryRepository(repository.defaultCategoryId)
        val createUseCase = CreateAccountUseCase(
            repository,
            categoryRepo,
            AccountLifecycleFamilyMemberRepository,
            lifecycleAccountAccess,
            RecurrenceGenerationService(),
            clock,
        )
        val deactivateUseCase = DeactivateAccountUseCase(repository, lifecycleAccountAccess, clock)

        val account = createUseCase.execute(
            accountLifecycleTestUserId,
            createInput(
                title = "Streaming",
                baseAmount = BigDecimal("30.00"),
                startDate = LocalDate.parse("2026-02-01"),
                endDate = LocalDate.parse("2026-02-20"),
                recurrenceType = RecurrenceType.DAILY,
                categoryId = repository.defaultCategoryId,
                notes = null,
            ),
        )

        deactivateUseCase.execute(accountLifecycleTestUserId, account.id)

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
        val categoryRepo = AccountLifecycleStubCategoryRepository(repository.defaultCategoryId)
        val createUseCase = CreateAccountUseCase(
            repository,
            categoryRepo,
            AccountLifecycleFamilyMemberRepository,
            lifecycleAccountAccess,
            RecurrenceGenerationService(),
            clock,
        )
        val deactivateUseCase = DeactivateAccountUseCase(repository, lifecycleAccountAccess, clock)
        val activateUseCase = ActivateAccountUseCase(
            repository,
            lifecycleAccountAccess,
            RecurrenceGenerationService(),
            clock,
        )

        val account = createUseCase.execute(
            accountLifecycleTestUserId,
            createInput(
                title = "Phone",
                baseAmount = BigDecimal("50.00"),
                startDate = LocalDate.parse("2026-02-01"),
                endDate = LocalDate.parse("2026-02-20"),
                recurrenceType = RecurrenceType.DAILY,
                categoryId = repository.defaultCategoryId,
                notes = null,
            ),
        )
        deactivateUseCase.execute(accountLifecycleTestUserId, account.id)
        activateUseCase.execute(accountLifecycleTestUserId, account.id)

        val openFuture = repository.findOccurrencesByAccountId(account.id)
            .filter { it.status == OccurrenceStatus.PENDING && it.dueDate >= LocalDate.parse("2026-02-10") }
        assertTrue(openFuture.isNotEmpty())
    }

    @Test
    fun `deletion only works when allowed`() {
        val repository = FakeAccountRepository()
        val clock = UtcClock(Clock.fixed(Instant.parse("2026-02-10T00:00:00Z"), ZoneOffset.UTC))
        val categoryRepo = AccountLifecycleStubCategoryRepository(repository.defaultCategoryId)
        val createUseCase = CreateAccountUseCase(
            repository,
            categoryRepo,
            AccountLifecycleFamilyMemberRepository,
            lifecycleAccountAccess,
            RecurrenceGenerationService(),
            clock,
        )
        val deleteUseCase = DeleteAccountUseCase(repository, lifecycleAccountAccess, clock)

        val deletable = createUseCase.execute(
            accountLifecycleTestUserId,
            createInput(
                title = "One time",
                baseAmount = BigDecimal("0.00"),
                startDate = LocalDate.parse("2026-03-01"),
                endDate = null,
                recurrenceType = RecurrenceType.UNIQUE,
                categoryId = repository.defaultCategoryId,
                notes = null,
            ),
        )
        deleteUseCase.execute(accountLifecycleTestUserId, deletable.id)
        assertEquals(null, repository.findById(deletable.id))

        val withHistory = createUseCase.execute(
            accountLifecycleTestUserId,
            createInput(
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
        deleteUseCase.execute(accountLifecycleTestUserId, withHistory.id)
        assertFalse(repository.findById(withHistory.id)!!.active)
    }
}

private class FakeAccountRepository : AccountRepository {
    private val accounts = linkedMapOf<UUID, Account>()
    private val occurrences = mutableListOf<AccountOccurrence>()
    val defaultCategoryId: UUID = UUID.randomUUID()

    private fun isVisible(account: Account, q: AccountViewerQuery): Boolean = when (account.ownershipType) {
        AccountOwnershipType.PERSONAL -> account.ownerUserId == q.userId
        AccountOwnershipType.FAMILY -> q.canViewFamilyAccounts && account.familyId == q.viewerFamilyId
    }

    override fun findVisibleForUser(query: AccountViewerQuery): List<Account> =
        accounts.values.filter { isVisible(it, query) }

    override fun findVisibleAccountIds(query: AccountViewerQuery): Set<UUID> =
        findVisibleForUser(query).map { it.id }.toSet()

    override fun findActiveRecurringAccounts(): List<Account> =
        accounts.values.filter { it.active && it.recurrenceType != RecurrenceType.UNIQUE }

    override fun findById(id: UUID): Account? = accounts[id]

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
            ownershipType = command.ownershipType,
            ownerUserId = command.ownerUserId,
            familyId = command.familyId,
            createdByUserId = command.createdByUserId,
            responsibleMemberId = command.responsibleMemberId,
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
            ownershipType = command.ownershipType,
            ownerUserId = command.ownerUserId,
            familyId = command.familyId,
            createdByUserId = command.createdByUserId,
            responsibleMemberId = command.responsibleMemberId,
            updatedAt = Instant.now(),
        )
        accounts[id] = updated
        return updated
    }

    override fun updateAndRefreshFuturePendingOccurrences(
        id: UUID,
        command: SaveAccountCommand,
        fromDate: LocalDate,
        futurePendingSnapshots: List<OccurrenceSnapshot>,
    ): Account {
        val updated = update(id, command)
        deleteFuturePendingOccurrences(id, fromDate)
        upsertOccurrences(futurePendingSnapshots)
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

private class AccountLifecycleStubCategoryRepository(
    private val visibleCategoryId: UUID,
) : CategoryRepository {
    override fun listForUser(userId: UUID, familyId: UUID?): List<Category> = emptyList()

    override fun findByIdForUser(userId: UUID, familyId: UUID?, id: UUID): Category? = null

    override fun existsByNameForUser(userId: UUID, familyId: UUID?, name: String): Boolean = false

    override fun existsByNameExcludingIdForUser(
        userId: UUID,
        familyId: UUID?,
        name: String,
        excludedId: UUID,
    ): Boolean = false

    override fun isVisibleToUser(categoryId: UUID, userId: UUID, familyId: UUID?): Boolean =
        categoryId == visibleCategoryId

    override fun createForUser(userId: UUID, familyId: UUID?, name: String, color: String?): Category =
        throw UnsupportedOperationException()

    override fun updateForUser(userId: UUID, familyId: UUID?, id: UUID, name: String, color: String?): Category =
        throw UnsupportedOperationException()

    override fun deleteByIdForUser(userId: UUID, familyId: UUID?, id: UUID) {}

    override fun isCategoryInUse(id: UUID): Boolean = false
}
