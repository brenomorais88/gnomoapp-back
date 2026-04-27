package com.dailyback.features.dashboard.application

import com.dailyback.features.accountoccurrences.application.OccurrenceFilters
import com.dailyback.features.accountoccurrences.application.OccurrenceRepository
import com.dailyback.features.accountoccurrences.domain.OccurrenceStatus
import com.dailyback.features.accounts.application.AccountAccessContextResolver
import com.dailyback.features.accounts.application.AccountRepository
import com.dailyback.features.accounts.application.AccountViewerQuery
import com.dailyback.features.accounts.application.SaveAccountCommand
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

private val dashboardTestUserId: UUID =
    UUID.fromString("88888888-8888-8888-8888-888888888888")

private val dashboardAccountId: UUID =
    UUID.fromString("77777777-7777-7777-7777-777777777777")

private object DashboardNoFamilyMemberRepository : FamilyMemberRepository {
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

private object DashboardStubPermissionRepository : FamilyMemberPermissionRepository {
    override fun findByMemberId(memberId: UUID): FamilyMemberPermissionFlags? = null

    override fun upsert(memberId: UUID, flags: FamilyMemberPermissionFlags): FamilyMemberPermissionFlags =
        throw UnsupportedOperationException()
}

private val dashboardAccountAccess = AccountAccessContextResolver(
    DashboardNoFamilyMemberRepository,
    DashboardStubPermissionRepository,
)

private val dashboardTestAccount: Account = run {
    val now = Instant.parse("2026-04-01T00:00:00Z")
    Account(
        id = dashboardAccountId,
        title = "Dashboard",
        baseAmount = BigDecimal.ZERO,
        startDate = LocalDate.parse("2026-04-01"),
        endDate = null,
        recurrenceType = RecurrenceType.UNIQUE,
        categoryId = UUID.randomUUID(),
        notes = null,
        active = true,
        ownershipType = AccountOwnershipType.PERSONAL,
        ownerUserId = dashboardTestUserId,
        familyId = null,
        createdByUserId = dashboardTestUserId,
        responsibleMemberId = null,
        createdAt = now,
        updatedAt = now,
    )
}

private class DashboardFakeAccountRepository : AccountRepository {
    override fun findVisibleForUser(query: AccountViewerQuery): List<Account> =
        if (dashboardTestAccount.ownerUserId == query.userId) listOf(dashboardTestAccount) else emptyList()

    override fun findVisibleAccountIds(query: AccountViewerQuery): Set<UUID> =
        if (dashboardTestAccount.ownerUserId == query.userId) setOf(dashboardTestAccount.id) else emptySet()

    override fun findActiveRecurringAccounts(): List<Account> = emptyList()

    override fun findById(id: UUID): Account? = if (id == dashboardTestAccount.id) dashboardTestAccount else null

    override fun create(command: SaveAccountCommand): Account = throw UnsupportedOperationException()

    override fun update(id: UUID, command: SaveAccountCommand): Account = throw UnsupportedOperationException()

    override fun setActive(id: UUID, active: Boolean): Account = throw UnsupportedOperationException()

    override fun delete(id: UUID) = throw UnsupportedOperationException()

    override fun upsertOccurrences(occurrences: List<com.dailyback.features.accounts.application.OccurrenceSnapshot>) =
        throw UnsupportedOperationException()

    override fun deleteFuturePendingOccurrences(accountId: UUID, fromDate: LocalDate) =
        throw UnsupportedOperationException()

    override fun findOccurrencesByAccountId(accountId: UUID): List<AccountOccurrence> = emptyList()

    override fun hasRelevantHistory(accountId: UUID, today: LocalDate): Boolean = false
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
        val accountIds = filters.accountIds
        if (accountIds == null || accountIds.isEmpty()) {
            return emptyList()
        }
        var data = occurrences.filter { it.accountId in accountIds }
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
        accountId = dashboardAccountId,
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

class DashboardUseCasesTest {

    @Test
    fun `overdue calculations and next 7 days reminder logic`() {
        val occurrenceRepository = FakeDashboardOccurrenceRepository()
        val accountRepository = DashboardFakeAccountRepository()
        val clock = UtcClock(Clock.fixed(Instant.parse("2026-04-10T00:00:00Z"), ZoneOffset.UTC))
        val useCase = GetDashboardHomeSummaryUseCase(
            occurrenceRepository,
            accountRepository,
            dashboardAccountAccess,
            clock,
        )

        val result = useCase.execute(dashboardTestUserId, month = "2026-04")

        assertEquals(1, result.overdue.size)
        assertEquals(LocalDate.parse("2026-04-09"), result.overdue.first().dueDate)
        assertEquals(2, result.next7Days.size)
        assertTrue(result.next7Days.all { it.dueDate in LocalDate.parse("2026-04-10")..LocalDate.parse("2026-04-17") })
    }

    @Test
    fun `month totals and category summary`() {
        val occurrenceRepository = FakeDashboardOccurrenceRepository()
        val accountRepository = DashboardFakeAccountRepository()
        val clock = UtcClock(Clock.fixed(Instant.parse("2026-04-10T00:00:00Z"), ZoneOffset.UTC))
        val home = GetDashboardHomeSummaryUseCase(
            occurrenceRepository,
            accountRepository,
            dashboardAccountAccess,
            clock,
        ).execute(dashboardTestUserId, month = "2026-04")
        val summary = GetDashboardCategorySummaryUseCase(
            occurrenceRepository,
            accountRepository,
            dashboardAccountAccess,
            clock,
        ).execute(dashboardTestUserId, month = "2026-04")

        assertEquals(BigDecimal("60.00"), home.totalPendingInMonth)
        assertEquals(BigDecimal("40.00"), home.totalPaidInMonth)
        assertTrue(summary.isNotEmpty())
        assertEquals(BigDecimal("70.00"), summary.first().totalAmount)
    }

    @Test
    fun `next 12 months projection grouping`() {
        val occurrenceRepository = FakeDashboardOccurrenceRepository()
        val accountRepository = DashboardFakeAccountRepository()
        val clock = UtcClock(Clock.fixed(Instant.parse("2026-04-10T00:00:00Z"), ZoneOffset.UTC))
        val useCase = GetDashboardNext12MonthsProjectionUseCase(
            occurrenceRepository,
            accountRepository,
            dashboardAccountAccess,
            clock,
        )

        val projection = useCase.execute(dashboardTestUserId, includeDetails = true)

        assertEquals(12, projection.size)
        assertEquals("2026-04", projection.first().month)
        assertEquals(BigDecimal("100.00"), projection.first().totalAmount)
        assertTrue(projection.first().occurrences.isNotEmpty())
    }
}
