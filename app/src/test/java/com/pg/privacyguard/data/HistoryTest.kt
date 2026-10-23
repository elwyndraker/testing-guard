package com.pg.privacyguard.data

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class HistoryTest {
    @Test
    fun formatAndParseRoundTrip() {
        val counts = mapOf("tracker.test" to 3, History.ADULT to 2)
        assertEquals(counts, History.parse(History.format(counts).lines()))
    }

    @Test
    fun parseSkipsBrokenLines() {
        assertEquals(mapOf("a.test" to 1), History.parse(listOf("1\ta.test", "x\tb.test", "2", "")))
    }

    @Test
    fun mergeAddsCounts() {
        assertEquals(
            mapOf("a" to 3, "b" to 1, "c" to 5),
            History.merge(mapOf("a" to 1, "b" to 1), mapOf("a" to 2, "c" to 5)),
        )
    }

    @Test
    fun summaryTotalsTheDay() {
        val day = History.summarize(LocalDate.of(2026, 10, 23), mapOf("a" to 1, "b" to 4))
        assertEquals(5, day.total)
    }
}
