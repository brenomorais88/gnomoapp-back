package com.dailyback.features.families.domain

import com.dailyback.shared.domain.family.FamilyMemberRole
import com.dailyback.shared.domain.family.FamilyMembershipStatus
import java.time.Instant
import java.util.UUID

data class FamilyMember(
    val id: UUID,
    val familyId: UUID,
    val userId: UUID?,
    val displayName: String,
    val document: String?,
    val email: String?,
    val phone: String?,
    val role: FamilyMemberRole,
    val status: FamilyMembershipStatus,
    val invitedByUserId: UUID?,
    val joinedAt: Instant?,
    val createdAt: Instant,
    val updatedAt: Instant,
)
