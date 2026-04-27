package com.dailyback.features.families.domain

import java.util.UUID

class InvalidFamilyNameException : RuntimeException("Invalid family name")

class UserAlreadyHasFamilyException : RuntimeException("User already belongs to a family")

class NoFamilyForUserException : RuntimeException("User has no family")

class FamilyNotFoundException(
    val familyId: UUID,
) : RuntimeException("Family not found: $familyId")
