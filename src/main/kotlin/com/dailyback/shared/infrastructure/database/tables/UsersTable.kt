package com.dailyback.shared.infrastructure.database.tables

import org.jetbrains.exposed.sql.javatime.date
import org.jetbrains.exposed.sql.javatime.timestamp

object UsersTable : BaseUuidTable("users") {
    val firstName = varchar("first_name", length = 120)
    val lastName = varchar("last_name", length = 180)
    val document = varchar("document", length = 32).uniqueIndex("uk_users_document")
    val birthDate = date("birth_date")
    val passwordHash = varchar("password_hash", length = 255)
    val phone = varchar("phone", length = 20).nullable()
    val email = varchar("email", length = 255).nullable()
    val status = varchar("status", length = 20)
    val lastLoginAt = timestamp("last_login_at").nullable()

    init {
        index(isUnique = false, createdAt)
        index(isUnique = false, status)
    }
}
