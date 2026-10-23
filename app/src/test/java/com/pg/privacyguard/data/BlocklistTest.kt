package com.pg.privacyguard.data

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BlocklistTest {

    private val sample = """
        # Title: example hosts file
        127.0.0.1 localhost
        0.0.0.0 0.0.0.0
        0.0.0.0 metrics.example.net
        0.0.0.0	telemetry.Example.com   # inline comment
          0.0.0.0 tracker.test
        ::1 localhost
    """.trimIndent()

    private fun parse(text: String) = Blocklist.parse(text.lineSequence())

    private fun assertDomains(expected: Set<String>, set: DomainSet) {
        assertEquals(expected.size, set.size)
        for (domain in expected) assertTrue(domain, set.contains(domain))
    }

    @Test
    fun parsesOnlyBlockedDomains() {
        assertDomains(setOf("metrics.example.net", "telemetry.example.com", "tracker.test"), parse(sample))
    }

    @Test
    fun parsesOtherHostsFormats() {
        val text = """
            ${"﻿"}# GoodbyeAds
            127.0.0.1 localhost.localdomain
            127.0.0.1 t.one.com
            :: t.two.com
            0.0.0.0 a.three.com b.three.com
            255.255.255.255 broadcasthost
            fe80::1%lo0 localhost
            1.2.3.4 not-a-sink.com
            0.0.0.0 1.2.3.4
        """.trimIndent()
        assertDomains(
            setOf("t.one.com", "t.two.com", "a.three.com", "b.three.com"),
            parse(text),
        )
    }

    @Test
    fun parsesDomainAndWildcardLists() {
        val text = """
            # plain and wildcard lists
            plain.example.org
            *.wild.example.org
            *.example.*
            -bad.example.org
            bad-.example.org
            dots..example.org
            nodot
        """.trimIndent()
        assertDomains(setOf("plain.example.org", "wild.example.org"), parse(text))
    }

    @Test
    fun parsesOnlyDomainRulesFromFilterRuleLists() {
        val text = """
            [Filter list 2.0]
            ! Title: EasyPrivacy
            ||tracker.example.com^
            ||third.example.com^${'$'}third-party
            ||example.com/path^
            @@||allowed.example.com^
            example.com##.banner
            banner.js
            *-track.sm.cn*
        """.trimIndent()
        assertDomains(setOf("tracker.example.com"), parse(text))
    }

    @Test
    fun skipsExcludedDomainsAndTheirSubdomains() {
        val excluded = parse("||metrics.example.net^\n||telemetry.example.org^")
        val set = Blocklist.parse(
            "stats.g.metrics.example.net\nmetrics.example.net\ntelemetry.example.org\nexample.org\ntracker.test".lineSequence(),
            excluded,
        )
        assertDomains(setOf("example.org", "tracker.test"), set)
    }

    @Test
    fun blocksSubdomainsButNotParents() {
        Blocklist.setForTest(mapOf(Filter.STANDARD to parse(sample)))
        assertTrue(Blocklist.isBlocked("metrics.example.net"))
        assertTrue(Blocklist.isBlocked("stats.g.metrics.example.net"))
        assertTrue(Blocklist.isBlocked("telemetry.example.com"))
        assertTrue(Blocklist.isBlocked("Stats.Metrics.Example.NET"))
        assertFalse(Blocklist.isBlocked("example.com"))
        assertFalse(Blocklist.isBlocked("notmetrics.example.net"))
        assertFalse(Blocklist.isBlocked(""))
    }
}

class BlocklistFiltersTest {
    private fun set(vararg names: String) = MyRules.toSet(names.toList())

    @After
    fun reset() {
        Pro.setForTest(false)
        MyRules.setForTest(emptyList(), emptyList())
        Blocklist.setForTest(emptyMap())
    }

    private fun setUp(pro: Boolean, on: Set<Filter>) {
        Pro.setForTest(pro)
        Blocklist.setForTest(
            mapOf(
                Filter.STANDARD to set("tracker.test", "both.test"),
                Filter.ADULT to set("adult.test", "both.test"),
                Filter.GAMBLING to set("casino.test"),
            ),
            on,
        )
    }

    @Test
    fun proFiltersApplyOnlyWithProAndWhenOn() {
        setUp(pro = false, on = setOf(Filter.ADULT, Filter.GAMBLING))
        assertEquals(Filter.STANDARD, Blocklist.check("tracker.test"))
        assertNull(Blocklist.check("adult.test"))
        assertNull(Blocklist.check("casino.test"))

        setUp(pro = true, on = setOf(Filter.ADULT))
        assertEquals(Filter.ADULT, Blocklist.check("www.adult.test"))
        assertNull(Blocklist.check("casino.test"))
        assertEquals(2 + 2, Blocklist.size.value)
    }

    @Test
    fun adultWinsSoItsNameIsHidden() {
        setUp(pro = true, on = setOf(Filter.ADULT))
        assertEquals(Filter.ADULT, Blocklist.check("both.test"))
    }

    @Test
    fun myRulesNeedProAndAllowWins() {
        setUp(pro = true, on = emptySet())
        MyRules.setForTest(block = listOf("mine.test", "tracker.test"), allow = listOf("tracker.test"))
        assertEquals(Filter.STANDARD, Blocklist.check("sub.mine.test"))
        assertNull(Blocklist.check("tracker.test"))
        assertNull(Blocklist.check("a.tracker.test"))

        Pro.setForTest(false)
        assertNull(Blocklist.check("mine.test"))
        assertEquals(Filter.STANDARD, Blocklist.check("tracker.test"))
    }

    @Test
    fun onlyApprovedSources() {
        // Every list is reviewed before it's added: privacy and security lists only. Google Play
        // doesn't allow a VPN app to interfere with how other apps earn money.
        for (filter in Filter.entries) for (url in filter.sources) {
            assertTrue("$url isn't approved; review it, then add it to APPROVED_SOURCES", url in APPROVED_SOURCES)
        }
        assertEquals("https://easylist.to/easylist/easylist.txt", Blocklist.EXCLUSION_SOURCE)
        for (filter in Filter.entries) assertFalse(Blocklist.EXCLUSION_SOURCE in filter.sources)
    }

    @Test
    fun standardIsTheOnlyFreeFilter() {
        assertEquals(listOf(Filter.STANDARD), Filter.entries.filter { !it.pro })
    }

    companion object {
        /** The reviewed privacy and security lists. A new source in [Filter] must be added here too. */
        val APPROVED_SOURCES = setOf(
            "https://easylist.to/easylist/easyprivacy.txt",
            "https://raw.githubusercontent.com/r-a-y/mobile-hosts/master/AdguardMobileSpyware.txt",
            "https://raw.githubusercontent.com/FadeMind/hosts.extras/master/add.2o7Net/hosts",
            "https://gitlab.com/quidsup/notrack-blocklists/-/raw/master/trackers.list",
            "https://raw.githubusercontent.com/hagezi/dns-blocklists/main/wildcard/native.xiaomi-onlydomains.txt",
            "https://raw.githubusercontent.com/hagezi/dns-blocklists/main/wildcard/native.samsung-onlydomains.txt",
            "https://raw.githubusercontent.com/hagezi/dns-blocklists/main/wildcard/native.huawei-onlydomains.txt",
            "https://raw.githubusercontent.com/hagezi/dns-blocklists/main/wildcard/native.oppo-realme-onlydomains.txt",
            "https://raw.githubusercontent.com/hagezi/dns-blocklists/main/wildcard/native.vivo-onlydomains.txt",
            "https://raw.githubusercontent.com/hagezi/dns-blocklists/main/wildcard/tif.mini-onlydomains.txt",
            "https://raw.githubusercontent.com/hagezi/dns-blocklists/main/wildcard/fake-onlydomains.txt",
            "https://raw.githubusercontent.com/hagezi/dns-blocklists/main/wildcard/nsfw-onlydomains.txt",
            "https://raw.githubusercontent.com/hagezi/dns-blocklists/main/wildcard/gambling.medium-onlydomains.txt",
            "https://raw.githubusercontent.com/hagezi/dns-blocklists/main/wildcard/doh-vpn-proxy-bypass-onlydomains.txt",
            "https://cdn.jsdelivr.net/gh/hagezi/nrd@latest/domains/dga7.txt",
            "https://raw.githubusercontent.com/hoshsadiq/adblock-nocoin-list/master/hosts.txt",
            "https://raw.githubusercontent.com/AssoEchap/stalkerware-indicators/master/generated/hosts",
        )
    }
}
