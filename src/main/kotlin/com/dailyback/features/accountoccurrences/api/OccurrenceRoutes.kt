package com.dailyback.features.accountoccurrences.api

import com.dailyback.features.accountoccurrences.application.GetOccurrenceByIdUseCase
import com.dailyback.features.accountoccurrences.application.ListOccurrencesUseCase
import com.dailyback.features.accountoccurrences.application.MarkOccurrencePaidUseCase
import com.dailyback.features.accountoccurrences.application.OccurrenceFilters
import com.dailyback.features.accountoccurrences.application.OverrideOccurrenceAmountUseCase
import com.dailyback.features.accountoccurrences.application.UnmarkOccurrencePaidUseCase
import com.dailyback.features.accountoccurrences.domain.InvalidOccurrenceAmountException
import com.dailyback.features.accountoccurrences.domain.OccurrenceNotFoundException
import com.dailyback.features.accountoccurrences.domain.OccurrenceStatus
import com.dailyback.shared.api.toUuidOrBadRequest
import com.dailyback.shared.errors.ApiException
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.route
import java.time.LocalDate
import java.util.UUID

fun Route.occurrenceRoutes(
    listOccurrencesUseCase: ListOccurrencesUseCase,
    getOccurrenceByIdUseCase: GetOccurrenceByIdUseCase,
    markOccurrencePaidUseCase: MarkOccurrencePaidUseCase,
    unmarkOccurrencePaidUseCase: UnmarkOccurrencePaidUseCase,
    overrideOccurrenceAmountUseCase: OverrideOccurrenceAmountUseCase,
) {
    route("/occurrences") {
        get {
            val filters = OccurrenceFilters(
                status = call.request.queryParameters["status"]?.let(OccurrenceStatus::fromValue),
                categoryId = call.request.queryParameters["categoryId"]?.let(UUID::fromString),
                text = call.request.queryParameters["text"],
                startDate = call.request.queryParameters["startDate"]?.let(LocalDate::parse),
                endDate = call.request.queryParameters["endDate"]?.let(LocalDate::parse),
                month = call.request.queryParameters["month"],
            )
            val list = runCatching { listOccurrencesUseCase.execute(filters) }
                .getOrElse { throw mapOccurrenceException(it) }
            call.respond(list.map { it.toResponse() })
        }

        get("/{id}") {
            val id = call.parameters["id"].toUuidOrBadRequest("id")
            val occurrence = runCatching { getOccurrenceByIdUseCase.execute(id) }
                .getOrElse { throw mapOccurrenceException(it) }
            call.respond(occurrence.toResponse())
        }

        patch("/{id}/mark-paid") {
            val id = call.parameters["id"].toUuidOrBadRequest("id")
            val occurrence = runCatching { markOccurrencePaidUseCase.execute(id) }
                .getOrElse { throw mapOccurrenceException(it) }
            call.respond(occurrence.toResponse())
        }

        patch("/{id}/unmark-paid") {
            val id = call.parameters["id"].toUuidOrBadRequest("id")
            val occurrence = runCatching { unmarkOccurrencePaidUseCase.execute(id) }
                .getOrElse { throw mapOccurrenceException(it) }
            call.respond(occurrence.toResponse())
        }

        patch("/{id}/override-amount") {
            val id = call.parameters["id"].toUuidOrBadRequest("id")
            val body = call.receive<OverrideOccurrenceAmountRequest>()
            val occurrence = runCatching {
                overrideOccurrenceAmountUseCase.execute(id, body.amountAsBigDecimal())
            }.getOrElse { throw mapOccurrenceException(it) }
            call.respond(occurrence.toResponse())
        }
    }
}

private fun mapOccurrenceException(cause: Throwable): Throwable = when (cause) {
    is OccurrenceNotFoundException -> ApiException(
        statusCode = HttpStatusCode.NotFound,
        errorCode = "OCCURRENCE_NOT_FOUND",
        message = cause.message ?: "Occurrence not found",
    )

    is InvalidOccurrenceAmountException -> ApiException(
        statusCode = HttpStatusCode.BadRequest,
        errorCode = "INVALID_OCCURRENCE_AMOUNT",
        message = cause.message ?: "Invalid amount",
    )

    is IllegalArgumentException, is NumberFormatException -> ApiException(
        statusCode = HttpStatusCode.BadRequest,
        errorCode = "INVALID_OCCURRENCE_REQUEST",
        message = "Request contains invalid values",
    )

    else -> cause
}
