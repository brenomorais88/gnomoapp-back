package com.dailyback.features.families.application

import com.dailyback.shared.domain.family.FamilyMemberPermissionFlags
import java.util.UUID

interface FamilyMemberPermissionRepository {
    fun findByMemberId(memberId: UUID): FamilyMemberPermissionFlags?

    fun upsert(memberId: UUID, flags: FamilyMemberPermissionFlags): FamilyMemberPermissionFlags
}
