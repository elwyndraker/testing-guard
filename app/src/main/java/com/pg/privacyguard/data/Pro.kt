package com.pg.privacyguard.data

import android.content.Context
import android.content.pm.ApplicationInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first

/**
 * Whether Pro is unlocked. Google Play decides (see billing/Billing.kt); the last answer is
 * saved on the phone so Pro filters keep working offline and when the VPN starts without the app.
 * Debug builds always have Pro, so its features can be tested without a Play purchase.
 */
object Pro {
    const val PRODUCT_ID = "pro_unlock"

    private val _active = MutableStateFlow(false)
    val active: StateFlow<Boolean> = _active

    @Volatile
    private var loaded = false

    /** Reads the saved status once. */
    suspend fun load(context: Context) {
        if (loaded) return
        _active.value = isDebugBuild(context) || Settings.pro(context).first()
        loaded = true
    }

    /** Called with Google Play's answer. */
    suspend fun set(context: Context, pro: Boolean) {
        val value = pro || isDebugBuild(context)
        _active.value = value
        loaded = true
        Settings.setPro(context, pro)
    }

    internal fun setForTest(pro: Boolean) {
        _active.value = pro
        loaded = true
    }

    private fun isDebugBuild(context: Context) =
        context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
}
