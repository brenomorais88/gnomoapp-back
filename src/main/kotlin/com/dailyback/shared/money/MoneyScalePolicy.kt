package com.dailyback.shared.money

import java.math.BigDecimal
import java.math.RoundingMode

object MoneyScalePolicy {
    private const val DEFAULT_SCALE = 2

    fun normalize(amount: BigDecimal): BigDecimal =
        amount.setScale(DEFAULT_SCALE, RoundingMode.HALF_UP)

    fun isValid(amount: BigDecimal): Boolean = normalize(amount) >= BigDecimal.ZERO
}
