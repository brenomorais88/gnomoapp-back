package com.dailyback.features.categories.application

import com.dailyback.features.categories.domain.Category
import com.dailyback.features.categories.domain.CategoryInUseException
import com.dailyback.features.categories.domain.CategoryNotFoundException
import com.dailyback.features.categories.domain.CategoryNotModifiableException
import com.dailyback.features.categories.domain.DuplicateCategoryNameException
import com.dailyback.features.categories.domain.InvalidCategoryNameException
import com.dailyback.features.families.application.FamilyMemberRepository
import java.util.UUID

class ListCategoriesUseCase(
    private val categoryRepository: CategoryRepository,
    private val familyMemberRepository: FamilyMemberRepository,
) {
    fun execute(userId: UUID): List<Category> {
        val familyId = familyMemberRepository.findActiveMembershipForUser(userId)?.familyId
        return categoryRepository.listForUser(userId, familyId)
    }
}

class GetCategoryByIdUseCase(
    private val categoryRepository: CategoryRepository,
    private val familyMemberRepository: FamilyMemberRepository,
) {
    fun execute(userId: UUID, id: UUID): Category {
        val familyId = familyMemberRepository.findActiveMembershipForUser(userId)?.familyId
        return categoryRepository.findByIdForUser(userId, familyId, id)
            ?: throw CategoryNotFoundException(id)
    }
}

class CreateCategoryUseCase(
    private val categoryRepository: CategoryRepository,
    private val familyMemberRepository: FamilyMemberRepository,
) {
    fun execute(userId: UUID, name: String, color: String?): Category {
        val familyId = familyMemberRepository.findActiveMembershipForUser(userId)?.familyId
        val normalizedName = name.trim()
        if (normalizedName.isBlank()) {
            throw InvalidCategoryNameException()
        }
        if (categoryRepository.existsByNameForUser(userId, familyId, normalizedName)) {
            throw DuplicateCategoryNameException(normalizedName)
        }
        return categoryRepository.createForUser(userId, familyId, normalizedName, color?.trim()?.ifBlank { null })
    }
}

class UpdateCategoryUseCase(
    private val categoryRepository: CategoryRepository,
    private val familyMemberRepository: FamilyMemberRepository,
) {
    fun execute(userId: UUID, id: UUID, name: String, color: String?): Category {
        val familyId = familyMemberRepository.findActiveMembershipForUser(userId)?.familyId
        val normalizedName = name.trim()
        if (normalizedName.isBlank()) {
            throw InvalidCategoryNameException()
        }
        val existing = categoryRepository.findByIdForUser(userId, familyId, id)
            ?: throw CategoryNotFoundException(id)
        if (existing.isGlobal) {
            throw CategoryNotModifiableException(id)
        }
        if (categoryRepository.existsByNameExcludingIdForUser(userId, familyId, normalizedName, id)) {
            throw DuplicateCategoryNameException(normalizedName)
        }
        return categoryRepository.updateForUser(userId, familyId, id, normalizedName, color?.trim()?.ifBlank { null })
    }
}

class DeleteCategoryUseCase(
    private val categoryRepository: CategoryRepository,
    private val familyMemberRepository: FamilyMemberRepository,
) {
    fun execute(userId: UUID, id: UUID) {
        val familyId = familyMemberRepository.findActiveMembershipForUser(userId)?.familyId
        val existing = categoryRepository.findByIdForUser(userId, familyId, id)
            ?: throw CategoryNotFoundException(id)
        if (existing.isGlobal) {
            throw CategoryNotModifiableException(id)
        }
        if (categoryRepository.isCategoryInUse(id)) {
            throw CategoryInUseException(id)
        }
        categoryRepository.deleteByIdForUser(userId, familyId, id)
    }
}
