package com.dailyback.shared.infrastructure.database.tables

import com.dailyback.features.accounts.domain.AccountOwnershipType
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.javatime.date

object AccountsTable : BaseUuidTable("accounts") {
    val title = varchar("title", length = 180)
    val baseAmount = decimal("base_amount", precision = 14, scale = 2)
    val startDate = date("start_date")
    val endDate = date("end_date").nullable()
    val recurrenceType = varchar("recurrence_type", length = 20)
    val categoryId = reference(
        name = "category_id",
        foreign = CategoriesTable,
        onDelete = ReferenceOption.RESTRICT,
        onUpdate = ReferenceOption.CASCADE,
    )
    val notes = text("notes").nullable()
    val active = bool("active").default(true)
    val ownershipType = varchar("ownership_type", length = 20)
        .default(AccountOwnershipType.FAMILY.name)
    val ownerUserId = optReference(
        name = "owner_user_id",
        foreign = UsersTable,
        onDelete = ReferenceOption.CASCADE,
        onUpdate = ReferenceOption.CASCADE,
    )
    val familyId = optReference(
        name = "family_id",
        foreign = FamiliesTable,
        onDelete = ReferenceOption.CASCADE,
        onUpdate = ReferenceOption.CASCADE,
    )
    val createdByUserId = optReference(
        name = "created_by_user_id",
        foreign = UsersTable,
        onDelete = ReferenceOption.SET_NULL,
        onUpdate = ReferenceOption.CASCADE,
    )
    val responsibleMemberId = optReference(
        name = "responsible_member_id",
        foreign = FamilyMembersTable,
        onDelete = ReferenceOption.SET_NULL,
        onUpdate = ReferenceOption.CASCADE,
    )

    init {
        index(isUnique = false, categoryId)
        index(isUnique = false, active)
        index(isUnique = false, startDate)
        index(isUnique = false, ownershipType)
        index(isUnique = false, familyId)
        index(isUnique = false, ownerUserId)
    }
}
