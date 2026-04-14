package com.dailyback.features.accountoccurrences.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class OccurrenceStatusTest {

    @Test
    fun `should map valid status values`() {
        assertEquals(OccurrenceStatus.PENDING, OccurrenceStatus.fromValue("pending"))
        assertEquals(OccurrenceStatus.PAID, OccurrenceStatus.fromValue("PAID"))
        assertEquals(OccurrenceStatus.PENDING, OccurrenceStatus.fromValue(" pending "))
    }

    @Test
    fun `should fail on unsupported status`() {
        assertFailsWith<IllegalArgumentException> {
            OccurrenceStatus.fromValue("CANCELED")
        }
    }
}
