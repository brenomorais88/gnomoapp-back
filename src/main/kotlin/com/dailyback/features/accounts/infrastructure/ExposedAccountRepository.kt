package com.dailyback.features.accounts.infrastructure

import com.dailyback.features.accountoccurrences.domain.OccurrenceStatus
import com.dailyback.features.accounts.application.AccountRepository
import com.dailyback.features.accounts.application.AccountViewerQuery
import com.dailyback.features.accounts.application.OccurrenceSnapshot
import com.dailyback.features.accounts.application.SaveAccountCommand
import com.dailyback.features.accounts.domain.Account
import com.dailyback.features.accounts.domain.AccountOccurrence
import com.dailyback.features.accounts.domain.AccountOwnershipType
import com.dailyback.features.accounts.domain.RecurrenceType
import com.dailyback.shared.domain.family.FamilyMembershipStatus
import com.dailyback.shared.infrastructure.database.DatabaseFactory
import com.dailyback.shared.infrastructure.database.tables.AccountOccurrencesTable
import com.dailyback.shared.infrastructure.database.tables.AccountsTable
import com.dailyback.shared.infrastructure.database.tables.FamilyMembersTable
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greaterEq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList
import org.jetbrains.exposed.sql.SqlExpressionBuilder.less
import org.jetbrains.exposed.sql.SqlExpressionBuilder.neq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

class ExposedAccountRepository(
    private val databaseFactory: DatabaseFactory,
) : AccountRepository {
    override fun findVisibleForUser(query: AccountViewerQuery): List<Account> {
        databaseFactory.connect()
        return transaction {
            val personalOwn = AccountsTable.selectAll()
                .where {
                    (AccountsTable.ownershipType eq AccountOwnershipType.PERSONAL.name) and
                        (AccountsTable.ownerUserId eq query.userId)
                }
                .map(::toAccount)

            val familyRows =
                if (query.canViewFamilyAccounts && query.viewerFamilyId != null) {
                    AccountsTable.selectAll()
                        .where {
                            (AccountsTable.ownershipType eq AccountOwnershipType.FAMILY.name) and
                                (AccountsTable.familyId eq query.viewerFamilyId)
                        }
                        .map(::toAccount)
                } else {
                    emptyList()
                }

            val othersPersonal =
                if (query.canViewOtherPersonalAccounts && query.viewerFamilyId != null) {
                    val userIdsInFamily = FamilyMembersTable.selectAll()
                        .where {
                            (FamilyMembersTable.familyId eq query.viewerFamilyId) and
                                (FamilyMembersTable.userId.isNotNull()) and
                                (
                                    FamilyMembersTable.status inList listOf(
                                        FamilyMembershipStatus.ACTIVE.name,
                                        FamilyMembershipStatus.PENDING_REGISTRATION.name,
                                    )
                                    ) and
                                (FamilyMembersTable.userId neq query.userId)
                        }
                        .mapNotNull { it[FamilyMembersTable.userId]?.value }
                        .distinct()
                    if (userIdsInFamily.isEmpty()) {
                        emptyList()
                    } else {
                        AccountsTable.selectAll()
                            .where {
                                (AccountsTable.ownershipType eq AccountOwnershipType.PERSONAL.name) and
                                    (AccountsTable.ownerUserId inList userIdsInFamily)
                            }
                            .map(::toAccount)
                    }
                } else {
                    emptyList()
                }

            (personalOwn + familyRows + othersPersonal).distinctBy { it.id }.sortedBy { it.createdAt }
        }
    }

    override fun findVisibleAccountIds(query: AccountViewerQuery): Set<UUID> =
        findVisibleForUser(query).map { it.id }.toSet()

    override fun findActiveRecurringAccounts(): List<Account> {
        databaseFactory.connect()
        return transaction {
            AccountsTable.selectAll()
                .where {
                    (AccountsTable.active eq true) and
                        (AccountsTable.recurrenceType neq RecurrenceType.UNIQUE.name)
                }
                .orderBy(AccountsTable.createdAt)
                .map(::toAccount)
        }
    }

    override fun findById(id: UUID): Account? {
        databaseFactory.connect()
        return transaction {
            AccountsTable.selectAll()
                .where { AccountsTable.id eq id }
                .limit(1)
                .map(::toAccount)
                .firstOrNull()
        }
    }

    override fun create(command: SaveAccountCommand): Account {
        databaseFactory.connect()
        return transaction {
            val accountId = AccountsTable.insert {
                it[title] = command.title
                it[baseAmount] = command.baseAmount
                it[startDate] = command.startDate
                it[endDate] = command.endDate
                it[recurrenceType] = command.recurrenceType.name
                it[categoryId] = command.categoryId
                it[notes] = command.notes
                it[active] = command.active
                it[ownershipType] = command.ownershipType.name
                it[ownerUserId] = command.ownerUserId
                it[familyId] = command.familyId
                it[createdByUserId] = command.createdByUserId
                it[responsibleMemberId] = command.responsibleMemberId
            }[AccountsTable.id].value

            AccountsTable.selectAll()
                .where { AccountsTable.id eq accountId }
                .limit(1)
                .map(::toAccount)
                .first()
        }
    }

    override fun update(id: UUID, command: SaveAccountCommand): Account {
        databaseFactory.connect()
        return transaction {
            AccountsTable.update({ AccountsTable.id eq id }) {
                it[title] = command.title
                it[baseAmount] = command.baseAmount
                it[startDate] = command.startDate
                it[endDate] = command.endDate
                it[recurrenceType] = command.recurrenceType.name
                it[categoryId] = command.categoryId
                it[notes] = command.notes
                it[active] = command.active
                it[updatedAt] = Instant.now()
            }

            AccountsTable.selectAll()
                .where { AccountsTable.id eq id }
                .limit(1)
                .map(::toAccount)
                .first()
        }
    }

    override fun setActive(id: UUID, active: Boolean): Account {
        databaseFactory.connect()
        return transaction {
            AccountsTable.update({ AccountsTable.id eq id }) {
                it[AccountsTable.active] = active
                it[updatedAt] = Instant.now()
            }
            AccountsTable.selectAll()
                .where { AccountsTable.id eq id }
                .limit(1)
                .map(::toAccount)
                .first()
        }
    }

    override fun delete(id: UUID) {
        databaseFactory.connect()
        transaction {
            AccountOccurrencesTable.deleteWhere { AccountOccurrencesTable.accountId eq id }
            AccountsTable.deleteWhere { AccountsTable.id eq id }
        }
    }

    override fun upsertOccurrences(occurrences: List<OccurrenceSnapshot>) {
        if (occurrences.isEmpty()) {
            return
        }
        databaseFactory.connect()
        transaction {
            occurrences.forEach { occurrence ->
                val escapedTitle = occurrence.titleSnapshot.replace("'", "''")
                val escapedNotes = occurrence.notesSnapshot?.replace("'", "''")
                val notesSql = escapedNotes?.let { "'$it'" } ?: "NULL"
                exec(
                    """
                    INSERT INTO account_occurrences (
                        account_id,
                        title_snapshot,
                        amount_snapshot,
                        due_date,
                        status,
                        paid_at,
                        notes_snapshot,
                        category_id_snapshot,
                        created_at,
                        updated_at
                    )
                    VALUES (
                        '${occurrence.accountId}',
                        '$escapedTitle',
                        ${occurrence.amountSnapshot},
                        '${occurrence.dueDate}',
                        '${occurrence.status.name}',
                        NULL,
                        $notesSql,
                        '${occurrence.categoryIdSnapshot}',
                        NOW(),
                        NOW()
                    )
                    ON CONFLICT (account_id, due_date) DO NOTHING
                    """.trimIndent(),
                )
            }
        }
    }

    override fun deleteFuturePendingOccurrences(accountId: UUID, fromDate: LocalDate) {
        databaseFactory.connect()
        transaction {
            AccountOccurrencesTable.deleteWhere {
                (AccountOccurrencesTable.accountId eq accountId) and
                    (AccountOccurrencesTable.dueDate greaterEq fromDate) and
                    (AccountOccurrencesTable.status eq OccurrenceStatus.PENDING.name)
            }
        }
    }

    override fun findOccurrencesByAccountId(accountId: UUID): List<AccountOccurrence> {
        databaseFactory.connect()
        return transaction {
            AccountOccurrencesTable.selectAll()
                .where { AccountOccurrencesTable.accountId eq accountId }
                .orderBy(AccountOccurrencesTable.dueDate)
                .map(::toOccurrence)
        }
    }

    override fun hasRelevantHistory(accountId: UUID, today: LocalDate): Boolean {
        databaseFactory.connect()
        return transaction {
            AccountOccurrencesTable.selectAll()
                .where {
                    (AccountOccurrencesTable.accountId eq accountId) and
                        (
                            (AccountOccurrencesTable.dueDate less today) or
                                (AccountOccurrencesTable.status eq OccurrenceStatus.PAID.name)
                            )
                }
                .limit(1)
                .count() > 0L
        }
    }

    private fun toAccount(row: ResultRow): Account = Account(
        id = row[AccountsTable.id].value,
        title = row[AccountsTable.title],
        baseAmount = row[AccountsTable.baseAmount],
        startDate = row[AccountsTable.startDate],
        endDate = row[AccountsTable.endDate],
        recurrenceType = RecurrenceType.fromValue(row[AccountsTable.recurrenceType]),
        categoryId = row[AccountsTable.categoryId].value,
        notes = row[AccountsTable.notes],
        active = row[AccountsTable.active],
        ownershipType = AccountOwnershipType.fromValue(row[AccountsTable.ownershipType]),
        ownerUserId = row[AccountsTable.ownerUserId]?.value,
        familyId = row[AccountsTable.familyId]?.value,
        createdByUserId = row[AccountsTable.createdByUserId]?.value,
        responsibleMemberId = row[AccountsTable.responsibleMemberId]?.value,
        createdAt = row[AccountsTable.createdAt],
        updatedAt = row[AccountsTable.updatedAt],
    )

    private fun toOccurrence(row: ResultRow): AccountOccurrence = AccountOccurrence(
        id = row[AccountOccurrencesTable.id].value,
        accountId = row[AccountOccurrencesTable.accountId].value,
        titleSnapshot = row[AccountOccurrencesTable.titleSnapshot],
        amountSnapshot = row[AccountOccurrencesTable.amountSnapshot],
        dueDate = row[AccountOccurrencesTable.dueDate],
        status = OccurrenceStatus.fromValue(row[AccountOccurrencesTable.status]),
        paidAt = row[AccountOccurrencesTable.paidAt],
        notesSnapshot = row[AccountOccurrencesTable.notesSnapshot],
        categoryIdSnapshot = row[AccountOccurrencesTable.categoryIdSnapshot],
        createdAt = row[AccountOccurrencesTable.createdAt],
        updatedAt = row[AccountOccurrencesTable.updatedAt],
    )
}
