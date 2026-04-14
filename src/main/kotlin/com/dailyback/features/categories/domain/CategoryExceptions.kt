package com.dailyback.features.categories.domain

import java.util.UUID

class CategoryNotFoundException(categoryId: UUID) : RuntimeException("Category not found: $categoryId")

class DuplicateCategoryNameException(name: String) : RuntimeException("Category name already exists: $name")

class CategoryInUseException(categoryId: UUID) : RuntimeException("Category is in use by accounts: $categoryId")

class InvalidCategoryNameException : RuntimeException("Category name must not be blank")
