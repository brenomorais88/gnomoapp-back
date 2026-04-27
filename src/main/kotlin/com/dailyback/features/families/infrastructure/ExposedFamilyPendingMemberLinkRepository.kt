package com.dailyback.features.families.infrastructure

import com.dailyback.features.families.application.FamilyPendingMemberLinkRepository
import com.dailyback.shared.domain.family.FamilyMembershipStatus
import com.dailyback.shared.infrastructure.database.DatabaseFactory
import com.dailyback.shared.infrastructure.database.tables.FamilyMembersTable
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.time.Instant
import java.util.UUID

class ExposedFamilyPendingMemberLinkRepository(
    private val databaseFactory: DatabaseFactory,
) : FamilyPendingMemberLinkRepository {
    override fun linkFirstPendingMemberByDocument(normalizedDocument: String, userId: UUID): Boolean {
        databaseFactory.connect()
        return transaction {
            val rowId = FamilyMembersTable.selectAll()
                .where {
                    (FamilyMembersTable.status eq FamilyMembershipStatus.PENDING_REGISTRATION.name) and
                        (FamilyMembersTable.userId.isNull()) and
                        (FamilyMembersTable.document eq normalizedDocument)
                }
                .orderBy(FamilyMembersTable.createdAt)
                .limit(1)
                .map { it[FamilyMembersTable.id].value }
                .firstOrNull()
                ?: return@transaction false

            FamilyMembersTable.update({ FamilyMembersTable.id eq rowId }) {
                it[FamilyMembersTable.userId] = userId
                it[FamilyMembersTable.status] = FamilyMembershipStatus.ACTIVE.name
                it[FamilyMembersTable.joinedAt] = Instant.now()
                it[FamilyMembersTable.updatedAt] = Instant.now()
            }
            true
        }
    }
}
