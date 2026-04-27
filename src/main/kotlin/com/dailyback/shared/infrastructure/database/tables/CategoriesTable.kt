package com.dailyback.shared.infrastructure.database.tables

import org.jetbrains.exposed.sql.ReferenceOption

object CategoriesTable : BaseUuidTable("categories") {
    val name = varchar("name", length = 120)
    val color = varchar("color", length = 20).nullable()
    val familyId = optReference(
        name = "family_id",
        foreign = FamiliesTable,
        onDelete = ReferenceOption.CASCADE,
        onUpdate = ReferenceOption.CASCADE,
    )
    val ownerUserId = optReference(
        name = "owner_user_id",
        foreign = UsersTable,
        onDelete = ReferenceOption.CASCADE,
        onUpdate = ReferenceOption.CASCADE,
    )

    init {
        index(isUnique = false, createdAt)
        index(isUnique = false, updatedAt)
    }
}
