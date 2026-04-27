package com.dailyback.features.families.domain

import com.dailyback.shared.domain.family.FamilyPermissionKey

class FamilyPermissionDeniedException(
    val permission: FamilyPermissionKey,
) : RuntimeException("Missing permission: ${permission.name}")
