package com.dailyback.features.families.infrastructure

import com.dailyback.features.families.application.FamilyRepository
import com.dailyback.features.families.domain.Family
import com.dailyback.features.families.domain.FamilyAggregateStatus
import com.dailyback.shared.infrastructure.database.DatabaseFactory
import com.dailyback.shared.infrastructure.database.tables.FamiliesTable
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID

class ExposedFamilyRepository(
    private val databaseFactory: DatabaseFactory,
) : FamilyRepository {
    override fun insert(name: String, createdByUserId: UUID, status: FamilyAggregateStatus): Family {
        databaseFactory.connect()
        return transaction {
            val id = FamiliesTable.insert {
                it[FamiliesTable.name] = name
                it[FamiliesTable.createdByUserId] = createdByUserId
                it[FamiliesTable.status] = status.name
            }[FamiliesTable.id].value

            FamiliesTable.selectAll()
                .where { FamiliesTable.id eq id }
                .limit(1)
                .map(::toFamily)
                .first()
        }
    }

    override fun findById(id: UUID): Family? {
        databaseFactory.connect()
        return transaction {
            FamiliesTable.selectAll()
                .where { FamiliesTable.id eq id }
                .limit(1)
                .map(::toFamily)
                .firstOrNull()
        }
    }

    private fun toFamily(row: ResultRow): Family = Family(
        id = row[FamiliesTable.id].value,
        name = row[FamiliesTable.name],
        createdByUserId = row[FamiliesTable.createdByUserId]?.value,
        status = FamilyAggregateStatus.fromValue(row[FamiliesTable.status]),
        createdAt = row[FamiliesTable.createdAt],
        updatedAt = row[FamiliesTable.updatedAt],
    )
}
