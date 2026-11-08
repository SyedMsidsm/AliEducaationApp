package com.alieducational.app.security

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.alieducational.app.data.service.SecretBox
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SecretBoxInstrumentedTest {

    @Test
    fun testSealAndOpenRoundTrip_SEC05() {
        val originalSecret = "ae_sync_secret_token_1234567890abcdef"
        val sealed = SecretBox.seal(originalSecret)

        assertTrue("Sealed value must start with prefix v1:", sealed.startsWith("v1:"))
        assertTrue("Sealed value must not contain plaintext", !sealed.contains(originalSecret))

        val opened = SecretBox.open(sealed)
        assertNotNull("Opened secret must not be null", opened)
        assertEquals("Opened secret must match original plaintext", originalSecret, opened)
    }

    @Test
    fun testTamperedCiphertextReturnsNull_SEC05() {
        val originalSecret = "highly_confidential_sync_token"
        val sealed = SecretBox.seal(originalSecret)

        // Tamper with the base64 ciphertext portion (flip a character)
        val prefix = "v1:"
        val payload = sealed.removePrefix(prefix)
        val tamperedPayload = if (payload.last() == 'A') payload.dropLast(1) + "B" else payload.dropLast(1) + "A"
        val tamperedSealed = prefix + tamperedPayload

        val opened = SecretBox.open(tamperedSealed)
        assertNull("Tampered ciphertext must fail authentication and return null", opened)
    }

    @Test
    fun testNonV1PrefixReturnsNull_SEC05() {
        val invalidCiphertext = "plain_legacy_token_without_v1_prefix"
        val opened = SecretBox.open(invalidCiphertext)
        assertNull("Invalid prefix must fail and return null", opened)
    }

    @Test
    fun testEmptySecretHandling_SEC05() {
        assertEquals("", SecretBox.seal(""))
        assertEquals("", SecretBox.open(""))
    }
}
