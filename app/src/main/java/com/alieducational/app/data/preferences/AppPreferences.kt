package com.alieducational.app.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import org.json.JSONObject
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "ali_educational_preferences")

@Singleton
class AppPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val IS_FIRST_LAUNCH = booleanPreferencesKey("is_first_launch")
        val IS_LOCK_ENABLED = booleanPreferencesKey("is_lock_enabled")
        val LOCK_TIMEOUT_SECONDS = intPreferencesKey("lock_timeout_seconds")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val APP_LANGUAGE = stringPreferencesKey("app_language")
        val SHOP_NAME = stringPreferencesKey("shop_name")
        val LAST_BACKUP_TIME = longPreferencesKey("last_backup_time")
        val LAST_EXTERNAL_SAVE_TIME = longPreferencesKey("last_external_save_time")
        val LAST_EXTERNAL_SAVE_DESTINATION = stringPreferencesKey("last_external_save_destination")
        val RECEIPT_ACTION = stringPreferencesKey("receipt_action")
        val RECEIPT_LANGUAGE = stringPreferencesKey("receipt_language")
        val PIGMY_USUAL_AMOUNT_PAISE = longPreferencesKey("pigmy_usual_amount_paise")
        val DEVICE_ROLE = stringPreferencesKey("device_role") // "OWNER" or "BILLING"
        val DEVICE_ID = stringPreferencesKey("device_id")
        val PAIRED_OWNER_HOST = stringPreferencesKey("paired_owner_host")
        val SYNC_TOKEN = stringPreferencesKey("sync_token")
        val SYNC_PORT = intPreferencesKey("sync_port")
        val APP_TEXT_SIZE = stringPreferencesKey("app_text_size") // "Small", "Default", "Large", "Extra Large"
        val GOOGLE_DRIVE_FOLDER_URI = stringPreferencesKey("google_drive_folder_uri")
        val GOOGLE_DRIVE_FOLDER_NAME = stringPreferencesKey("google_drive_folder_name")
        val OWNER_PIN = stringPreferencesKey("owner_pin")
        val PIN_FAILED_ATTEMPTS = intPreferencesKey("pin_failed_attempts")
        val PIN_BLOCKED_UNTIL = longPreferencesKey("pin_blocked_until")
        val OWNER_CERT_PIN = stringPreferencesKey("owner_cert_pin")
    }

    val startupSettings = context.dataStore.data.map { p -> Pair(p[Keys.IS_FIRST_LAUNCH] ?: true, p[Keys.IS_LOCK_ENABLED] ?: false) }

    val isFirstLaunch: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[Keys.IS_FIRST_LAUNCH] ?: true
    }

    val isLockEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[Keys.IS_LOCK_ENABLED] ?: false
    }

    val lockTimeoutSeconds: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[Keys.LOCK_TIMEOUT_SECONDS] ?: 120
    }

    val themeMode: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[Keys.THEME_MODE] ?: "dark"
    }

    val appLanguage: Flow<String> = context.dataStore.data.map { preferences ->
        // Enforce English UI as per latest decision
        "en"
    }

    val shopName: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[Keys.SHOP_NAME] ?: "Ali Educational"
    }

    val lastBackupTime: Flow<Long> = context.dataStore.data.map { preferences ->
        preferences[Keys.LAST_BACKUP_TIME] ?: 0L
    }

    val receiptAction: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[Keys.RECEIPT_ACTION] ?: "preview"
    }

    val receiptLanguage: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[Keys.RECEIPT_LANGUAGE] ?: "en"
    }

    val pigmyUsualAmountPaise: Flow<Long> = context.dataStore.data.map { preferences ->
        preferences[Keys.PIGMY_USUAL_AMOUNT_PAISE] ?: 100000L // default ₹1,000.00
    }

    val deviceRole: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[Keys.DEVICE_ROLE] ?: "OWNER"
    }

    val deviceId: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[Keys.DEVICE_ID] ?: "device-main"
    }

    val pairedOwnerHost: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[Keys.PAIRED_OWNER_HOST] ?: ""
    }

    val syncToken: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[Keys.SYNC_TOKEN]?.takeUnless { it == "AE-SYNC-987654" } ?: ""
    }

    val syncPort: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[Keys.SYNC_PORT] ?: 8080
    }

    val appTextSize: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[Keys.APP_TEXT_SIZE] ?: "Default"
    }

    val googleDriveFolderUri: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[Keys.GOOGLE_DRIVE_FOLDER_URI] ?: ""
    }

    val googleDriveFolderName: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[Keys.GOOGLE_DRIVE_FOLDER_NAME] ?: ""
    }

    val ownerPin: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[Keys.OWNER_PIN]?.takeIf { it.startsWith("pbkdf2:") } ?: ""
    }

    val lastExternalSaveTime: Flow<Long> = context.dataStore.data.map { preferences ->
        preferences[Keys.LAST_EXTERNAL_SAVE_TIME] ?: 0L
    }

    val lastExternalSaveDestination: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[Keys.LAST_EXTERNAL_SAVE_DESTINATION] ?: ""
    }

    suspend fun exportSupportedSettings(): JSONObject {
        val p = context.dataStore.data.first()
        val result = JSONObject().put("version", 1)
        listOf(Keys.THEME_MODE, Keys.APP_LANGUAGE, Keys.SHOP_NAME, Keys.RECEIPT_ACTION, Keys.RECEIPT_LANGUAGE, Keys.APP_TEXT_SIZE).forEach { k ->
            p[k]?.let { result.put(k.name, it) }
        }
        result.put("lock_timeout_seconds", p[Keys.LOCK_TIMEOUT_SECONDS] ?: 120)
        result.put("pigmy_usual_amount_paise", p[Keys.PIGMY_USUAL_AMOUNT_PAISE] ?: 100000L)
        return result
    }

    suspend fun importSupportedSettings(settings: JSONObject) {
        require(settings.optInt("version", 1) == 1) { "Unsupported settings format" }
        context.dataStore.edit { p ->
            listOf(Keys.THEME_MODE, Keys.APP_LANGUAGE, Keys.SHOP_NAME, Keys.RECEIPT_ACTION, Keys.RECEIPT_LANGUAGE, Keys.APP_TEXT_SIZE).forEach { k ->
                if (settings.has(k.name)) p[k] = settings.getString(k.name) else p.remove(k)
            }
            if (settings.has("pigmy_usual_amount_paise")) p[Keys.PIGMY_USUAL_AMOUNT_PAISE] = settings.getLong("pigmy_usual_amount_paise").coerceAtLeast(0)
            if (settings.has("lock_timeout_seconds")) p[Keys.LOCK_TIMEOUT_SECONDS] = settings.getInt("lock_timeout_seconds").coerceIn(0, 3600)
        }
    }

    suspend fun setAppTextSize(size: String) {
        context.dataStore.edit { preferences ->
            preferences[Keys.APP_TEXT_SIZE] = size
        }
    }

    suspend fun setGoogleDriveFolder(uri: String, name: String) {
        context.dataStore.edit { preferences ->
            preferences[Keys.GOOGLE_DRIVE_FOLDER_URI] = uri
            preferences[Keys.GOOGLE_DRIVE_FOLDER_NAME] = name
        }
    }

    suspend fun clearGoogleDriveFolder() {
        context.dataStore.edit { preferences ->
            preferences[Keys.GOOGLE_DRIVE_FOLDER_URI] = ""
            preferences[Keys.GOOGLE_DRIVE_FOLDER_NAME] = ""
        }
    }

    val ownerCertificatePin = context.dataStore.data.map { it[Keys.OWNER_CERT_PIN] ?: "" }
    suspend fun setOwnerCertificatePin(pin: String) { context.dataStore.edit { it[Keys.OWNER_CERT_PIN] = pin.trim() } }
    suspend fun ensureSyncToken(): String {
        var token = ""
        context.dataStore.edit { p ->
            token = p[Keys.SYNC_TOKEN]?.takeIf { it.length >= 32 && it != "AE-SYNC-987654" }
                ?: java.util.UUID.randomUUID().toString() + java.util.UUID.randomUUID().toString()
            p[Keys.SYNC_TOKEN] = token
        }
        return token
    }
    fun recoverOwnerOnTrustedDevice(activity: androidx.fragment.app.FragmentActivity, done: (String?) -> Unit) {
        val proof = java.io.File(context.getExternalFilesDir(null), "owner-recovery-once.txt")
        fun authorized(): Boolean = runCatching {
            val hash = java.security.MessageDigest.getInstance("SHA-256").digest(proof.readText().trim().toByteArray()).joinToString("") { "%02x".format(it) }
            hash == "73b4f082c2baa60945eab42fd74bd247316adc0008d9d0fa0e960e083fe02263"
        }.getOrDefault(false)
        if (!authorized()) { done("Recovery is not provisioned on this device. Contact the shop owner."); return }
        val auth = com.alieducational.app.util.BiometricAuthManager(context)
        if (!auth.canAuthenticate()) { done("Set up a secure device screen lock first."); return }
        auth.promptBiometric(activity, "Recover Owner access", "Authenticate with this trusted phone's fingerprint or screen lock", onSuccess = {
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                try {
                    check(authorized()) { "Recovery authorization expired" }
                    context.dataStore.edit { p ->
                        p[Keys.DEVICE_ROLE] = "OWNER"
                        p.remove(Keys.OWNER_PIN)
                        p[Keys.PIN_FAILED_ATTEMPTS] = 0
                        p[Keys.PIN_BLOCKED_UNTIL] = 0L
                    }
                    proof.delete()
                    done(null)
                } catch (e: Exception) { done(e.message ?: "Recovery failed") }
            }
        }, onError = { done(it) })
    }

    suspend fun setOwnerPin(pin: String) {
        require(deviceRole.first() == "OWNER" && pin.matches(Regex("[0-9]{6,12}"))) { "Owner PIN must contain 6–12 digits" }
        val salt = ByteArray(16).also { java.security.SecureRandom().nextBytes(it) }
        val hash = javax.crypto.SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(javax.crypto.spec.PBEKeySpec(pin.toCharArray(), salt, 120000, 256)).encoded
        context.dataStore.edit { p -> p[Keys.OWNER_PIN] = "pbkdf2:" + android.util.Base64.encodeToString(salt, 2) + ":" + android.util.Base64.encodeToString(hash, 2) }
    }
    private val pinMutex = kotlinx.coroutines.sync.Mutex()
    suspend fun authorizeOwner(pin: String): Boolean = pinMutex.withLock {
        val settings = context.dataStore.data.first()
        if (System.currentTimeMillis() < (settings[Keys.PIN_BLOCKED_UNTIL] ?: 0L)) return@withLock false
        val stored = ownerPin.first().split(":")
        if (stored.size != 3) return@withLock false
        val valid = if (pin.matches(Regex("[0-9]{6,12}"))) {
            val salt = android.util.Base64.decode(stored[1], 2)
            val expected = android.util.Base64.decode(stored[2], 2)
            val actual = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
                javax.crypto.SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(javax.crypto.spec.PBEKeySpec(pin.toCharArray(), salt, 120000, 256)).encoded
            }
            java.security.MessageDigest.isEqual(expected, actual)
        } else false
        context.dataStore.edit { p ->
            if (valid) {
                p[Keys.DEVICE_ROLE] = "OWNER"
                p[Keys.PIN_FAILED_ATTEMPTS] = 0
                p[Keys.PIN_BLOCKED_UNTIL] = 0L
            } else {
                val count = (p[Keys.PIN_FAILED_ATTEMPTS] ?: 0) + 1
                p[Keys.PIN_FAILED_ATTEMPTS] = if (count >= 5) 0 else count
                if (count >= 5) p[Keys.PIN_BLOCKED_UNTIL] = System.currentTimeMillis() + 60000
            }
        }
        valid
    }

    suspend fun setPigmyUsualAmountPaise(amountPaise: Long) {
        context.dataStore.edit { preferences ->
            preferences[Keys.PIGMY_USUAL_AMOUNT_PAISE] = amountPaise
        }
    }

    suspend fun setDeviceRole(role: String) {
        require(role in setOf("OWNER", "BILLING")) { "Invalid device role" }
        require(role != "BILLING" || deviceRole.first() != "OWNER" || ownerPin.first().isNotBlank()) { "Set an Owner PIN before switching to Billing" }
        require(role != "OWNER" || deviceRole.first() == "OWNER") { "Owner authorization required" }
        context.dataStore.edit { preferences ->
            preferences[Keys.DEVICE_ROLE] = role
        }
    }

    suspend fun setDeviceId(id: String) {
        context.dataStore.edit { preferences ->
            preferences[Keys.DEVICE_ID] = id
        }
    }

    suspend fun setPairedOwnerHost(host: String) {
        context.dataStore.edit { preferences ->
            preferences[Keys.PAIRED_OWNER_HOST] = host
        }
    }

    suspend fun setSyncToken(token: String) {
        context.dataStore.edit { preferences ->
            preferences[Keys.SYNC_TOKEN] = token
        }
    }

    suspend fun setSyncPort(port: Int) {
        context.dataStore.edit { preferences ->
            preferences[Keys.SYNC_PORT] = port
        }
    }

    suspend fun setFirstLaunch(isFirst: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[Keys.IS_FIRST_LAUNCH] = isFirst
        }
    }

    suspend fun setLockEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[Keys.IS_LOCK_ENABLED] = enabled
        }
    }

    suspend fun setLockTimeoutSeconds(seconds: Int) {
        context.dataStore.edit { preferences ->
            preferences[Keys.LOCK_TIMEOUT_SECONDS] = seconds
        }
    }

    suspend fun setThemeMode(mode: String) {
        context.dataStore.edit { preferences ->
            preferences[Keys.THEME_MODE] = mode
        }
    }

    suspend fun setAppLanguage(language: String) {
        context.dataStore.edit { preferences ->
            preferences[Keys.APP_LANGUAGE] = language
        }
    }

    suspend fun setShopName(name: String) {
        context.dataStore.edit { preferences ->
            preferences[Keys.SHOP_NAME] = name
        }
    }

    suspend fun setLastExternalSave(timestamp: Long, destination: String) {
        context.dataStore.edit { preferences ->
            preferences[Keys.LAST_EXTERNAL_SAVE_TIME] = timestamp
            preferences[Keys.LAST_EXTERNAL_SAVE_DESTINATION] = destination
        }
    }

    suspend fun setLastBackupTime(timestamp: Long) {
        context.dataStore.edit { preferences ->
            preferences[Keys.LAST_BACKUP_TIME] = timestamp
        }
    }

    suspend fun setReceiptAction(action: String) {
        context.dataStore.edit { preferences ->
            preferences[Keys.RECEIPT_ACTION] = action
        }
    }

    suspend fun setReceiptLanguage(language: String) {
        context.dataStore.edit { preferences ->
            preferences[Keys.RECEIPT_LANGUAGE] = language
        }
    }
}
