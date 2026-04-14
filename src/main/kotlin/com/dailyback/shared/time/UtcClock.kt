package com.dailyback.shared.time

import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

class UtcClock(
    private val clock: Clock = Clock.systemUTC(),
) {
    fun nowInstant(): Instant = Instant.now(clock)

    fun today(): LocalDate = LocalDate.now(clock)

    fun zoneOffset(): ZoneOffset = ZoneOffset.UTC
}
