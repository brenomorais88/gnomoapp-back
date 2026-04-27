package com.dailyback.features.categories.infrastructure

import com.dailyback.features.categories.application.CategoryRepository
import com.dailyback.features.categories.domain.Category
import com.dailyback.shared.infrastructure.database.DatabaseFactory
import com.dailyback.shared.infrastructure.database.tables.AccountsTable
import com.dailyback.shared.infrastructure.database.tables.CategoriesTable
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.isNotNull
import org.jetbrains.exposed.sql.SqlExpressionBuilder.isNull
import org.jetbrains.exposed.sql.SqlExpressionBuilder.neq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.lowerCase
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.util.UUID

class ExposedCategoryRepository(
    private val databaseFactory: DatabaseFactory,
) : CategoryRepository {
    override fun listForUser(userId: UUID, familyId: UUID?): List<Category> {
        databaseFactory.connect()
        return transaction {
            CategoriesTable.selectAll()
                .where { visibleToUser(userId, familyId) }
                .orderBy(CategoriesTable.name)
                .map(::toCategory)
        }
    }

    override fun findByIdForUser(userId: UUID, familyId: UUID?, id: UUID): Category? {
        databaseFactory.connect()
        return transaction {
            CategoriesTable.selectAll()
                .where { (CategoriesTable.id eq id) and visibleToUser(userId, familyId) }
                .limit(1)
                .map(::toCategory)
                .firstOrNull()
        }
    }

    override fun existsByNameForUser(userId: UUID, familyId: UUID?, name: String): Boolean {
        val key = name.trim().lowercase()
        databaseFactory.connect()
        return transaction {
            CategoriesTable.selectAll()
                .where {
                    visibleToUser(userId, familyId) and
                        (CategoriesTable.name.lowerCase() eq key)
                }
                .limit(1)
                .count() > 0L
        }
    }

    override fun existsByNameExcludingIdForUser(
        userId: UUID,
        familyId: UUID?,
        name: String,
        excludedId: UUID,
    ): Boolean {
        val key = name.trim().lowercase()
        databaseFactory.connect()
        return transaction {
            CategoriesTable.selectAll()
                .where {
                    visibleToUser(userId, familyId) and
                        (CategoriesTable.name.lowerCase() eq key) and
                        (CategoriesTable.id neq excludedId)
                }
                .limit(1)
                .count() > 0L
        }
    }

    override fun isVisibleToUser(categoryId: UUID, userId: UUID, familyId: UUID?): Boolean {
        databaseFactory.connect()
        return transaction {
            CategoriesTable.selectAll()
                .where { (CategoriesTable.id eq categoryId) and visibleToUser(userId, familyId) }
                .limit(1)
                .count() > 0L
        }
    }

    override fun createForUser(userId: UUID, familyId: UUID?, name: String, color: String?): Category {
        databaseFactory.connect()
        return transaction {
            val insertedId = CategoriesTable.insert {
                it[CategoriesTable.name] = name
                it[CategoriesTable.color] = color
                if (familyId != null) {
                    it[CategoriesTable.familyId] = familyId
                    it[CategoriesTable.ownerUserId] = null
                } else {
                    it[CategoriesTable.familyId] = null
                    it[CategoriesTable.ownerUserId] = userId
                }
            }[CategoriesTable.id].value

            CategoriesTable.selectAll()
                .where { CategoriesTable.id eq insertedId }
                .limit(1)
                .map(::toCategory)
                .first()
        }
    }

    override fun updateForUser(userId: UUID, familyId: UUID?, id: UUID, name: String, color: String?): Category {
        databaseFactory.connect()
        return transaction {
            CategoriesTable.update({
                (CategoriesTable.id eq id) and visibleToUser(userId, familyId) and nonGlobalCategory()
            }) {
                it[CategoriesTable.name] = name
                it[CategoriesTable.color] = color
                it[CategoriesTable.updatedAt] = java.time.Instant.now()
            }

            CategoriesTable.selectAll()
                .where {
                    (CategoriesTable.id eq id) and visibleToUser(userId, familyId) and nonGlobalCategory()
                }
                .limit(1)
                .map(::toCategory)
                .firstOrNull()
                ?: error("Category update produced no row for id=$id")
        }
    }

    override fun deleteByIdForUser(userId: UUID, familyId: UUID?, id: UUID) {
        databaseFactory.connect()
        transaction {
            CategoriesTable.deleteWhere {
                (CategoriesTable.id eq id) and visibleToUser(userId, familyId) and nonGlobalCategory()
            }
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

    private fun visibleToUser(userId: UUID, familyId: UUID?): Op<Boolean> {
        val global = CategoriesTable.familyId.isNull() and CategoriesTable.ownerUserId.isNull()
        val personal = CategoriesTable.ownerUserId eq userId
        return if (familyId != null) {
            global or (CategoriesTable.familyId eq familyId) or personal
        } else {
            global or personal
        }
    }

    private fun nonGlobalCategory(): Op<Boolean> =
        CategoriesTable.familyId.isNotNull() or CategoriesTable.ownerUserId.isNotNull()

    private fun toCategory(row: ResultRow): Category = Category(
        id = row[CategoriesTable.id].value,
        name = row[CategoriesTable.name],
        color = row[CategoriesTable.color],
        familyId = row[CategoriesTable.familyId]?.value,
        ownerUserId = row[CategoriesTable.ownerUserId]?.value,
        createdAt = row[CategoriesTable.createdAt],
        updatedAt = row[CategoriesTable.updatedAt],
    )
}
