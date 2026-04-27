package com.dailyback.shared.errors

import io.ktor.http.HttpStatusCode

fun validationError(
    message: String,
    details: Map<String, String>? = null,
): ApiException =
    ApiException(
        statusCode = HttpStatusCode.BadRequest,
        errorCode = ErrorCodes.VALIDATION_ERROR,
        message = message,
        details = details,
    )

fun notFound(
    message: String,
    details: Map<String, String>? = null,
): ApiException =
    ApiException(
        statusCode = HttpStatusCode.NotFound,
        errorCode = ErrorCodes.NOT_FOUND,
        message = message,
        details = details,
    )

fun conflict(
    message: String,
    details: Map<String, String>? = null,
): ApiException =
    ApiException(
        statusCode = HttpStatusCode.Conflict,
        errorCode = ErrorCodes.CONFLICT,
        message = message,
        details = details,
    )

fun forbidden(
    message: String,
    details: Map<String, String>? = null,
): ApiException =
    ApiException(
        statusCode = HttpStatusCode.Forbidden,
        errorCode = ErrorCodes.FORBIDDEN,
        message = message,
        details = details,
    )

fun unauthorized(
    message: String,
    details: Map<String, String>? = null,
): ApiException =
    ApiException(
        statusCode = HttpStatusCode.Unauthorized,
        errorCode = ErrorCodes.UNAUTHORIZED,
        message = message,
        details = details,
    )
