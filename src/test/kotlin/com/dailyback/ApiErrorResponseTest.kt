package com.dailyback

import com.dailyback.shared.errors.ApiErrorResponse
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import kotlin.test.assertNull

class ApiErrorResponseTest {

    @Test
    fun `should keep fields values`() {
        val now = "2026-04-14T10:00:00Z"

        val response = ApiErrorResponse(
            timestamp = now,
            path = "/v1/accounts",
            errorCode = "VALIDATION_ERROR",
            message = "Validation failed",
            details = mapOf("categoryId" to "must not be null"),
            traceId = "trace-123",
        )

        assertEquals(now, response.timestamp)
        assertEquals("/v1/accounts", response.path)
        assertEquals("VALIDATION_ERROR", response.errorCode)
        assertEquals("Validation failed", response.message)
        assertEquals("must not be null", response.details?.get("categoryId"))
        assertEquals("trace-123", response.traceId)
    }

    @Test
    fun `should allow optional fields as null`() {
        val response = ApiErrorResponse(
            timestamp = "2026-04-14T10:00:00Z",
            path = "/v1/accounts",
            errorCode = "NOT_FOUND",
            message = "Account not found",
            details = null,
            traceId = null,
        )

        assertNull(response.details)
        assertNull(response.traceId)
    }

    @Test
    fun `should support equality hashcode and toString`() {
        val timestamp = "2026-04-14T10:00:00Z"

        val left = ApiErrorResponse(
            timestamp = timestamp,
            path = "/v1/accounts",
            errorCode = "NOT_FOUND",
            message = "Account not found",
            details = null,
            traceId = null,
        )
        val right = left.copy()
        val different = left.copy(errorCode = "VALIDATION_ERROR")

        assertEquals(left, right)
        assertEquals(left.hashCode(), right.hashCode())
        assertNotEquals(left, different)
        assertTrue(left.toString().contains("NOT_FOUND"))
    }
}
