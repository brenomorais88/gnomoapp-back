package com.dailyback.shared.infrastructure.database.tables

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

    init {
        index(isUnique = false, categoryId)
        index(isUnique = false, active)
        index(isUnique = false, startDate)
    }
}
