package com.dailyback.shared.api

import com.dailyback.features.accounts.application.AccountAccessContextResolver
import com.dailyback.features.accounts.application.AccountQueryScope
import com.dailyback.features.families.application.FamilyPermissionAuthorizer
import com.dailyback.shared.domain.family.FamilyPermissionKey
import java.util.UUID
import io.ktor.server.application.ApplicationCall

fun ApplicationCall.requireJwtUserIdIfFamilyRequiresPermission(
    accountAccess: AccountAccessContextResolver,
    authorizer: FamilyPermissionAuthorizer,
    permission: FamilyPermissionKey,
): UUID {
    val userId = requireJwtUserId()
    if (accountAccess.hasActiveFamilyMembership(userId)) {
        authorizer.require(userId, permission)
    }
    return userId
}

fun ApplicationCall.requireFamilyPermissionForScope(
    userId: UUID,
    scope: AccountQueryScope,
    authorizer: FamilyPermissionAuthorizer,
    permission: FamilyPermissionKey,
) {
    if (scope == AccountQueryScope.FAMILY) {
        authorizer.require(userId, permission)
    }
}
