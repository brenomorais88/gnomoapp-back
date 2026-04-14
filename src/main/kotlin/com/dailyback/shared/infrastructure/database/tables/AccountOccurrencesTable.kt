package com.dailyback.shared.infrastructure.database.tables

import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.javatime.date
import org.jetbrains.exposed.sql.javatime.timestamp

object AccountOccurrencesTable : BaseUuidTable("account_occurrences") {
    val accountId = reference(
        name = "account_id",
        foreign = AccountsTable,
        onDelete = ReferenceOption.RESTRICT,
        onUpdate = ReferenceOption.CASCADE,
    )
    val titleSnapshot = varchar("title_snapshot", length = 180)
    val amountSnapshot = decimal("amount_snapshot", precision = 14, scale = 2)
    val dueDate = date("due_date")
    val status = varchar("status", length = 20)
    val paidAt = timestamp("paid_at").nullable()
    val notesSnapshot = text("notes_snapshot").nullable()
    val categoryIdSnapshot = uuid("category_id_snapshot")

    init {
        uniqueIndex("uk_account_occurrences_account_due_date", accountId, dueDate)
        index(isUnique = false, dueDate)
        index(isUnique = false, status)
        index(isUnique = false, accountId)
        index(isUnique = false, categoryIdSnapshot)
    }
}
