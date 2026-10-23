package com.pg.privacyguard.ui

import android.text.format.DateFormat
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pg.privacyguard.BuildConfigInfo
import com.pg.privacyguard.R
import com.pg.privacyguard.data.Blocklist
import com.pg.privacyguard.data.Filter
import com.pg.privacyguard.data.Pro
import com.pg.privacyguard.data.Settings
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Date

@Composable
fun FiltersScreen() {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val scope = rememberCoroutineScope()
    val domainCount by Blocklist.size.collectAsStateWithLifecycle()
    val lastUpdated by Settings.lastUpdated(context).collectAsStateWithLifecycle(initialValue = 0L)
    var updating by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var showAbout by remember { mutableStateOf(false) }
    var showCredits by remember { mutableStateOf(false) }
    var downloading by remember { mutableStateOf<Filter?>(null) }
    var lockedFilter by remember { mutableStateOf<Filter?>(null) }
    val pro by Pro.active.collectAsStateWithLifecycle()
    val enabledFilters by Settings.enabledFilters(context).collectAsStateWithLifecycle(initialValue = emptySet())
    val fallback by Settings.publicDnsFallback(context).collectAsStateWithLifecycle(initialValue = false)
    val pinGate = rememberPinGate()

    val doneText = stringResource(R.string.update_done)
    val partialText = stringResource(R.string.update_done_partial)
    val failedText = stringResource(R.string.update_failed)

    if (showCredits) CreditsDialog(onDismiss = { showCredits = false })
    lockedFilter?.let { ProDialog(stringResource(filterTitle(it)), onDismiss = { lockedFilter = null }) }

    if (showAbout) {
        AlertDialog(
            onDismissRequest = { showAbout = false },
            title = { Text(stringResource(R.string.about)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(R.string.about_text))
                    Text(
                        stringResource(R.string.version, BuildConfigInfo.versionName(context)),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showAbout = false }) { Text(stringResource(R.string.close)) }
            },
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(stringResource(R.string.filters_title), style = MaterialTheme.typography.headlineSmall)

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp)) {
                Text(
                    text = NumberFormat.getIntegerInstance().format(domainCount),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(stringResource(R.string.domains_in_list))
                Spacer(Modifier.size(12.dp))
                Text(
                    text = if (lastUpdated == 0L) {
                        stringResource(R.string.built_in_list)
                    } else {
                        val date = Date(lastUpdated)
                        stringResource(
                            R.string.last_updated,
                            "${DateFormat.getMediumDateFormat(context).format(date)} " +
                                DateFormat.getTimeFormat(context).format(date)
                        )
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Button(
            onClick = {
                updating = true
                message = null
                scope.launch {
                    message = try {
                        val result = Blocklist.update(context)
                        if (result.failed == 0) doneText
                        else partialText.format(result.failed, result.total)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        failedText.format(e.message ?: e.javaClass.simpleName)
                    }
                    updating = false
                }
            },
            enabled = !updating,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (updating) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(12.dp))
                    Text(stringResource(R.string.updating))
                } else {
                    Text(stringResource(R.string.update_now))
                }
            }
        }
        message?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
        Text(
            text = stringResource(R.string.auto_update_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        FilterRow(Filter.STANDARD, checked = true, enabled = false, onChange = {})

        Text(stringResource(R.string.pro_filters), style = MaterialTheme.typography.titleMedium)
        if (!pro) {
            Text(
                stringResource(R.string.pro_needed),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        for (filter in Filter.entries.filter { it.pro }) {
            FilterRow(
                filter,
                checked = pro && filter in enabledFilters,
                enabled = pro && downloading == null,
                downloading = downloading == filter,
                onLockedClick = if (pro) null else ({ lockedFilter = filter }),
                onChange = { on ->
                    pinGate.run {
                        downloading = filter
                        message = null
                        scope.launch {
                            try {
                                Blocklist.setEnabled(context, filter, on)
                            } catch (e: CancellationException) {
                                throw e
                            } catch (e: Exception) {
                                message = failedText.format(e.message ?: e.javaClass.simpleName)
                            }
                            downloading = null
                        }
                    }
                },
            )
        }

        SwitchRow(
            title = stringResource(R.string.public_dns_fallback),
            detail = stringResource(R.string.public_dns_fallback_detail),
            checked = fallback,
            onChange = { on -> pinGate.run { scope.launch { Settings.setPublicDnsFallback(context, on) } } },
        )

        Spacer(Modifier.size(8.dp))
        OutlinedButton(onClick = { showAbout = true }, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.about))
        }
        val policyUrl = stringResource(R.string.privacy_policy_url)
        OutlinedButton(
            onClick = { runCatching { uriHandler.openUri(policyUrl) } },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.privacy_policy))
        }
        TextButton(onClick = { showCredits = true }) {
            Text(stringResource(R.string.licenses))
        }
    }
}

/** A filter's switch. With [onLockedClick], the filter needs Pro: a lock replaces the switch and tapping the row calls it. */
@Composable
private fun FilterRow(
    filter: Filter,
    checked: Boolean,
    enabled: Boolean,
    onChange: (Boolean) -> Unit,
    downloading: Boolean = false,
    onLockedClick: (() -> Unit)? = null,
) {
    if (onLockedClick != null) {
        val proLabel = stringResource(R.string.tab_pro)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClickLabel = stringResource(R.string.pro_unlock_action), role = Role.Button, onClick = onLockedClick),
        ) {
            Column(Modifier.weight(1f).padding(end = 16.dp)) {
                Text(stringResource(filterTitle(filter)), style = MaterialTheme.typography.bodyLarge)
                Text(
                    stringResource(filterDetail(filter)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painterResource(R.drawable.ic_lock),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(4.dp))
                Text(proLabel, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            }
        }
        return
    }
    SwitchRow(
        title = stringResource(filterTitle(filter)),
        detail = if (downloading) stringResource(R.string.filter_downloading) else stringResource(filterDetail(filter)),
        checked = checked,
        enabled = enabled,
        onChange = onChange,
    )
}

@Composable
fun SwitchRow(title: String, detail: String, checked: Boolean, onChange: (Boolean) -> Unit, enabled: Boolean = true) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.weight(1f).padding(end = 16.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                detail,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = checked, onCheckedChange = onChange, enabled = enabled)
    }
}
