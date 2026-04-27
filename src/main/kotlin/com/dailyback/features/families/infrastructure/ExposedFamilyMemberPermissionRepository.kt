package com.dailyback.features.families.infrastructure

import com.dailyback.features.families.application.FamilyMemberPermissionRepository
import com.dailyback.shared.domain.family.FamilyMemberPermissionFlags
import com.dailyback.shared.infrastructure.database.DatabaseFactory
import com.dailyback.shared.infrastructure.database.tables.FamilyMemberPermissionsTable
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.statements.UpdateBuilder
import org.jetbrains.exposed.sql.update
import java.time.Instant
import java.util.UUID

class ExposedFamilyMemberPermissionRepository(
    private val databaseFactory: DatabaseFactory,
) : FamilyMemberPermissionRepository {
    override fun findByMemberId(memberId: UUID): FamilyMemberPermissionFlags? {
        databaseFactory.connect()
        return transaction {
            FamilyMemberPermissionsTable.selectAll()
                .where { FamilyMemberPermissionsTable.memberId eq memberId }
                .limit(1)
                .map(::toFlags)
                .firstOrNull()
        }
    }

    override fun upsert(memberId: UUID, flags: FamilyMemberPermissionFlags): FamilyMemberPermissionFlags {
        databaseFactory.connect()
        return transaction {
            val exists = FamilyMemberPermissionsTable.selectAll()
                .where { FamilyMemberPermissionsTable.memberId eq memberId }
                .limit(1)
                .count() > 0L
            val now = Instant.now()
            if (exists) {
                FamilyMemberPermissionsTable.update({ FamilyMemberPermissionsTable.memberId eq memberId }) {
                    writeFlags(it, flags)
                    it[updatedAt] = now
                }
            } else {
                FamilyMemberPermissionsTable.insert {
                    it[FamilyMemberPermissionsTable.memberId] = memberId
                    writeFlags(it, flags)
                    it[createdAt] = now
                    it[updatedAt] = now
                }
            }
            FamilyMemberPermissionsTable.selectAll()
                .where { FamilyMemberPermissionsTable.memberId eq memberId }
                .limit(1)
                .map(::toFlags)
                .first()
        }
    }

    private fun writeFlags(
        it: UpdateBuilder<*>,
        flags: FamilyMemberPermissionFlags,
    ) {
        it[FamilyMemberPermissionsTable.canViewFamilyAccounts] = flags.canViewFamilyAccounts
        it[FamilyMemberPermissionsTable.canCreateFamilyAccounts] = flags.canCreateFamilyAccounts
        it[FamilyMemberPermissionsTable.canEditFamilyAccounts] = flags.canEditFamilyAccounts
        it[FamilyMemberPermissionsTable.canDeleteFamilyAccounts] = flags.canDeleteFamilyAccounts
        it[FamilyMemberPermissionsTable.canMarkFamilyAccountsPaid] = flags.canMarkFamilyAccountsPaid
        it[FamilyMemberPermissionsTable.canManageCategories] = flags.canManageCategories
        it[FamilyMemberPermissionsTable.canInviteMembers] = flags.canInviteMembers
        it[FamilyMemberPermissionsTable.canManageMembers] = flags.canManageMembers
        it[FamilyMemberPermissionsTable.canViewOtherPersonalAccounts] = flags.canViewOtherPersonalAccounts
        it[FamilyMemberPermissionsTable.canEditOtherPersonalAccounts] = flags.canEditOtherPersonalAccounts
    }

    private fun toFlags(row: ResultRow): FamilyMemberPermissionFlags = FamilyMemberPermissionFlags(
        canViewFamilyAccounts = row[FamilyMemberPermissionsTable.canViewFamilyAccounts],
        canCreateFamilyAccounts = row[FamilyMemberPermissionsTable.canCreateFamilyAccounts],
        canEditFamilyAccounts = row[FamilyMemberPermissionsTable.canEditFamilyAccounts],
        canDeleteFamilyAccounts = row[FamilyMemberPermissionsTable.canDeleteFamilyAccounts],
        canMarkFamilyAccountsPaid = row[FamilyMemberPermissionsTable.canMarkFamilyAccountsPaid],
        canManageCategories = row[FamilyMemberPermissionsTable.canManageCategories],
        canInviteMembers = row[FamilyMemberPermissionsTable.canInviteMembers],
        canManageMembers = row[FamilyMemberPermissionsTable.canManageMembers],
        canViewOtherPersonalAccounts = row[FamilyMemberPermissionsTable.canViewOtherPersonalAccounts],
        canEditOtherPersonalAccounts = row[FamilyMemberPermissionsTable.canEditOtherPersonalAccounts],
    )
}
