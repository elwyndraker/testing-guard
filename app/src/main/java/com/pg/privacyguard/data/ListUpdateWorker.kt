package com.pg.privacyguard.data

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

/**
 * Updates the lists every [ListUpdateWorker.INTERVAL_HOURS] hours in the background, even when
 * the app is closed. Android runs it only with a network connection and may delay it a little
 * to save battery; it keeps running after a reboot.
 */
class ListUpdateWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    companion object {
        const val INTERVAL_HOURS = 12L
        const val INTERVAL_MS = INTERVAL_HOURS * 60 * 60 * 1000
        private const val NAME = "list_update"

        // An update this recent (e.g. Update now, or the app just opened) is not repeated.
        private const val RECENT_MS = 60L * 60 * 1000

        /** Safe to call often: an already scheduled update keeps its timing. */
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<ListUpdateWorker>(INTERVAL_HOURS, TimeUnit.HOURS)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .setInitialDelay(INTERVAL_HOURS, TimeUnit.HOURS)
                .build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }
    }

    override suspend fun doWork(): Result {
        val context = applicationContext
        Blocklist.load(context)
        val sourcesChanged = Settings.sourcesHash(context).first() != Blocklist.sourcesHash
        if (!sourcesChanged && System.currentTimeMillis() - Settings.lastUpdated(context).first() < RECENT_MS) {
            return Result.success()
        }
        return try {
            Blocklist.update(context)
            Result.success()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // No internet or a source is down; the current lists keep working. Try again soon.
            Result.retry()
        }
    }
}
