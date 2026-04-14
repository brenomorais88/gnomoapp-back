package com.dailyback.shared.infrastructure.database.tables

object CategoriesTable : BaseUuidTable("categories") {
    val name = varchar("name", length = 120).uniqueIndex("uk_categories_name")
    val color = varchar("color", length = 20).nullable()

    init {
        index(isUnique = false, createdAt)
        index(isUnique = false, updatedAt)
    }
}
