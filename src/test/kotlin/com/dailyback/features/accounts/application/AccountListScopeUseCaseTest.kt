package com.dailyback.features.accounts.application

import com.dailyback.features.accounts.domain.Account
import com.dailyback.features.accounts.domain.AccountOccurrence
import com.dailyback.features.accounts.domain.AccountOwnershipType
import com.dailyback.features.accounts.domain.RecurrenceType
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

class AccountListScopeUseCaseTest {
    private val userId: UUID = UUID.fromString("10000000-0000-0000-0000-000000000001")
    private val otherUserId: UUID = UUID.fromString("10000000-0000-0000-0000-000000000002")
    private val familyId: UUID = UUID.fromString("20000000-0000-0000-0000-000000000001")

    @Test
    fun `scope PERSONAL returns only my personal accounts`() {
        val repository = ScopedAccountRepositoryFixture(accounts(userId, otherUserId, familyId))
        val useCase = ListAccountsUseCase(repository, resolver(canViewFamily = true, canViewOtherPersonal = true))

        val result = useCase.execute(userId, AccountQueryScope.PERSONAL)

        assertEquals(1, result.size)
        assertEquals(AccountOwnershipType.PERSONAL, result.first().ownershipType)
        assertEquals(userId, result.first().ownerUserId)
    }

    @Test
    fun `scope FAMILY returns only family accounts user can view`() {
        val repository = ScopedAccountRepositoryFixture(accounts(userId, otherUserId, familyId))
        val useCase = ListAccountsUseCase(repository, resolver(canViewFamily = true, canViewOtherPersonal = true))

        val result = useCase.execute(userId, AccountQueryScope.FAMILY)

        assertEquals(1, result.size)
        assertEquals(AccountOwnershipType.FAMILY, result.first().ownershipType)
        assertEquals(familyId, result.first().familyId)
    }

    @Test
    fun `scope VISIBLE_TO_ME returns personal own plus permitted family and other personal`() {
        val repository = ScopedAccountRepositoryFixture(accounts(userId, otherUserId, familyId))
        val useCase = ListAccountsUseCase(repository, resolver(canViewFamily = true, canViewOtherPersonal = true))

        val result = useCase.execute(userId, AccountQueryScope.VISIBLE_TO_ME)

        assertEquals(3, result.size)
    }

    @Test
    fun `scope FAMILY returns empty when member lacks family view permission`() {
        val repository = ScopedAccountRepositoryFixture(accounts(userId, otherUserId, familyId))
        val useCase = ListAccountsUseCase(repository, resolver(canViewFamily = false, canViewOtherPersonal = false))

        val result = useCase.execute(userId, AccountQueryScope.FAMILY)

        assertEquals(emptyList(), result)
    }
}

private fun accounts(userId: UUID, otherUserId: UUID, familyId: UUID): List<Account> {
    val now = Instant.parse("2026-01-01T00:00:00Z")
    return listOf(
        Account(
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
        ),
        Account(
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
        ),
        Account(
            id = UUID.randomUUID(),
            title = "Other personal",
            baseAmount = BigDecimal.TEN,
            startDate = LocalDate.parse("2026-01-01"),
            endDate = null,
            recurrenceType = RecurrenceType.UNIQUE,
            categoryId = UUID.randomUUID(),
            notes = null,
            active = true,
            ownershipType = AccountOwnershipType.PERSONAL,
            ownerUserId = otherUserId,
            familyId = null,
            createdByUserId = otherUserId,
            responsibleMemberId = null,
            createdAt = now,
            updatedAt = now,
        ),
    )
}

private fun resolver(
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

private class ScopedAccountRepositoryFixture(
    private val all: List<Account>,
) : AccountRepository {
    override fun findVisibleForUser(query: AccountViewerQuery): List<Account> =
        all.filter { account ->
            when (account.ownershipType) {
                AccountOwnershipType.PERSONAL ->
                    account.ownerUserId == query.userId || query.canViewOtherPersonalAccounts

                AccountOwnershipType.FAMILY ->
                    query.canViewFamilyAccounts && account.familyId == query.viewerFamilyId
            }
        }

    override fun findActiveRecurringAccounts(): List<Account> = emptyList()
    override fun findById(id: UUID): Account? = all.firstOrNull { it.id == id }
    override fun findVisibleAccountIds(query: AccountViewerQuery): Set<UUID> = findVisibleForUser(query).map { it.id }.toSet()
    override fun create(command: SaveAccountCommand): Account = throw UnsupportedOperationException()
    override fun update(id: UUID, command: SaveAccountCommand): Account = throw UnsupportedOperationException()
    override fun setActive(id: UUID, active: Boolean): Account = throw UnsupportedOperationException()
    override fun delete(id: UUID) = throw UnsupportedOperationException()
    override fun upsertOccurrences(occurrences: List<OccurrenceSnapshot>) = throw UnsupportedOperationException()
    override fun deleteFuturePendingOccurrences(accountId: UUID, fromDate: LocalDate) = throw UnsupportedOperationException()
    override fun findOccurrencesByAccountId(accountId: UUID): List<AccountOccurrence> = emptyList()
    override fun hasRelevantHistory(accountId: UUID, today: LocalDate): Boolean = false
}
