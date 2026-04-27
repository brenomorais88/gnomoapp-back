package com.dailyback.features.accounts.api

import com.dailyback.features.accounts.application.ActivateAccountUseCase
import com.dailyback.features.accounts.application.CreateAccountUseCase
import com.dailyback.features.accounts.application.DeactivateAccountUseCase
import com.dailyback.features.accounts.application.DeleteAccountUseCase
import com.dailyback.features.accounts.application.GetAccountByIdUseCase
import com.dailyback.features.accounts.application.ListAccountsUseCase
import com.dailyback.features.accounts.application.UpdateAccountUseCase
import com.dailyback.features.accounts.domain.AccountAccessDeniedException
import com.dailyback.features.accounts.domain.AccountCategoryNotFoundException
import com.dailyback.features.accounts.domain.AccountInvalidAmountException
import com.dailyback.features.accounts.domain.AccountInvalidDateRangeException
import com.dailyback.features.accounts.domain.AccountInvalidOwnershipException
import com.dailyback.features.accounts.domain.AccountInvalidTitleException
import com.dailyback.features.accounts.domain.AccountNotFoundException
import com.dailyback.features.accounts.domain.AccountOwnershipType
import com.dailyback.features.families.application.FamilyPermissionAuthorizer
import com.dailyback.shared.api.requireFamilyPermissionForScope
import com.dailyback.shared.api.readAccountQueryScopeOrDefault
import com.dailyback.shared.api.requireJwtUserId
import com.dailyback.shared.api.toUuidOrBadRequest
import com.dailyback.shared.domain.family.FamilyPermissionKey
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
    familyPermissionAuthorizer: FamilyPermissionAuthorizer,
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
            val userId = call.requireJwtUserId()
            val scope = call.readAccountQueryScopeOrDefault(
                invalidErrorCode = "INVALID_ACCOUNT_REQUEST",
            )
            call.requireFamilyPermissionForScope(
                userId = userId,
                scope = scope,
                authorizer = familyPermissionAuthorizer,
                permission = FamilyPermissionKey.CAN_VIEW_FAMILY_ACCOUNTS,
            )
            call.respond(listAccountsUseCase.execute(userId, scope).map { it.toResponse() })
        }

        get("/{id}") {
            val userId = call.requireJwtUserId()
            val id = call.parameters["id"].toUuidOrBadRequest("id")
            val account = runCatching { getAccountByIdUseCase.execute(userId, id) }
                .getOrElse { throw mapAccountException(it) }
            call.respond(account.toResponse())
        }

        post {
            val userId = call.requireJwtUserId()
            val request = call.receive<CreateAccountRequest>()
            if (request.ownershipType.equals(AccountOwnershipType.FAMILY.name, ignoreCase = true)) {
                familyPermissionAuthorizer.require(userId, FamilyPermissionKey.CAN_CREATE_FAMILY_ACCOUNTS)
            }
            val created = runCatching {
                createAccountUseCase.execute(userId, request.toCreateInput())
            }.getOrElse { throw mapAccountException(it) }
            call.respond(HttpStatusCode.Created, created.toResponse())
        }

        put("/{id}") {
            val userId = call.requireJwtUserId()
            val id = call.parameters["id"].toUuidOrBadRequest("id")
            val request = call.receive<UpsertAccountRequest>()
            val updated = runCatching {
                updateAccountUseCase.execute(userId, id, request.toUpdateInput())
            }.getOrElse { throw mapAccountException(it) }
            call.respond(updated.toResponse())
        }

        delete("/{id}") {
            val userId = call.requireJwtUserId()
            val id = call.parameters["id"].toUuidOrBadRequest("id")
            runCatching { deleteAccountUseCase.execute(userId, id) }
                .getOrElse { throw mapAccountException(it) }
            call.respond(HttpStatusCode.NoContent)
        }

        patch("/{id}/activate") {
            val userId = call.requireJwtUserId()
            val id = call.parameters["id"].toUuidOrBadRequest("id")
            val account = runCatching { activateAccountUseCase.execute(userId, id) }
                .getOrElse { throw mapAccountException(it) }
            call.respond(account.toResponse())
        }

        patch("/{id}/deactivate") {
            val userId = call.requireJwtUserId()
            val id = call.parameters["id"].toUuidOrBadRequest("id")
            val account = runCatching { deactivateAccountUseCase.execute(userId, id) }
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

    is AccountAccessDeniedException -> ApiException(
        statusCode = HttpStatusCode.Forbidden,
        errorCode = "ACCOUNT_ACCESS_DENIED",
        message = cause.message ?: "Access denied",
    )

    is AccountCategoryNotFoundException -> ApiException(
        statusCode = HttpStatusCode.BadRequest,
        errorCode = "CATEGORY_NOT_FOUND",
        message = cause.message ?: "Category not found",
    )

    is AccountInvalidOwnershipException -> ApiException(
        statusCode = HttpStatusCode.BadRequest,
        errorCode = "INVALID_ACCOUNT_OWNERSHIP",
        message = cause.message ?: "Invalid ownership",
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
