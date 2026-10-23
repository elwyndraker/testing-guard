package com.pg.privacyguard.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.pg.privacyguard.R
import com.pg.privacyguard.data.PinLock
import kotlinx.coroutines.launch

/** Runs actions that the Pro PIN lock protects; asks for the PIN first when one is needed. */
class PinGate internal constructor(private val request: (() -> Unit) -> Unit) {
    fun run(action: () -> Unit) = request(action)
}

/** Returns a [PinGate] and shows the PIN dialog when one of its actions needs it. */
@Composable
fun rememberPinGate(): PinGate {
    var pending by remember { mutableStateOf<(() -> Unit)?>(null) }
    pending?.let { action ->
        PinEntryDialog(
            onDismiss = { pending = null },
            onUnlocked = {
                pending = null
                action()
            },
        )
    }
    return remember {
        PinGate { action -> if (PinLock.isOpen()) action() else pending = action }
    }
}

@Composable
private fun PinEntryDialog(onDismiss: () -> Unit, onUnlocked: () -> Unit) {
    val scope = rememberCoroutineScope()
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val wrongText = stringResource(R.string.pin_wrong)
    val waitText = stringResource(R.string.pin_wait)

    fun submit() = scope.launch {
        if (PinLock.unlock(pin)) {
            onUnlocked()
        } else {
            pin = ""
            val wait = PinLock.waitMs()
            error = if (wait > 0) waitText.format((wait + 999) / 1000) else wrongText
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.pin_enter)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                PinField(pin, { pin = it; error = null }, error)
                Text(
                    stringResource(R.string.pin_forgot),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { submit() }, enabled = pin.isNotEmpty()) { Text(stringResource(R.string.ok)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

/** Asks for a new PIN twice and saves it. */
@Composable
fun SetPinDialog(onDismiss: () -> Unit) {
    val scope = rememberCoroutineScope()
    var first by remember { mutableStateOf("") }
    var second by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val mismatchText = stringResource(R.string.pin_mismatch)
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.pin_set)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                PinField(first, { first = it; error = null }, null,
                    stringResource(R.string.pin_new, PinLock.MIN_LENGTH, PinLock.MAX_LENGTH))
                PinField(second, { second = it; error = null }, error, stringResource(R.string.pin_confirm))
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (first != second) {
                        error = mismatchText
                    } else {
                        scope.launch {
                            PinLock.setPin(context, first)
                            onDismiss()
                        }
                    }
                },
                enabled = PinLock.isValidPin(first) && second.isNotEmpty(),
            ) { Text(stringResource(R.string.ok)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

@Composable
private fun PinField(value: String, onChange: (String) -> Unit, error: String?, label: String? = null) {
    OutlinedTextField(
        value = value,
        onValueChange = { new -> onChange(new.filter { it in '0'..'9' }.take(PinLock.MAX_LENGTH)) },
        label = label?.let { { Text(it) } },
        singleLine = true,
        isError = error != null,
        supportingText = error?.let { { Text(it) } },
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
    )
}
