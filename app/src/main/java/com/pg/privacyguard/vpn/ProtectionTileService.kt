package com.pg.privacyguard.vpn

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.pg.privacyguard.MainActivity
import com.pg.privacyguard.data.PinLock
import com.pg.privacyguard.data.Settings
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first

/**
 * Quick Settings tile to turn protection on and off. Opens the app instead when it can't act on
 * its own: before the first-run disclosure and Android's VPN prompt, or when a PIN is set.
 */
class ProtectionTileService : TileService() {

    companion object {
        /** Asks Android to refresh the tile after protection changed. */
        fun refresh(context: Context) {
            try {
                requestListeningState(context, ComponentName(context, ProtectionTileService::class.java))
            } catch (e: Exception) {
                // No tile added, or an Android build without Quick Settings; nothing to refresh.
            }
        }
    }

    override fun onStartListening() {
        val tile = qsTile ?: return
        tile.state = if (VpnController.isRunning.value) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.updateTile()
    }

    override fun onClick() {
        if (isLocked) {
            // Don't let anyone change protection from the lock screen.
            unlockAndRun { toggle() }
        } else {
            toggle()
        }
    }

    private fun toggle() {
        if (VpnController.isRunning.value) {
            if (PinLock.isOpen()) VpnController.stop(this) else openApp()
            return
        }
        // DataStore read on the main thread: one tiny file, only on a tap.
        val accepted = runBlocking { Settings.disclosureAccepted(this@ProtectionTileService).first() }
        if (!accepted || VpnService.prepare(this) != null) {
            openApp()
            return
        }
        try {
            VpnController.start(this)
        } catch (e: IllegalStateException) {
            // Android didn't allow starting from the tile right now; the app can.
            openApp()
        }
    }

    private fun openApp() {
        val intent = Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (Build.VERSION.SDK_INT >= 34) {
            startActivityAndCollapse(PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE))
        } else {
            @Suppress("DEPRECATION", "StartActivityAndCollapseDeprecated")
            startActivityAndCollapse(intent)
        }
    }
}
