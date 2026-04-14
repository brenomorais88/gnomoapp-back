package com.dailyback.features.categories.api

import com.dailyback.features.categories.domain.Category
import kotlinx.serialization.Serializable

@Serializable
data class CreateCategoryRequest(
    val name: String,
    val color: String? = null,
)

@Serializable
data class UpdateCategoryRequest(
    val name: String,
    val color: String? = null,
)

@Serializable
data class CategoryResponse(
    val id: String,
    val name: String,
    val color: String? = null,
    val createdAt: String,
    val updatedAt: String,
)

fun Category.toResponse(): CategoryResponse = CategoryResponse(
    id = id.toString(),
    name = name,
    color = color,
    createdAt = createdAt.toString(),
    updatedAt = updatedAt.toString(),
)
