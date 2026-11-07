package com.alieducational.inventory.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.AtomicFile
import androidx.room.Room
import androidx.room.RoomDatabase
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import java.io.File
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** All calls are serialized by the model's worker. No destructive migration fallback. */
class EncryptedStore(private val context: Context) {
    private var database: ShopDatabase? = null
    private var password: ByteArray? = null
    val db: ShopDatabase get() = checkNotNull(database) { "Database is locked" }

    fun open() {
        if (database != null) return
        val key = loadDatabaseKey()
        try {
            System.loadLibrary("sqlcipher")
            val candidate = Room.databaseBuilder(context, ShopDatabase::class.java, "shop.db")
                .openHelperFactory(SupportOpenHelperFactory(key))
                .setJournalMode(RoomDatabase.JournalMode.TRUNCATE)
                .addMigrations(DatabaseMigrations.V1_TO_V2,DatabaseMigrations.V2_TO_V3)
                .build()
            try {
                candidate.openHelper.writableDatabase // Force authentication/open before exposing UI.
                if (candidate.foundation().settings() == null) candidate.foundation().saveSettings(SettingsRecord())
                database = candidate
                password = key
            } catch (error: Exception) {
                candidate.close()
                throw error
            }
        } catch (error: Exception) {
            key.fill(0)
            throw error
        }
    }

    fun close() {
        database?.close()
        database = null
        password?.fill(0)
        password = null
    }

    private fun loadDatabaseKey(): ByteArray {
        val alias = "ali.database.wrap.v1"
        val file = AtomicFile(File(context.noBackupFilesDir, "database-key.v1"))
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        // If a device-bound key disappears, never replace the existing database or wrapped key.
        if (!keyStore.containsAlias(alias)) {
            check(!file.baseFile.exists() && !context.getDatabasePath("shop.db").exists()) {
                "Device key unavailable; recovery is required"
            }
            KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
                init(KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                    .setKeySize(256)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setUserAuthenticationRequired(true)
                    .setUserAuthenticationParameters(300, KeyProperties.AUTH_BIOMETRIC_STRONG or KeyProperties.AUTH_DEVICE_CREDENTIAL)
                    .build())
                generateKey()
            }
        }
        val wrappingKey = keyStore.getKey(alias, null) as SecretKey
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val aad = "Ali Educational database key v1".toByteArray(Charsets.UTF_8)
        if (file.baseFile.exists()) {
            val bytes = file.readFully()
            check(bytes.size == 60) { "Invalid wrapped key; recovery is required" }
            cipher.init(Cipher.DECRYPT_MODE, wrappingKey, GCMParameterSpec(128, bytes.copyOfRange(0, 12)))
            cipher.updateAAD(aad)
            return cipher.doFinal(bytes.copyOfRange(12, bytes.size))
        }
        check(!context.getDatabasePath("shop.db").exists()) { "Database key missing; recovery is required" }
        val key = ByteArray(32).also { SecureRandom().nextBytes(it) }
        try {
            cipher.init(Cipher.ENCRYPT_MODE, wrappingKey)
            cipher.updateAAD(aad)
            val output = file.startWrite()
            try {
                output.write(cipher.iv + cipher.doFinal(key))
                file.finishWrite(output)
            } catch (error: Exception) {
                file.failWrite(output)
                throw error
            }
            return key
        } catch (error: Exception) {
            key.fill(0)
            throw error
        }
    }
}
