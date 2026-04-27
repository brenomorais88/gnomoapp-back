package com.dailyback.features.families.application

import com.dailyback.features.families.domain.DuplicateFamilyMemberInviteException
import com.dailyback.features.families.domain.Family
import com.dailyback.features.families.domain.FamilyMember
import com.dailyback.features.families.domain.FamilyMemberNotFoundException
import com.dailyback.features.families.domain.FamilyNotFoundException
import com.dailyback.features.families.domain.InvalidFamilyMemberDisplayNameException
import com.dailyback.features.families.domain.InvalidFamilyMemberRoleChangeException
import com.dailyback.features.families.domain.InvalidFamilyMemberRoleValueException
import com.dailyback.features.families.domain.InvalidFamilyNameException
import com.dailyback.features.families.domain.InviteTargetInAnotherFamilyException
import com.dailyback.features.families.domain.LastFamilyAdminException
import com.dailyback.features.families.domain.NoFamilyForUserException
import com.dailyback.features.families.domain.NotFamilyAdminException
import com.dailyback.features.families.domain.UserAlreadyHasFamilyException
import com.dailyback.features.users.application.UserRepository
import com.dailyback.features.users.domain.UserNotFoundException
import com.dailyback.shared.domain.family.FamilyAdminPolicy
import com.dailyback.shared.domain.family.FamilyMemberPermissionFlags
import com.dailyback.shared.domain.family.FamilyMemberRole
import com.dailyback.shared.domain.family.FamilyMembershipStatus
import com.dailyback.shared.domain.identity.LoginIdentifier
import com.dailyback.shared.validation.IdentifierNormalizer
import java.time.Instant
import java.util.UUID

class FamilyService(
    private val familyRepository: FamilyRepository,
    private val familyMemberRepository: FamilyMemberRepository,
    private val userRepository: UserRepository,
    private val familyMemberPermissionRepository: FamilyMemberPermissionRepository,
) {
    fun createFamily(
        actorUserId: UUID,
        rawName: String,
    ): Family {
        val name = rawName.trim()
        if (name.isBlank() || name.length > MAX_FAMILY_NAME_LENGTH) {
            throw InvalidFamilyNameException()
        }

        if (familyMemberRepository.findActiveMembershipForUser(actorUserId) != null) {
            throw UserAlreadyHasFamilyException()
        }

        val user = userRepository.findById(actorUserId) ?: throw UserNotFoundException(actorUserId)

        val family = familyRepository.insert(
            name = name,
            createdByUserId = actorUserId,
        )

        val displayName = "${user.firstName} ${user.lastName}".trim().ifBlank { DEFAULT_DISPLAY_NAME }

        familyMemberRepository.insertMember(
            familyId = family.id,
            userId = actorUserId,
            displayName = displayName,
            document = user.document,
            email = user.email,
            phone = user.phone,
            role = FamilyMemberRole.ADMIN,
            status = FamilyMembershipStatus.ACTIVE,
            invitedByUserId = null,
            joinedAt = Instant.now(),
        )

        return family
    }

    fun getMyFamily(actorUserId: UUID): Family {
        val membership = familyMemberRepository.findActiveMembershipForUser(actorUserId)
            ?: throw NoFamilyForUserException()
        return familyRepository.findById(membership.familyId)
            ?: throw FamilyNotFoundException(membership.familyId)
    }

    fun listMyFamilyMembers(actorUserId: UUID): List<FamilyMember> {
        val membership = familyMemberRepository.findActiveMembershipForUser(actorUserId)
            ?: throw NoFamilyForUserException()
        return familyMemberRepository.findActiveMembersByFamily(membership.familyId)
    }

    fun inviteMember(
        actorUserId: UUID,
        displayName: String,
        rawDocument: String?,
        rawEmail: String?,
        rawPhone: String?,
    ): FamilyMember {
        val membership = requireActiveAdminOrInviteMembers(actorUserId)

        val display = displayName.trim()
        if (display.isBlank() || display.length > MAX_DISPLAY_NAME_LENGTH) {
            throw InvalidFamilyMemberDisplayNameException()
        }

        val docNorm = rawDocument?.trim()?.takeIf { it.isNotEmpty() }
            ?.let(IdentifierNormalizer::normalizeDocumentKey)
            ?.takeIf { it.isNotEmpty() }
        val emailLower = rawEmail?.trim()?.takeIf { it.isNotEmpty() }?.let(IdentifierNormalizer::normalizeEmail)
        val phoneDigits = rawPhone?.trim()?.takeIf { it.isNotEmpty() }?.let(IdentifierNormalizer::digitsOnly)
            ?.takeIf { it.length >= MIN_PHONE_DIGITS }

        val familyId = membership.familyId

        familyMemberRepository.findConflictingInviteInFamily(
            familyId = familyId,
            documentNormalized = docNorm,
            emailLower = emailLower,
            phoneDigits = phoneDigits,
        )?.let {
            throw DuplicateFamilyMemberInviteException()
        }

        if (docNorm != null) {
            val existingUser = userRepository.findByLoginIdentifier(LoginIdentifier.Document(docNorm))
            if (existingUser != null) {
                if (existingUser.id == actorUserId) {
                    throw DuplicateFamilyMemberInviteException()
                }
                familyMemberRepository.findNonRemovedMemberInFamilyByUser(familyId, existingUser.id)
                    ?.let { throw DuplicateFamilyMemberInviteException() }

                val otherMembership = familyMemberRepository.findActiveMembershipForUser(existingUser.id)
                if (otherMembership != null && otherMembership.familyId != familyId) {
                    throw InviteTargetInAnotherFamilyException()
                }

                return familyMemberRepository.insertMember(
                    familyId = familyId,
                    userId = existingUser.id,
                    displayName = display,
                    document = existingUser.document,
                    email = existingUser.email,
                    phone = existingUser.phone,
                    role = FamilyMemberRole.MEMBER,
                    status = FamilyMembershipStatus.ACTIVE,
                    invitedByUserId = actorUserId,
                    joinedAt = Instant.now(),
                )
            }
        }

        return familyMemberRepository.insertMember(
            familyId = familyId,
            userId = null,
            displayName = display,
            document = docNorm,
            email = emailLower,
            phone = phoneDigits,
            role = FamilyMemberRole.MEMBER,
            status = FamilyMembershipStatus.PENDING_REGISTRATION,
            invitedByUserId = actorUserId,
            joinedAt = null,
        )
    }

    fun updateMemberRole(
        actorUserId: UUID,
        targetMemberId: UUID,
        rawRole: String,
    ): FamilyMember {
        val newRole = try {
            FamilyMemberRole.fromValue(rawRole)
        } catch (_: IllegalArgumentException) {
            throw InvalidFamilyMemberRoleValueException(rawRole)
        }
        val actorMembership = requireActiveAdminOrManageMembers(actorUserId)
        val familyId = actorMembership.familyId
        val target = familyMemberRepository.findMemberByIdInFamily(targetMemberId, familyId)
            ?: throw FamilyMemberNotFoundException(targetMemberId)
        if (target.status != FamilyMembershipStatus.ACTIVE) {
            throw InvalidFamilyMemberRoleChangeException()
        }
        if (target.role == newRole) {
            return target
        }
        return when {
            target.role == FamilyMemberRole.MEMBER && newRole == FamilyMemberRole.ADMIN ->
                familyMemberRepository.updateMemberRole(targetMemberId, familyId, newRole)
                    ?: throw FamilyMemberNotFoundException(targetMemberId)
            target.role == FamilyMemberRole.ADMIN && newRole == FamilyMemberRole.MEMBER -> {
                val adminCount = familyMemberRepository.countActiveAdminsInFamily(familyId)
                if (!FamilyAdminPolicy.canDemoteAdmin(adminCount)) {
                    throw LastFamilyAdminException()
                }
                familyMemberRepository.updateMemberRole(targetMemberId, familyId, newRole)
                    ?: throw FamilyMemberNotFoundException(targetMemberId)
            }
            else -> throw InvalidFamilyMemberRoleChangeException()
        }
    }

    fun removeMember(actorUserId: UUID, targetMemberId: UUID) {
        val actorMembership = requireActiveAdminOrManageMembers(actorUserId)
        val familyId = actorMembership.familyId
        val target = familyMemberRepository.findMemberByIdInFamily(targetMemberId, familyId)
            ?: throw FamilyMemberNotFoundException(targetMemberId)
        if (target.status == FamilyMembershipStatus.ACTIVE) {
            val adminCount = familyMemberRepository.countActiveAdminsInFamily(familyId)
            if (!FamilyAdminPolicy.canRemoveMember(target.role, adminCount)) {
                throw LastFamilyAdminException()
            }
        }
        familyMemberRepository.markMemberRemoved(targetMemberId, familyId)
            ?: throw FamilyMemberNotFoundException(targetMemberId)
    }

    private fun requireActiveAdminOrInviteMembers(actorUserId: UUID): FamilyMember {
        val membership = familyMemberRepository.findActiveMembershipForUser(actorUserId)
            ?: throw NoFamilyForUserException()
        if (membership.status != FamilyMembershipStatus.ACTIVE) {
            throw NotFamilyAdminException()
        }
        if (membership.role == FamilyMemberRole.ADMIN) {
            return membership
        }
        val flags = familyMemberPermissionRepository.findByMemberId(membership.id)
            ?: FamilyMemberPermissionFlags.memberDefaults()
        if (!flags.canInviteMembers) {
            throw NotFamilyAdminException()
        }
        return membership
    }

    private fun requireActiveAdminOrManageMembers(actorUserId: UUID): FamilyMember {
        val membership = familyMemberRepository.findActiveMembershipForUser(actorUserId)
            ?: throw NoFamilyForUserException()
        if (membership.status != FamilyMembershipStatus.ACTIVE) {
            throw NotFamilyAdminException()
        }
        if (membership.role == FamilyMemberRole.ADMIN) {
            return membership
        }
        val flags = familyMemberPermissionRepository.findByMemberId(membership.id)
            ?: FamilyMemberPermissionFlags.memberDefaults()
        if (!flags.canManageMembers) {
            throw NotFamilyAdminException()
        }
        return membership
    }

    companion object {
        private const val MAX_FAMILY_NAME_LENGTH = 200
        private const val MAX_DISPLAY_NAME_LENGTH = 200
        private const val DEFAULT_DISPLAY_NAME = "Member"
        private const val MIN_PHONE_DIGITS = 10
    }
}
