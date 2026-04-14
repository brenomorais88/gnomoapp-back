package com.dailyback.features.accounts.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class RecurrenceTypeTest {

    @Test
    fun `should map valid recurrence type values`() {
        assertEquals(RecurrenceType.UNIQUE, RecurrenceType.fromValue("unique"))
        assertEquals(RecurrenceType.DAILY, RecurrenceType.fromValue("DAILY"))
        assertEquals(RecurrenceType.WEEKLY, RecurrenceType.fromValue(" weekly "))
        assertEquals(RecurrenceType.MONTHLY, RecurrenceType.fromValue("MONTHLY"))
    }

    @Test
    fun `should fail on unsupported recurrence type`() {
        assertFailsWith<IllegalArgumentException> {
            RecurrenceType.fromValue("YEARLY")
        }
    }
}
