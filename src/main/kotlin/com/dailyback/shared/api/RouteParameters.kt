package com.dailyback.shared.api

import com.dailyback.shared.errors.ApiException
import io.ktor.http.HttpStatusCode
import java.util.UUID

fun String?.toUuidOrBadRequest(parameterName: String): UUID =
    runCatching { UUID.fromString(this) }.getOrElse {
        throw ApiException(
            statusCode = HttpStatusCode.BadRequest,
            errorCode = "INVALID_PATH_PARAMETER",
            message = "Path parameter '$parameterName' must be a valid UUID",
        )
    }
