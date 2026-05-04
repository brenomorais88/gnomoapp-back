package com.dailyback.features.accountoccurrences.application

import com.dailyback.features.accounts.application.AccountAccessContextResolver
import com.dailyback.features.accounts.application.AccountRepository
import com.dailyback.features.accounts.application.AccountViewerQuery
import com.dailyback.features.accounts.application.SaveAccountCommand
import com.dailyback.features.accounts.domain.Account
import com.dailyback.features.accounts.domain.AccountOccurrence
import com.dailyback.features.accounts.domain.AccountOwnershipType
import com.dailyback.features.accounts.domain.RecurrenceType
import com.dailyback.features.accountoccurrences.domain.OccurrenceStatus
import com.dailyback.features.families.application.FamilyMemberPermissionRepository
import com.dailyback.features.families.application.FamilyMemberRepository
import com.dailyback.features.families.domain.FamilyMember
import com.dailyback.shared.domain.family.FamilyMemberPermissionFlags
import com.dailyback.shared.domain.family.FamilyMemberRole
import com.dailyback.shared.domain.family.FamilyMembershipStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

private val occurrenceTestUserId: UUID =
    UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa")

private object OccurrenceNoFamilyMemberRepository : FamilyMemberRepository {
    override fun findActiveMembershipForUser(userId: UUID): FamilyMember? = null

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

private object OccurrenceStubPermissionRepository : FamilyMemberPermissionRepository {
    override fun findByMemberId(memberId: UUID): FamilyMemberPermissionFlags? = null

    override fun upsert(memberId: UUID, flags: FamilyMemberPermissionFlags): FamilyMemberPermissionFlags =
        throw UnsupportedOperationException()
}

private val occurrenceAccountAccess = AccountAccessContextResolver(
    OccurrenceNoFamilyMemberRepository,
    OccurrenceStubPermissionRepository,
)

private class SinglePersonalAccountRepository(
    private val account: Account,
) : AccountRepository {
    override fun findVisibleForUser(query: AccountViewerQuery): List<Account> =
        findVisibleAccountIds(query).mapNotNull { id -> if (id == account.id) account else null }

    override fun findVisibleAccountIds(query: AccountViewerQuery): Set<UUID> =
        if (account.ownershipType == AccountOwnershipType.PERSONAL && account.ownerUserId == query.userId) {
            setOf(account.id)
        } else {
            emptySet()
        }

    override fun findActiveRecurringAccounts(): List<Account> = emptyList()

    override fun findById(id: UUID): Account? = if (id == account.id) account else null

    override fun create(command: SaveAccountCommand): Account = throw UnsupportedOperationException()

    override fun update(id: UUID, command: SaveAccountCommand): Account = throw UnsupportedOperationException()

    override fun updateAndRefreshFuturePendingOccurrences(
        id: UUID,
        command: SaveAccountCommand,
        fromDate: LocalDate,
        futurePendingSnapshots: List<com.dailyback.features.accounts.application.OccurrenceSnapshot>,
    ): Account = throw UnsupportedOperationException()

    override fun setActive(id: UUID, active: Boolean): Account = throw UnsupportedOperationException()

    override fun delete(id: UUID) = throw UnsupportedOperationException()

    override fun upsertOccurrences(occurrences: List<com.dailyback.features.accounts.application.OccurrenceSnapshot>) =
        throw UnsupportedOperationException()

    override fun deleteFuturePendingOccurrences(accountId: UUID, fromDate: LocalDate) =
        throw UnsupportedOperationException()

    override fun findOccurrencesByAccountId(accountId: UUID): List<AccountOccurrence> = emptyList()

    override fun hasRelevantHistory(accountId: UUID, today: LocalDate): Boolean = false
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
        val accountIds = filters.accountIds
        if (accountIds == null || accountIds.isEmpty()) {
            return emptyList()
        }
        var data = items.values.filter { it.accountId in accountIds }
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

private data class OccurrenceTestContext(
    val occurrenceRepo: FakeOccurrenceRepository,
    val accountRepo: AccountRepository,
)

private fun occurrenceTestContext(): OccurrenceTestContext {
    val accountId = UUID.randomUUID()
    val categoryA = UUID.randomUUID()
    val categoryB = UUID.randomUUID()
    val now = Instant.parse("2026-04-01T00:00:00Z")
    val account = Account(
        id = accountId,
        title = "Test",
        baseAmount = BigDecimal.ZERO,
        startDate = LocalDate.parse("2026-04-01"),
        endDate = null,
        recurrenceType = RecurrenceType.UNIQUE,
        categoryId = categoryA,
        notes = null,
        active = true,
        ownershipType = AccountOwnershipType.PERSONAL,
        ownerUserId = occurrenceTestUserId,
        familyId = null,
        createdByUserId = occurrenceTestUserId,
        responsibleMemberId = null,
        createdAt = now,
        updatedAt = now,
    )
    val occurrenceRepo = FakeOccurrenceRepository(
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
    return OccurrenceTestContext(occurrenceRepo, SinglePersonalAccountRepository(account))
}

class OccurrenceUseCasesTest {

    @Test
    fun `mark as paid`() {
        val ctx = occurrenceTestContext()
        val useCase = MarkOccurrencePaidUseCase(ctx.occurrenceRepo, ctx.accountRepo, occurrenceAccountAccess)

        val updated = useCase.execute(occurrenceTestUserId, ctx.occurrenceRepo.occurrenceA.id)

        assertEquals(OccurrenceStatus.PAID, updated.status)
        assertFalse(updated.paidAt == null)
    }

    @Test
    fun `unmark as paid`() {
        val ctx = occurrenceTestContext().apply {
            occurrenceRepo.markPaid(occurrenceRepo.occurrenceA.id)
        }
        val useCase = UnmarkOccurrencePaidUseCase(ctx.occurrenceRepo, ctx.accountRepo, occurrenceAccountAccess)

        val updated = useCase.execute(occurrenceTestUserId, ctx.occurrenceRepo.occurrenceA.id)

        assertEquals(OccurrenceStatus.PENDING, updated.status)
        assertEquals(null, updated.paidAt)
    }

    @Test
    fun `override amount changes only one occurrence`() {
        val ctx = occurrenceTestContext()
        val useCase = OverrideOccurrenceAmountUseCase(ctx.occurrenceRepo, ctx.accountRepo, occurrenceAccountAccess)

        useCase.execute(occurrenceTestUserId, ctx.occurrenceRepo.occurrenceA.id, BigDecimal("555.55"))

        val first = ctx.occurrenceRepo.findById(ctx.occurrenceRepo.occurrenceA.id)!!
        val second = ctx.occurrenceRepo.findById(ctx.occurrenceRepo.occurrenceB.id)!!
        val baseAccount = ctx.accountRepo.findById(first.accountId)!!
        assertEquals(BigDecimal("555.55"), first.amountSnapshot)
        assertEquals(BigDecimal("20.00"), second.amountSnapshot)
        assertEquals(BigDecimal.ZERO, baseAccount.baseAmount)
    }

    @Test
    fun `list filtering by status`() {
        val ctx = occurrenceTestContext().apply {
            occurrenceRepo.markPaid(occurrenceRepo.occurrenceA.id)
        }
        val useCase = ListOccurrencesUseCase(ctx.occurrenceRepo, ctx.accountRepo, occurrenceAccountAccess)

        val result = useCase.execute(occurrenceTestUserId, OccurrenceFilters(status = OccurrenceStatus.PAID))

        assertEquals(1, result.size)
        assertEquals(OccurrenceStatus.PAID, result.first().status)
    }

    @Test
    fun `list filtering by category`() {
        val ctx = occurrenceTestContext()
        val useCase = ListOccurrencesUseCase(ctx.occurrenceRepo, ctx.accountRepo, occurrenceAccountAccess)

        val result = useCase.execute(occurrenceTestUserId, OccurrenceFilters(categoryId = ctx.occurrenceRepo.categoryB))

        assertEquals(1, result.size)
        assertEquals(ctx.occurrenceRepo.categoryB, result.first().categoryIdSnapshot)
    }

    @Test
    fun `list filtering by text`() {
        val ctx = occurrenceTestContext()
        val useCase = ListOccurrencesUseCase(ctx.occurrenceRepo, ctx.accountRepo, occurrenceAccountAccess)

        val result = useCase.execute(occurrenceTestUserId, OccurrenceFilters(text = "market"))

        assertEquals(1, result.size)
        assertTrue(result.first().titleSnapshot.contains("Market"))
    }

    @Test
    fun `list filtering by date range`() {
        val ctx = occurrenceTestContext()
        val useCase = ListOccurrencesUseCase(ctx.occurrenceRepo, ctx.accountRepo, occurrenceAccountAccess)

        val result = useCase.execute(
            occurrenceTestUserId,
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
        val ctx = occurrenceTestContext()
        val useCase = ListOccurrencesUseCase(ctx.occurrenceRepo, ctx.accountRepo, occurrenceAccountAccess)

        val result = useCase.execute(occurrenceTestUserId, OccurrenceFilters())

        assertTrue(result[0].dueDate <= result[1].dueDate)
        assertTrue(result[1].dueDate <= result[2].dueDate)
    }
}
