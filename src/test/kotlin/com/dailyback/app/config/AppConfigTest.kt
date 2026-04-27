package com.dailyback.app.config

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AppConfigTest {

    @Test
    fun `should use defaults when environment is empty`() {
        val config = AppConfig.fromMap(emptyMap())

        assertEquals("0.0.0.0", config.server.host)
        assertEquals(8080, config.server.port)
        assertEquals("localhost", config.database.host)
        assertEquals(5432, config.database.port)
        assertEquals("daily", config.database.name)
        assertEquals("daily", config.database.user)
        assertEquals("daily", config.database.password)
        assertEquals("public", config.database.schema)
        assertFalse(config.database.ssl)
        assertTrue(config.flyway.enabled)
        assertEquals("classpath:db/migration", config.flyway.location)
        assertTrue(config.seed.enabled)
        assertFalse(config.seed.scenarioEnabled)
        assertTrue(config.scheduler.recurrenceMaintenanceEnabled)
        assertEquals(24L, config.scheduler.recurrenceMaintenanceIntervalHours)
        assertEquals("daily-back", config.security.jwt.issuer)
        assertEquals("daily-clients", config.security.jwt.audience)
        assertEquals(86_400L, config.security.jwt.accessTokenTtlSeconds)
    }

    @Test
    fun `should map explicit environment values`() {
        val config = AppConfig.fromMap(
            mapOf(
                "APP_HOST" to "127.0.0.1",
                "APP_PORT" to "9090",
                "DB_HOST" to "db",
                "DB_PORT" to "6432",
                "DB_NAME" to "daily_local",
                "DB_USER" to "user_local",
                "DB_PASSWORD" to "pass_local",
                "DB_SCHEMA" to "daily_schema",
                "DB_SSL" to "true",
                "DB_JDBC_URL" to "jdbc:postgresql://custom:5432/custom_db",
                "FLYWAY_ENABLED" to "false",
                "FLYWAY_LOCATION" to "classpath:db/custom",
                "SEED_ENABLED" to "false",
                "SEED_SCENARIO_ENABLED" to "true",
                "RECURRENCE_MAINTENANCE_ENABLED" to "false",
                "RECURRENCE_MAINTENANCE_INTERVAL_HOURS" to "12",
            ),
        )

        assertEquals("127.0.0.1", config.server.host)
        assertEquals(9090, config.server.port)
        assertEquals("db", config.database.host)
        assertEquals(6432, config.database.port)
        assertEquals("daily_local", config.database.name)
        assertEquals("user_local", config.database.user)
        assertEquals("pass_local", config.database.password)
        assertEquals("daily_schema", config.database.schema)
        assertTrue(config.database.ssl)
        assertEquals("jdbc:postgresql://custom:5432/custom_db", config.database.jdbcUrl)
        assertFalse(config.flyway.enabled)
        assertEquals("classpath:db/custom", config.flyway.location)
        assertFalse(config.seed.enabled)
        assertTrue(config.seed.scenarioEnabled)
        assertFalse(config.scheduler.recurrenceMaintenanceEnabled)
        assertEquals(12L, config.scheduler.recurrenceMaintenanceIntervalHours)
    }

    @Test
    fun `should build jdbc url from database values when no override exists`() {
        val config = AppConfig.fromMap(
            mapOf(
                "DB_HOST" to "postgres",
                "DB_PORT" to "5433",
                "DB_NAME" to "daily_dev",
                "DB_SCHEMA" to "public",
                "DB_SSL" to "false",
            ),
        )

        assertEquals(
            "jdbc:postgresql://postgres:5433/daily_dev?currentSchema=public&ssl=false",
            config.database.jdbcUrl,
        )
    }
}
