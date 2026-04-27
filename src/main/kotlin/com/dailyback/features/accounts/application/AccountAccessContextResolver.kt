package com.dailyback.features.accounts.application

import com.dailyback.features.accounts.domain.Account
import com.dailyback.features.accounts.domain.AccountOwnershipType
import com.dailyback.features.families.application.FamilyMemberPermissionRepository
import com.dailyback.features.families.application.FamilyMemberRepository
import com.dailyback.shared.domain.family.FamilyMemberPermissionFlags
import com.dailyback.shared.domain.family.FamilyMemberRole
import com.dailyback.shared.domain.family.FamilyMembershipStatus
import java.util.UUID

data class AccountViewerQuery(
    val userId: UUID,
    val viewerFamilyId: UUID?,
    val canViewFamilyAccounts: Boolean,
    val canViewOtherPersonalAccounts: Boolean,
    val canEditFamilyAccounts: Boolean,
    val canDeleteFamilyAccounts: Boolean,
    val canEditOtherPersonalAccounts: Boolean,
    val canMarkFamilyAccountsPaid: Boolean,
)

class AccountAccessContextResolver(
    private val familyMemberRepository: FamilyMemberRepository,
    private val permissionRepository: FamilyMemberPermissionRepository,
) {
    fun buildViewerQuery(userId: UUID): AccountViewerQuery {
        val membership = familyMemberRepository.findActiveMembershipForUser(userId)
            ?: return AccountViewerQuery(
                userId = userId,
                viewerFamilyId = null,
                canViewFamilyAccounts = false,
                canViewOtherPersonalAccounts = false,
                canEditFamilyAccounts = false,
                canDeleteFamilyAccounts = false,
                canEditOtherPersonalAccounts = false,
                canMarkFamilyAccountsPaid = false,
            )

        if (membership.role == FamilyMemberRole.ADMIN && membership.status == FamilyMembershipStatus.ACTIVE) {
            return AccountViewerQuery(
                userId = userId,
                viewerFamilyId = membership.familyId,
                canViewFamilyAccounts = true,
                canViewOtherPersonalAccounts = true,
                canEditFamilyAccounts = true,
                canDeleteFamilyAccounts = true,
                canEditOtherPersonalAccounts = true,
                canMarkFamilyAccountsPaid = true,
            )
        }

        if (membership.status != FamilyMembershipStatus.ACTIVE) {
            return AccountViewerQuery(
                userId = userId,
                viewerFamilyId = null,
                canViewFamilyAccounts = false,
                canViewOtherPersonalAccounts = false,
                canEditFamilyAccounts = false,
                canDeleteFamilyAccounts = false,
                canEditOtherPersonalAccounts = false,
                canMarkFamilyAccountsPaid = false,
            )
        }

        val flags = permissionRepository.findByMemberId(membership.id)
            ?: FamilyMemberPermissionFlags.memberDefaults()
        return AccountViewerQuery(
            userId = userId,
            viewerFamilyId = membership.familyId,
            canViewFamilyAccounts = flags.canViewFamilyAccounts,
            canViewOtherPersonalAccounts = flags.canViewOtherPersonalAccounts,
            canEditFamilyAccounts = flags.canEditFamilyAccounts,
            canDeleteFamilyAccounts = flags.canDeleteFamilyAccounts,
            canEditOtherPersonalAccounts = flags.canEditOtherPersonalAccounts,
            canMarkFamilyAccountsPaid = flags.canMarkFamilyAccountsPaid,
        )
    }

    fun canView(account: Account, q: AccountViewerQuery): Boolean {
        if (account.ownershipType == AccountOwnershipType.PERSONAL) {
            if (account.ownerUserId == q.userId) return true
            if (!q.canViewOtherPersonalAccounts || q.viewerFamilyId == null) return false
            return isUserInFamily(account.ownerUserId, q.viewerFamilyId)
        }
        if (account.ownershipType == AccountOwnershipType.FAMILY) {
            return q.canViewFamilyAccounts && account.familyId == q.viewerFamilyId
        }
        return false
    }

    fun canEdit(account: Account, q: AccountViewerQuery): Boolean {
        if (account.ownershipType == AccountOwnershipType.PERSONAL) {
            if (account.ownerUserId == q.userId) return true
            if (!q.canEditOtherPersonalAccounts || q.viewerFamilyId == null) return false
            return isUserInFamily(account.ownerUserId, q.viewerFamilyId)
        }
        return q.canEditFamilyAccounts && account.familyId == q.viewerFamilyId
    }

    fun canDelete(account: Account, q: AccountViewerQuery): Boolean {
        if (account.ownershipType == AccountOwnershipType.PERSONAL) {
            if (account.ownerUserId == q.userId) return true
            if (!q.canEditOtherPersonalAccounts || q.viewerFamilyId == null) return false
            return isUserInFamily(account.ownerUserId, q.viewerFamilyId)
        }
        return q.canDeleteFamilyAccounts && account.familyId == q.viewerFamilyId
    }

    fun canMarkPaid(account: Account, q: AccountViewerQuery): Boolean {
        if (account.ownershipType == AccountOwnershipType.PERSONAL) {
            if (account.ownerUserId == q.userId) return true
            if (!q.canEditOtherPersonalAccounts || q.viewerFamilyId == null) return false
            return isUserInFamily(account.ownerUserId, q.viewerFamilyId)
        }
        return q.canMarkFamilyAccountsPaid && account.familyId == q.viewerFamilyId
    }

    private fun isUserInFamily(ownerUserId: UUID?, familyId: UUID): Boolean {
        if (ownerUserId == null) return false
        return familyMemberRepository.findNonRemovedMemberInFamilyByUser(familyId, ownerUserId) != null
    }

    fun hasActiveFamilyMembership(userId: UUID): Boolean {
        val m = familyMemberRepository.findActiveMembershipForUser(userId) ?: return false
        if (m.role == FamilyMemberRole.ADMIN && m.status == FamilyMembershipStatus.ACTIVE) return true
        return m.status == FamilyMembershipStatus.ACTIVE
    }
}
