package com.dailyback.features.families.application

import com.dailyback.features.families.domain.Family
import com.dailyback.features.families.domain.FamilyAggregateStatus
import java.util.UUID

interface FamilyRepository {
    fun insert(
        name: String,
        createdByUserId: UUID,
        status: FamilyAggregateStatus = FamilyAggregateStatus.ACTIVE,
    ): Family

    fun findById(id: UUID): Family?
}
