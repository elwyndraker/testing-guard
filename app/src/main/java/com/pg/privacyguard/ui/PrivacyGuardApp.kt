package com.pg.privacyguard.ui

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.pg.privacyguard.R
import com.pg.privacyguard.billing.Billing
import com.pg.privacyguard.data.Blocklist
import com.pg.privacyguard.data.Settings
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first

private const val WEEK_MS = 7L * 24 * 60 * 60 * 1000

private enum class Tab(@StringRes val label: Int, @DrawableRes val icon: Int) {
    Home(R.string.tab_home, R.drawable.ic_shield),
    Blocked(R.string.tab_blocked, R.drawable.ic_list),
    Filters(R.string.tab_filters, R.drawable.ic_filter),
    Pro(R.string.tab_pro, R.drawable.ic_star),
}

@Composable
fun PrivacyGuardApp() {
    val context = LocalContext.current
    var selected by rememberSaveable { mutableIntStateOf(0) }

    // Every time the app comes to the front: restores Pro on a new phone, picks up a purchase
    // finished in the Play Store app, and removes Pro after a refund.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { Billing.connect(context) }

    LaunchedEffect(Unit) {
        Blocklist.load(context)
        // Automatic weekly update, and right away when the list of sources has changed
        // (e.g. after an app update). Failure is fine: the current list keeps working.
        val lastUpdated = Settings.lastUpdated(context).first()
        val sourcesChanged = Settings.sourcesHash(context).first() != Blocklist.sourcesHash
        if (sourcesChanged || System.currentTimeMillis() - lastUpdated > WEEK_MS) {
            try {
                Blocklist.update(context)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // No internet right now; try again next time the app opens.
            }
        }
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                Tab.entries.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        selected = selected == index,
                        onClick = { selected = index },
                        icon = { Icon(painterResource(tab.icon), contentDescription = null) },
                        label = { Text(stringResource(tab.label)) },
                    )
                }
            }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            AppUpdateBanner()
            Box(Modifier.weight(1f)) {
                when (Tab.entries[selected]) {
                    Tab.Home -> HomeScreen()
                    Tab.Blocked -> BlockedScreen()
                    Tab.Filters -> FiltersScreen()
                    Tab.Pro -> ProScreen()
                }
            }
        }
    }
}
