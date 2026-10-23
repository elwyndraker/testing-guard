package com.pg.privacyguard.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/** How many sources an update tried and how many of them failed. */
data class UpdateResult(val failed: Int, val total: Int)

/**
 * The blocked domains, one [DomainSet] per [Filter]. Each filter is saved as its own file in
 * filesDir/lists. The free [Filter.STANDARD] list also ships in assets, so blocking works on
 * first launch, even offline.
 */
object Blocklist {
    /**
     * An ads-only list. Its domains are removed from every downloaded filter, so the app
     * provably never blocks ad delivery (Google Play doesn't allow a VPN app to block ads).
     */
    const val ADS_EXCLUSION_SOURCE = "https://easylist.to/easylist/easylist.txt"

    private const val DIR = "lists"
    private const val ASSET = "standard.txt"

    // Older versions saved one merged list that included ads lists; it must never be loaded again.
    private const val LEGACY_FILE = "hosts.txt"

    // Below this, the exclusion list download is broken and the update can't prove anything.
    private const val MIN_EXCLUDED = 10_000

    // Adult first, so a name that is on several lists is always reported (and hidden) as adult.
    private val CHECK_ORDER = listOf(Filter.ADULT) + (Filter.entries - Filter.ADULT)

    private val SPACES = Regex("\\s+")
    private val SINK_ADDRESSES = setOf("0.0.0.0", "127.0.0.1", "::", "::1", "0")
    private val LOCAL_NAMES = setOf("localhost", "localhost.localdomain", "local", "broadcasthost")
    private val mutex = Mutex()

    @Volatile
    private var sets: Map<Filter, DomainSet> = emptyMap()

    @Volatile
    private var enabled: Set<Filter> = emptySet()

    @Volatile
    private var loaded = false

    /** Domains in the filters that are on right now. */
    private val _size = MutableStateFlow(0)
    val size: StateFlow<Int> = _size

    /** Identifies every source, so an app update that changes them triggers a fresh download. */
    val sourcesHash: Int get() = (Filter.entries.map { it.sources } + listOf(ADS_EXCLUSION_SOURCE)).hashCode()

    /**
     * The filter that blocks [name] or one of its parents, or null if it is allowed. The
     * user's own block rules count as [Filter.STANDARD]. Pro filters and rules apply only
     * while Pro is active.
     */
    fun check(name: String): Filter? {
        val pro = Pro.active.value
        if (pro) {
            if (MyRules.allowed.containsDomainOrParent(name)) return null
            if (MyRules.blocked.containsDomainOrParent(name)) return Filter.STANDARD
        }
        val current = sets
        val on = enabled
        for (filter in CHECK_ORDER) {
            if (filter.pro && (!pro || filter !in on)) continue
            if (current[filter]?.containsDomainOrParent(name) == true) return filter
        }
        return null
    }

    fun isBlocked(name: String): Boolean = check(name) != null

    /** Loads the lists once. Safe to call from both the screen and the VPN service. */
    suspend fun load(context: Context) = withContext(Dispatchers.IO) {
        // Don't wait behind a running update when the lists are already in memory.
        if (loaded) return@withContext
        mutex.withLock {
            if (loaded) return@withLock
            // Pro decides which filters apply, so it must be known before the first lookup.
            Pro.load(context)
            PinLock.load(context)
            MyRules.load(context)
            File(context.filesDir, LEGACY_FILE).delete()
            enabled = Settings.enabledFilters(context).first()

            val fresh = mutableMapOf<Filter, DomainSet>()
            for (filter in Filter.entries) {
                if (filter.pro && filter !in enabled) continue
                readFile(context, filter)?.let { fresh[filter] = it }
            }
            if (Filter.STANDARD !in fresh) {
                fresh[Filter.STANDARD] = context.assets.open(ASSET).bufferedReader().useLines { parse(it) }
            }
            sets = fresh
            publishSize()
            loaded = true
        }
    }

    /** The filters an update downloads: [Filter.STANDARD], plus the Pro filters that are on. */
    fun filtersToUpdate(): List<Filter> =
        Filter.entries.filter { !it.pro || (Pro.active.value && it in enabled) }

    /**
     * Downloads [filters] and saves each one. Sources that fail are skipped; a filter whose
     * download looks broken keeps its old list. Throws IOException if the ads exclusion list or
     * the standard list can't be downloaded, and then nothing changes.
     */
    suspend fun update(context: Context, filters: List<Filter> = filtersToUpdate()): UpdateResult =
        withContext(Dispatchers.IO) {
            mutex.withLock {
                val excluded = DomainSet.Builder().also { download(ADS_EXCLUSION_SOURCE, it, null) }.build()
                if (excluded.size < MIN_EXCLUDED) {
                    throw IOException("the ads exclusion list looks incomplete (${excluded.size} domains)")
                }

                var failed = 0
                var total = 0
                for (filter in filters) {
                    val builder = DomainSet.Builder()
                    var lastError: IOException? = null
                    for (url in filter.sources) {
                        ensureActive()
                        total++
                        try {
                            download(url, builder, excluded)
                        } catch (e: IOException) {
                            failed++
                            lastError = e
                        }
                    }
                    if (builder.size < filter.minDomains) {
                        if (filter == Filter.STANDARD) {
                            throw lastError ?: IOException("list looks incomplete (${builder.size} domains)")
                        }
                        continue
                    }
                    val list = builder.build()
                    saveFile(context, filter, list)
                    if (!filter.pro || filter in enabled) {
                        sets = sets + (filter to list)
                        publishSize()
                    }
                }
                if (Filter.STANDARD in filters) {
                    Settings.setLastUpdated(context, System.currentTimeMillis(), sourcesHash)
                }
                UpdateResult(failed, total)
            }
        }

    /**
     * Turns a Pro filter on or off. Turning on a filter that was never downloaded downloads it,
     * which can throw IOException; the filter stays on and is tried again at the next update.
     */
    suspend fun setEnabled(context: Context, filter: Filter, on: Boolean) {
        require(filter.pro)
        Settings.setFilterEnabled(context, filter, on)
        val needsDownload = mutex.withLock {
            enabled = if (on) enabled + filter else enabled - filter
            if (!on) {
                sets = sets - filter
                publishSize()
                return@withLock false
            }
            val saved = withContext(Dispatchers.IO) { readFile(context, filter) }
            if (saved != null) {
                sets = sets + (filter to saved)
                publishSize()
            }
            saved == null
        }
        if (needsDownload) update(context, listOf(filter))
    }

    fun isEnabled(filter: Filter) = !filter.pro || filter in enabled

    private fun file(context: Context, filter: Filter) = File(File(context.filesDir, DIR), "${filter.fileName}.txt")

    private fun readFile(context: Context, filter: Filter): DomainSet? {
        val file = file(context, filter)
        if (!file.exists()) return null
        val set = try {
            file.bufferedReader().useLines { parse(it) }
        } catch (e: IOException) {
            return null
        }
        return set.takeIf { it.size >= filter.minDomains }
    }

    private fun saveFile(context: Context, filter: Filter, list: DomainSet) {
        val target = file(context, filter)
        target.parentFile?.mkdirs()
        val tmp = File(target.parentFile, "${filter.fileName}.tmp")
        tmp.outputStream().buffered().use { list.writeTo(it) }
        if (!tmp.renameTo(target)) {
            tmp.delete()
            throw IOException("could not save the list")
        }
    }

    private fun publishSize() {
        val on = enabled
        val pro = Pro.active.value
        _size.value = sets.entries.sumOf { (filter, set) ->
            if (!filter.pro || (pro && filter in on)) set.size else 0
        }
    }

    /** Recounts after Pro changes, since Pro filters only count while Pro is active. */
    fun onProChanged() = publishSize()

    private fun download(url: String, into: DomainSet.Builder, excluded: DomainSet?) {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = 15_000
        connection.readTimeout = 30_000
        connection.setRequestProperty("User-Agent", "PrivacyGuard")
        try {
            val code = connection.responseCode
            if (code != HttpURLConnection.HTTP_OK) throw IOException("${connection.url.host} answered $code")
            connection.inputStream.bufferedReader().useLines { parse(it, into, excluded) }
        } finally {
            connection.disconnect()
        }
    }

    internal fun parse(lines: Sequence<String>, excluded: DomainSet? = null): DomainSet =
        DomainSet.Builder().also { parse(lines, it, excluded) }.build()

    /**
     * Understands the common list formats, one rule per line:
     * hosts files ("0.0.0.0 ads.example.com", also 127.0.0.1 and ::), plain domains
     * ("ads.example.com"), wildcards ("*.ads.example.com") and adblock domain rules
     * ("||ads.example.com^"). Other adblock rules (paths, options, element hiding, exceptions)
     * can't be applied to DNS and are skipped, as are comments, IP addresses and localhost.
     * Names in [excluded], or under a name in it, are skipped too.
     */
    internal fun parse(lines: Sequence<String>, into: DomainSet.Builder, excluded: DomainSet? = null) {
        // In an adblock list a bare word like "ad-banner.js" is a URL pattern, not a domain.
        var adblock = false
        for (raw in lines) {
            val line = raw.removePrefix("\uFEFF").trim()
            if (line.startsWith("[Adblock") || line.startsWith("||")) adblock = true
            if (line.isEmpty() || line[0] == '!' || line[0] == '[') continue
            if ("##" in line || "#@#" in line || "#?#" in line || "#$#" in line) continue
            val parts = line.substringBefore('#').trim().split(SPACES)
            if (parts.size >= 2) {
                if (parts[0] !in SINK_ADDRESSES) continue
                for (i in 1 until parts.size) addIfDomain(parts[i], into, excluded)
            } else {
                val rule = parts[0]
                when {
                    rule.startsWith("||") ->
                        if (rule.endsWith("^")) addIfDomain(rule.substring(2, rule.length - 1), into, excluded)
                    adblock -> {}
                    rule.startsWith("*.") -> addIfDomain(rule.substring(2), into, excluded)
                    else -> addIfDomain(rule, into, excluded)
                }
            }
        }
    }

    private fun addIfDomain(candidate: String, into: DomainSet.Builder, excluded: DomainSet?) {
        val name = candidate.lowercase()
        if (name in LOCAL_NAMES || !isDomain(name)) return
        if (excluded?.containsDomainOrParent(name) == true) return
        into.add(name)
    }

    /**
     * Letters, digits, '-' and '_' in at least two dot-separated labels, no label starting or
     * ending with '-', and not an IP address.
     */
    internal fun isDomain(name: String): Boolean {
        if (name.length > 253) return false
        var labels = 1
        var letters = false
        var labelStart = 0
        for (i in 0..name.length) {
            if (i == name.length || name[i] == '.') {
                if (i == labelStart || name[labelStart] == '-' || name[i - 1] == '-') return false
                if (i < name.length) labels++
                labelStart = i + 1
                continue
            }
            when (name[i]) {
                in 'a'..'z', '-', '_' -> letters = true
                in '0'..'9' -> {}
                else -> return false
            }
        }
        return labels >= 2 && letters
    }

    internal fun setForTest(lists: Map<Filter, DomainSet>, on: Set<Filter> = emptySet()) {
        sets = lists
        enabled = on
        publishSize()
    }
}
