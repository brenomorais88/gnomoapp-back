package com.dailyback.shared.infrastructure.database.seeds

import com.dailyback.shared.application.seeds.CategorySeedRepository
import com.dailyback.shared.infrastructure.database.DatabaseFactory
import org.jetbrains.exposed.sql.transactions.transaction

class ExposedCategorySeedRepository(
    private val databaseFactory: DatabaseFactory,
) : CategorySeedRepository {
    override fun upsertByName(name: String) {
        databaseFactory.connect()
        val escapedName = name.replace("'", "''")
        transaction {
            exec(
                """
                INSERT INTO categories (name, color, created_at, updated_at)
                VALUES ('$escapedName', NULL, NOW(), NOW())
                ON CONFLICT (name) DO UPDATE
                SET updated_at = NOW()
                """.trimIndent(),
            )
        }
    }
}
