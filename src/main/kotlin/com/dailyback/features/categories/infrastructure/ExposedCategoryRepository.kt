package com.dailyback.features.categories.infrastructure

import com.dailyback.features.categories.application.CategoryRepository
import com.dailyback.features.categories.domain.Category
import com.dailyback.shared.infrastructure.database.DatabaseFactory
import com.dailyback.shared.infrastructure.database.tables.AccountsTable
import com.dailyback.shared.infrastructure.database.tables.CategoriesTable
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.util.UUID

class ExposedCategoryRepository(
    private val databaseFactory: DatabaseFactory,
) : CategoryRepository {
    override fun findAll(): List<Category> {
        databaseFactory.connect()
        return transaction {
            CategoriesTable.selectAll()
                .orderBy(CategoriesTable.name)
                .map(::toCategory)
        }
    }

    override fun findById(id: UUID): Category? {
        databaseFactory.connect()
        return transaction {
            CategoriesTable.selectAll()
                .where { CategoriesTable.id eq id }
                .limit(1)
                .map(::toCategory)
                .firstOrNull()
        }
    }

    override fun existsByName(name: String): Boolean {
        databaseFactory.connect()
        return transaction {
            CategoriesTable.selectAll()
                .where { CategoriesTable.name eq name }
                .limit(1)
                .count() > 0L
        }
    }

    override fun existsByNameExcludingId(name: String, excludedId: UUID): Boolean {
        databaseFactory.connect()
        return transaction {
            CategoriesTable.selectAll()
                .where { (CategoriesTable.name eq name) and (CategoriesTable.id neq excludedId) }
                .limit(1)
                .count() > 0L
        }
    }

    override fun create(name: String, color: String?): Category {
        databaseFactory.connect()
        return transaction {
            val insertedId = CategoriesTable.insert {
                it[CategoriesTable.name] = name
                it[CategoriesTable.color] = color
            }[CategoriesTable.id].value

            CategoriesTable.selectAll()
                .where { CategoriesTable.id eq insertedId }
                .limit(1)
                .map(::toCategory)
                .first()
        }
    }

    override fun update(id: UUID, name: String, color: String?): Category {
        databaseFactory.connect()
        return transaction {
            CategoriesTable.update({ CategoriesTable.id eq id }) {
                it[CategoriesTable.name] = name
                it[CategoriesTable.color] = color
                it[updatedAt] = java.time.Instant.now()
            }

            CategoriesTable.selectAll()
                .where { CategoriesTable.id eq id }
                .limit(1)
                .map(::toCategory)
                .first()
        }
    }

    override fun deleteById(id: UUID) {
        databaseFactory.connect()
        transaction {
            CategoriesTable.deleteWhere { CategoriesTable.id eq id }
        }
    }

    override fun isCategoryInUse(id: UUID): Boolean {
        databaseFactory.connect()
        return transaction {
            AccountsTable.selectAll()
                .where { AccountsTable.categoryId eq id }
                .limit(1)
                .count() > 0L
        }
    }

    private fun toCategory(row: ResultRow): Category = Category(
        id = row[CategoriesTable.id].value,
        name = row[CategoriesTable.name],
        color = row[CategoriesTable.color],
        createdAt = row[CategoriesTable.createdAt],
        updatedAt = row[CategoriesTable.updatedAt],
    )
}
