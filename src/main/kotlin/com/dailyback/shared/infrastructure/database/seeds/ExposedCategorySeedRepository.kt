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
                INSERT INTO categories (name, color, family_id, owner_user_id, created_at, updated_at)
                SELECT '$escapedName', NULL, NULL, NULL, NOW(), NOW()
                WHERE NOT EXISTS (
                    SELECT 1 FROM categories c
                    WHERE c.family_id IS NULL
                      AND c.owner_user_id IS NULL
                      AND lower(c.name) = lower('$escapedName')
                )
                """.trimIndent(),
            )
        }
    }
}
