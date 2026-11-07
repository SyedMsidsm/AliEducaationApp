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
import kotlinx.coroutines.flow.Flow
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
    }

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
        preferences[Keys.SYNC_TOKEN] ?: "AE-SYNC-987654"
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
        preferences[Keys.OWNER_PIN] ?: "1234"
    }

    val lastExternalSaveTime: Flow<Long> = context.dataStore.data.map { preferences ->
        preferences[Keys.LAST_EXTERNAL_SAVE_TIME] ?: 0L
    }

    val lastExternalSaveDestination: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[Keys.LAST_EXTERNAL_SAVE_DESTINATION] ?: ""
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

    suspend fun setOwnerPin(pin: String) {
        context.dataStore.edit { preferences ->
            preferences[Keys.OWNER_PIN] = pin
        }
    }

    suspend fun setPigmyUsualAmountPaise(amountPaise: Long) {
        context.dataStore.edit { preferences ->
            preferences[Keys.PIGMY_USUAL_AMOUNT_PAISE] = amountPaise
        }
    }

    suspend fun setDeviceRole(role: String) {
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
