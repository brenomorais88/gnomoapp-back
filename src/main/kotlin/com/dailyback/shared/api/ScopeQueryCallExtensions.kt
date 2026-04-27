package com.dailyback.shared.api

import com.dailyback.features.accounts.application.AccountQueryScope
import com.dailyback.shared.errors.ApiException
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall

fun ApplicationCall.readAccountQueryScopeOrDefault(
    paramName: String = "scope",
    default: AccountQueryScope = AccountQueryScope.VISIBLE_TO_ME,
    invalidErrorCode: String,
): AccountQueryScope {
    val raw = request.queryParameters[paramName] ?: return default
    return runCatching { AccountQueryScope.fromValue(raw) }.getOrElse {
        throw ApiException(
            statusCode = HttpStatusCode.BadRequest,
            errorCode = invalidErrorCode,
            message = "Invalid query parameter '$paramName'",
        )
    }
}
