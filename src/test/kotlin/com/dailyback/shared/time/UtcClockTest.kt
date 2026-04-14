package com.dailyback.shared.time

import kotlin.test.Test
import kotlin.test.assertEquals
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

class UtcClockTest {

    @Test
    fun `should provide instant and date in utc`() {
        val instant = Instant.parse("2026-04-14T10:00:00Z")
        val fixedClock = Clock.fixed(instant, ZoneOffset.UTC)
        val utcClock = UtcClock(fixedClock)

        assertEquals(instant, utcClock.nowInstant())
        assertEquals(LocalDate.parse("2026-04-14"), utcClock.today())
        assertEquals(ZoneOffset.UTC, utcClock.zoneOffset())
    }
}
