package com.dailyback.features.families.application

import com.dailyback.features.families.domain.FamilyMember
import com.dailyback.shared.domain.family.FamilyMemberRole
import com.dailyback.shared.domain.family.FamilyMembershipStatus
import java.time.Instant
import java.util.UUID

interface FamilyMemberRepository {
    fun findActiveMembershipForUser(userId: UUID): FamilyMember?

    fun findActiveMembersByFamily(familyId: UUID): List<FamilyMember>

    /**
     * Returns a member in [familyId] with the same user that is not REMOVED, if any.
     */
    fun findNonRemovedMemberInFamilyByUser(familyId: UUID, userId: UUID): FamilyMember?

    /**
     * Finds an ACTIVE or PENDING_REGISTRATION row in the family that conflicts with any provided identifier.
     */
    fun findConflictingInviteInFamily(
        familyId: UUID,
        documentNormalized: String?,
        emailLower: String?,
        phoneDigits: String?,
    ): FamilyMember?

    fun insertMember(
        familyId: UUID,
        userId: UUID?,
        displayName: String,
        document: String?,
        email: String?,
        phone: String?,
        role: FamilyMemberRole,
        status: FamilyMembershipStatus,
        invitedByUserId: UUID?,
        joinedAt: Instant?,
    ): FamilyMember

    /**
     * Returns a non-REMOVED member row in [familyId] with the given [memberId], if any.
     */
    fun findMemberByIdInFamily(memberId: UUID, familyId: UUID): FamilyMember?

    fun countActiveAdminsInFamily(familyId: UUID): Int

    fun updateMemberRole(memberId: UUID, familyId: UUID, newRole: FamilyMemberRole): FamilyMember?

    /**
     * Soft removal: sets status to [FamilyMembershipStatus.REMOVED]. Returns the updated row or null if none matched.
     */
    fun markMemberRemoved(memberId: UUID, familyId: UUID): FamilyMember?
}
