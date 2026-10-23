package com.pg.privacyguard.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pg.privacyguard.R
import com.pg.privacyguard.billing.Billing
import com.pg.privacyguard.data.DaySummary
import com.pg.privacyguard.data.History
import com.pg.privacyguard.data.MyRules
import com.pg.privacyguard.data.PinLock
import com.pg.privacyguard.data.Pro
import com.pg.privacyguard.data.Settings
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun ProScreen() {
    val pro by Pro.active.collectAsStateWithLifecycle()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(stringResource(R.string.pro_title), style = MaterialTheme.typography.headlineSmall)
        PurchaseCard(pro)
        if (pro) {
            val pinGate = rememberPinGate()
            PinSection(pinGate)
            HorizontalDivider()
            RulesSection(pinGate)
            HorizontalDivider()
            HistorySection()
        }
    }
}

@Composable
private fun PurchaseCard(pro: Boolean) {
    val context = LocalContext.current
    val billing by Billing.state.collectAsStateWithLifecycle()
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (pro) {
                Text(stringResource(R.string.pro_active), fontWeight = FontWeight.SemiBold)
                return@Column
            }
            Text(stringResource(R.string.pro_pitch))
            for (feature in listOf(
                R.string.pro_feature_filters,
                R.string.pro_feature_pin,
                R.string.pro_feature_security,
                R.string.pro_feature_rules,
                R.string.pro_feature_history,
            )) {
                Text("•  " + stringResource(feature), style = MaterialTheme.typography.bodyMedium)
            }
            if (billing.pending) {
                Text(stringResource(R.string.pro_pending), fontWeight = FontWeight.SemiBold)
            } else {
                Button(
                    onClick = {
                        Billing.clearMessage()
                        context.findActivity()?.let { Billing.buy(it) }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        billing.price?.let { stringResource(R.string.pro_buy, it) }
                            ?: stringResource(R.string.pro_buy_no_price)
                    )
                }
            }
            billing.message?.let { Text(stringResource(it), color = MaterialTheme.colorScheme.error) }
            Text(stringResource(R.string.pro_restore_note), style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun PinSection(pinGate: PinGate) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val pinHash by PinLock.pinHash.collectAsStateWithLifecycle()
    var settingPin by remember { mutableStateOf(false) }

    if (settingPin) SetPinDialog(onDismiss = { settingPin = false })

    Text(stringResource(R.string.pin_title), style = MaterialTheme.typography.titleMedium)
    Text(
        stringResource(R.string.pin_detail),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    if (pinHash == null) {
        Button(onClick = { settingPin = true }) { Text(stringResource(R.string.pin_set)) }
    } else {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { pinGate.run { settingPin = true } }) {
                Text(stringResource(R.string.pin_change))
            }
            OutlinedButton(onClick = { pinGate.run { scope.launch { PinLock.removePin(context) } } }) {
                Text(stringResource(R.string.pin_remove))
            }
        }
    }
}

@Composable
private fun RulesSection(pinGate: PinGate) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val blocked by Settings.rules(context, block = true).collectAsStateWithLifecycle(initialValue = emptySet())
    val allowed by Settings.rules(context, block = false).collectAsStateWithLifecycle(initialValue = emptySet())
    var input by remember { mutableStateOf("") }
    var invalid by remember { mutableStateOf(false) }

    fun add(block: Boolean) = pinGate.run {
        scope.launch {
            if (MyRules.add(context, input, block)) input = "" else invalid = true
        }
    }

    Text(stringResource(R.string.rules_title), style = MaterialTheme.typography.titleMedium)
    Text(
        stringResource(R.string.rules_detail),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    OutlinedTextField(
        value = input,
        onValueChange = { input = it; invalid = false },
        label = { Text(stringResource(R.string.rules_domain)) },
        singleLine = true,
        isError = invalid,
        supportingText = if (invalid) ({ Text(stringResource(R.string.rules_invalid)) }) else null,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Done),
        modifier = Modifier.fillMaxWidth(),
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = { add(block = true) }, enabled = input.isNotBlank()) { Text(stringResource(R.string.rules_block)) }
        OutlinedButton(onClick = { add(block = false) }, enabled = input.isNotBlank()) {
            Text(stringResource(R.string.rules_allow))
        }
    }
    if (blocked.isEmpty() && allowed.isEmpty()) {
        Text(stringResource(R.string.rules_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    RuleList(R.string.rules_blocked, blocked) { domain ->
        pinGate.run { scope.launch { MyRules.remove(context, domain, block = true) } }
    }
    RuleList(R.string.rules_allowed, allowed) { domain ->
        pinGate.run { scope.launch { MyRules.remove(context, domain, block = false) } }
    }
}

@Composable
private fun RuleList(title: Int, domains: Set<String>, onRemove: (String) -> Unit) {
    if (domains.isEmpty()) return
    Text(stringResource(title), style = MaterialTheme.typography.titleSmall)
    for (domain in domains.sorted()) {
        val removeLabel = stringResource(R.string.rules_remove, domain)
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text(domain, Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            TextButton(
                onClick = { onRemove(domain) },
                modifier = Modifier.semantics { contentDescription = removeLabel },
            ) { Text("✕") }
        }
    }
}

@Composable
private fun HistorySection() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var days by remember { mutableStateOf<List<DaySummary>>(emptyList()) }
    var version by remember { mutableIntStateOf(0) }
    LaunchedEffect(version) { days = History.read(context) }

    Text(stringResource(R.string.history_title), style = MaterialTheme.typography.titleMedium)
    Text(
        stringResource(R.string.history_detail),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    if (days.all { it.total == 0 }) {
        Text(stringResource(R.string.history_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
        return
    }

    val max = days.maxOf { it.total }.coerceAtLeast(1)
    val dateFormat = remember { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM) }
    for ((index, day) in days.withIndex()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (index == 0) stringResource(R.string.history_today) else day.date.format(dateFormat),
                Modifier.weight(0.4f),
                style = MaterialTheme.typography.bodySmall,
            )
            Row(Modifier.weight(0.6f), verticalAlignment = Alignment.CenterVertically) {
                if (day.total > 0) {
                    Card(
                        modifier = Modifier
                            .weight(day.total.toFloat() / max)
                            .padding(vertical = 2.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
                    ) { Text(" ", style = MaterialTheme.typography.bodySmall) }
                }
                if (day.total < max) Spacer(Modifier.weight(1f - day.total.toFloat() / max))
            }
            Text(day.total.toString(), Modifier.padding(start = 8.dp), style = MaterialTheme.typography.bodySmall)
        }
    }

    val week = days.fold(emptyMap<String, Int>()) { acc, day -> History.merge(acc, day.counts) }
    Text(stringResource(R.string.history_top), style = MaterialTheme.typography.titleSmall)
    for ((domain, count) in week.entries.sortedByDescending { it.value }.take(10)) {
        Row {
            Text(
                if (domain == History.ADULT) stringResource(R.string.adult_sites) else domain,
                Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodySmall,
            )
            Text(count.toString(), style = MaterialTheme.typography.bodySmall)
        }
    }
    TextButton(onClick = { scope.launch { History.clear(context); version++ } }) {
        Text(stringResource(R.string.history_clear))
    }
}

private fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
