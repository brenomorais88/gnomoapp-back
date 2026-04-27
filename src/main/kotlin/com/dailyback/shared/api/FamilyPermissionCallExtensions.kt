package com.dailyback.shared.api

import com.dailyback.features.families.application.FamilyPermissionAuthorizer
import com.dailyback.shared.domain.family.FamilyPermissionKey
import io.ktor.server.application.ApplicationCall
import java.util.UUID

fun ApplicationCall.requireJwtUserIdAndPermission(
    authorizer: FamilyPermissionAuthorizer,
    permission: FamilyPermissionKey,
): UUID {
    val userId = requireJwtUserId()
    authorizer.require(userId, permission)
    return userId
}
