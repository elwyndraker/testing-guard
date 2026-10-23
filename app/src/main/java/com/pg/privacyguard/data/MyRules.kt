package com.pg.privacyguard.data

import android.content.Context
import kotlinx.coroutines.flow.first

/**
 * Pro "My rules": single domains the user blocks or allows, typed one at a time. A rule also
 * covers subdomains. Allow wins over everything, including the user's own block rules.
 */
object MyRules {
    @Volatile
    var blocked: DomainSet = DomainSet.EMPTY
        private set

    @Volatile
    var allowed: DomainSet = DomainSet.EMPTY
        private set

    suspend fun load(context: Context) {
        blocked = toSet(Settings.rules(context, block = true).first())
        allowed = toSet(Settings.rules(context, block = false).first())
    }

    /** Adds a rule; returns false if [input] isn't a domain name. */
    suspend fun add(context: Context, input: String, block: Boolean): Boolean {
        val domain = normalize(input) ?: return false
        val current = Settings.rules(context, block).first()
        save(context, block, current + domain)
        return true
    }

    suspend fun remove(context: Context, domain: String, block: Boolean) {
        save(context, block, Settings.rules(context, block).first() - domain)
    }

    private suspend fun save(context: Context, block: Boolean, domains: Set<String>) {
        Settings.setRules(context, block, domains)
        if (block) blocked = toSet(domains) else allowed = toSet(domains)
    }

    /**
     * Turns what people paste ("https://Www.Example.com/page", "*.example.com.") into a plain
     * lowercase domain, or null if it isn't one.
     */
    internal fun normalize(input: String): String? {
        val name = input.trim().lowercase()
            .substringAfter("://")
            .substringBefore('/')
            .substringBefore('?')
            .substringBefore(':')
            .removePrefix("*.")
            .removeSuffix(".")
        return name.takeIf { Blocklist.isDomain(it) }
    }

    internal fun toSet(domains: Collection<String>): DomainSet =
        DomainSet.Builder().also { builder -> domains.forEach { builder.add(it) } }.build()

    internal fun setForTest(block: Collection<String>, allow: Collection<String>) {
        blocked = toSet(block)
        allowed = toSet(allow)
    }
}
