package com.dailyback.shared.money

import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MoneyScalePolicyTest {

    @Test
    fun `should normalize value with scale 2`() {
        val normalized = MoneyScalePolicy.normalize(BigDecimal("10.125"))

        assertEquals(BigDecimal("10.13"), normalized)
    }

    @Test
    fun `should accept zero and positive values`() {
        assertTrue(MoneyScalePolicy.isValid(BigDecimal("0")))
        assertTrue(MoneyScalePolicy.isValid(BigDecimal("10.00")))
    }

    @Test
    fun `should reject negative values`() {
        assertFalse(MoneyScalePolicy.isValid(BigDecimal("-0.01")))
    }
}
