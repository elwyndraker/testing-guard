package com.pg.privacyguard.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Pro PIN that protects turning protection off and changing filters, e.g. for a parent
 * setting up a child's phone. Only a salted hash of the PIN is saved. It doesn't stop someone
 * from turning the VPN off in Android's settings or uninstalling the app; the screen says so.
 */
object PinLock {
    const val MIN_LENGTH = 4
    const val MAX_LENGTH = 8

    // After a correct PIN, settings stay open this long so each change doesn't ask again.
    private const val UNLOCK_MS = 5 * 60 * 1000L
    private const val MAX_TRIES = 5
    private const val WAIT_AFTER_TRIES_MS = 30_000L
    private const val ITERATIONS = 20_000

    private val _hash = MutableStateFlow<String?>(null)

    @Volatile
    private var loaded = false

    @Volatile
    private var unlockedUntil = 0L
    private var wrongTries = 0
    private var waitUntil = 0L

    /** True when a PIN is set and Pro is active, i.e. the PIN must be asked for. */
    val isLocked: Boolean get() = _hash.value != null && Pro.active.value

    /** The saved PIN hash, or null when no PIN is set (whether or not Pro is active). */
    val pinHash: StateFlow<String?> = _hash

    suspend fun load(context: Context) {
        if (loaded) return
        _hash.value = Settings.pinHash(context).first()
        loaded = true
    }

    /** True if no PIN is needed right now: none set, Pro off, or entered in the last few minutes. */
    fun isOpen(now: Long = System.currentTimeMillis()) = !isLocked || now < unlockedUntil

    /** Milliseconds until another try is allowed after too many wrong PINs, or 0. */
    @Synchronized
    fun waitMs(now: Long = System.currentTimeMillis()) = (waitUntil - now).coerceAtLeast(0)

    suspend fun setPin(context: Context, pin: String) {
        val hash = withContext(Dispatchers.Default) { hash(pin) }
        Settings.setPinHash(context, hash)
        _hash.value = hash
        unlockedUntil = System.currentTimeMillis() + UNLOCK_MS
    }

    suspend fun removePin(context: Context) {
        Settings.setPinHash(context, null)
        _hash.value = null
    }

    /** Checks [pin]; a correct one keeps settings open for a few minutes. */
    suspend fun unlock(pin: String): Boolean {
        val stored = _hash.value ?: return true
        if (waitMs() > 0) return false
        val ok = withContext(Dispatchers.Default) { matches(pin, stored) }
        synchronized(this) {
            val now = System.currentTimeMillis()
            if (ok) {
                wrongTries = 0
                unlockedUntil = now + UNLOCK_MS
            } else if (++wrongTries >= MAX_TRIES) {
                wrongTries = 0
                waitUntil = now + WAIT_AFTER_TRIES_MS
            }
        }
        return ok
    }

    fun isValidPin(pin: String) = pin.length in MIN_LENGTH..MAX_LENGTH && pin.all { it in '0'..'9' }

    internal fun hash(pin: String, salt: ByteArray = ByteArray(16).also { SecureRandom().nextBytes(it) }): String =
        encode(salt) + ":" + encode(derive(pin, salt))

    internal fun matches(pin: String, stored: String): Boolean {
        val parts = stored.split(':')
        if (parts.size != 2) return false
        val salt = Base64.getDecoder().decode(parts[0])
        return MessageDigest.isEqual(derive(pin, salt), Base64.getDecoder().decode(parts[1]))
    }

    private fun derive(pin: String, salt: ByteArray): ByteArray =
        SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            .generateSecret(PBEKeySpec(pin.toCharArray(), salt, ITERATIONS, 256))
            .encoded

    private fun encode(bytes: ByteArray) = Base64.getEncoder().encodeToString(bytes)
}
