package com.dailyback.features.categories.application

import com.dailyback.features.categories.domain.Category
import com.dailyback.features.categories.domain.CategoryInUseException
import com.dailyback.features.categories.domain.CategoryNotFoundException
import com.dailyback.features.categories.domain.DuplicateCategoryNameException
import com.dailyback.features.categories.domain.InvalidCategoryNameException
import java.util.UUID

class ListCategoriesUseCase(
    private val categoryRepository: CategoryRepository,
) {
    fun execute(): List<Category> = categoryRepository.findAll()
}

class GetCategoryByIdUseCase(
    private val categoryRepository: CategoryRepository,
) {
    fun execute(id: UUID): Category =
        categoryRepository.findById(id) ?: throw CategoryNotFoundException(id)
}

class CreateCategoryUseCase(
    private val categoryRepository: CategoryRepository,
) {
    fun execute(name: String, color: String?): Category {
        val normalizedName = name.trim()
        if (normalizedName.isBlank()) {
            throw InvalidCategoryNameException()
        }
        if (categoryRepository.existsByName(normalizedName)) {
            throw DuplicateCategoryNameException(normalizedName)
        }
        return categoryRepository.create(
            name = normalizedName,
            color = color?.trim()?.ifBlank { null },
        )
    }
}

class UpdateCategoryUseCase(
    private val categoryRepository: CategoryRepository,
) {
    fun execute(id: UUID, name: String, color: String?): Category {
        val normalizedName = name.trim()
        if (normalizedName.isBlank()) {
            throw InvalidCategoryNameException()
        }
        categoryRepository.findById(id) ?: throw CategoryNotFoundException(id)
        if (categoryRepository.existsByNameExcludingId(normalizedName, id)) {
            throw DuplicateCategoryNameException(normalizedName)
        }
        return categoryRepository.update(
            id = id,
            name = normalizedName,
            color = color?.trim()?.ifBlank { null },
        )
    }
}

class DeleteCategoryUseCase(
    private val categoryRepository: CategoryRepository,
) {
    fun execute(id: UUID) {
        categoryRepository.findById(id) ?: throw CategoryNotFoundException(id)
        if (categoryRepository.isCategoryInUse(id)) {
            throw CategoryInUseException(id)
        }
        categoryRepository.deleteById(id)
    }
}
