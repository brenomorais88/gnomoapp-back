package com.dailyback.features.accountoccurrences.application

import com.dailyback.features.accounts.application.AccountAccessContextResolver
import com.dailyback.features.accounts.application.AccountRepository
import com.dailyback.features.accounts.application.AccountViewerQuery
import com.dailyback.features.accounts.application.SaveAccountCommand
import com.dailyback.features.accounts.domain.Account
import com.dailyback.features.accounts.domain.AccountAccessDeniedException
import com.dailyback.features.accounts.domain.AccountOccurrence
import com.dailyback.features.accounts.domain.AccountOwnershipType
import com.dailyback.features.accounts.domain.RecurrenceType
import com.dailyback.features.accountoccurrences.domain.OccurrenceNotFoundException
import com.dailyback.features.accountoccurrences.domain.OccurrenceStatus
import com.dailyback.features.families.application.FamilyMemberPermissionRepository
import com.dailyback.features.families.application.FamilyMemberRepository
import com.dailyback.features.families.domain.FamilyMember
import com.dailyback.shared.domain.family.FamilyMemberPermissionFlags
import com.dailyback.shared.domain.family.FamilyMemberRole
import com.dailyback.shared.domain.family.FamilyMembershipStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

private val familyViewerUserId: UUID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb")
private val otherPersonalOwnerId: UUID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc")
private val testFamilyId: UUID = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd")
private val testFamilyMemberId: UUID = UUID.fromString("eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee")

private class MutablePermissionRepository(
    var flags: FamilyMemberPermissionFlags = FamilyMemberPermissionFlags.memberDefaults(),
) : FamilyMemberPermissionRepository {
    override fun findByMemberId(memberId: UUID): FamilyMemberPermissionFlags? =
        if (memberId == testFamilyMemberId) flags else null

    override fun upsert(memberId: UUID, flags: FamilyMemberPermissionFlags): FamilyMemberPermissionFlags =
        throw UnsupportedOperationException()
}

private class ActiveFamilyMemberRepository : FamilyMemberRepository {
    override fun findActiveMembershipForUser(userId: UUID): FamilyMember? {
        if (userId != familyViewerUserId) return null
        val now = Instant.parse("2026-01-01T00:00:00Z")
        return FamilyMember(
            id = testFamilyMemberId,
            familyId = testFamilyId,
            userId = userId,
            displayName = "Member",
            document = null,
            email = null,
            phone = null,
            role = FamilyMemberRole.MEMBER,
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

/**
 * Mirrors visibility rules used by [com.dailyback.features.accounts.infrastructure.ExposedAccountRepository]
 * for PERSONAL / FAMILY without duplicating SQL.
 */
private class ScopedAccountRepository(
    private val account: Account,
) : AccountRepository {
    override fun findVisibleForUser(query: AccountViewerQuery): List<Account> {
        val ids = findVisibleAccountIds(query)
        return if (account.id in ids) listOf(account) else emptyList()
    }

    override fun findVisibleAccountIds(query: AccountViewerQuery): Set<UUID> = when (account.ownershipType) {
        AccountOwnershipType.PERSONAL ->
            if (account.ownerUserId == query.userId) setOf(account.id) else emptySet()

        AccountOwnershipType.FAMILY ->
            if (query.canViewFamilyAccounts && account.familyId == query.viewerFamilyId) {
                setOf(account.id)
            } else {
                emptySet()
            }
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

private class RecordingOccurrenceRepository(
    private val occurrence: AccountOccurrence,
) : OccurrenceRepository {
    var lastMarkPaidId: UUID? = null
    var lastOverrideId: UUID? = null

    override fun findByFilters(filters: OccurrenceFilters): List<AccountOccurrence> = emptyList()

    override fun findById(id: UUID): AccountOccurrence? = if (id == occurrence.id) occurrence else null

    override fun markPaid(id: UUID): AccountOccurrence {
        lastMarkPaidId = id
        return occurrence.copy(status = OccurrenceStatus.PAID, paidAt = Instant.now())
    }

    override fun unmarkPaid(id: UUID): AccountOccurrence = throw UnsupportedOperationException()

    override fun overrideAmount(id: UUID, amount: BigDecimal): AccountOccurrence {
        lastOverrideId = id
        return occurrence.copy(amountSnapshot = amount)
    }
}

class OccurrenceOwnershipPermissionsTest {

    @Test
    fun `stranger cannot get another users personal occurrence`() {
        val now = Instant.parse("2026-04-01T00:00:00Z")
        val accountId = UUID.randomUUID()
        val account = Account(
            id = accountId,
            title = "Secret",
            baseAmount = BigDecimal.TEN,
            startDate = LocalDate.parse("2026-04-01"),
            endDate = null,
            recurrenceType = RecurrenceType.UNIQUE,
            categoryId = UUID.randomUUID(),
            notes = null,
            active = true,
            ownershipType = AccountOwnershipType.PERSONAL,
            ownerUserId = otherPersonalOwnerId,
            familyId = null,
            createdByUserId = otherPersonalOwnerId,
            responsibleMemberId = null,
            createdAt = now,
            updatedAt = now,
        )
        val occurrence = AccountOccurrence(
            id = UUID.randomUUID(),
            accountId = accountId,
            titleSnapshot = "Secret",
            amountSnapshot = BigDecimal.TEN,
            dueDate = LocalDate.parse("2026-04-10"),
            status = OccurrenceStatus.PENDING,
            paidAt = null,
            notesSnapshot = null,
            categoryIdSnapshot = UUID.randomUUID(),
            createdAt = now,
            updatedAt = now,
        )
        val occurrenceRepo = RecordingOccurrenceRepository(occurrence)
        val accountRepo = ScopedAccountRepository(account)
        val access = AccountAccessContextResolver(ActiveFamilyMemberRepository(), MutablePermissionRepository())
        val useCase = GetOccurrenceByIdUseCase(occurrenceRepo, accountRepo, access)

        assertFailsWith<OccurrenceNotFoundException> {
            useCase.execute(familyViewerUserId, occurrence.id)
        }
    }

    @Test
    fun `family member without mark permission cannot mark family occurrence paid`() {
        val permRepo = MutablePermissionRepository(
            FamilyMemberPermissionFlags.memberDefaults().copy(canMarkFamilyAccountsPaid = false),
        )
        val access = AccountAccessContextResolver(ActiveFamilyMemberRepository(), permRepo)
        val ctx = familyAccountAndOccurrence()
        val markUseCase = MarkOccurrencePaidUseCase(ctx.occurrenceRepo, ctx.accountRepo, access)

        assertFailsWith<AccountAccessDeniedException> {
            markUseCase.execute(familyViewerUserId, ctx.occurrence.id)
        }
        assertEquals(null, ctx.occurrenceRepo.lastMarkPaidId)
    }

    @Test
    fun `family member with mark permission can mark family occurrence paid`() {
        val permRepo = MutablePermissionRepository(
            FamilyMemberPermissionFlags.memberDefaults().copy(canMarkFamilyAccountsPaid = true),
        )
        val access = AccountAccessContextResolver(ActiveFamilyMemberRepository(), permRepo)
        val ctx = familyAccountAndOccurrence()
        val markUseCase = MarkOccurrencePaidUseCase(ctx.occurrenceRepo, ctx.accountRepo, access)

        markUseCase.execute(familyViewerUserId, ctx.occurrence.id)

        assertEquals(ctx.occurrence.id, ctx.occurrenceRepo.lastMarkPaidId)
    }

    @Test
    fun `family member without edit permission cannot override amount`() {
        val permRepo = MutablePermissionRepository(
            FamilyMemberPermissionFlags.memberDefaults().copy(
                canEditFamilyAccounts = false,
                canMarkFamilyAccountsPaid = true,
            ),
        )
        val access = AccountAccessContextResolver(ActiveFamilyMemberRepository(), permRepo)
        val ctx = familyAccountAndOccurrence()
        val overrideUseCase = OverrideOccurrenceAmountUseCase(ctx.occurrenceRepo, ctx.accountRepo, access)

        assertFailsWith<AccountAccessDeniedException> {
            overrideUseCase.execute(familyViewerUserId, ctx.occurrence.id, BigDecimal("99.99"))
        }
        assertEquals(null, ctx.occurrenceRepo.lastOverrideId)
    }

    @Test
    fun `family member with edit permission can override amount`() {
        val permRepo = MutablePermissionRepository(
            FamilyMemberPermissionFlags.memberDefaults().copy(
                canEditFamilyAccounts = true,
                canMarkFamilyAccountsPaid = true,
            ),
        )
        val access = AccountAccessContextResolver(ActiveFamilyMemberRepository(), permRepo)
        val ctx = familyAccountAndOccurrence()
        val overrideUseCase = OverrideOccurrenceAmountUseCase(ctx.occurrenceRepo, ctx.accountRepo, access)

        overrideUseCase.execute(familyViewerUserId, ctx.occurrence.id, BigDecimal("42.00"))

        assertEquals(ctx.occurrence.id, ctx.occurrenceRepo.lastOverrideId)
    }
}

private data class FamilyOccurrenceTestContext(
    val occurrenceRepo: RecordingOccurrenceRepository,
    val accountRepo: ScopedAccountRepository,
    val occurrence: AccountOccurrence,
)

private fun familyAccountAndOccurrence(): FamilyOccurrenceTestContext {
    val now = Instant.parse("2026-04-01T00:00:00Z")
    val accountId = UUID.randomUUID()
    val account = Account(
        id = accountId,
        title = "Rent",
        baseAmount = BigDecimal("100.00"),
        startDate = LocalDate.parse("2026-04-01"),
        endDate = null,
        recurrenceType = RecurrenceType.MONTHLY,
        categoryId = UUID.randomUUID(),
        notes = null,
        active = true,
        ownershipType = AccountOwnershipType.FAMILY,
        ownerUserId = null,
        familyId = testFamilyId,
        createdByUserId = familyViewerUserId,
        responsibleMemberId = null,
        createdAt = now,
        updatedAt = now,
    )
    val occurrence = AccountOccurrence(
        id = UUID.randomUUID(),
        accountId = accountId,
        titleSnapshot = "Rent",
        amountSnapshot = BigDecimal("100.00"),
        dueDate = LocalDate.parse("2026-04-05"),
        status = OccurrenceStatus.PENDING,
        paidAt = null,
        notesSnapshot = null,
        categoryIdSnapshot = UUID.randomUUID(),
        createdAt = now,
        updatedAt = now,
    )
    return FamilyOccurrenceTestContext(
        occurrenceRepo = RecordingOccurrenceRepository(occurrence),
        accountRepo = ScopedAccountRepository(account),
        occurrence = occurrence,
    )
}
