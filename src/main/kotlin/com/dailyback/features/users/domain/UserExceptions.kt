package com.dailyback.features.users.domain

import java.util.UUID

class UserNotFoundException(
    val userId: UUID,
) : RuntimeException("User not found: $userId")

class DuplicateUserDocumentException(
    val document: String,
) : RuntimeException("Document already registered")

class DuplicateUserEmailException : RuntimeException("Email already registered")

class DuplicateUserPhoneException : RuntimeException("Phone already registered")

class InvalidCredentialsException : RuntimeException("Invalid credentials")

class UserDisabledException : RuntimeException("User is disabled")

class InvalidUserRegistrationException(
    val details: Map<String, String>,
) : RuntimeException("Invalid registration")
