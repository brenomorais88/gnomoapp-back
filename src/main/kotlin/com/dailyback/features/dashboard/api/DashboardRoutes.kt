package com.dailyback.features.dashboard.api

import com.dailyback.features.accounts.application.AccountAccessContextResolver
import com.dailyback.features.dashboard.application.GetDashboardCategorySummaryUseCase
import com.dailyback.features.families.application.FamilyPermissionAuthorizer
import com.dailyback.features.dashboard.application.GetDashboardDayDetailsUseCase
import com.dailyback.features.dashboard.application.GetDashboardHomeSummaryUseCase
import com.dailyback.features.dashboard.application.GetDashboardNext12MonthsProjectionUseCase
import com.dailyback.shared.api.requireJwtUserIdIfFamilyRequiresPermission
import com.dailyback.shared.domain.family.FamilyPermissionKey
import com.dailyback.shared.errors.ApiException
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import java.time.LocalDate

fun Route.dashboardRoutes(
    familyPermissionAuthorizer: FamilyPermissionAuthorizer,
    accountAccessContextResolver: AccountAccessContextResolver,
    getDashboardHomeSummaryUseCase: GetDashboardHomeSummaryUseCase,
    getDashboardDayDetailsUseCase: GetDashboardDayDetailsUseCase,
    getDashboardNext12MonthsProjectionUseCase: GetDashboardNext12MonthsProjectionUseCase,
    getDashboardCategorySummaryUseCase: GetDashboardCategorySummaryUseCase,
) {
    route("/dashboard") {
        get("/home") {
            val userId = call.requireJwtUserIdIfFamilyRequiresPermission(
                accountAccess = accountAccessContextResolver,
                authorizer = familyPermissionAuthorizer,
                permission = FamilyPermissionKey.CAN_VIEW_FAMILY_ACCOUNTS,
            )
            val month = call.request.queryParameters["month"]
            val response = runCatching { getDashboardHomeSummaryUseCase.execute(userId, month) }
                .getOrElse { throw mapDashboardError(it) }
            call.respond(response.toResponse())
        }

        get("/day") {
            val userId = call.requireJwtUserIdIfFamilyRequiresPermission(
                accountAccess = accountAccessContextResolver,
                authorizer = familyPermissionAuthorizer,
                permission = FamilyPermissionKey.CAN_VIEW_FAMILY_ACCOUNTS,
            )
            val dateParam = call.request.queryParameters["date"] ?: throw ApiException(
                statusCode = HttpStatusCode.BadRequest,
                errorCode = "MISSING_QUERY_PARAMETER",
                message = "Query parameter 'date' is required",
            )
            val response = runCatching {
                getDashboardDayDetailsUseCase.execute(userId, LocalDate.parse(dateParam))
            }
                .getOrElse { throw mapDashboardError(it) }
            call.respond(response.map { it.toResponse() })
        }

        get("/next-12-months") {
            val userId = call.requireJwtUserIdIfFamilyRequiresPermission(
                accountAccess = accountAccessContextResolver,
                authorizer = familyPermissionAuthorizer,
                permission = FamilyPermissionKey.CAN_VIEW_FAMILY_ACCOUNTS,
            )
            val includeDetails = call.request.queryParameters["includeDetails"]?.toBooleanStrictOrNull() ?: false
            val response = runCatching {
                getDashboardNext12MonthsProjectionUseCase.execute(userId, includeDetails)
            }
                .getOrElse { throw mapDashboardError(it) }
            call.respond(response.map { it.toResponse() })
        }

        get("/category-summary") {
            val userId = call.requireJwtUserIdIfFamilyRequiresPermission(
                accountAccess = accountAccessContextResolver,
                authorizer = familyPermissionAuthorizer,
                permission = FamilyPermissionKey.CAN_VIEW_FAMILY_ACCOUNTS,
            )
            val month = call.request.queryParameters["month"]
            val response = runCatching { getDashboardCategorySummaryUseCase.execute(userId, month) }
                .getOrElse { throw mapDashboardError(it) }
            call.respond(response.map { it.toResponse() })
        }
    }
}

private fun mapDashboardError(cause: Throwable): Throwable = when (cause) {
    is IllegalArgumentException -> ApiException(
        statusCode = HttpStatusCode.BadRequest,
        errorCode = "INVALID_DASHBOARD_QUERY",
        message = "Invalid dashboard query parameters",
    )

    else -> cause
}
