package com.dailyback.features.users.infrastructure

import com.dailyback.features.users.application.PasswordHasher
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder

class BcryptPasswordHasher(
    strength: Int = 12,
) : PasswordHasher {
    private val encoder = BCryptPasswordEncoder(strength)

    override fun hash(rawPassword: String): String = encoder.encode(rawPassword)

    override fun verify(rawPassword: String, storedHash: String): Boolean =
        encoder.matches(rawPassword, storedHash)
}
