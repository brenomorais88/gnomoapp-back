package com.dailyback.features.families.application

import java.util.UUID

interface FamilyPendingMemberLinkRepository {
    fun linkFirstPendingMemberByDocument(normalizedDocument: String, userId: UUID): Boolean
}
