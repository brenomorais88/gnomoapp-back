package com.dailyback.features.security

import com.dailyback.app.bootstrap.module
import com.dailyback.app.config.AppConfig
import com.dailyback.app.config.DatabaseConfig
import com.dailyback.app.config.FlywayConfig
import com.dailyback.app.config.JwtAuthConfig
import com.dailyback.app.config.SchedulerConfig
import com.dailyback.app.config.SeedConfig
import com.dailyback.app.config.SecurityConfig
import com.dailyback.app.config.ServerConfig
import com.dailyback.features.families.application.FamilyPermissionAuthorizer
import com.dailyback.features.families.domain.FamilyPermissionDeniedException
import com.dailyback.features.users.application.JwtTokenService
import com.dailyback.shared.domain.family.FamilyPermissionKey
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AuthorizationSecurityRoutesTest {
    @Test
    fun `unauthenticated user is blocked`() = testApplication {
        application {
            module(
                appConfig = securityTestConfig(),
                runStartup = false,
            )
        }

        val response = client.get("/accounts")

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `family scope requires family permission`() = testApplication {
        application {
            module(
                appConfig = securityTestConfig(),
                familyPermissionAuthorizerOverride = FamilyPermissionAuthorizer { _, permission ->
                    throw FamilyPermissionDeniedException(permission)
                },
                runStartup = false,
            )
        }
        val token = JwtTokenService(securityTestConfig().security.jwt)
            .createAccessToken(UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee"))
            .token
        val authClient = createClient {
            defaultRequest {
                headers.append(HttpHeaders.Authorization, "Bearer $token")
            }
        }

        val response = authClient.get("/accounts?scope=FAMILY")

        assertEquals(HttpStatusCode.Forbidden, response.status)
        assertTrue(response.bodyAsText().contains("FAMILY_PERMISSION_DENIED"))
        assertTrue(response.bodyAsText().contains(FamilyPermissionKey.CAN_VIEW_FAMILY_ACCOUNTS.name))
    }

    @Test
    fun `family scope on occurrences requires family permission`() = testApplication {
        application {
            module(
                appConfig = securityTestConfig(),
                familyPermissionAuthorizerOverride = FamilyPermissionAuthorizer { _, permission ->
                    throw FamilyPermissionDeniedException(permission)
                },
                runStartup = false,
            )
        }
        val token = JwtTokenService(securityTestConfig().security.jwt)
            .createAccessToken(UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee"))
            .token
        val authClient = createClient {
            defaultRequest {
                headers.append(HttpHeaders.Authorization, "Bearer $token")
            }
        }

        val response = authClient.get("/occurrences?scope=FAMILY")

        assertEquals(HttpStatusCode.Forbidden, response.status)
        assertTrue(response.bodyAsText().contains("FAMILY_PERMISSION_DENIED"))
    }

    @Test
    fun `invalid scope returns bad request`() = testApplication {
        application {
            module(
                appConfig = securityTestConfig(),
                runStartup = false,
            )
        }
        val token = JwtTokenService(securityTestConfig().security.jwt)
            .createAccessToken(UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee"))
            .token
        val authClient = createClient {
            defaultRequest {
                headers.append(HttpHeaders.Authorization, "Bearer $token")
            }
        }

        val accountsResponse = authClient.get("/accounts?scope=NOT_VALID")
        val occurrencesResponse = authClient.get("/occurrences?scope=NOT_VALID")

        assertEquals(HttpStatusCode.BadRequest, accountsResponse.status)
        assertTrue(accountsResponse.bodyAsText().contains("INVALID_ACCOUNT_REQUEST"))
        assertEquals(HttpStatusCode.BadRequest, occurrencesResponse.status)
        assertTrue(occurrencesResponse.bodyAsText().contains("INVALID_OCCURRENCE_REQUEST"))
    }
}

private fun securityTestConfig(): AppConfig = AppConfig(
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
        scenarioEnabled = false,
    ),
    scheduler = SchedulerConfig(
        recurrenceMaintenanceEnabled = false,
        recurrenceMaintenanceIntervalHours = 24,
    ),
    security = SecurityConfig(
        jwt = JwtAuthConfig(
            secret = "unit-test-secret-key-for-jwt-hs256-must-be-long-enough",
            issuer = "test",
            audience = "test",
            accessTokenTtlSeconds = 3600,
        ),
    ),
)
