package com.dailyback.shared.infrastructure.database.tables

import org.jetbrains.exposed.sql.ReferenceOption

object FamiliesTable : BaseUuidTable("families") {
    val name = varchar("name", length = 200)
    val createdByUserId = optReference(
        name = "created_by_user_id",
        foreign = UsersTable,
        onDelete = ReferenceOption.SET_NULL,
        onUpdate = ReferenceOption.CASCADE,
    )
    val status = varchar("status", length = 20).default("ACTIVE")

    init {
        index(isUnique = false, createdAt)
        index(isUnique = false, status)
    }
}
