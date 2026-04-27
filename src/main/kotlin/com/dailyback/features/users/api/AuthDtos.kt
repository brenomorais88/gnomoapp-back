package com.dailyback.features.users.api

import com.dailyback.features.users.application.AuthSession
import com.dailyback.features.users.domain.User
import kotlinx.serialization.Serializable

@Serializable
data class RegisterUserRequest(
    val firstName: String,
    val lastName: String,
    val document: String,
    val birthDate: String,
    val password: String,
    val email: String? = null,
    val phone: String? = null,
)

@Serializable
data class LoginRequest(
    val login: String,
    val password: String,
)

@Serializable
data class AuthUserResponse(
    val id: String,
    val firstName: String,
    val lastName: String,
    val document: String,
    val birthDate: String,
    val phone: String? = null,
    val email: String? = null,
    val status: String,
    val createdAt: String,
    val updatedAt: String,
    val lastLoginAt: String? = null,
)

@Serializable
data class AuthTokenResponse(
    val accessToken: String,
    val tokenType: String = "Bearer",
    val expiresInSeconds: Long,
    val user: AuthUserResponse,
)

fun User.toAuthUserResponse(): AuthUserResponse = AuthUserResponse(
    id = id.toString(),
    firstName = firstName,
    lastName = lastName,
    document = document,
    birthDate = birthDate.toString(),
    phone = phone,
    email = email,
    status = status.name,
    createdAt = createdAt.toString(),
    updatedAt = updatedAt.toString(),
    lastLoginAt = lastLoginAt?.toString(),
)

fun AuthSession.toAuthTokenResponse(): AuthTokenResponse = AuthTokenResponse(
    accessToken = accessToken,
    expiresInSeconds = expiresInSeconds,
    user = user.toAuthUserResponse(),
)
