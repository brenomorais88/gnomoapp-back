package com.dailyback.features.families.infrastructure

import com.dailyback.features.families.application.FamilyMemberRepository
import com.dailyback.features.families.domain.FamilyMember
import com.dailyback.shared.domain.family.FamilyMemberRole
import com.dailyback.shared.domain.family.FamilyMembershipStatus
import com.dailyback.shared.infrastructure.database.DatabaseFactory
import com.dailyback.shared.infrastructure.database.tables.FamilyMembersTable
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList
import org.jetbrains.exposed.sql.SqlExpressionBuilder.neq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.lowerCase
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.time.Instant
import java.util.UUID

class ExposedFamilyMemberRepository(
    private val databaseFactory: DatabaseFactory,
) : FamilyMemberRepository {
    override fun findActiveMembershipForUser(userId: UUID): FamilyMember? {
        databaseFactory.connect()
        return transaction {
            FamilyMembersTable.selectAll()
                .where {
                    (FamilyMembersTable.userId eq userId) and
                        (FamilyMembersTable.status inList activeMembershipStatuses())
                }
                .limit(1)
                .map(::toMember)
                .firstOrNull()
        }
    }

    override fun findActiveMembersByFamily(familyId: UUID): List<FamilyMember> {
        databaseFactory.connect()
        return transaction {
            FamilyMembersTable.selectAll()
                .where {
                    (FamilyMembersTable.familyId eq familyId) and
                        (FamilyMembersTable.status inList listableStatuses())
                }
                .orderBy(FamilyMembersTable.createdAt)
                .map(::toMember)
        }
    }

    override fun findNonRemovedMemberInFamilyByUser(familyId: UUID, userId: UUID): FamilyMember? {
        databaseFactory.connect()
        return transaction {
            FamilyMembersTable.selectAll()
                .where {
                    (FamilyMembersTable.familyId eq familyId) and
                        (FamilyMembersTable.userId eq userId) and
                        (FamilyMembersTable.status neq FamilyMembershipStatus.REMOVED.name)
                }
                .limit(1)
                .map(::toMember)
                .firstOrNull()
        }
    }

    override fun findConflictingInviteInFamily(
        familyId: UUID,
        documentNormalized: String?,
        emailLower: String?,
        phoneDigits: String?,
    ): FamilyMember? {
        databaseFactory.connect()
        return transaction {
            val statusCond = FamilyMembersTable.status inList listableStatuses()
            val base = (FamilyMembersTable.familyId eq familyId) and statusCond

            val parts = mutableListOf<Op<Boolean>>()
            if (documentNormalized != null) {
                parts.add(FamilyMembersTable.document eq documentNormalized)
            }
            if (emailLower != null) {
                parts.add(FamilyMembersTable.email.lowerCase() eq emailLower)
            }
            if (phoneDigits != null) {
                parts.add(FamilyMembersTable.phone eq phoneDigits)
            }
            if (parts.isEmpty()) {
                return@transaction null
            }
            val combined = parts.reduce { acc, op -> acc or op }
            FamilyMembersTable.selectAll()
                .where { base and combined }
                .limit(1)
                .map(::toMember)
                .firstOrNull()
        }
    }

    override fun insertMember(
        familyId: UUID,
        userId: UUID?,
        displayName: String,
        document: String?,
        email: String?,
        phone: String?,
        role: FamilyMemberRole,
        status: FamilyMembershipStatus,
        invitedByUserId: UUID?,
        joinedAt: Instant?,
    ): FamilyMember {
        databaseFactory.connect()
        return transaction {
            val id = FamilyMembersTable.insert {
                it[FamilyMembersTable.familyId] = familyId
                it[FamilyMembersTable.userId] = userId
                it[FamilyMembersTable.displayName] = displayName
                it[FamilyMembersTable.document] = document
                it[FamilyMembersTable.email] = email
                it[FamilyMembersTable.phone] = phone
                it[FamilyMembersTable.role] = role.name
                it[FamilyMembersTable.status] = status.name
                it[FamilyMembersTable.invitedByUserId] = invitedByUserId
                it[FamilyMembersTable.joinedAt] = joinedAt
            }[FamilyMembersTable.id].value

            FamilyMembersTable.selectAll()
                .where { FamilyMembersTable.id eq id }
                .limit(1)
                .map(::toMember)
                .first()
        }
    }

    override fun findMemberByIdInFamily(memberId: UUID, familyId: UUID): FamilyMember? {
        databaseFactory.connect()
        return transaction {
            FamilyMembersTable.selectAll()
                .where {
                    (FamilyMembersTable.id eq memberId) and
                        (FamilyMembersTable.familyId eq familyId) and
                        (FamilyMembersTable.status neq FamilyMembershipStatus.REMOVED.name)
                }
                .limit(1)
                .map(::toMember)
                .firstOrNull()
        }
    }

    override fun countActiveAdminsInFamily(familyId: UUID): Int {
        databaseFactory.connect()
        return transaction {
            FamilyMembersTable.selectAll()
                .where {
                    (FamilyMembersTable.familyId eq familyId) and
                        (FamilyMembersTable.role eq FamilyMemberRole.ADMIN.name) and
                        (FamilyMembersTable.status eq FamilyMembershipStatus.ACTIVE.name)
                }
                .count()
                .toInt()
        }
    }

    override fun updateMemberRole(memberId: UUID, familyId: UUID, newRole: FamilyMemberRole): FamilyMember? {
        databaseFactory.connect()
        return transaction {
            val updated = FamilyMembersTable.update(
                where = {
                    (FamilyMembersTable.id eq memberId) and
                        (FamilyMembersTable.familyId eq familyId) and
                        (FamilyMembersTable.status eq FamilyMembershipStatus.ACTIVE.name)
                },
            ) {
                it[FamilyMembersTable.role] = newRole.name
                it[FamilyMembersTable.updatedAt] = Instant.now()
            }
            if (updated == 0) {
                return@transaction null
            }
            FamilyMembersTable.selectAll()
                .where { FamilyMembersTable.id eq memberId }
                .limit(1)
                .map(::toMember)
                .firstOrNull()
        }
    }

    override fun markMemberRemoved(memberId: UUID, familyId: UUID): FamilyMember? {
        databaseFactory.connect()
        return transaction {
            val updated = FamilyMembersTable.update(
                where = {
                    (FamilyMembersTable.id eq memberId) and
                        (FamilyMembersTable.familyId eq familyId) and
                        (FamilyMembersTable.status inList listableStatuses())
                },
            ) {
                it[FamilyMembersTable.status] = FamilyMembershipStatus.REMOVED.name
                it[FamilyMembersTable.updatedAt] = Instant.now()
            }
            if (updated == 0) {
                return@transaction null
            }
            FamilyMembersTable.selectAll()
                .where { FamilyMembersTable.id eq memberId }
                .limit(1)
                .map(::toMember)
                .firstOrNull()
        }
    }

    private fun activeMembershipStatuses(): List<String> =
        listOf(
            FamilyMembershipStatus.ACTIVE.name,
            FamilyMembershipStatus.PENDING_REGISTRATION.name,
        )

    private fun listableStatuses(): List<String> =
        listOf(
            FamilyMembershipStatus.ACTIVE.name,
            FamilyMembershipStatus.PENDING_REGISTRATION.name,
        )

    private fun toMember(row: ResultRow): FamilyMember = FamilyMember(
        id = row[FamilyMembersTable.id].value,
        familyId = row[FamilyMembersTable.familyId].value,
        userId = row[FamilyMembersTable.userId]?.value,
        displayName = row[FamilyMembersTable.displayName],
        document = row[FamilyMembersTable.document],
        email = row[FamilyMembersTable.email],
        phone = row[FamilyMembersTable.phone],
        role = FamilyMemberRole.fromValue(row[FamilyMembersTable.role]),
        status = FamilyMembershipStatus.fromValue(row[FamilyMembersTable.status]),
        invitedByUserId = row[FamilyMembersTable.invitedByUserId]?.value,
        joinedAt = row[FamilyMembersTable.joinedAt],
        createdAt = row[FamilyMembersTable.createdAt],
        updatedAt = row[FamilyMembersTable.updatedAt],
    )
}
