package com.dailyback.features.users.application

import com.dailyback.features.users.domain.User
import com.dailyback.shared.domain.identity.LoginIdentifier
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

interface UserRepository {
    fun findById(id: UUID): User?

    fun findByLoginIdentifier(identifier: LoginIdentifier): User?

    fun existsByDocumentNormalized(document: String): Boolean

    fun existsByEmailNormalized(email: String): Boolean

    fun existsByPhoneDigits(phone: String): Boolean

    fun create(
        firstName: String,
        lastName: String,
        documentNormalized: String,
        birthDate: LocalDate,
        passwordHash: String,
        phoneDigits: String?,
        emailNormalized: String?,
    ): User

    fun updateLastLoginAt(userId: UUID, at: Instant)
}
