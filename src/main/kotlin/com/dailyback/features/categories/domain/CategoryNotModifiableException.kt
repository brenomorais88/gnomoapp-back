package com.dailyback.features.categories.domain

import java.util.UUID

class CategoryNotModifiableException(
    val categoryId: UUID,
) : RuntimeException("Category cannot be modified: $categoryId")
