package com.dailyback.features.categories.application

import com.dailyback.features.categories.domain.Category
import java.util.UUID

interface CategoryRepository {
    fun listForUser(userId: UUID, familyId: UUID?): List<Category>

    fun findByIdForUser(userId: UUID, familyId: UUID?, id: UUID): Category?

    fun existsByNameForUser(userId: UUID, familyId: UUID?, name: String): Boolean

    fun existsByNameExcludingIdForUser(
        userId: UUID,
        familyId: UUID?,
        name: String,
        excludedId: UUID,
    ): Boolean

    fun isVisibleToUser(categoryId: UUID, userId: UUID, familyId: UUID?): Boolean

    fun createForUser(userId: UUID, familyId: UUID?, name: String, color: String?): Category

    fun updateForUser(userId: UUID, familyId: UUID?, id: UUID, name: String, color: String?): Category

    fun deleteByIdForUser(userId: UUID, familyId: UUID?, id: UUID)

    fun isCategoryInUse(id: UUID): Boolean
}
