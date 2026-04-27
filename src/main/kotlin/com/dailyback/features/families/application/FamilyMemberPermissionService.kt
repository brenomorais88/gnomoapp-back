package com.dailyback.features.families.application

import com.dailyback.features.families.domain.FamilyMember
import com.dailyback.features.families.domain.FamilyMemberNotFoundException
import com.dailyback.features.families.domain.NoFamilyForUserException
import com.dailyback.features.families.domain.NotFamilyAdminException
import com.dailyback.shared.domain.family.FamilyMemberPermissionFlags
import com.dailyback.shared.domain.family.FamilyMemberRole
import com.dailyback.shared.domain.family.FamilyMembershipStatus
import java.util.UUID

class FamilyMemberPermissionService(
    private val familyMemberRepository: FamilyMemberRepository,
    private val permissionRepository: FamilyMemberPermissionRepository,
) {
    fun getMyPermissions(actorUserId: UUID): FamilyMemberPermissionFlags {
        val membership = familyMemberRepository.findActiveMembershipForUser(actorUserId)
            ?: throw NoFamilyForUserException()
        if (membership.status == FamilyMembershipStatus.REMOVED) {
            throw NoFamilyForUserException()
        }
        if (membership.role == FamilyMemberRole.ADMIN && membership.status == FamilyMembershipStatus.ACTIVE) {
            return FamilyMemberPermissionFlags.allGranted()
        }
        return permissionRepository.findByMemberId(membership.id) ?: FamilyMemberPermissionFlags.memberDefaults()
    }

    fun getMemberPermissions(actorUserId: UUID, targetMemberId: UUID): FamilyMemberPermissionFlags {
        val actor = requireActiveFamilyAdmin(actorUserId)
        val target = loadTargetInFamily(targetMemberId, actor.familyId)
        if (target.role == FamilyMemberRole.ADMIN && target.status == FamilyMembershipStatus.ACTIVE) {
            return FamilyMemberPermissionFlags.allGranted()
        }
        return permissionRepository.findByMemberId(target.id) ?: FamilyMemberPermissionFlags.memberDefaults()
    }

    fun putMemberPermissions(
        actorUserId: UUID,
        targetMemberId: UUID,
        flags: FamilyMemberPermissionFlags,
    ): FamilyMemberPermissionFlags {
        val actor = requireActiveFamilyAdmin(actorUserId)
        val target = loadTargetInFamily(targetMemberId, actor.familyId)
        return permissionRepository.upsert(target.id, flags)
    }

    private fun loadTargetInFamily(targetMemberId: UUID, familyId: UUID): FamilyMember {
        val target = familyMemberRepository.findMemberByIdInFamily(targetMemberId, familyId)
            ?: throw FamilyMemberNotFoundException(targetMemberId)
        if (target.status == FamilyMembershipStatus.REMOVED) {
            throw FamilyMemberNotFoundException(targetMemberId)
        }
        return target
    }

    private fun requireActiveFamilyAdmin(actorUserId: UUID): FamilyMember {
        val membership = familyMemberRepository.findActiveMembershipForUser(actorUserId)
            ?: throw NoFamilyForUserException()
        if (membership.status != FamilyMembershipStatus.ACTIVE || membership.role != FamilyMemberRole.ADMIN) {
            throw NotFamilyAdminException()
        }
        return membership
    }
}
