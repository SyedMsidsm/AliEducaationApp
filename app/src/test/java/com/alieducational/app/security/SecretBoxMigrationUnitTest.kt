package com.alieducational.app.security

import com.alieducational.app.data.service.SecretBoxMigration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SecretBoxMigrationUnitTest {

    @Test
    fun testLegacyPlaintextDetectedAndMarkedForReseal_SEC05() {
        val legacyToken = "a1b2c3d4e5f678901234567890abcdef"
        val result = SecretBoxMigration.resolve(legacyToken) {
            throw IllegalStateException("unsealFn must NOT be called for legacy plaintext")
        }

        assertEquals("Legacy plaintext value must be preserved on read", legacyToken, result.value)
        assertTrue("Legacy value must be flagged as needing re-sealing", result.needsReSeal)
    }

    @Test
    fun testSealedValueOpenedAndNotMarkedForReseal_SEC05() {
        val sealedToken = "v1:dGVzdF9jaXBoZXJ0ZXh0X2Jsb2I="
        var unsealCalledWith: String? = null

        val result = SecretBoxMigration.resolve(sealedToken) { encrypted ->
            unsealCalledWith = encrypted
            "decrypted_plain_text"
        }

        assertEquals("unsealFn must be invoked with sealed token", sealedToken, unsealCalledWith)
        assertEquals("Decrypted plain text must be returned", "decrypted_plain_text", result.value)
        assertFalse("Already sealed value does NOT need re-sealing", result.needsReSeal)
    }

    @Test
    fun testTamperedOrCorruptedSealedValueReturnsEmpty_SEC05() {
        val tamperedToken = "v1:dGFtcGVyZWRfYnl0ZXM="
        val result = SecretBoxMigration.resolve(tamperedToken) { null }

        assertEquals("Failed unseal must produce empty string to trigger re-pair", "", result.value)
        assertFalse("Failed unseal must not be flagged for re-sealing", result.needsReSeal)
    }

    @Test
    fun testEmptyOrNullValueProducesEmptyResult_SEC05() {
        val nullResult = SecretBoxMigration.resolve(null) { "never" }
        assertEquals("", nullResult.value)
        assertFalse(nullResult.needsReSeal)

        val emptyResult = SecretBoxMigration.resolve("") { "never" }
        assertEquals("", emptyResult.value)
        assertFalse(emptyResult.needsReSeal)
    }

    @Test
    fun testIsSealedPrefixCheck_SEC05() {
        assertTrue(SecretBoxMigration.isSealed("v1:any_data"))
        assertFalse(SecretBoxMigration.isSealed("any_data"))
        assertFalse(SecretBoxMigration.isSealed(""))
        assertFalse(SecretBoxMigration.isSealed("v2:different_version"))
    }
}
