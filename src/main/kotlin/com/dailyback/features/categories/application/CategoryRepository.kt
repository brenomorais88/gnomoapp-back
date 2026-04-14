package com.dailyback.features.categories.application

import com.dailyback.features.categories.domain.Category
import java.util.UUID

interface CategoryRepository {
    fun findAll(): List<Category>
    fun findById(id: UUID): Category?
    fun existsByName(name: String): Boolean
    fun existsByNameExcludingId(name: String, excludedId: UUID): Boolean
    fun create(name: String, color: String?): Category
    fun update(id: UUID, name: String, color: String?): Category
    fun deleteById(id: UUID)
    fun isCategoryInUse(id: UUID): Boolean
}
