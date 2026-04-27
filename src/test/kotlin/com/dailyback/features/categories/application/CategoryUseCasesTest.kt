package com.dailyback.features.categories.application

import com.dailyback.features.categories.domain.Category
import com.dailyback.features.categories.domain.CategoryInUseException
import com.dailyback.features.categories.domain.CategoryNotModifiableException
import com.dailyback.features.categories.domain.DuplicateCategoryNameException
import com.dailyback.features.categories.domain.InvalidCategoryNameException
import com.dailyback.features.families.application.FamilyMemberRepository
import com.dailyback.features.families.domain.FamilyMember
import com.dailyback.shared.domain.family.FamilyMemberRole
import com.dailyback.shared.domain.family.FamilyMembershipStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import java.time.Instant
import java.util.UUID

private val testUserId: UUID = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee")

class CategoryUseCasesTest {

    @Test
    fun `should create category`() {
        val repository = InMemoryCategoryRepository()
        val useCase = CreateCategoryUseCase(repository, StubFamilyMemberRepo(null))

        val created = useCase.execute(testUserId, name = "Leisure", color = "#AA0000")

        assertEquals("Leisure", created.name)
        assertEquals("#AA0000", created.color)
        assertEquals(testUserId, created.ownerUserId)
        assertEquals(null, created.familyId)
        assertEquals(1, repository.items.size)
    }

    @Test
    fun `should reject duplicate category name on create`() {
        val repository = InMemoryCategoryRepository()
        val familyRepo = StubFamilyMemberRepo(null)
        CreateCategoryUseCase(repository, familyRepo).execute(testUserId, name = "Leisure", color = null)
        val useCase = CreateCategoryUseCase(repository, familyRepo)

        assertFailsWith<DuplicateCategoryNameException> {
            useCase.execute(testUserId, name = "Leisure", color = "#111111")
        }
    }

    @Test
    fun `should update category`() {
        val repository = InMemoryCategoryRepository()
        val familyRepo = StubFamilyMemberRepo(null)
        val created = CreateCategoryUseCase(repository, familyRepo).execute(testUserId, name = "Home", color = null)
        val useCase = UpdateCategoryUseCase(repository, familyRepo)

        val updated = useCase.execute(testUserId, id = created.id, name = "Home Essentials", color = "#ABCDEF")

        assertEquals("Home Essentials", updated.name)
        assertEquals("#ABCDEF", updated.color)
    }

    @Test
    fun `should delete category when not in use`() {
        val repository = InMemoryCategoryRepository()
        val familyRepo = StubFamilyMemberRepo(null)
        val created = CreateCategoryUseCase(repository, familyRepo).execute(testUserId, name = "Travel", color = null)
        val useCase = DeleteCategoryUseCase(repository, familyRepo)

        useCase.execute(testUserId, created.id)

        assertEquals(0, repository.items.size)
    }

    @Test
    fun `should reject delete when category in use`() {
        val repository = InMemoryCategoryRepository()
        val familyRepo = StubFamilyMemberRepo(null)
        val created = CreateCategoryUseCase(repository, familyRepo).execute(testUserId, name = "Debt", color = null)
        repository.markInUse(created.id)
        val useCase = DeleteCategoryUseCase(repository, familyRepo)

        assertFailsWith<CategoryInUseException> {
            useCase.execute(testUserId, created.id)
        }
    }

    @Test
    fun `should reject blank category name`() {
        val repository = InMemoryCategoryRepository()
        val useCase = CreateCategoryUseCase(repository, StubFamilyMemberRepo(null))

        assertFailsWith<InvalidCategoryNameException> {
            useCase.execute(testUserId, name = "   ", color = null)
        }
    }

    @Test
    fun `should reject update on global category`() {
        val repository = InMemoryCategoryRepository().apply {
            putGlobal("Fixed")
        }
        val useCase = UpdateCategoryUseCase(repository, StubFamilyMemberRepo(null))
        val globalId = repository.items.values.first { it.isGlobal }.id

        assertFailsWith<CategoryNotModifiableException> {
            useCase.execute(testUserId, id = globalId, name = "Renamed", color = null)
        }
    }
}

private class StubFamilyMemberRepo(
    private val membership: FamilyMember?,
) : FamilyMemberRepository {
    override fun findActiveMembershipForUser(userId: UUID): FamilyMember? =
        membership?.takeIf { it.userId == userId }

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

private class InMemoryCategoryRepository : CategoryRepository {
    val items = linkedMapOf<UUID, Category>()
    private val inUseIds = mutableSetOf<UUID>()

    fun putGlobal(name: String) {
        val now = Instant.now()
        val id = UUID.randomUUID()
        items[id] = Category(
            id = id,
            name = name,
            color = null,
            familyId = null,
            ownerUserId = null,
            createdAt = now,
            updatedAt = now,
        )
    }

    private fun visible(cat: Category, userId: UUID, familyId: UUID?): Boolean {
        if (cat.isGlobal) return true
        if (familyId != null && cat.familyId == familyId) return true
        if (cat.ownerUserId == userId) return true
        return false
    }

    override fun listForUser(userId: UUID, familyId: UUID?): List<Category> =
        items.values.filter { visible(it, userId, familyId) }.sortedBy { it.name }

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
        val existing = findByIdForUser(userId, familyId, id) ?: error("missing $id")
        if (existing.isGlobal) error("global")
        val updated = existing.copy(
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

    fun markInUse(id: UUID) {
        inUseIds.add(id)
    }
}
