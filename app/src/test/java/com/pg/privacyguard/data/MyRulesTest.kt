package com.pg.privacyguard.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MyRulesTest {
    @Test
    fun normalizesWhatPeoplePaste() {
        assertEquals("example.com", MyRules.normalize("  Example.COM "))
        assertEquals("www.example.com", MyRules.normalize("https://www.example.com/page?x=1"))
        assertEquals("example.com", MyRules.normalize("*.example.com."))
        assertEquals("example.com", MyRules.normalize("example.com:443"))
    }

    @Test
    fun rejectsNonDomains() {
        assertNull(MyRules.normalize(""))
        assertNull(MyRules.normalize("example"))
        assertNull(MyRules.normalize("1.2.3.4"))
        assertNull(MyRules.normalize("bad domain.com"))
    }
}
