package com.dailyback.features.users.application

interface PasswordHasher {
    fun hash(rawPassword: String): String

    fun verify(rawPassword: String, storedHash: String): Boolean
}
