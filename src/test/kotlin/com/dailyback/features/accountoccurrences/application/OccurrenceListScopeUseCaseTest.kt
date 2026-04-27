package com.dailyback.features.accountoccurrences.application

import com.dailyback.features.accounts.application.AccountAccessContextResolver
import com.dailyback.features.accounts.application.AccountQueryScope
import com.dailyback.features.accounts.application.AccountRepository
import com.dailyback.features.accounts.application.AccountViewerQuery
import com.dailyback.features.accounts.application.OccurrenceSnapshot
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
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

class OccurrenceListScopeUseCaseTest {
    private val userId: UUID = UUID.fromString("10000000-0000-0000-0000-000000000001")
    private val familyId: UUID = UUID.fromString("20000000-0000-0000-0000-000000000001")

    @Test
    fun `scope PERSONAL only queries personal account ids`() {
        val accounts = fixtureAccounts(userId, familyId)
        val accountRepo = OccurrenceScopeAccountRepository(accounts)
        val occurrenceRepo = CapturingOccurrenceRepository()
        val useCase = ListOccurrencesUseCase(
            occurrenceRepository = occurrenceRepo,
            accountRepository = accountRepo,
            accountAccess = scopeResolver(canViewFamily = true, canViewOtherPersonal = true),
        )

        useCase.execute(userId, OccurrenceFilters(), AccountQueryScope.PERSONAL)

        assertEquals(setOf(accounts.personalOwn.id), occurrenceRepo.lastFilters?.accountIds)
    }

    @Test
    fun `scope FAMILY only queries family account ids when allowed`() {
        val accounts = fixtureAccounts(userId, familyId)
        val accountRepo = OccurrenceScopeAccountRepository(accounts)
        val occurrenceRepo = CapturingOccurrenceRepository()
        val useCase = ListOccurrencesUseCase(
            occurrenceRepository = occurrenceRepo,
            accountRepository = accountRepo,
            accountAccess = scopeResolver(canViewFamily = true, canViewOtherPersonal = false),
        )

        useCase.execute(userId, OccurrenceFilters(), AccountQueryScope.FAMILY)

        assertEquals(setOf(accounts.family.id), occurrenceRepo.lastFilters?.accountIds)
    }

    @Test
    fun `scope FAMILY with no permission yields empty result and skips repository query`() {
        val accounts = fixtureAccounts(userId, familyId)
        val accountRepo = OccurrenceScopeAccountRepository(accounts)
        val occurrenceRepo = CapturingOccurrenceRepository()
        val useCase = ListOccurrencesUseCase(
            occurrenceRepository = occurrenceRepo,
            accountRepository = accountRepo,
            accountAccess = scopeResolver(canViewFamily = false, canViewOtherPersonal = false),
        )

        val result = useCase.execute(userId, OccurrenceFilters(), AccountQueryScope.FAMILY)

        assertEquals(emptyList(), result)
        assertEquals(null, occurrenceRepo.lastFilters)
    }
}

private data class ScopeAccounts(
    val personalOwn: Account,
    val family: Account,
    val personalOther: Account,
)

private fun fixtureAccounts(userId: UUID, familyId: UUID): ScopeAccounts {
    val now = Instant.parse("2026-01-01T00:00:00Z")
    val personalOwn = Account(
        id = UUID.randomUUID(),
        title = "Mine",
        baseAmount = BigDecimal.TEN,
        startDate = LocalDate.parse("2026-01-01"),
        endDate = null,
        recurrenceType = RecurrenceType.UNIQUE,
        categoryId = UUID.randomUUID(),
        notes = null,
        active = true,
        ownershipType = AccountOwnershipType.PERSONAL,
        ownerUserId = userId,
        familyId = null,
        createdByUserId = userId,
        responsibleMemberId = null,
        createdAt = now,
        updatedAt = now,
    )
    val family = Account(
        id = UUID.randomUUID(),
        title = "Family",
        baseAmount = BigDecimal.TEN,
        startDate = LocalDate.parse("2026-01-01"),
        endDate = null,
        recurrenceType = RecurrenceType.UNIQUE,
        categoryId = UUID.randomUUID(),
        notes = null,
        active = true,
        ownershipType = AccountOwnershipType.FAMILY,
        ownerUserId = null,
        familyId = familyId,
        createdByUserId = userId,
        responsibleMemberId = null,
        createdAt = now,
        updatedAt = now,
    )
    val personalOther = personalOwn.copy(
        id = UUID.randomUUID(),
        ownerUserId = UUID.fromString("10000000-0000-0000-0000-000000000002"),
        createdByUserId = UUID.fromString("10000000-0000-0000-0000-000000000002"),
        title = "Other",
    )
    return ScopeAccounts(personalOwn, family, personalOther)
}

private fun scopeResolver(
    canViewFamily: Boolean,
    canViewOtherPersonal: Boolean,
): AccountAccessContextResolver {
    val familyId = UUID.fromString("20000000-0000-0000-0000-000000000001")
    val memberId = UUID.fromString("30000000-0000-0000-0000-000000000001")
    val userId = UUID.fromString("10000000-0000-0000-0000-000000000001")
    val memberRepo = object : FamilyMemberRepository {
        override fun findActiveMembershipForUser(userIdParam: UUID): FamilyMember? {
            if (userIdParam != userId) return null
            val now = Instant.parse("2026-01-01T00:00:00Z")
            return FamilyMember(
                id = memberId,
                familyId = familyId,
                userId = userId,
                displayName = "User",
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
    val permissionRepo = object : FamilyMemberPermissionRepository {
        override fun findByMemberId(memberIdParam: UUID): FamilyMemberPermissionFlags? =
            if (memberIdParam == memberId) {
                FamilyMemberPermissionFlags.memberDefaults().copy(
                    canViewFamilyAccounts = canViewFamily,
                    canViewOtherPersonalAccounts = canViewOtherPersonal,
                )
            } else {
                null
            }

        override fun upsert(memberId: UUID, flags: FamilyMemberPermissionFlags): FamilyMemberPermissionFlags =
            throw UnsupportedOperationException()
    }
    return AccountAccessContextResolver(memberRepo, permissionRepo)
}

private class OccurrenceScopeAccountRepository(
    private val scopeAccounts: ScopeAccounts,
) : AccountRepository {
    override fun findVisibleForUser(query: AccountViewerQuery): List<Account> {
        val all = listOf(scopeAccounts.personalOwn, scopeAccounts.family, scopeAccounts.personalOther)
        return all.filter { account ->
            when (account.ownershipType) {
                AccountOwnershipType.PERSONAL ->
                    account.ownerUserId == query.userId || query.canViewOtherPersonalAccounts

                AccountOwnershipType.FAMILY ->
                    query.canViewFamilyAccounts && account.familyId == query.viewerFamilyId
            }
        }
    }

    override fun findActiveRecurringAccounts(): List<Account> = emptyList()
    override fun findById(id: UUID): Account? = null
    override fun findVisibleAccountIds(query: AccountViewerQuery): Set<UUID> = emptySet()
    override fun create(command: SaveAccountCommand): Account = throw UnsupportedOperationException()
    override fun update(id: UUID, command: SaveAccountCommand): Account = throw UnsupportedOperationException()
    override fun setActive(id: UUID, active: Boolean): Account = throw UnsupportedOperationException()
    override fun delete(id: UUID) = throw UnsupportedOperationException()
    override fun upsertOccurrences(occurrences: List<OccurrenceSnapshot>) = throw UnsupportedOperationException()
    override fun deleteFuturePendingOccurrences(accountId: UUID, fromDate: LocalDate) = throw UnsupportedOperationException()
    override fun findOccurrencesByAccountId(accountId: UUID): List<AccountOccurrence> = emptyList()
    override fun hasRelevantHistory(accountId: UUID, today: LocalDate): Boolean = false
}

private class CapturingOccurrenceRepository : OccurrenceRepository {
    var lastFilters: OccurrenceFilters? = null

    override fun findByFilters(filters: OccurrenceFilters): List<AccountOccurrence> {
        lastFilters = filters
        return emptyList()
    }

    override fun findById(id: UUID): AccountOccurrence? = null
    override fun markPaid(id: UUID): AccountOccurrence = throw UnsupportedOperationException()
    override fun unmarkPaid(id: UUID): AccountOccurrence = throw UnsupportedOperationException()
    override fun overrideAmount(id: UUID, amount: BigDecimal): AccountOccurrence = throw UnsupportedOperationException()
}
