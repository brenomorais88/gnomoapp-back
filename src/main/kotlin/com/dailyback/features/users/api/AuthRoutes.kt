package com.dailyback.features.users.api

import com.dailyback.features.users.application.UserAuthService
import com.dailyback.features.users.domain.DuplicateUserDocumentException
import com.dailyback.features.users.domain.DuplicateUserEmailException
import com.dailyback.features.users.domain.DuplicateUserPhoneException
import com.dailyback.features.users.domain.InvalidCredentialsException
import com.dailyback.features.users.domain.InvalidUserRegistrationException
import com.dailyback.features.users.domain.UserDisabledException
import com.dailyback.features.users.domain.UserNotFoundException
import com.dailyback.shared.api.requireJwtUserId
import com.dailyback.shared.errors.ApiException
import com.dailyback.shared.errors.ErrorCodes
import com.dailyback.shared.errors.validationError
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import java.time.LocalDate

fun Route.authRoutes(
    userAuthService: UserAuthService,
) {
    route("/auth") {
        post("/register") {
            val request = call.receive<RegisterUserRequest>()
            val birthDate = runCatching { LocalDate.parse(request.birthDate.trim()) }
                .getOrElse {
                    throw validationError(
                        message = "Invalid birthDate",
                        details = mapOf("birthDate" to "must be a valid ISO date (yyyy-MM-dd)"),
                    )
                }
            val session = runCatching {
                userAuthService.register(
                    firstName = request.firstName,
                    lastName = request.lastName,
                    rawDocument = request.document,
                    birthDate = birthDate,
                    plainPassword = request.password,
                    rawEmail = request.email,
                    rawPhone = request.phone,
                )
            }.getOrElse { throw mapAuthDomainException(it) }
            call.respond(HttpStatusCode.Created, session.toAuthTokenResponse())
        }

        post("/login") {
            val request = call.receive<LoginRequest>()
            val session = runCatching {
                userAuthService.login(
                    rawLogin = request.login,
                    plainPassword = request.password,
                )
            }.getOrElse { throw mapAuthDomainException(it) }
            call.respond(session.toAuthTokenResponse())
        }

        authenticate("auth-jwt") {
            get("/me") {
                val userId = call.requireJwtUserId()
                val user = runCatching { userAuthService.getAuthenticatedUser(userId) }
                    .getOrElse { throw mapAuthDomainException(it) }
                call.respond(user.toAuthUserResponse())
            }
        }
    }
}

private fun mapAuthDomainException(cause: Throwable): Throwable = when (cause) {
    is DuplicateUserDocumentException -> ApiException(
        statusCode = HttpStatusCode.Conflict,
        errorCode = ErrorCodes.DUPLICATE_USER_DOCUMENT,
        message = cause.message ?: "Document already registered",
    )

    is DuplicateUserEmailException -> ApiException(
        statusCode = HttpStatusCode.Conflict,
        errorCode = ErrorCodes.DUPLICATE_USER_EMAIL,
        message = cause.message ?: "Email already registered",
    )

    is DuplicateUserPhoneException -> ApiException(
        statusCode = HttpStatusCode.Conflict,
        errorCode = ErrorCodes.DUPLICATE_USER_PHONE,
        message = cause.message ?: "Phone already registered",
    )

    is InvalidUserRegistrationException -> validationError(
        message = "Invalid registration",
        details = cause.details,
    )

    is InvalidCredentialsException -> ApiException(
        statusCode = HttpStatusCode.Unauthorized,
        errorCode = ErrorCodes.INVALID_CREDENTIALS,
        message = cause.message ?: "Invalid credentials",
    )

    is UserDisabledException -> ApiException(
        statusCode = HttpStatusCode.Forbidden,
        errorCode = ErrorCodes.USER_DISABLED,
        message = cause.message ?: "User is disabled",
    )

    is UserNotFoundException -> ApiException(
        statusCode = HttpStatusCode.NotFound,
        errorCode = ErrorCodes.USER_NOT_FOUND,
        message = cause.message ?: "User not found",
    )

    else -> cause
}
