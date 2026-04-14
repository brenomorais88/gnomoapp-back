package com.dailyback.features.accounts.api

import com.dailyback.features.accounts.application.ActivateAccountUseCase
import com.dailyback.features.accounts.application.CreateAccountUseCase
import com.dailyback.features.accounts.application.DeactivateAccountUseCase
import com.dailyback.features.accounts.application.DeleteAccountUseCase
import com.dailyback.features.accounts.application.GetAccountByIdUseCase
import com.dailyback.features.accounts.application.ListAccountsUseCase
import com.dailyback.features.accounts.application.UpdateAccountUseCase
import com.dailyback.features.accounts.domain.AccountCategoryNotFoundException
import com.dailyback.features.accounts.domain.AccountInvalidAmountException
import com.dailyback.features.accounts.domain.AccountInvalidDateRangeException
import com.dailyback.features.accounts.domain.AccountInvalidTitleException
import com.dailyback.features.accounts.domain.AccountNotFoundException
import com.dailyback.shared.api.toUuidOrBadRequest
import com.dailyback.shared.errors.ApiException
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route

fun Route.accountRoutes(
    listAccountsUseCase: ListAccountsUseCase,
    getAccountByIdUseCase: GetAccountByIdUseCase,
    createAccountUseCase: CreateAccountUseCase,
    updateAccountUseCase: UpdateAccountUseCase,
    deleteAccountUseCase: DeleteAccountUseCase,
    activateAccountUseCase: ActivateAccountUseCase,
    deactivateAccountUseCase: DeactivateAccountUseCase,
) {
    route("/accounts") {
        get {
            call.respond(listAccountsUseCase.execute().map { it.toResponse() })
        }

        get("/{id}") {
            val id = call.parameters["id"].toUuidOrBadRequest("id")
            val account = runCatching { getAccountByIdUseCase.execute(id) }
                .getOrElse { throw mapAccountException(it) }
            call.respond(account.toResponse())
        }

        post {
            val request = call.receive<UpsertAccountRequest>()
            val created = runCatching {
                createAccountUseCase.execute(request.toInput())
            }.getOrElse { throw mapAccountException(it) }
            call.respond(HttpStatusCode.Created, created.toResponse())
        }

        put("/{id}") {
            val id = call.parameters["id"].toUuidOrBadRequest("id")
            val request = call.receive<UpsertAccountRequest>()
            val updated = runCatching {
                updateAccountUseCase.execute(id, request.toInput())
            }.getOrElse { throw mapAccountException(it) }
            call.respond(updated.toResponse())
        }

        delete("/{id}") {
            val id = call.parameters["id"].toUuidOrBadRequest("id")
            runCatching { deleteAccountUseCase.execute(id) }
                .getOrElse { throw mapAccountException(it) }
            call.respond(HttpStatusCode.NoContent)
        }

        patch("/{id}/activate") {
            val id = call.parameters["id"].toUuidOrBadRequest("id")
            val account = runCatching { activateAccountUseCase.execute(id) }
                .getOrElse { throw mapAccountException(it) }
            call.respond(account.toResponse())
        }

        patch("/{id}/deactivate") {
            val id = call.parameters["id"].toUuidOrBadRequest("id")
            val account = runCatching { deactivateAccountUseCase.execute(id) }
                .getOrElse { throw mapAccountException(it) }
            call.respond(account.toResponse())
        }
    }
}

private fun mapAccountException(cause: Throwable): Throwable = when (cause) {
    is AccountNotFoundException -> ApiException(
        statusCode = HttpStatusCode.NotFound,
        errorCode = "ACCOUNT_NOT_FOUND",
        message = cause.message ?: "Account not found",
    )

    is AccountCategoryNotFoundException -> ApiException(
        statusCode = HttpStatusCode.BadRequest,
        errorCode = "CATEGORY_NOT_FOUND",
        message = cause.message ?: "Category not found",
    )

    is AccountInvalidTitleException -> ApiException(
        statusCode = HttpStatusCode.BadRequest,
        errorCode = "INVALID_ACCOUNT_TITLE",
        message = cause.message ?: "Invalid account title",
    )

    is AccountInvalidAmountException -> ApiException(
        statusCode = HttpStatusCode.BadRequest,
        errorCode = "INVALID_ACCOUNT_AMOUNT",
        message = cause.message ?: "Invalid account amount",
    )

    is AccountInvalidDateRangeException -> ApiException(
        statusCode = HttpStatusCode.BadRequest,
        errorCode = "INVALID_ACCOUNT_DATE_RANGE",
        message = cause.message ?: "Invalid account date range",
    )

    is NumberFormatException, is IllegalArgumentException -> ApiException(
        statusCode = HttpStatusCode.BadRequest,
        errorCode = "INVALID_ACCOUNT_REQUEST",
        message = "Request body contains invalid values",
    )

    else -> cause
}
