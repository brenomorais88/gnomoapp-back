package com.dailyback.shared.domain.family

enum class FamilyPermissionKey {
    CAN_VIEW_FAMILY_ACCOUNTS,
    CAN_CREATE_FAMILY_ACCOUNTS,
    CAN_EDIT_FAMILY_ACCOUNTS,
    CAN_DELETE_FAMILY_ACCOUNTS,
    CAN_MARK_FAMILY_ACCOUNTS_PAID,
    CAN_MANAGE_CATEGORIES,
    CAN_INVITE_MEMBERS,
    CAN_MANAGE_MEMBERS,
    CAN_VIEW_OTHER_PERSONAL_ACCOUNTS,
    CAN_EDIT_OTHER_PERSONAL_ACCOUNTS,
    ;

    companion object {
        fun fromValue(value: String): FamilyPermissionKey =
            entries.firstOrNull { it.name == value.trim().uppercase() }
                ?: throw IllegalArgumentException("Unsupported family permission: $value")
    }
}
