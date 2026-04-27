package com.dailyback.shared.domain.family

/**
 * Pure rules for ADMIN invariants (last ADMIN, promotions, removals).
 * Call sites supply counts derived from persisted membership rows.
 */
object FamilyAdminPolicy {
    fun canRemoveMember(
        targetRole: FamilyMemberRole,
        activeAdminCount: Int,
    ): Boolean =
        when (targetRole) {
            FamilyMemberRole.MEMBER -> true
            FamilyMemberRole.ADMIN -> activeAdminCount > 1
        }

    fun canDemoteAdmin(activeAdminCount: Int): Boolean = activeAdminCount > 1

    fun hasAtLeastOneAdmin(activeAdminCount: Int): Boolean = activeAdminCount >= 1
}
