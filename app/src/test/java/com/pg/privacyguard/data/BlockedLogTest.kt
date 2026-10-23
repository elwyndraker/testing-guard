package com.pg.privacyguard.data

import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class BlockedLogTest {

    @Before
    fun reset() = BlockedLog.clear()

    @Test
    fun repeatsWithinAMinuteShareOneRow() {
        BlockedLog.add("telemetry.example.com", now = 1_000)
        BlockedLog.add("tracker.test", now = 2_000)
        BlockedLog.add("telemetry.example.com", now = 3_000)

        val entries = BlockedLog.entries.value
        assertEquals(listOf("telemetry.example.com", "tracker.test"), entries.map { it.domain })
        assertEquals(2, entries.first().count)
    }

    @Test
    fun repeatsAfterAMinuteGetANewRow() {
        BlockedLog.add("telemetry.example.com", now = 1_000)
        BlockedLog.add("telemetry.example.com", now = 1_000 + 61_000)

        assertEquals(2, BlockedLog.entries.value.size)
    }
}
