package com.alieducational.app.data.service

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Android Keystore AES-GCM authenticated encryption for sensitive local secrets (Section SEC-05).
 * Sealing format: "v1:" + Base64(IV[12] + CiphertextWithAuthTag).
 */
object SecretBox {
    private const val ALIAS = "ali-secret-box-v1"
    private const val PREFIX = "v1:"

    @Synchronized
    private fun key(): SecretKey {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        if (!ks.containsAlias(ALIAS)) {
            val keyGen = KeyGenerator.getInstance("AES", "AndroidKeyStore")
            keyGen.init(
                KeyGenParameterSpec.Builder(
                    ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .build()
            )
            keyGen.generateKey()
        }
        return ks.getKey(ALIAS, null) as SecretKey
    }

    /**
     * Seals a plaintext secret using AES-256-GCM authenticated encryption bound to the device's hardware Keystore.
     */
    fun seal(plain: String): String {
        if (plain.isEmpty()) return ""
        val c = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.ENCRYPT_MODE, key()) }
        val cipherBytes = c.doFinal(plain.toByteArray(Charsets.UTF_8))
        return PREFIX + Base64.encodeToString(c.iv + cipherBytes, Base64.NO_WRAP)
    }

    /**
     * Unseals a sealed secret. Returns the original plaintext, or null if the ciphertext is tampered,
     * invalid, or originating from a different device keystore.
     */
    fun open(sealed: String): String? = runCatching {
        if (sealed.isEmpty()) return ""
        require(sealed.startsWith(PREFIX)) { "Invalid secret box prefix" }
        val b = Base64.decode(sealed.removePrefix(PREFIX), Base64.NO_WRAP)
        require(b.size > 28) { "Ciphertext too short for IV and GCM tag" }
        val c = Cipher.getInstance("AES/GCM/NoPadding").apply {
            init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, b.copyOfRange(0, 12)))
        }
        String(c.doFinal(b.copyOfRange(12, b.size)), Charsets.UTF_8)
    }.getOrNull()
}

/**
 * Pure helper for migrating legacy plaintext secrets to sealed format (SEC-05).
 */
object SecretBoxMigration {
    fun isSealed(value: String): Boolean = value.startsWith("v1:")

    data class ResolveResult(
        val value: String,
        val needsReSeal: Boolean
    )

    fun resolve(stored: String?, unsealFn: (String) -> String?): ResolveResult {
        if (stored.isNullOrEmpty()) return ResolveResult("", false)
        if (isSealed(stored)) {
            val opened = unsealFn(stored) ?: ""
            return ResolveResult(opened, false)
        }
        // Legacy plaintext: return value as-is and flag for re-sealing
        return ResolveResult(stored, true)
    }
}
