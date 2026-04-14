package com.dailyback.shared.infrastructure.migration

import com.dailyback.app.config.DatabaseConfig
import com.dailyback.app.config.FlywayConfig
import org.flywaydb.core.Flyway

class FlywayRunner(
    private val databaseConfig: DatabaseConfig,
    private val flywayConfig: FlywayConfig,
) {
    fun migrate() {
        if (!flywayConfig.enabled) {
            return
        }

        Flyway.configure()
            .dataSource(
                databaseConfig.jdbcUrl,
                databaseConfig.user,
                databaseConfig.password,
            )
            .locations(flywayConfig.location)
            .baselineOnMigrate(true)
            .load()
            .migrate()
    }
}
