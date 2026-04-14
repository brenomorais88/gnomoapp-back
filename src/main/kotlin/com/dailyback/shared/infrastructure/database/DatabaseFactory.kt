package com.dailyback.shared.infrastructure.database

import com.dailyback.app.config.DatabaseConfig
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.concurrent.atomic.AtomicBoolean

class DatabaseFactory(
    private val databaseConfig: DatabaseConfig,
) {
    private val connected = AtomicBoolean(false)

    fun connect() {
        if (connected.compareAndSet(false, true)) {
            Database.connect(
                url = databaseConfig.jdbcUrl,
                driver = "org.postgresql.Driver",
                user = databaseConfig.user,
                password = databaseConfig.password,
            )
        }
    }

    fun isConnectionHealthy(): Boolean = runCatching {
        connect()
        transaction {
            exec("SELECT 1") { resultSet -> resultSet.next() } ?: false
        }
    }.getOrDefault(false)
}
