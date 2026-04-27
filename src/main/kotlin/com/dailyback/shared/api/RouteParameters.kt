package com.dailyback.shared.api

import com.dailyback.shared.errors.ApiException
import com.dailyback.shared.errors.ErrorCodes
import io.ktor.http.HttpStatusCode
import java.util.UUID

fun String?.toUuidOrBadRequest(parameterName: String): UUID =
    runCatching { UUID.fromString(this) }.getOrElse {
        throw ApiException(
            statusCode = HttpStatusCode.BadRequest,
            errorCode = ErrorCodes.INVALID_PATH_PARAMETER,
            message = "Path parameter '$parameterName' must be a valid UUID",
        )
    }
