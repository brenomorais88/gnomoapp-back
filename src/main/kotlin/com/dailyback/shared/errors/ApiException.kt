package com.dailyback.shared.errors

import io.ktor.http.HttpStatusCode

class ApiException(
    val statusCode: HttpStatusCode,
    val errorCode: String,
    override val message: String,
    val details: Map<String, String>? = null,
) : RuntimeException(message)
