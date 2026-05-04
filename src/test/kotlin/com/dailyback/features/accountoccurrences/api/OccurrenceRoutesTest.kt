package com.dailyback.features.accountoccurrences.api

import com.dailyback.app.config.JwtAuthConfig
import com.dailyback.features.accountoccurrences.application.GetOccurrenceByIdUseCase
import com.dailyback.features.accountoccurrences.application.ListOccurrencesUseCase
import com.dailyback.features.accountoccurrences.application.MarkOccurrencePaidUseCase
import com.dailyback.features.accountoccurrences.application.OccurrenceFilters
import com.dailyback.features.accountoccurrences.application.OccurrenceRepository
import com.dailyback.features.accountoccurrences.application.OverrideOccurrenceAmountUseCase
import com.dailyback.features.accountoccurrences.application.UnmarkOccurrencePaidUseCase
import com.dailyback.features.accountoccurrences.domain.OccurrenceStatus
import com.dailyback.features.accounts.application.AccountAccessContextResolver
import com.dailyback.features.accounts.application.AccountRepository
import com.dailyback.features.accounts.application.AccountViewerQuery
import com.dailyback.features.accounts.application.OccurrenceSnapshot
import com.dailyback.features.accounts.application.SaveAccountCommand
import com.dailyback.features.accounts.domain.Account
import com.dailyback.features.accounts.domain.AccountOccurrence
import com.dailyback.features.accounts.domain.AccountOwnershipType
import com.dailyback.features.accounts.domain.RecurrenceType
import com.dailyback.features.families.application.FamilyMemberPermissionRepository
import com.dailyback.features.families.application.FamilyMemberRepository
import com.dailyback.features.families.application.FamilyPermissionAuthorizer
import com.dailyback.features.families.domain.FamilyMember
import com.dailyback.features.users.application.JwtTokenService
import com.dailyback.shared.domain.family.FamilyMemberPermissionFlags
import com.dailyback.shared.domain.family.FamilyMemberRole
import com.dailyback.shared.domain.family.FamilyMembershipStatus
import com.dailyback.shared.errors.ApiErrorResponse
import com.dailyback.shared.errors.ApiException
import io.ktor.client.HttpClient
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import io.ktor.server.routing.routing
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OccurrenceRoutesTest {
    private val userId: UUID = UUID.fromString("aaaaaaaa-1111-1111-1111-111111111111")
    private val accountId: UUID = UUID.fromString("bbbbbbbb-2222-2222-2222-222222222222")
    private val categoryA: UUID = UUID.fromString("cccccccc-3333-3333-3333-333333333333")
    private val categoryB: UUID = UUID.fromString("dddddddd-4444-4444-4444-444444444444")

    @Test
    fun `returns 400 when startDate is missing`() = testApplication {
        val client = createAuthenticatedClient()
        setupOccurrenceRoutesApp()
        val response = client.get("/occurrences?endDate=2026-04-30")
        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertTrue(response.bodyAsText().contains("startDate"))
    }

    @Test
    fun `returns occurrences inside informed period`() = testApplication {
        val client = createAuthenticatedClient()
        setupOccurrenceRoutesApp()
        val response = client.get("/occurrences?startDate=2026-04-10&endDate=2026-04-20")
        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.bodyAsText()
        assertTrue(body.contains("\"dueDate\":\"2026-04-15\""))
        assertTrue(!body.contains("\"dueDate\":\"2026-04-05\""))
        assertTrue(!body.contains("\"dueDate\":\"2026-04-25\""))
    }

    @Test
    fun `supports optional combined filters with required date range`() = testApplication {
        val client = createAuthenticatedClient()
        setupOccurrenceRoutesApp()
        val response = client.get(
            "/occurrences?startDate=2026-04-01&endDate=2026-04-30" +
                "&status=PENDING&categoryId=$categoryB&text=internet&month=2026-04",
        )
        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.bodyAsText()
        assertTrue(body.contains("\"titleSnapshot\":\"Internet\""))
        assertTrue(!body.contains("\"titleSnapshot\":\"Market\""))
        assertTrue(!body.contains("\"titleSnapshot\":\"Gym\""))
    }

    private fun ApplicationTestBuilder.setupOccurrenceRoutesApp() {
        application {
            val account = testAccount(userId, accountId, categoryA)
            val occurrenceRepository = RouteFakeOccurrenceRepository(accountId, categoryA, categoryB)
            val accountRepository = RouteFakeAccountRepository(account)
            val access = AccountAccessContextResolver(RouteNoFamilyMemberRepository, RouteNoPermissionRepository)
            install(ContentNegotiation) { json() }
            install(Authentication) {
                val jwtService = JwtTokenService(routeJwtConfig)
                jwt("auth-jwt") {
                    verifier(jwtService.verifier)
                    validate { credential ->
                        val subject = credential.payload.subject ?: return@validate null
                        runCatching { UUID.fromString(subject) }.getOrNull()?.let { JWTPrincipal(credential.payload) }
                    }
                }
            }
            install(StatusPages) {
                exception<ApiException> { call, cause ->
                    call.respond(
                        cause.statusCode,
                        ApiErrorResponse(
                            timestamp = Instant.now().toString(),
                            path = call.request.local.uri,
                            errorCode = cause.errorCode,
                            message = cause.message,
                            details = cause.details,
                            traceId = UUID.randomUUID().toString(),
                        ),
                    )
                }
            }
            routing {
                authenticate("auth-jwt") {
                    occurrenceRoutes(
                        familyPermissionAuthorizer = FamilyPermissionAuthorizer { _, _ -> },
                        listOccurrencesUseCase = ListOccurrencesUseCase(occurrenceRepository, accountRepository, access),
                        getOccurrenceByIdUseCase = GetOccurrenceByIdUseCase(occurrenceRepository, accountRepository, access),
                        markOccurrencePaidUseCase = MarkOccurrencePaidUseCase(occurrenceRepository, accountRepository, access),
                        unmarkOccurrencePaidUseCase = UnmarkOccurrencePaidUseCase(occurrenceRepository, accountRepository, access),
                        overrideOccurrenceAmountUseCase = OverrideOccurrenceAmountUseCase(occurrenceRepository, accountRepository, access),
                    )
                }
            }
        }
    }

    private fun ApplicationTestBuilder.createAuthenticatedClient(): HttpClient =
        createClient {
            defaultRequest {
                headers.append(HttpHeaders.Authorization, "Bearer ${JwtTokenService(routeJwtConfig).createAccessToken(userId).token}")
            }
        }
}

private val routeJwtConfig = JwtAuthConfig(
    secret = "unit-test-secret-key-for-jwt-hs256-must-be-long-enough",
    issuer = "test",
    audience = "test",
    accessTokenTtlSeconds = 3600,
)

private fun testAccount(userId: UUID, accountId: UUID, categoryId: UUID): Account {
    val now = Instant.parse("2026-04-01T00:00:00Z")
    return Account(
        id = accountId,
        title = "Account",
        baseAmount = BigDecimal("100.00"),
        startDate = LocalDate.parse("2026-04-01"),
        endDate = null,
        recurrenceType = RecurrenceType.MONTHLY,
        categoryId = categoryId,
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
}

private object RouteNoFamilyMemberRepository : FamilyMemberRepository {
    override fun findActiveMembershipForUser(userId: UUID): FamilyMember? = null
    override fun findActiveMembersByFamily(familyId: UUID): List<FamilyMember> = emptyList()
    override fun findNonRemovedMemberInFamilyByUser(familyId: UUID, userId: UUID): FamilyMember? = null
    override fun findConflictingInviteInFamily(familyId: UUID, documentNormalized: String?, emailLower: String?, phoneDigits: String?): FamilyMember? = null
    override fun insertMember(familyId: UUID, userId: UUID?, displayName: String, document: String?, email: String?, phone: String?, role: FamilyMemberRole, status: FamilyMembershipStatus, invitedByUserId: UUID?, joinedAt: Instant?): FamilyMember = throw UnsupportedOperationException()
    override fun findMemberByIdInFamily(memberId: UUID, familyId: UUID): FamilyMember? = null
    override fun countActiveAdminsInFamily(familyId: UUID): Int = 0
    override fun updateMemberRole(memberId: UUID, familyId: UUID, newRole: FamilyMemberRole): FamilyMember? = null
    override fun markMemberRemoved(memberId: UUID, familyId: UUID): FamilyMember? = null
}

private object RouteNoPermissionRepository : FamilyMemberPermissionRepository {
    override fun findByMemberId(memberId: UUID): FamilyMemberPermissionFlags? = null
    override fun upsert(memberId: UUID, flags: FamilyMemberPermissionFlags): FamilyMemberPermissionFlags = throw UnsupportedOperationException()
}

private class RouteFakeAccountRepository(private val account: Account) : AccountRepository {
    override fun findVisibleForUser(query: AccountViewerQuery): List<Account> = if (account.ownerUserId == query.userId) listOf(account) else emptyList()
    override fun findActiveRecurringAccounts(): List<Account> = emptyList()
    override fun findById(id: UUID): Account? = if (id == account.id) account else null
    override fun findVisibleAccountIds(query: AccountViewerQuery): Set<UUID> = findVisibleForUser(query).map { it.id }.toSet()
    override fun create(command: SaveAccountCommand): Account = throw UnsupportedOperationException()
    override fun update(id: UUID, command: SaveAccountCommand): Account = throw UnsupportedOperationException()
    override fun updateAndRefreshFuturePendingOccurrences(id: UUID, command: SaveAccountCommand, fromDate: LocalDate, futurePendingSnapshots: List<OccurrenceSnapshot>): Account = throw UnsupportedOperationException()
    override fun setActive(id: UUID, active: Boolean): Account = throw UnsupportedOperationException()
    override fun delete(id: UUID) = throw UnsupportedOperationException()
    override fun upsertOccurrences(occurrences: List<OccurrenceSnapshot>) = throw UnsupportedOperationException()
    override fun deleteFuturePendingOccurrences(accountId: UUID, fromDate: LocalDate) = throw UnsupportedOperationException()
    override fun findOccurrencesByAccountId(accountId: UUID): List<AccountOccurrence> = emptyList()
    override fun hasRelevantHistory(accountId: UUID, today: LocalDate): Boolean = false
}

private class RouteFakeOccurrenceRepository(
    accountId: UUID,
    categoryA: UUID,
    categoryB: UUID,
) : OccurrenceRepository {
    private val now = Instant.parse("2026-04-01T00:00:00Z")
    private val rows = listOf(
        row(accountId, "Market", "10.00", "2026-04-05", OccurrenceStatus.PENDING, categoryA, "weekly groceries"),
        row(accountId, "Internet", "20.00", "2026-04-15", OccurrenceStatus.PENDING, categoryB, "home internet"),
        row(accountId, "Gym", "30.00", "2026-04-25", OccurrenceStatus.PAID, categoryA, "health"),
    )

    override fun findByFilters(filters: OccurrenceFilters): List<AccountOccurrence> {
        val ids = filters.accountIds ?: return emptyList()
        var data = rows.filter { it.accountId in ids }
        filters.status?.let { s -> data = data.filter { it.status == s } }
        filters.categoryId?.let { c -> data = data.filter { it.categoryIdSnapshot == c } }
        filters.startDate?.let { start -> data = data.filter { it.dueDate >= start } }
        filters.endDate?.let { end -> data = data.filter { it.dueDate <= end } }
        filters.month?.let { month ->
            val ym = java.time.YearMonth.parse(month)
            data = data.filter { java.time.YearMonth.from(it.dueDate) == ym }
        }
        filters.text?.trim()?.takeIf { it.isNotBlank() }?.let { q ->
            val text = q.lowercase()
            data = data.filter { it.titleSnapshot.lowercase().contains(text) || (it.notesSnapshot?.lowercase()?.contains(text) == true) }
        }
        return data.sortedBy { it.dueDate }
    }

    override fun findById(id: UUID): AccountOccurrence? = rows.firstOrNull { it.id == id }
    override fun markPaid(id: UUID): AccountOccurrence = rows.first { it.id == id }.copy(status = OccurrenceStatus.PAID, paidAt = now)
    override fun unmarkPaid(id: UUID): AccountOccurrence = rows.first { it.id == id }.copy(status = OccurrenceStatus.PENDING, paidAt = null)
    override fun overrideAmount(id: UUID, amount: BigDecimal): AccountOccurrence = rows.first { it.id == id }.copy(amountSnapshot = amount)

    private fun row(accountId: UUID, title: String, amount: String, dueDate: String, status: OccurrenceStatus, categoryId: UUID, notes: String?): AccountOccurrence =
        AccountOccurrence(
            id = UUID.randomUUID(),
            accountId = accountId,
            titleSnapshot = title,
            amountSnapshot = BigDecimal(amount),
            dueDate = LocalDate.parse(dueDate),
            status = status,
            paidAt = if (status == OccurrenceStatus.PAID) now else null,
            notesSnapshot = notes,
            categoryIdSnapshot = categoryId,
            createdAt = now,
            updatedAt = now,
        )
}
