package com.pg.privacyguard.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.time.LocalDate

/** Blocked lookups of one day: the total and the count per domain. */
data class DaySummary(val date: LocalDate, val total: Int, val counts: Map<String, Int>)

/**
 * Pro: blocked lookups of the last [DAYS] days, counted per day and domain. Saved on the phone
 * only (filesDir/history, one file per day) and never sent anywhere. Adult domains are counted
 * under [ADULT] instead of their names, so no adult name is ever saved or shown.
 */
object History {
    const val DAYS = 7
    const val ADULT = "*adult"
    private const val DIR = "history"

    // Counts since the last flush, per day. Files hold the totals.
    private val pending = mutableMapOf<LocalDate, MutableMap<String, Int>>()
    private val fileMutex = Mutex()

    @Synchronized
    fun record(domain: String, filter: Filter, date: LocalDate = LocalDate.now()) {
        val key = if (filter == Filter.ADULT) ADULT else domain
        val day = pending.getOrPut(date) { mutableMapOf() }
        day[key] = (day[key] ?: 0) + 1
    }

    @Synchronized
    private fun takePending(): Map<LocalDate, Map<String, Int>> {
        val copy = pending.toMap()
        pending.clear()
        return copy
    }

    /** Adds the counts collected in memory to the files and deletes days older than [DAYS]. */
    suspend fun flush(context: Context) = withContext(Dispatchers.IO) {
        fileMutex.withLock {
            val dir = File(context.filesDir, DIR).apply { mkdirs() }
            for ((date, counts) in takePending()) {
                val file = File(dir, "$date.tsv")
                val merged = merge(readCounts(file), counts)
                try {
                    file.writeText(format(merged))
                } catch (e: IOException) {
                    // History is a convenience; losing a minute of counts is fine.
                }
            }
            val oldest = LocalDate.now().minusDays(DAYS - 1L)
            dir.listFiles()?.forEach { file ->
                val date = runCatching { LocalDate.parse(file.nameWithoutExtension) }.getOrNull()
                if (date == null || date < oldest) file.delete()
            }
        }
    }

    /** The last [DAYS] days, newest first, including days with nothing blocked. */
    suspend fun read(context: Context): List<DaySummary> {
        flush(context)
        return withContext(Dispatchers.IO) {
            fileMutex.withLock {
                val dir = File(context.filesDir, DIR)
                (0 until DAYS).map { back ->
                    val date = LocalDate.now().minusDays(back.toLong())
                    summarize(date, readCounts(File(dir, "$date.tsv")))
                }
            }
        }
    }

    suspend fun clear(context: Context) = withContext(Dispatchers.IO) {
        takePending()
        fileMutex.withLock { File(context.filesDir, DIR).deleteRecursively() }
    }

    private fun readCounts(file: File): Map<String, Int> = try {
        if (file.exists()) parse(file.readLines()) else emptyMap()
    } catch (e: IOException) {
        emptyMap()
    }

    internal fun merge(a: Map<String, Int>, b: Map<String, Int>): Map<String, Int> =
        (a.keys + b.keys).associateWith { (a[it] ?: 0) + (b[it] ?: 0) }

    /** One "count<TAB>domain" per line. */
    internal fun format(counts: Map<String, Int>) =
        counts.entries.joinToString("") { "${it.value}\t${it.key}\n" }

    internal fun parse(lines: List<String>): Map<String, Int> = lines.mapNotNull { line ->
        val count = line.substringBefore('\t').toIntOrNull() ?: return@mapNotNull null
        line.substringAfter('\t', "").takeIf { it.isNotEmpty() }?.let { it to count }
    }.toMap()

    internal fun summarize(date: LocalDate, counts: Map<String, Int>) = DaySummary(date, counts.values.sum(), counts)
}
