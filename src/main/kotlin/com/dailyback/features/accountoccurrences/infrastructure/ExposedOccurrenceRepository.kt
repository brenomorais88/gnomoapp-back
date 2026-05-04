package com.dailyback.features.accountoccurrences.infrastructure

import com.dailyback.features.accounts.domain.AccountOccurrence
import com.dailyback.features.accountoccurrences.application.OccurrenceFilters
import com.dailyback.features.accountoccurrences.application.OccurrenceRepository
import com.dailyback.features.accountoccurrences.domain.OccurrenceStatus
import com.dailyback.shared.infrastructure.database.DatabaseFactory
import com.dailyback.shared.infrastructure.database.tables.AccountOccurrencesTable
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greaterEq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList
import org.jetbrains.exposed.sql.SqlExpressionBuilder.lessEq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset
import java.util.UUID

class ExposedOccurrenceRepository(
    private val databaseFactory: DatabaseFactory,
) : OccurrenceRepository {
    override fun findByFilters(filters: OccurrenceFilters): List<AccountOccurrence> {
        databaseFactory.connect()
        return transaction {
            val accountIds = filters.accountIds
            if (accountIds == null || accountIds.isEmpty()) {
                return@transaction emptyList()
            }

            val conditions = mutableListOf<Op<Boolean>>()
            conditions += AccountOccurrencesTable.accountId inList accountIds.toList()

            filters.status?.let { conditions += AccountOccurrencesTable.status eq it.name }
            filters.categoryId?.let { conditions += AccountOccurrencesTable.categoryIdSnapshot eq it }

            val range = monthRange(filters.month, filters.startDate, filters.endDate)
            range.first?.let { conditions += AccountOccurrencesTable.dueDate greaterEq it }
            range.second?.let { conditions += AccountOccurrencesTable.dueDate lessEq it }

            val where = conditions.fold<Op<Boolean>, Op<Boolean>?>(null) { acc, op ->
                if (acc == null) op else acc and op
            }

            val query = if (where == null) {
                AccountOccurrencesTable.selectAll()
            } else {
                AccountOccurrencesTable.selectAll().where { where }
            }

            val data = query.orderBy(AccountOccurrencesTable.dueDate to SortOrder.ASC)
                .map(::toOccurrence)

            val text = filters.text?.trim()?.lowercase()
            if (text.isNullOrBlank()) {
                data
            } else {
                data.filter {
                    it.titleSnapshot.lowercase().contains(text) ||
                        (it.notesSnapshot?.lowercase()?.contains(text) == true)
                }
            }
        }
    }

    override fun findById(id: UUID): AccountOccurrence? {
        databaseFactory.connect()
        return transaction {
            AccountOccurrencesTable.selectAll()
                .where { AccountOccurrencesTable.id eq id }
                .limit(1)
                .map(::toOccurrence)
                .firstOrNull()
        }
    }

    override fun markPaid(id: UUID): AccountOccurrence {
        databaseFactory.connect()
        return transaction {
            AccountOccurrencesTable.update({ AccountOccurrencesTable.id eq id }) {
                it[status] = OccurrenceStatus.PAID.name
                it[paidAt] = Instant.now()
                it[updatedAt] = Instant.now()
            }
            readRequired(id)
        }
    }

    override fun unmarkPaid(id: UUID): AccountOccurrence {
        databaseFactory.connect()
        return transaction {
            AccountOccurrencesTable.update({ AccountOccurrencesTable.id eq id }) {
                it[status] = OccurrenceStatus.PENDING.name
                it[paidAt] = null
                it[updatedAt] = Instant.now()
            }
            readRequired(id)
        }
    }

    override fun overrideAmount(id: UUID, amount: BigDecimal): AccountOccurrence {
        databaseFactory.connect()
        return transaction {
            AccountOccurrencesTable.update({ AccountOccurrencesTable.id eq id }) {
                it[amountSnapshot] = amount
                it[updatedAt] = Instant.now()
            }
            readRequired(id)
        }
    }

    private fun readRequired(id: UUID): AccountOccurrence =
        AccountOccurrencesTable.selectAll().where { AccountOccurrencesTable.id eq id }
            .limit(1)
            .map(::toOccurrence)
            .first()

    private fun monthRange(month: String?, startDate: LocalDate?, endDate: LocalDate?): Pair<LocalDate?, LocalDate?> {
        if (month.isNullOrBlank()) {
            return startDate to endDate
        }
        val ym = YearMonth.parse(month.trim())
        val monthStart = ym.atDay(1)
        val monthEnd = ym.atEndOfMonth()
        val rangeStart = listOfNotNull(startDate, monthStart).maxOrNull()
        val rangeEnd = listOfNotNull(endDate, monthEnd).minOrNull()
        return rangeStart to rangeEnd
    }

    private fun toOccurrence(row: ResultRow): AccountOccurrence = AccountOccurrence(
        id = row[AccountOccurrencesTable.id].value,
        accountId = row[AccountOccurrencesTable.accountId].value,
        titleSnapshot = row[AccountOccurrencesTable.titleSnapshot],
        amountSnapshot = row[AccountOccurrencesTable.amountSnapshot],
        dueDate = row[AccountOccurrencesTable.dueDate],
        status = OccurrenceStatus.fromValue(row[AccountOccurrencesTable.status]),
        paidAt = row[AccountOccurrencesTable.paidAt]?.atOffset(ZoneOffset.UTC)?.toInstant(),
        notesSnapshot = row[AccountOccurrencesTable.notesSnapshot],
        categoryIdSnapshot = row[AccountOccurrencesTable.categoryIdSnapshot],
        createdAt = row[AccountOccurrencesTable.createdAt],
        updatedAt = row[AccountOccurrencesTable.updatedAt],
    )
}
