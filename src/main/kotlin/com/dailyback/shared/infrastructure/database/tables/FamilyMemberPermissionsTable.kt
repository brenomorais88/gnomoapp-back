package com.dailyback.shared.infrastructure.database.tables

import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.CurrentTimestamp
import org.jetbrains.exposed.sql.javatime.timestamp

object FamilyMemberPermissionsTable : Table("family_member_permissions") {
    val memberId = reference(
        name = "member_id",
        foreign = FamilyMembersTable,
        onDelete = ReferenceOption.CASCADE,
        onUpdate = ReferenceOption.CASCADE,
    )
    val canViewFamilyAccounts = bool("can_view_family_accounts")
    val canCreateFamilyAccounts = bool("can_create_family_accounts")
    val canEditFamilyAccounts = bool("can_edit_family_accounts")
    val canDeleteFamilyAccounts = bool("can_delete_family_accounts")
    val canMarkFamilyAccountsPaid = bool("can_mark_family_accounts_paid")
    val canManageCategories = bool("can_manage_categories")
    val canInviteMembers = bool("can_invite_members")
    val canManageMembers = bool("can_manage_members")
    val canViewOtherPersonalAccounts = bool("can_view_other_personal_accounts")
    val canEditOtherPersonalAccounts = bool("can_edit_other_personal_accounts")
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp)
    val updatedAt = timestamp("updated_at").defaultExpression(CurrentTimestamp)

    override val primaryKey = PrimaryKey(memberId)
}
