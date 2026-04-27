package com.dailyback.shared.domain.family

data class FamilyMemberPermissionFlags(
    val canViewFamilyAccounts: Boolean,
    val canCreateFamilyAccounts: Boolean,
    val canEditFamilyAccounts: Boolean,
    val canDeleteFamilyAccounts: Boolean,
    val canMarkFamilyAccountsPaid: Boolean,
    val canManageCategories: Boolean,
    val canInviteMembers: Boolean,
    val canManageMembers: Boolean,
    val canViewOtherPersonalAccounts: Boolean,
    val canEditOtherPersonalAccounts: Boolean,
) {
    fun allows(permission: FamilyPermissionKey): Boolean =
        when (permission) {
            FamilyPermissionKey.CAN_VIEW_FAMILY_ACCOUNTS -> canViewFamilyAccounts
            FamilyPermissionKey.CAN_CREATE_FAMILY_ACCOUNTS -> canCreateFamilyAccounts
            FamilyPermissionKey.CAN_EDIT_FAMILY_ACCOUNTS -> canEditFamilyAccounts
            FamilyPermissionKey.CAN_DELETE_FAMILY_ACCOUNTS -> canDeleteFamilyAccounts
            FamilyPermissionKey.CAN_MARK_FAMILY_ACCOUNTS_PAID -> canMarkFamilyAccountsPaid
            FamilyPermissionKey.CAN_MANAGE_CATEGORIES -> canManageCategories
            FamilyPermissionKey.CAN_INVITE_MEMBERS -> canInviteMembers
            FamilyPermissionKey.CAN_MANAGE_MEMBERS -> canManageMembers
            FamilyPermissionKey.CAN_VIEW_OTHER_PERSONAL_ACCOUNTS -> canViewOtherPersonalAccounts
            FamilyPermissionKey.CAN_EDIT_OTHER_PERSONAL_ACCOUNTS -> canEditOtherPersonalAccounts
        }

    companion object {
        fun memberDefaults(): FamilyMemberPermissionFlags = FamilyMemberPermissionFlags(
            canViewFamilyAccounts = true,
            canCreateFamilyAccounts = false,
            canEditFamilyAccounts = false,
            canDeleteFamilyAccounts = false,
            canMarkFamilyAccountsPaid = false,
            canManageCategories = false,
            canInviteMembers = false,
            canManageMembers = false,
            canViewOtherPersonalAccounts = false,
            canEditOtherPersonalAccounts = false,
        )

        fun allGranted(): FamilyMemberPermissionFlags = FamilyMemberPermissionFlags(
            canViewFamilyAccounts = true,
            canCreateFamilyAccounts = true,
            canEditFamilyAccounts = true,
            canDeleteFamilyAccounts = true,
            canMarkFamilyAccountsPaid = true,
            canManageCategories = true,
            canInviteMembers = true,
            canManageMembers = true,
            canViewOtherPersonalAccounts = true,
            canEditOtherPersonalAccounts = true,
        )
    }
}
