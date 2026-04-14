package com.dailyback.shared.errors

import kotlinx.serialization.Serializable

@Serializable
data class ApiErrorResponse(
    val timestamp: String,
    val path: String,
    val errorCode: String,
    val message: String,
    val details: Map<String, String>? = null,
    val traceId: String? = null,
)
