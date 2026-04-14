package com.dailyback.shared.api.routes

import com.dailyback.app.bootstrap.module
import com.dailyback.app.config.AppConfig
import com.dailyback.app.config.DatabaseConfig
import com.dailyback.app.config.FlywayConfig
import com.dailyback.app.config.SchedulerConfig
import com.dailyback.app.config.SeedConfig
import com.dailyback.app.config.ServerConfig
import com.dailyback.shared.domain.health.DatabaseHealthChecker
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HealthRoutesTest {

    @Test
    fun `should return 200 when database checker is healthy`() = testApplication {
        application {
            module(
                appConfig = testConfig(),
                databaseHealthCheckerOverride = FixedDatabaseHealthChecker(true),
                runStartup = false,
            )
        }

        val response = client.get("/health")

        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.bodyAsText().contains("\"status\":\"UP\""))
        assertTrue(response.bodyAsText().contains("\"database\":\"UP\""))
    }

    @Test
    fun `should return 503 when database checker is unhealthy`() = testApplication {
        application {
            module(
                appConfig = testConfig(),
                databaseHealthCheckerOverride = FixedDatabaseHealthChecker(false),
                runStartup = false,
            )
        }

        val response = client.get("/health")

        assertEquals(HttpStatusCode.ServiceUnavailable, response.status)
        assertTrue(response.bodyAsText().contains("\"status\":\"DEGRADED\""))
        assertTrue(response.bodyAsText().contains("\"database\":\"DOWN\""))
    }
}

private class FixedDatabaseHealthChecker(
    private val healthy: Boolean,
) : DatabaseHealthChecker {
    override fun isHealthy(): Boolean = healthy
}

private fun testConfig(): AppConfig = AppConfig(
    server = ServerConfig(host = "127.0.0.1", port = 8080),
    database = DatabaseConfig(
        host = "localhost",
        port = 5432,
        name = "daily_test",
        user = "daily",
        password = "daily",
        schema = "public",
        ssl = false,
        jdbcUrlOverride = null,
    ),
    flyway = FlywayConfig(
        enabled = false,
        location = "classpath:db/migration",
    ),
    seed = SeedConfig(
        enabled = false,
    ),
    scheduler = SchedulerConfig(
        recurrenceMaintenanceEnabled = false,
        recurrenceMaintenanceIntervalHours = 24,
    ),
)
