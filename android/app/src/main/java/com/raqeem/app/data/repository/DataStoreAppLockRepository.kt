package com.raqeem.app.data.repository

import android.content.SharedPreferences
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import com.raqeem.app.domain.model.Result
import com.raqeem.app.domain.model.UnlockState
import com.raqeem.app.domain.repository.AppLockRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DataStoreAppLockRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val securePreferences: SharedPreferences,
) : AppLockRepository {

    override fun observeUnlockState(): Flow<UnlockState> {
        return dataStore.data.map { prefs ->
            UnlockState(
                isLockOnLaunchEnabled = prefs[LOCK_ON_LAUNCH] ?: false,
                isBiometricEnabled = prefs[BIOMETRIC_ENABLED] ?: false,
                hasPin = securePreferences.contains(PIN_HASH_KEY) && securePreferences.contains(PIN_SALT_KEY),
                isLocked = prefs[IS_LOCKED] ?: false,
                lastBackgroundAtMillis = prefs[LAST_BACKGROUND_AT],
            )
        }
    }

    override suspend fun refreshLockState() {
        dataStore.edit { prefs ->
            val isLockEnabled = prefs[LOCK_ON_LAUNCH] ?: false
            val hasPin = securePreferences.contains(PIN_HASH_KEY) && securePreferences.contains(PIN_SALT_KEY)
            val hasBiometric = prefs[BIOMETRIC_ENABLED] ?: false
            val lastBackgroundAt = prefs[LAST_BACKGROUND_AT]
            val shouldLock = isLockEnabled &&
                (hasPin || hasBiometric) &&
                (
                    prefs[IS_LOCKED] == true ||
                        lastBackgroundAt?.let { System.currentTimeMillis() - it >= AUTO_LOCK_TIMEOUT_MS } == true
                    )
            prefs[IS_LOCKED] = shouldLock
        }
    }

    override suspend fun markAppBackgrounded() {
        dataStore.edit { prefs ->
            prefs[LAST_BACKGROUND_AT] = System.currentTimeMillis()
        }
    }

    override suspend fun markUnlocked() {
        dataStore.edit { prefs ->
            prefs[IS_LOCKED] = false
            prefs[LAST_BACKGROUND_AT] = 0L
        }
    }

    override suspend fun setLockOnLaunchEnabled(enabled: Boolean): Result<Unit> {
        dataStore.edit { prefs ->
            prefs[LOCK_ON_LAUNCH] = enabled
            if (!enabled) {
                prefs[IS_LOCKED] = false
            }
        }
        return Result.Success(Unit)
    }

    override suspend fun setBiometricEnabled(enabled: Boolean): Result<Unit> {
        dataStore.edit { prefs ->
            prefs[BIOMETRIC_ENABLED] = enabled
        }
        return Result.Success(Unit)
    }

    override suspend fun setPin(pin: String): Result<Unit> {
        if (!pin.matches(Regex("\\d{4,6}"))) {
            return Result.Error("PIN must be 4 to 6 digits.")
        }
        val salt = ByteArray(16).also(SecureRandom()::nextBytes)
        val hash = derivePinHash(pin, salt)
        securePreferences.edit()
            .putString(PIN_SALT_KEY, salt.toHex())
            .putString(PIN_HASH_KEY, hash.toHex())
            .apply()
        dataStore.edit { prefs ->
            prefs[LOCK_ON_LAUNCH] = true
        }
        return Result.Success(Unit)
    }

    override suspend fun clearPin(): Result<Unit> {
        securePreferences.edit()
            .remove(PIN_SALT_KEY)
            .remove(PIN_HASH_KEY)
            .apply()
        return Result.Success(Unit)
    }

    override suspend fun verifyPin(pin: String): Boolean {
        val salt = securePreferences.getString(PIN_SALT_KEY, null)?.hexToBytes() ?: return false
        val expectedHash = securePreferences.getString(PIN_HASH_KEY, null)?.hexToBytes() ?: return false
        val actualHash = derivePinHash(pin, salt)
        return MessageDigest.isEqual(actualHash, expectedHash)
    }

    override suspend fun unlockWithBiometric(): Result<Unit> {
        markUnlocked()
        return Result.Success(Unit)
    }

    override suspend fun lockNow(): Result<Unit> {
        dataStore.edit { prefs ->
            prefs[IS_LOCKED] = true
        }
        return Result.Success(Unit)
    }

    private fun derivePinHash(pin: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(pin.toCharArray(), salt, 120_000, 256)
        return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            .generateSecret(spec)
            .encoded
    }

    private fun ByteArray.toHex(): String = joinToString("") { byte -> "%02x".format(byte) }

    private fun String.hexToBytes(): ByteArray {
        return chunked(2).map { it.toInt(16).toByte() }.toByteArray()
    }

    private companion object {
        private const val PIN_HASH_KEY = "lock_pin_hash"
        private const val PIN_SALT_KEY = "lock_pin_salt"
        private const val AUTO_LOCK_TIMEOUT_MS = 5 * 60 * 1000L

        private val LOCK_ON_LAUNCH = booleanPreferencesKey("lock_on_launch")
        private val BIOMETRIC_ENABLED = booleanPreferencesKey("biometric_enabled")
        private val IS_LOCKED = booleanPreferencesKey("is_locked")
        private val LAST_BACKGROUND_AT = longPreferencesKey("last_background_at")
    }
}

