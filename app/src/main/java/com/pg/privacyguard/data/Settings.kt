package com.pg.privacyguard.data

import android.content.Context
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

/** Small settings saved on the phone only. */
object Settings {
    private val LAST_UPDATED = longPreferencesKey("last_updated")
    private val SOURCES_HASH = intPreferencesKey("sources_hash")
    private val DISCLOSURE_ACCEPTED = booleanPreferencesKey("disclosure_accepted")
    private val ENABLED_FILTERS = stringSetPreferencesKey("enabled_filters")
    private val BLOCK_RULES = stringSetPreferencesKey("block_rules")
    private val ALLOW_RULES = stringSetPreferencesKey("allow_rules")
    private val PUBLIC_DNS_FALLBACK = booleanPreferencesKey("public_dns_fallback")
    private val PRO = booleanPreferencesKey("pro")
    private val PIN_HASH = stringPreferencesKey("pin_hash")

    private fun <T> read(context: Context, get: (Preferences) -> T): Flow<T> =
        context.applicationContext.dataStore.data.map(get)

    private suspend fun write(context: Context, set: (MutablePreferences) -> Unit) {
        context.applicationContext.dataStore.edit(set)
    }

    /** When the list was last downloaded, or 0 if only the built-in list has been used. */
    fun lastUpdated(context: Context): Flow<Long> = read(context) { it[LAST_UPDATED] ?: 0L }

    /** Saves the update time together with which source list was used for it. */
    suspend fun setLastUpdated(context: Context, time: Long, sourcesHash: Int) = write(context) {
        it[LAST_UPDATED] = time
        it[SOURCES_HASH] = sourcesHash
    }

    /** Identifies the source list of the last update; 0 before the first multi-source update. */
    fun sourcesHash(context: Context): Flow<Int> = read(context) { it[SOURCES_HASH] ?: 0 }

    fun disclosureAccepted(context: Context): Flow<Boolean> = read(context) { it[DISCLOSURE_ACCEPTED] ?: false }

    suspend fun setDisclosureAccepted(context: Context) = write(context) { it[DISCLOSURE_ACCEPTED] = true }

    /** Pro filters the user turned on. [Filter.STANDARD] is always on and never stored. */
    fun enabledFilters(context: Context): Flow<Set<Filter>> = read(context) { prefs ->
        prefs[ENABLED_FILTERS].orEmpty().mapNotNull { name -> Filter.entries.find { it.name == name } }.toSet()
    }

    suspend fun setFilterEnabled(context: Context, filter: Filter, on: Boolean) = write(context) {
        val current = it[ENABLED_FILTERS].orEmpty()
        it[ENABLED_FILTERS] = if (on) current + filter.name else current - filter.name
    }

    fun rules(context: Context, block: Boolean): Flow<Set<String>> =
        read(context) { it[if (block) BLOCK_RULES else ALLOW_RULES].orEmpty() }

    suspend fun setRules(context: Context, block: Boolean, domains: Set<String>) =
        write(context) { it[if (block) BLOCK_RULES else ALLOW_RULES] = domains }

    /** Off by default: with it on, lookups can go to Cloudflare or Google (see UpstreamDns). */
    fun publicDnsFallback(context: Context): Flow<Boolean> = read(context) { it[PUBLIC_DNS_FALLBACK] ?: false }

    suspend fun setPublicDnsFallback(context: Context, on: Boolean) = write(context) { it[PUBLIC_DNS_FALLBACK] = on }

    /** Last Pro status Google Play confirmed, so Pro works offline and before Play answers. */
    fun pro(context: Context): Flow<Boolean> = read(context) { it[PRO] ?: false }

    suspend fun setPro(context: Context, pro: Boolean) = write(context) { it[PRO] = pro }

    /** "salt:hash" of the PIN (see [PinLock]), or null if no PIN is set. */
    fun pinHash(context: Context): Flow<String?> = read(context) { it[PIN_HASH] }

    suspend fun setPinHash(context: Context, hash: String?) = write(context) {
        if (hash == null) it.remove(PIN_HASH) else it[PIN_HASH] = hash
    }
}
