package com.pg.privacyguard.data

private const val HAGEZI = "https://raw.githubusercontent.com/hagezi/dns-blocklists/main/wildcard"

/**
 * One group of blocklists that is saved, loaded and switched on or off as a whole.
 * [STANDARD] is free and always on; the others are Pro filters the user turns on.
 *
 * Never add an ads list here: Google Play doesn't allow a VPN app to block ads.
 * Hosts files, plain domain lists and adblock lists all work (see [Blocklist.parse]).
 */
enum class Filter(
    val fileName: String,
    val pro: Boolean,
    /** Fewer domains than this after an update means the download was broken. */
    val minDomains: Int,
    val sources: List<String>,
) {
    /** Trackers, phone telemetry, malware, phishing and scams. */
    STANDARD(
        "standard", pro = false, minDomains = 10_000,
        listOf(
            "https://easylist-downloads.adblockplus.org/easyprivacy.txt",
            "https://raw.githubusercontent.com/r-a-y/mobile-hosts/master/AdguardMobileSpyware.txt",
            "https://raw.githubusercontent.com/FadeMind/hosts.extras/master/add.2o7Net/hosts",
            "https://gitlab.com/quidsup/notrack-blocklists/-/raw/master/trackers.list",
            "$HAGEZI/native.xiaomi-onlydomains.txt",
            "$HAGEZI/native.samsung-onlydomains.txt",
            "$HAGEZI/native.huawei-onlydomains.txt",
            "$HAGEZI/native.oppo-realme-onlydomains.txt",
            "$HAGEZI/native.vivo-onlydomains.txt",
            "$HAGEZI/tif.mini-onlydomains.txt",
            "$HAGEZI/fake-onlydomains.txt",
        ),
    ),
    ADULT("adult", pro = true, minDomains = 10_000, listOf("$HAGEZI/nsfw-onlydomains.txt")),
    GAMBLING("gambling", pro = true, minDomains = 10_000, listOf("$HAGEZI/gambling.medium-onlydomains.txt")),

    /** Public DNS-over-HTTPS, VPN and proxy services that apps could use to skip the filter. */
    BYPASS("bypass", pro = true, minDomains = 1_000, listOf("$HAGEZI/doh-vpn-proxy-bypass-onlydomains.txt")),

    /** Machine-generated domains registered in the past 7 days. Large: about 800,000 domains. */
    NEW_DOMAINS("new_domains", pro = true, minDomains = 10_000, listOf("https://cdn.jsdelivr.net/gh/hagezi/nrd@latest/domains/dga7.txt")),
    CRYPTOJACKING("cryptojacking", pro = true, minDomains = 100, listOf("https://raw.githubusercontent.com/hoshsadiq/adblock-nocoin-list/master/hosts.txt")),
    STALKERWARE("stalkerware", pro = true, minDomains = 100, listOf("https://raw.githubusercontent.com/AssoEchap/stalkerware-indicators/master/generated/hosts")),
}
