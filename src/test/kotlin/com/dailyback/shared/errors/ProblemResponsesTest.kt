package com.dailyback.shared.errors

import io.ktor.http.HttpStatusCode
import kotlin.test.Test
import kotlin.test.assertEquals

class ProblemResponsesTest {

    @Test
    fun `should build validation error`() {
        val ex = validationError("bad", mapOf("f" to "x"))
        assertEquals(HttpStatusCode.BadRequest, ex.statusCode)
        assertEquals(ErrorCodes.VALIDATION_ERROR, ex.errorCode)
        assertEquals("bad", ex.message)
        assertEquals("x", ex.details?.get("f"))
    }

    @Test
    fun `should build not found`() {
        val ex = notFound("missing")
        assertEquals(HttpStatusCode.NotFound, ex.statusCode)
        assertEquals(ErrorCodes.NOT_FOUND, ex.errorCode)
    }

    @Test
    fun `should build conflict`() {
        val ex = conflict("dup")
        assertEquals(HttpStatusCode.Conflict, ex.statusCode)
        assertEquals(ErrorCodes.CONFLICT, ex.errorCode)
    }

    @Test
    fun `should build forbidden`() {
        val ex = forbidden("nope")
        assertEquals(HttpStatusCode.Forbidden, ex.statusCode)
        assertEquals(ErrorCodes.FORBIDDEN, ex.errorCode)
    }

    @Test
    fun `should build unauthorized`() {
        val ex = unauthorized("sign in")
        assertEquals(HttpStatusCode.Unauthorized, ex.statusCode)
        assertEquals(ErrorCodes.UNAUTHORIZED, ex.errorCode)
    }
}
