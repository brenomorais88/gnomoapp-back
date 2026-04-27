package com.dailyback.features.categories.api

import com.dailyback.app.bootstrap.module
import com.dailyback.app.config.AppConfig
import com.dailyback.app.config.DatabaseConfig
import com.dailyback.app.config.FlywayConfig
import com.dailyback.app.config.JwtAuthConfig
import com.dailyback.app.config.SchedulerConfig
import com.dailyback.app.config.SeedConfig
import com.dailyback.app.config.SecurityConfig
import com.dailyback.app.config.ServerConfig
import com.dailyback.features.categories.application.CategoryRepository
import com.dailyback.features.categories.domain.Category
import com.dailyback.features.families.application.FamilyMemberRepository
import com.dailyback.features.families.application.FamilyPermissionAuthorizer
import com.dailyback.features.families.domain.FamilyMember
import com.dailyback.features.users.application.JwtTokenService
import com.dailyback.shared.domain.family.FamilyMemberRole
import com.dailyback.shared.domain.family.FamilyMembershipStatus
import io.ktor.client.HttpClient
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headers
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import java.time.Instant
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private val categoryRoutesJwtUserId: UUID = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee")

class CategoryRoutesTest {

    @Test
    fun `should create and list categories`() = testApplication {
        val repository = InMemoryRouteCategoryRepository()
        application {
            module(
                appConfig = testConfig(),
                categoryRepositoryOverride = repository,
                familyPermissionAuthorizerOverride = FamilyPermissionAuthorizer { _, _ -> },
                familyMemberRepositoryOverride = CategoryRoutesStubMemberRepo(null),
                runStartup = false,
            )
        }
        val client = createAuthenticatedClient()

        val createResponse = client.post("/categories") {
            contentType(ContentType.Application.Json)
            setBody("""{"name":"Leisure","color":"#445566"}""")
        }

        assertEquals(HttpStatusCode.Created, createResponse.status)

        val listResponse = client.get("/categories")

        assertEquals(HttpStatusCode.OK, listResponse.status)
        assertTrue(listResponse.bodyAsText().contains("\"name\":\"Leisure\""))
    }

    @Test
    fun `should reject deletion when category is in use`() = testApplication {
        val repository = InMemoryRouteCategoryRepository()
        val category = repository.createForUser(categoryRoutesJwtUserId, null, "Debt", null)
        repository.inUseIds.add(category.id)
        application {
            module(
                appConfig = testConfig(),
                categoryRepositoryOverride = repository,
                familyPermissionAuthorizerOverride = FamilyPermissionAuthorizer { _, _ -> },
                familyMemberRepositoryOverride = CategoryRoutesStubMemberRepo(null),
                runStartup = false,
            )
        }
        val client = createAuthenticatedClient()

        val response = client.delete("/categories/${category.id}")

        assertEquals(HttpStatusCode.Conflict, response.status)
        assertTrue(response.bodyAsText().contains("CATEGORY_IN_USE"))
    }

    @Test
    fun `should get category by id`() = testApplication {
        val repository = InMemoryRouteCategoryRepository()
        val category = repository.createForUser(categoryRoutesJwtUserId, null, "Health", "#00AA00")
        application {
            module(
                appConfig = testConfig(),
                categoryRepositoryOverride = repository,
                familyPermissionAuthorizerOverride = FamilyPermissionAuthorizer { _, _ -> },
                familyMemberRepositoryOverride = CategoryRoutesStubMemberRepo(null),
                runStartup = false,
            )
        }
        val client = createAuthenticatedClient()

        val response = client.get("/categories/${category.id}")

        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.bodyAsText().contains("\"name\":\"Health\""))
    }

    @Test
    fun `should reject duplicate category on update`() = testApplication {
        val repository = InMemoryRouteCategoryRepository()
        val first = repository.createForUser(categoryRoutesJwtUserId, null, "Home", null)
        val second = repository.createForUser(categoryRoutesJwtUserId, null, "Debt", null)
        application {
            module(
                appConfig = testConfig(),
                categoryRepositoryOverride = repository,
                familyPermissionAuthorizerOverride = FamilyPermissionAuthorizer { _, _ -> },
                familyMemberRepositoryOverride = CategoryRoutesStubMemberRepo(null),
                runStartup = false,
            )
        }
        val client = createAuthenticatedClient()

        val response = client.put("/categories/${second.id}") {
            contentType(ContentType.Application.Json)
            setBody("""{"name":"Home","color":null}""")
        }

        assertEquals(HttpStatusCode.Conflict, response.status)
        assertTrue(response.bodyAsText().contains("CATEGORY_NAME_ALREADY_EXISTS"))
        assertTrue(repository.findByIdForUser(categoryRoutesJwtUserId, null, first.id) != null)
    }

    @Test
    fun `should return bad request for invalid uuid path parameter`() = testApplication {
        val repository = InMemoryRouteCategoryRepository()
        application {
            module(
                appConfig = testConfig(),
                categoryRepositoryOverride = repository,
                familyPermissionAuthorizerOverride = FamilyPermissionAuthorizer { _, _ -> },
                familyMemberRepositoryOverride = CategoryRoutesStubMemberRepo(null),
                runStartup = false,
            )
        }
        val client = createAuthenticatedClient()

        val response = client.get("/categories/not-a-uuid")

        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertTrue(response.bodyAsText().contains("INVALID_PATH_PARAMETER"))
    }

    private fun io.ktor.server.testing.ApplicationTestBuilder.createAuthenticatedClient(): HttpClient =
        createClient {
            defaultRequest {
                headers.append(HttpHeaders.Authorization, categoryRoutesTestJwt())
            }
        }

    private fun categoryRoutesTestJwt(): String {
        val token = JwtTokenService(testConfig().security.jwt)
            .createAccessToken(categoryRoutesJwtUserId)
            .token
        return "Bearer $token"
    }
}

private class CategoryRoutesStubMemberRepo(
    private val row: FamilyMember?,
) : FamilyMemberRepository {
    override fun findActiveMembershipForUser(userId: UUID): FamilyMember? = row?.takeIf { it.userId == userId }

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

private class InMemoryRouteCategoryRepository : CategoryRepository {
    private val items = linkedMapOf<UUID, Category>()
    val inUseIds = mutableSetOf<UUID>()

    private fun visible(cat: Category, userId: UUID, familyId: UUID?): Boolean {
        if (cat.isGlobal) return true
        if (familyId != null && cat.familyId == familyId) return true
        if (cat.ownerUserId == userId) return true
        return false
    }

    override fun listForUser(userId: UUID, familyId: UUID?): List<Category> =
        items.values.filter { visible(it, userId, familyId) }

    override fun findByIdForUser(userId: UUID, familyId: UUID?, id: UUID): Category? =
        items[id]?.takeIf { visible(it, userId, familyId) }

    override fun existsByNameForUser(userId: UUID, familyId: UUID?, name: String): Boolean {
        val key = name.trim().lowercase()
        return items.values.any { visible(it, userId, familyId) && it.name.lowercase() == key }
    }

    override fun existsByNameExcludingIdForUser(
        userId: UUID,
        familyId: UUID?,
        name: String,
        excludedId: UUID,
    ): Boolean {
        val key = name.trim().lowercase()
        return items.values.any {
            it.id != excludedId && visible(it, userId, familyId) && it.name.lowercase() == key
        }
    }

    override fun isVisibleToUser(categoryId: UUID, userId: UUID, familyId: UUID?): Boolean =
        items[categoryId]?.let { visible(it, userId, familyId) } == true

    override fun createForUser(userId: UUID, familyId: UUID?, name: String, color: String?): Category {
        val now = Instant.now()
        val category = Category(
            id = UUID.randomUUID(),
            name = name,
            color = color,
            familyId = familyId,
            ownerUserId = if (familyId != null) null else userId,
            createdAt = now,
            updatedAt = now,
        )
        items[category.id] = category
        return category
    }

    override fun updateForUser(userId: UUID, familyId: UUID?, id: UUID, name: String, color: String?): Category {
        val previous = findByIdForUser(userId, familyId, id) ?: error("not found")
        val updated = previous.copy(
            name = name,
            color = color,
            updatedAt = Instant.now(),
        )
        items[id] = updated
        return updated
    }

    override fun deleteByIdForUser(userId: UUID, familyId: UUID?, id: UUID) {
        val existing = findByIdForUser(userId, familyId, id) ?: return
        if (!existing.isGlobal) {
            items.remove(id)
        }
    }

    override fun isCategoryInUse(id: UUID): Boolean = inUseIds.contains(id)
}

private fun testConfig(): AppConfig = AppConfig(
    server = ServerConfig(host = "127.0.0.1", port = 8080),
    database = DatabaseConfig(
        host = "localhost",
        port = 5432,
        name = "daily_test",
        user = "daily",
        password = "daily",
        schema = "public",
        ssl = false,
        jdbcUrlOverride = null,
    ),
    flyway = FlywayConfig(
        enabled = false,
        location = "classpath:db/migration",
    ),
    seed = SeedConfig(
        enabled = false,
        scenarioEnabled = false,
    ),
    scheduler = SchedulerConfig(
        recurrenceMaintenanceEnabled = false,
        recurrenceMaintenanceIntervalHours = 24,
    ),
    security = SecurityConfig(
        jwt = JwtAuthConfig(
            secret = "unit-test-secret-key-for-jwt-hs256-must-be-long-enough",
            issuer = "test",
            audience = "test",
            accessTokenTtlSeconds = 3600,
        ),
    ),
)
