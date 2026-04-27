package com.dailyback.shared.infrastructure.database.tables

import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.javatime.timestamp

object FamilyMembersTable : BaseUuidTable("family_members") {
    val familyId = reference(
        name = "family_id",
        foreign = FamiliesTable,
        onDelete = ReferenceOption.CASCADE,
        onUpdate = ReferenceOption.CASCADE,
    )
    val userId = optReference(
        name = "user_id",
        foreign = UsersTable,
        onDelete = ReferenceOption.SET_NULL,
        onUpdate = ReferenceOption.CASCADE,
    )
    val displayName = varchar("display_name", length = 200)
    val document = varchar("document", length = 32).nullable()
    val email = varchar("email", length = 255).nullable()
    val phone = varchar("phone", length = 20).nullable()
    val role = varchar("role", length = 20)
    val status = varchar("status", length = 20)
    val invitedByUserId = optReference(
        name = "invited_by_user_id",
        foreign = UsersTable,
        onDelete = ReferenceOption.SET_NULL,
        onUpdate = ReferenceOption.CASCADE,
    )
    val joinedAt = timestamp("joined_at").nullable()

    init {
        index(isUnique = false, familyId)
        index(isUnique = false, userId)
    }
}
