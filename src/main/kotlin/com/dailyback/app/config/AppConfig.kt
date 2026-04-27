package com.dailyback.app.config

data class AppConfig(
    val server: ServerConfig,
    val database: DatabaseConfig,
    val flyway: FlywayConfig,
    val seed: SeedConfig,
    val scheduler: SchedulerConfig,
    val security: SecurityConfig,
) {
    companion object {
        fun fromEnvironment(): AppConfig = fromMap(System.getenv())

        fun fromMap(environment: Map<String, String>): AppConfig {
            val serverHost = environment["APP_HOST"] ?: "0.0.0.0"
            val serverPort = environment["APP_PORT"]?.toIntOrNull() ?: 8080

            val databaseHost = environment["DB_HOST"] ?: "localhost"
            val databasePort = environment["DB_PORT"]?.toIntOrNull() ?: 5432
            val databaseName = environment["DB_NAME"] ?: "daily"
            val databaseUser = environment["DB_USER"] ?: "daily"
            val databasePassword = environment["DB_PASSWORD"] ?: "daily"
            val databaseSchema = environment["DB_SCHEMA"] ?: "public"
            val databaseSsl = environment["DB_SSL"]?.toBooleanStrictOrNull() ?: false
            val jdbcUrlOverride = environment["DB_JDBC_URL"]

            val migrationEnabled = environment["FLYWAY_ENABLED"]?.toBooleanStrictOrNull() ?: true
            val migrationLocation = environment["FLYWAY_LOCATION"] ?: "classpath:db/migration"
            val seedEnabled = environment["SEED_ENABLED"]?.toBooleanStrictOrNull() ?: true
            val scenarioSeedEnabled = environment["SEED_SCENARIO_ENABLED"]?.toBooleanStrictOrNull() ?: false
            val maintenanceEnabled = environment["RECURRENCE_MAINTENANCE_ENABLED"]?.toBooleanStrictOrNull() ?: true
            val maintenanceIntervalHours = environment["RECURRENCE_MAINTENANCE_INTERVAL_HOURS"]?.toLongOrNull() ?: 24L

            val jwtSecret = environment["JWT_SECRET"]
                ?: "local-dev-only-change-in-production-min-32-chars!!"
            val jwtIssuer = environment["JWT_ISSUER"] ?: "daily-back"
            val jwtAudience = environment["JWT_AUDIENCE"] ?: "daily-clients"
            val jwtAccessTtlSeconds = environment["JWT_ACCESS_TOKEN_TTL_SECONDS"]?.toLongOrNull() ?: 86_400L

            return AppConfig(
                server = ServerConfig(host = serverHost, port = serverPort),
                database = DatabaseConfig(
                    host = databaseHost,
                    port = databasePort,
                    name = databaseName,
                    user = databaseUser,
                    password = databasePassword,
                    schema = databaseSchema,
                    ssl = databaseSsl,
                    jdbcUrlOverride = jdbcUrlOverride,
                ),
                flyway = FlywayConfig(
                    enabled = migrationEnabled,
                    location = migrationLocation,
                ),
                seed = SeedConfig(
                    enabled = seedEnabled,
                    scenarioEnabled = scenarioSeedEnabled,
                ),
                scheduler = SchedulerConfig(
                    recurrenceMaintenanceEnabled = maintenanceEnabled,
                    recurrenceMaintenanceIntervalHours = maintenanceIntervalHours,
                ),
                security = SecurityConfig(
                    jwt = JwtAuthConfig(
                        secret = jwtSecret,
                        issuer = jwtIssuer,
                        audience = jwtAudience,
                        accessTokenTtlSeconds = jwtAccessTtlSeconds,
                    ),
                ),
            )
        }
    }
}

data class SecurityConfig(
    val jwt: JwtAuthConfig,
)

data class JwtAuthConfig(
    val secret: String,
    val issuer: String,
    val audience: String,
    val accessTokenTtlSeconds: Long,
)

data class ServerConfig(
    val host: String,
    val port: Int,
)

data class DatabaseConfig(
    val host: String,
    val port: Int,
    val name: String,
    val user: String,
    val password: String,
    val schema: String,
    val ssl: Boolean,
    val jdbcUrlOverride: String?,
) {
    val jdbcUrl: String
        get() = jdbcUrlOverride ?: buildString {
            append("jdbc:postgresql://")
            append(host)
            append(":")
            append(port)
            append("/")
            append(name)
            append("?currentSchema=")
            append(schema)
            append("&ssl=")
            append(ssl)
        }
}

data class FlywayConfig(
    val enabled: Boolean,
    val location: String,
)

data class SeedConfig(
    val enabled: Boolean,
    val scenarioEnabled: Boolean,
)

data class SchedulerConfig(
    val recurrenceMaintenanceEnabled: Boolean,
    val recurrenceMaintenanceIntervalHours: Long,
)
