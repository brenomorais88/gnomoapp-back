package com.dailyback.features.users.domain

import java.time.Instant
import java.time.LocalDate
import java.util.UUID

data class User(
    val id: UUID,
    val firstName: String,
    val lastName: String,
    val document: String,
    val birthDate: LocalDate,
    val passwordHash: String,
    val phone: String?,
    val email: String?,
    val status: UserStatus,
    val lastLoginAt: Instant?,
    val createdAt: Instant,
    val updatedAt: Instant,
)
