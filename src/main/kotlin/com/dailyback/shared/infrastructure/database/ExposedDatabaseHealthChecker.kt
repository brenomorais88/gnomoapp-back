package com.dailyback.shared.infrastructure.database

import com.dailyback.shared.domain.health.DatabaseHealthChecker

class ExposedDatabaseHealthChecker(
    private val databaseFactory: DatabaseFactory,
) : DatabaseHealthChecker {
    override fun isHealthy(): Boolean = databaseFactory.isConnectionHealthy()
}
