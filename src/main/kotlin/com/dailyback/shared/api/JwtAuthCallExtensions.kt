package com.dailyback.shared.api

import com.dailyback.shared.errors.ApiException
import com.dailyback.shared.errors.ErrorCodes
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import java.util.UUID

fun ApplicationCall.requireJwtUserId(): UUID {
    val principal = principal<JWTPrincipal>()
        ?: throw ApiException(
            statusCode = HttpStatusCode.Unauthorized,
            errorCode = ErrorCodes.UNAUTHORIZED,
            message = "Missing or invalid token",
        )
    return runCatching { UUID.fromString(principal.payload.subject) }.getOrElse {
        throw ApiException(
            statusCode = HttpStatusCode.Unauthorized,
            errorCode = ErrorCodes.UNAUTHORIZED,
            message = "Invalid token subject",
        )
    }
}
