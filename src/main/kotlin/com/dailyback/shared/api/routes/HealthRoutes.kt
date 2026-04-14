package com.dailyback.shared.api.routes

import com.dailyback.shared.application.health.GetHealthStatusUseCase
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import kotlinx.serialization.Serializable

fun Route.healthRoutes(getHealthStatusUseCase: GetHealthStatusUseCase) {
    get("/health") {
        val health = getHealthStatusUseCase.execute()
        val statusCode = if (health.databaseStatus == "UP") {
            HttpStatusCode.OK
        } else {
            HttpStatusCode.ServiceUnavailable
        }

        call.respond(
            status = statusCode,
            message = HealthResponse(
                status = health.status,
                checks = mapOf("database" to health.databaseStatus),
                timestamp = health.timestamp.toString(),
            ),
        )
    }
}

@Serializable
data class HealthResponse(
    val status: String,
    val checks: Map<String, String>,
    val timestamp: String,
)
