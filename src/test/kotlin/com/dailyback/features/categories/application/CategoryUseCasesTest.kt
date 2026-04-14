package com.dailyback.features.categories.application

import com.dailyback.features.categories.domain.Category
import com.dailyback.features.categories.domain.CategoryInUseException
import com.dailyback.features.categories.domain.DuplicateCategoryNameException
import com.dailyback.features.categories.domain.InvalidCategoryNameException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import java.time.Instant
import java.util.UUID

class CategoryUseCasesTest {

    @Test
    fun `should create category`() {
        val repository = InMemoryCategoryRepository()
        val useCase = CreateCategoryUseCase(repository)

        val created = useCase.execute(name = "Leisure", color = "#AA0000")

        assertEquals("Leisure", created.name)
        assertEquals("#AA0000", created.color)
        assertEquals(1, repository.items.size)
    }

    @Test
    fun `should reject duplicate category name on create`() {
        val repository = InMemoryCategoryRepository()
        CreateCategoryUseCase(repository).execute(name = "Leisure", color = null)
        val useCase = CreateCategoryUseCase(repository)

        assertFailsWith<DuplicateCategoryNameException> {
            useCase.execute(name = "Leisure", color = "#111111")
        }
    }

    @Test
    fun `should update category`() {
        val repository = InMemoryCategoryRepository()
        val created = CreateCategoryUseCase(repository).execute(name = "Home", color = null)
        val useCase = UpdateCategoryUseCase(repository)

        val updated = useCase.execute(id = created.id, name = "Home Essentials", color = "#ABCDEF")

        assertEquals("Home Essentials", updated.name)
        assertEquals("#ABCDEF", updated.color)
    }

    @Test
    fun `should delete category when not in use`() {
        val repository = InMemoryCategoryRepository()
        val created = CreateCategoryUseCase(repository).execute(name = "Travel", color = null)
        val useCase = DeleteCategoryUseCase(repository)

        useCase.execute(created.id)

        assertEquals(0, repository.items.size)
    }

    @Test
    fun `should reject delete when category in use`() {
        val repository = InMemoryCategoryRepository()
        val created = CreateCategoryUseCase(repository).execute(name = "Debt", color = null)
        repository.markInUse(created.id)
        val useCase = DeleteCategoryUseCase(repository)

        assertFailsWith<CategoryInUseException> {
            useCase.execute(created.id)
        }
    }

    @Test
    fun `should reject blank category name`() {
        val repository = InMemoryCategoryRepository()
        val useCase = CreateCategoryUseCase(repository)

        assertFailsWith<InvalidCategoryNameException> {
            useCase.execute(name = "   ", color = null)
        }
    }
}

private class InMemoryCategoryRepository : CategoryRepository {
    val items = linkedMapOf<UUID, Category>()
    private val inUseIds = mutableSetOf<UUID>()

    override fun findAll(): List<Category> = items.values.sortedBy { it.name }

    override fun findById(id: UUID): Category? = items[id]

    override fun existsByName(name: String): Boolean = items.values.any { it.name.equals(name, ignoreCase = true) }

    override fun existsByNameExcludingId(name: String, excludedId: UUID): Boolean =
        items.values.any { it.id != excludedId && it.name.equals(name, ignoreCase = true) }

    override fun create(name: String, color: String?): Category {
        val now = Instant.now()
        val category = Category(
            id = UUID.randomUUID(),
            name = name,
            color = color,
            createdAt = now,
            updatedAt = now,
        )
        items[category.id] = category
        return category
    }

    override fun update(id: UUID, name: String, color: String?): Category {
        val existing = items.getValue(id)
        val updated = existing.copy(
            name = name,
            color = color,
            updatedAt = Instant.now(),
        )
        items[id] = updated
        return updated
    }

    override fun deleteById(id: UUID) {
        items.remove(id)
    }

    override fun isCategoryInUse(id: UUID): Boolean = inUseIds.contains(id)

    fun markInUse(id: UUID) {
        inUseIds.add(id)
    }
}
