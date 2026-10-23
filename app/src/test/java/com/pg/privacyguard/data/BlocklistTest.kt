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
        0.0.0.0 doubleclick.net
        0.0.0.0	ads.Example.com   # inline comment
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
        assertDomains(setOf("doubleclick.net", "ads.example.com", "tracker.test"), parse(sample))
    }

    @Test
    fun parsesOtherHostsFormats() {
        val text = """
            ${"﻿"}# GoodbyeAds
            127.0.0.1 localhost.localdomain
            127.0.0.1 ads.one.com
            :: ads.two.com
            0.0.0.0 a.three.com b.three.com
            255.255.255.255 broadcasthost
            fe80::1%lo0 localhost
            1.2.3.4 not-a-sink.com
            0.0.0.0 1.2.3.4
        """.trimIndent()
        assertDomains(
            setOf("ads.one.com", "ads.two.com", "a.three.com", "b.three.com"),
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
    fun parsesOnlyDomainRulesFromAdblockLists() {
        val text = """
            [Adblock Plus 2.0]
            ! Title: EasyPrivacy
            ||tracker.example.com^
            ||third.example.com^${'$'}third-party
            ||example.com/path^
            @@||allowed.example.com^
            example.com##.ad-banner
            ad-banner.js
            *-ad.sm.cn*
        """.trimIndent()
        assertDomains(setOf("tracker.example.com"), parse(text))
    }

    @Test
    fun skipsExcludedDomainsAndTheirSubdomains() {
        val excluded = parse("||doubleclick.net^\n||ads.example.org^")
        val set = Blocklist.parse(
            "stats.g.doubleclick.net\ndoubleclick.net\nads.example.org\nexample.org\ntracker.test".lineSequence(),
            excluded,
        )
        assertDomains(setOf("example.org", "tracker.test"), set)
    }

    @Test
    fun blocksSubdomainsButNotParents() {
        Blocklist.setForTest(mapOf(Filter.STANDARD to parse(sample)))
        assertTrue(Blocklist.isBlocked("doubleclick.net"))
        assertTrue(Blocklist.isBlocked("stats.g.doubleclick.net"))
        assertTrue(Blocklist.isBlocked("ads.example.com"))
        assertTrue(Blocklist.isBlocked("Stats.DoubleClick.NET"))
        assertFalse(Blocklist.isBlocked("example.com"))
        assertFalse(Blocklist.isBlocked("notdoubleclick.net"))
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
    fun noAdsListsInSources() {
        // Lists the first version merged that are ads or mixed ads/tracker lists. Google Play
        // doesn't allow a VPN app to block ads, so none may come back.
        val adsLists = listOf(
            "jdlingyu/ad-wars", "anti-AD", "antipopads", "dns-blocklists/main/wildcard/pro.txt",
            "hblock", "Ad-set-hosts", "AWAvenue-Ads-Rule", "GoodbyeAds", "ads-and-tracking",
            "notracking/hosts-blocklists", "flossboxin", "CombinedPrivacyBlockLists", "indianlist",
            "easylist.txt",
        )
        for (filter in Filter.entries) for (url in filter.sources) {
            for (ads in adsLists) assertFalse("$url is an ads list", url.contains(ads, ignoreCase = true))
        }
        assertTrue(Blocklist.ADS_EXCLUSION_SOURCE.endsWith("easylist.txt"))
    }

    @Test
    fun standardIsTheOnlyFreeFilter() {
        assertEquals(listOf(Filter.STANDARD), Filter.entries.filter { !it.pro })
    }
}
