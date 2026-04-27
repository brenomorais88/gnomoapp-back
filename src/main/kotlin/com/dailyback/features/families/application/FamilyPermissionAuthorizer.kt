package com.dailyback.features.families.application

import com.dailyback.features.families.domain.FamilyPermissionDeniedException
import com.dailyback.shared.domain.family.FamilyMemberPermissionFlags
import com.dailyback.shared.domain.family.FamilyMemberRole
import com.dailyback.shared.domain.family.FamilyMembershipStatus
import com.dailyback.shared.domain.family.FamilyPermissionKey
import java.util.UUID

fun interface FamilyPermissionAuthorizer {
    /**
     * Ensures [userId] may perform an action gated by [permission].
     * @throws FamilyPermissionDeniedException when not allowed
     */
    fun require(userId: UUID, permission: FamilyPermissionKey)
}

class DefaultFamilyPermissionAuthorizer(
    private val familyMemberRepository: FamilyMemberRepository,
    private val permissionRepository: FamilyMemberPermissionRepository,
) : FamilyPermissionAuthorizer {
    override fun require(userId: UUID, permission: FamilyPermissionKey) {
        val membership = familyMemberRepository.findActiveMembershipForUser(userId)
            ?: throw FamilyPermissionDeniedException(permission)
        if (membership.role == FamilyMemberRole.ADMIN && membership.status == FamilyMembershipStatus.ACTIVE) {
            return
        }
        if (membership.status != FamilyMembershipStatus.ACTIVE) {
            throw FamilyPermissionDeniedException(permission)
        }
        val flags = permissionRepository.findByMemberId(membership.id)
            ?: FamilyMemberPermissionFlags.memberDefaults()
        if (!flags.allows(permission)) {
            throw FamilyPermissionDeniedException(permission)
        }
    }
}
