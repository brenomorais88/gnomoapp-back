package com.dailyback.features.users.application

import com.auth0.jwt.JWT
import com.dailyback.app.config.JwtAuthConfig
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class JwtTokenServiceTest {

    private val config = JwtAuthConfig(
        secret = "unit-test-secret-key-for-jwt-hs256-must-be-long-enough",
        issuer = "test-issuer",
        audience = "test-audience",
        accessTokenTtlSeconds = 120,
    )

    private val service = JwtTokenService(config)

    @Test
    fun `should issue token with subject user id`() {
        val userId = UUID.randomUUID()
        val issued = service.createAccessToken(userId)
        val decoded = JWT.decode(issued.token)
        assertEquals(userId.toString(), decoded.subject)
        assertEquals("test-issuer", decoded.issuer)
        assertEquals(listOf("test-audience"), decoded.audience)
        assertEquals(120L, issued.expiresInSeconds)
    }

    @Test
    fun `should verify issued token`() {
        val userId = UUID.randomUUID()
        val token = service.createAccessToken(userId).token
        val verified = service.verifier.verify(token)
        assertEquals(userId.toString(), verified.subject)
    }

    @Test
    fun `should reject tampered token`() {
        val token = service.createAccessToken(UUID.randomUUID()).token
        val tampered = token.dropLast(3) + "xxx"
        assertFailsWith<Exception> {
            service.verifier.verify(tampered)
        }
    }
}
