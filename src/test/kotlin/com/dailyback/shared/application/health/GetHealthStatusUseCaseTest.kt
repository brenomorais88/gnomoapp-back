package com.dailyback.shared.application.health

import com.dailyback.shared.domain.health.DatabaseHealthChecker
import com.dailyback.shared.time.UtcClock
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals

class GetHealthStatusUseCaseTest {

    @Test
    fun `should return up when database is healthy`() {
        val useCase = GetHealthStatusUseCase(
            databaseHealthChecker = FakeDatabaseHealthChecker(true),
            utcClock = UtcClock(
                Clock.fixed(
                    Instant.parse("2026-04-14T12:00:00Z"),
                    ZoneOffset.UTC,
                ),
            ),
        )

        val result = useCase.execute()

        assertEquals("UP", result.status)
        assertEquals("UP", result.databaseStatus)
        assertEquals(Instant.parse("2026-04-14T12:00:00Z"), result.timestamp)
    }

    @Test
    fun `should return degraded when database is down`() {
        val useCase = GetHealthStatusUseCase(
            databaseHealthChecker = FakeDatabaseHealthChecker(false),
            utcClock = UtcClock(
                Clock.fixed(
                    Instant.parse("2026-04-14T12:00:00Z"),
                    ZoneOffset.UTC,
                ),
            ),
        )

        val result = useCase.execute()

        assertEquals("DEGRADED", result.status)
        assertEquals("DOWN", result.databaseStatus)
    }
}

private class FakeDatabaseHealthChecker(
    private val healthy: Boolean,
) : DatabaseHealthChecker {
    override fun isHealthy(): Boolean = healthy
}
