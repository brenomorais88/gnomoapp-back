package com.dailyback.shared.application.health

import com.dailyback.shared.domain.health.DatabaseHealthChecker
import com.dailyback.shared.time.UtcClock
import java.time.Instant

class GetHealthStatusUseCase(
    private val databaseHealthChecker: DatabaseHealthChecker,
    private val utcClock: UtcClock,
) {
    fun execute(): HealthStatus {
        val isDatabaseHealthy = databaseHealthChecker.isHealthy()

        return HealthStatus(
            status = if (isDatabaseHealthy) "UP" else "DEGRADED",
            databaseStatus = if (isDatabaseHealthy) "UP" else "DOWN",
            timestamp = utcClock.nowInstant(),
        )
    }
}

data class HealthStatus(
    val status: String,
    val databaseStatus: String,
    val timestamp: Instant,
)
