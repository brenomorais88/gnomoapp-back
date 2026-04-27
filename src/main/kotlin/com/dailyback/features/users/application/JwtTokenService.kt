package com.dailyback.features.users.application

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import com.dailyback.app.config.JwtAuthConfig
import java.time.Instant
import java.util.Date
import java.util.UUID

class JwtTokenService(
    jwtAuthConfig: JwtAuthConfig,
) {
    private val algorithm: Algorithm = Algorithm.HMAC256(jwtAuthConfig.secret.toByteArray(Charsets.UTF_8))

    val verifier: JWTVerifier =
        JWT.require(algorithm)
            .withIssuer(jwtAuthConfig.issuer)
            .withAudience(jwtAuthConfig.audience)
            .build()

    private val issuer = jwtAuthConfig.issuer
    private val audience = jwtAuthConfig.audience
    private val ttlSeconds = jwtAuthConfig.accessTokenTtlSeconds

    fun createAccessToken(userId: UUID): IssuedAccessToken {
        val now = Instant.now()
        val expiresAt = now.plusSeconds(ttlSeconds)
        val token = JWT.create()
            .withIssuer(issuer)
            .withAudience(audience)
            .withSubject(userId.toString())
            .withIssuedAt(Date.from(now))
            .withExpiresAt(Date.from(expiresAt))
            .sign(algorithm)
        return IssuedAccessToken(
            token = token,
            expiresInSeconds = ttlSeconds,
        )
    }
}

data class IssuedAccessToken(
    val token: String,
    val expiresInSeconds: Long,
)
