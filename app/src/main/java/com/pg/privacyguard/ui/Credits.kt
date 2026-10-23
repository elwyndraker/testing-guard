package com.pg.privacyguard.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.pg.privacyguard.R
import com.pg.privacyguard.data.Filter

/** A blocklist project, for the credits screen. Keep in step with [Filter] and Blocklist.ADS_EXCLUSION_SOURCE. */
private data class Credit(val name: String, val license: String, val url: String)

private val CREDITS = listOf(
    Credit("EasyPrivacy", "GPL-3.0 / CC BY-SA 3.0", "https://easylist.to/"),
    Credit("EasyList (used only to remove ads domains from the other lists)", "GPL-3.0 / CC BY-SA 3.0", "https://easylist.to/"),
    Credit("r-a-y/mobile-hosts (AdguardMobileSpyware)", "GPL-3.0", "https://github.com/r-a-y/mobile-hosts"),
    Credit("FadeMind/hosts.extras (add.2o7Net)", "MIT", "https://github.com/FadeMind/hosts.extras"),
    Credit("NoTrack trackers", "GPL-3.0", "https://gitlab.com/quidsup/notrack-blocklists"),
    Credit(
        "HaGeZi DNS blocklists (Native Tracker, Threat Intelligence Feeds, Fake, NSFW, Gambling, Bypass)",
        "GPL-3.0",
        "https://github.com/hagezi/dns-blocklists",
    ),
    Credit("HaGeZi NRD (newly registered domains)", "GPL-3.0", "https://github.com/hagezi/nrd"),
    Credit("NoCoin adblock list", "MIT", "https://github.com/hoshsadiq/adblock-nocoin-list"),
    Credit("Echap stalkerware indicators", "CC BY 4.0", "https://github.com/AssoEchap/stalkerware-indicators"),
)

@Composable
fun CreditsDialog(onDismiss: () -> Unit) {
    val uriHandler = LocalUriHandler.current
    val sourceUrl = stringResource(R.string.source_code_url)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.licenses)) },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(stringResource(R.string.licenses_intro))
                TextButton(onClick = { runCatching { uriHandler.openUri(sourceUrl) } }) {
                    Text(stringResource(R.string.source_code))
                }
                for (credit in CREDITS) {
                    Column {
                        Text(credit.name, style = MaterialTheme.typography.titleSmall)
                        Text(credit.license, style = MaterialTheme.typography.bodySmall)
                        Text(
                            credit.url,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable { runCatching { uriHandler.openUri(credit.url) } },
                        )
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) } },
    )
}

/** Name and description of a filter. */
@StringRes
fun filterTitle(filter: Filter): Int = when (filter) {
    Filter.STANDARD -> R.string.filter_standard
    Filter.ADULT -> R.string.filter_adult
    Filter.GAMBLING -> R.string.filter_gambling
    Filter.BYPASS -> R.string.filter_bypass
    Filter.NEW_DOMAINS -> R.string.filter_new_domains
    Filter.CRYPTOJACKING -> R.string.filter_cryptojacking
    Filter.STALKERWARE -> R.string.filter_stalkerware
}

@StringRes
fun filterDetail(filter: Filter): Int = when (filter) {
    Filter.STANDARD -> R.string.filter_standard_detail
    Filter.ADULT -> R.string.filter_adult_detail
    Filter.GAMBLING -> R.string.filter_gambling_detail
    Filter.BYPASS -> R.string.filter_bypass_detail
    Filter.NEW_DOMAINS -> R.string.filter_new_domains_detail
    Filter.CRYPTOJACKING -> R.string.filter_cryptojacking_detail
    Filter.STALKERWARE -> R.string.filter_stalkerware_detail
}
