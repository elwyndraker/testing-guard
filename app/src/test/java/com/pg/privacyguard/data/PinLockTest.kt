package com.pg.privacyguard.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PinLockTest {
    @Test
    fun hashMatchesOnlyTheSamePin() {
        val stored = PinLock.hash("1234")
        assertTrue(PinLock.matches("1234", stored))
        assertFalse(PinLock.matches("1235", stored))
        assertFalse(PinLock.matches("1234", "broken"))
    }

    @Test
    fun saltMakesEachHashDifferent() {
        assertNotEquals(PinLock.hash("1234"), PinLock.hash("1234"))
    }

    @Test
    fun validPins() {
        assertTrue(PinLock.isValidPin("1234"))
        assertTrue(PinLock.isValidPin("12345678"))
        assertFalse(PinLock.isValidPin("123"))
        assertFalse(PinLock.isValidPin("123456789"))
        assertFalse(PinLock.isValidPin("12a4"))
    }

    @Test
    fun noPinNeededWithoutPro() {
        Pro.setForTest(false)
        assertTrue(PinLock.isOpen())
    }
}
