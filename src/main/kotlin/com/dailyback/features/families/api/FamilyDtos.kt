package com.dailyback.features.families.api

import com.dailyback.features.families.domain.Family
import com.dailyback.features.families.domain.FamilyMember
import com.dailyback.shared.domain.family.FamilyMemberPermissionFlags
import kotlinx.serialization.Serializable

@Serializable
data class CreateFamilyRequest(
    val name: String,
)

@Serializable
data class InviteFamilyMemberRequest(
    val displayName: String,
    val document: String? = null,
    val email: String? = null,
    val phone: String? = null,
)

@Serializable
data class UpdateFamilyMemberRoleRequest(
    val role: String,
)

@Serializable
data class FamilyResponse(
    val id: String,
    val name: String,
    val createdByUserId: String?,
    val status: String,
    val createdAt: String,
    val updatedAt: String,
)

@Serializable
data class FamilyMemberPermissionsResponse(
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
)

@Serializable
data class FamilyMemberResponse(
    val id: String,
    val familyId: String,
    val userId: String?,
    val displayName: String,
    val document: String?,
    val email: String?,
    val phone: String?,
    val role: String,
    val status: String,
    val invitedByUserId: String?,
    val joinedAt: String?,
    val createdAt: String,
    val updatedAt: String,
)

fun Family.toResponse(): FamilyResponse = FamilyResponse(
    id = id.toString(),
    name = name,
    createdByUserId = createdByUserId?.toString(),
    status = status.name,
    createdAt = createdAt.toString(),
    updatedAt = updatedAt.toString(),
)

fun FamilyMemberPermissionFlags.toResponse(): FamilyMemberPermissionsResponse = FamilyMemberPermissionsResponse(
    canViewFamilyAccounts = canViewFamilyAccounts,
    canCreateFamilyAccounts = canCreateFamilyAccounts,
    canEditFamilyAccounts = canEditFamilyAccounts,
    canDeleteFamilyAccounts = canDeleteFamilyAccounts,
    canMarkFamilyAccountsPaid = canMarkFamilyAccountsPaid,
    canManageCategories = canManageCategories,
    canInviteMembers = canInviteMembers,
    canManageMembers = canManageMembers,
    canViewOtherPersonalAccounts = canViewOtherPersonalAccounts,
    canEditOtherPersonalAccounts = canEditOtherPersonalAccounts,
)

fun FamilyMemberPermissionsResponse.toFlags(): FamilyMemberPermissionFlags = FamilyMemberPermissionFlags(
    canViewFamilyAccounts = canViewFamilyAccounts,
    canCreateFamilyAccounts = canCreateFamilyAccounts,
    canEditFamilyAccounts = canEditFamilyAccounts,
    canDeleteFamilyAccounts = canDeleteFamilyAccounts,
    canMarkFamilyAccountsPaid = canMarkFamilyAccountsPaid,
    canManageCategories = canManageCategories,
    canInviteMembers = canInviteMembers,
    canManageMembers = canManageMembers,
    canViewOtherPersonalAccounts = canViewOtherPersonalAccounts,
    canEditOtherPersonalAccounts = canEditOtherPersonalAccounts,
)

fun FamilyMember.toResponse(): FamilyMemberResponse = FamilyMemberResponse(
    id = id.toString(),
    familyId = familyId.toString(),
    userId = userId?.toString(),
    displayName = displayName,
    document = document,
    email = email,
    phone = phone,
    role = role.name,
    status = status.name,
    invitedByUserId = invitedByUserId?.toString(),
    joinedAt = joinedAt?.toString(),
    createdAt = createdAt.toString(),
    updatedAt = updatedAt.toString(),
)
