package com.dailyback.features.users.infrastructure

import com.dailyback.features.users.application.UserRepository
import com.dailyback.features.users.domain.User
import com.dailyback.features.users.domain.UserStatus
import com.dailyback.shared.domain.identity.LoginIdentifier
import com.dailyback.shared.infrastructure.database.DatabaseFactory
import com.dailyback.shared.infrastructure.database.tables.UsersTable
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.lowerCase
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

class ExposedUserRepository(
    private val databaseFactory: DatabaseFactory,
) : UserRepository {
    override fun findById(id: UUID): User? {
        databaseFactory.connect()
        return transaction {
            UsersTable.selectAll()
                .where { UsersTable.id eq id }
                .limit(1)
                .map(::toUser)
                .firstOrNull()
        }
    }

    override fun findByLoginIdentifier(identifier: LoginIdentifier): User? {
        databaseFactory.connect()
        return transaction {
            when (identifier) {
                is LoginIdentifier.Document ->
                    UsersTable.selectAll()
                        .where { UsersTable.document eq identifier.normalized }
                is LoginIdentifier.Email ->
                    UsersTable.selectAll()
                        .where { UsersTable.email eq identifier.normalized }
                is LoginIdentifier.Phone ->
                    UsersTable.selectAll()
                        .where { UsersTable.phone eq identifier.normalized }
            }
                .limit(1)
                .map(::toUser)
                .firstOrNull()
        }
    }

    override fun existsByDocumentNormalized(document: String): Boolean {
        databaseFactory.connect()
        return transaction {
            UsersTable.selectAll()
                .where { UsersTable.document eq document }
                .limit(1)
                .count() > 0L
        }
    }

    override fun existsByEmailNormalized(email: String): Boolean {
        databaseFactory.connect()
        return transaction {
            UsersTable.selectAll()
                .where { UsersTable.email.lowerCase() eq email.lowercase() }
                .limit(1)
                .count() > 0L
        }
    }

    override fun existsByPhoneDigits(phone: String): Boolean {
        databaseFactory.connect()
        return transaction {
            UsersTable.selectAll()
                .where { UsersTable.phone eq phone }
                .limit(1)
                .count() > 0L
        }
    }

    override fun create(
        firstName: String,
        lastName: String,
        documentNormalized: String,
        birthDate: LocalDate,
        passwordHash: String,
        phoneDigits: String?,
        emailNormalized: String?,
    ): User {
        databaseFactory.connect()
        return transaction {
            val insertedId = UsersTable.insert {
                it[UsersTable.firstName] = firstName
                it[UsersTable.lastName] = lastName
                it[UsersTable.document] = documentNormalized
                it[UsersTable.birthDate] = birthDate
                it[UsersTable.passwordHash] = passwordHash
                it[UsersTable.phone] = phoneDigits
                it[UsersTable.email] = emailNormalized
                it[UsersTable.status] = UserStatus.ACTIVE.name
                it[UsersTable.lastLoginAt] = null
            }[UsersTable.id].value

            UsersTable.selectAll()
                .where { UsersTable.id eq insertedId }
                .limit(1)
                .map(::toUser)
                .first()
        }
    }

    override fun updateLastLoginAt(userId: UUID, at: Instant) {
        databaseFactory.connect()
        transaction {
            UsersTable.update({ UsersTable.id eq userId }) {
                it[lastLoginAt] = at
                it[updatedAt] = at
            }
        }
    }

    private fun toUser(row: ResultRow): User = User(
        id = row[UsersTable.id].value,
        firstName = row[UsersTable.firstName],
        lastName = row[UsersTable.lastName],
        document = row[UsersTable.document],
        birthDate = row[UsersTable.birthDate],
        passwordHash = row[UsersTable.passwordHash],
        phone = row[UsersTable.phone],
        email = row[UsersTable.email],
        status = UserStatus.fromValue(row[UsersTable.status]),
        lastLoginAt = row[UsersTable.lastLoginAt],
        createdAt = row[UsersTable.createdAt],
        updatedAt = row[UsersTable.updatedAt],
    )
}
