package com.dailyback.features.families.domain

import java.util.UUID

class FamilyMemberNotFoundException(
    val memberId: UUID,
) : RuntimeException("Family member not found")

class LastFamilyAdminException : RuntimeException("Cannot remove or demote the last active family admin")

class InvalidFamilyMemberRoleValueException(
    val rawRole: String,
) : RuntimeException("Unsupported family member role: $rawRole")

class InvalidFamilyMemberRoleChangeException : RuntimeException("Role cannot be changed for this member in the current state")
