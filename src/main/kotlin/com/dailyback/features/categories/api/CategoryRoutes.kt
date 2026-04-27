package com.dailyback.features.categories.api

import com.dailyback.features.categories.application.CreateCategoryUseCase
import com.dailyback.features.categories.application.DeleteCategoryUseCase
import com.dailyback.features.categories.application.GetCategoryByIdUseCase
import com.dailyback.features.categories.application.ListCategoriesUseCase
import com.dailyback.features.categories.application.UpdateCategoryUseCase
import com.dailyback.features.categories.domain.CategoryInUseException
import com.dailyback.features.categories.domain.CategoryNotFoundException
import com.dailyback.features.categories.domain.CategoryNotModifiableException
import com.dailyback.features.categories.domain.DuplicateCategoryNameException
import com.dailyback.features.categories.domain.InvalidCategoryNameException
import com.dailyback.features.accounts.application.AccountAccessContextResolver
import com.dailyback.features.families.application.FamilyPermissionAuthorizer
import com.dailyback.shared.api.requireJwtUserIdIfFamilyRequiresPermission
import com.dailyback.shared.api.toUuidOrBadRequest
import com.dailyback.shared.domain.family.FamilyPermissionKey
import com.dailyback.shared.errors.ApiException
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route

fun Route.categoryRoutes(
    familyPermissionAuthorizer: FamilyPermissionAuthorizer,
    accountAccessContextResolver: AccountAccessContextResolver,
    listCategoriesUseCase: ListCategoriesUseCase,
    getCategoryByIdUseCase: GetCategoryByIdUseCase,
    createCategoryUseCase: CreateCategoryUseCase,
    updateCategoryUseCase: UpdateCategoryUseCase,
    deleteCategoryUseCase: DeleteCategoryUseCase,
) {
    route("/categories") {
        get {
            val userId = call.requireJwtUserIdIfFamilyRequiresPermission(
                accountAccess = accountAccessContextResolver,
                authorizer = familyPermissionAuthorizer,
                permission = FamilyPermissionKey.CAN_VIEW_FAMILY_ACCOUNTS,
            )
            val response = listCategoriesUseCase.execute(userId).map { it.toResponse() }
            call.respond(response)
        }

        get("/{id}") {
            val userId = call.requireJwtUserIdIfFamilyRequiresPermission(
                accountAccess = accountAccessContextResolver,
                authorizer = familyPermissionAuthorizer,
                permission = FamilyPermissionKey.CAN_VIEW_FAMILY_ACCOUNTS,
            )
            val id = call.parameters["id"].toUuidOrBadRequest("id")
            val category = runCatching { getCategoryByIdUseCase.execute(userId, id) }
                .getOrElse { throw mapCategoryException(it) }
            call.respond(category.toResponse())
        }

        post {
            val userId = call.requireJwtUserIdIfFamilyRequiresPermission(
                accountAccess = accountAccessContextResolver,
                authorizer = familyPermissionAuthorizer,
                permission = FamilyPermissionKey.CAN_MANAGE_CATEGORIES,
            )
            val request = call.receive<CreateCategoryRequest>()
            val category = runCatching {
                createCategoryUseCase.execute(userId, request.name, request.color)
            }.getOrElse { throw mapCategoryException(it) }
            call.respond(HttpStatusCode.Created, category.toResponse())
        }

        put("/{id}") {
            val userId = call.requireJwtUserIdIfFamilyRequiresPermission(
                accountAccess = accountAccessContextResolver,
                authorizer = familyPermissionAuthorizer,
                permission = FamilyPermissionKey.CAN_MANAGE_CATEGORIES,
            )
            val id = call.parameters["id"].toUuidOrBadRequest("id")
            val request = call.receive<UpdateCategoryRequest>()
            val category = runCatching {
                updateCategoryUseCase.execute(userId, id, request.name, request.color)
            }.getOrElse { throw mapCategoryException(it) }
            call.respond(category.toResponse())
        }

        delete("/{id}") {
            val userId = call.requireJwtUserIdIfFamilyRequiresPermission(
                accountAccess = accountAccessContextResolver,
                authorizer = familyPermissionAuthorizer,
                permission = FamilyPermissionKey.CAN_MANAGE_CATEGORIES,
            )
            val id = call.parameters["id"].toUuidOrBadRequest("id")
            runCatching { deleteCategoryUseCase.execute(userId, id) }
                .getOrElse { throw mapCategoryException(it) }
            call.respond(HttpStatusCode.NoContent)
        }
    }
}

private fun mapCategoryException(cause: Throwable): Throwable = when (cause) {
    is CategoryNotFoundException -> ApiException(
        statusCode = HttpStatusCode.NotFound,
        errorCode = "CATEGORY_NOT_FOUND",
        message = cause.message ?: "Category not found",
    )

    is DuplicateCategoryNameException -> ApiException(
        statusCode = HttpStatusCode.Conflict,
        errorCode = "CATEGORY_NAME_ALREADY_EXISTS",
        message = cause.message ?: "Category name already exists",
    )

    is CategoryInUseException -> ApiException(
        statusCode = HttpStatusCode.Conflict,
        errorCode = "CATEGORY_IN_USE",
        message = cause.message ?: "Category is in use",
    )

    is InvalidCategoryNameException -> ApiException(
        statusCode = HttpStatusCode.BadRequest,
        errorCode = "INVALID_CATEGORY_NAME",
        message = cause.message ?: "Invalid category name",
    )

    is CategoryNotModifiableException -> ApiException(
        statusCode = HttpStatusCode.Forbidden,
        errorCode = "CATEGORY_NOT_MODIFIABLE",
        message = cause.message ?: "Category cannot be modified",
    )

    else -> cause
}
