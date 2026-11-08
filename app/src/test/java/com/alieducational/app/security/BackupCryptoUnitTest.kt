package com.alieducational.app.security

import com.alieducational.app.data.service.BackupCrypto
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.Random

class BackupCryptoUnitTest {

    private val passphrase = "CorrectHorseBatteryStaple2026!".toCharArray()
    private val wrongPassphrase = "WrongPassword123!".toCharArray()

    @Test
    fun testEmptyStreamRoundTrip_SEC06() {
        val input = ByteArray(0)
        val encryptedOut = ByteArrayOutputStream()
        BackupCrypto.encrypt(ByteArrayInputStream(input), encryptedOut, passphrase)

        val encryptedBytes = encryptedOut.toByteArray()
        assertTrue("Output must start with ALIBK1 magic header", BackupCrypto.isEncryptedBackup(ByteArrayInputStream(encryptedBytes)))

        val decryptedOut = ByteArrayOutputStream()
        val success = BackupCrypto.decrypt(ByteArrayInputStream(encryptedBytes), decryptedOut, passphrase)
        assertTrue("Decryption of empty stream must succeed", success)
        assertArrayEquals(input, decryptedOut.toByteArray())
    }

    @Test
    fun testSingleByteRoundTrip_SEC06() {
        val input = byteArrayOf(42)
        val encryptedOut = ByteArrayOutputStream()
        BackupCrypto.encrypt(ByteArrayInputStream(input), encryptedOut, passphrase)

        val decryptedOut = ByteArrayOutputStream()
        val success = BackupCrypto.decrypt(ByteArrayInputStream(encryptedOut.toByteArray()), decryptedOut, passphrase)
        assertTrue(success)
        assertArrayEquals(input, decryptedOut.toByteArray())
    }

    @Test
    fun testExactOneMebibyteChunkRoundTrip_SEC06() {
        val input = ByteArray(1024 * 1024)
        Random(12345).nextBytes(input)

        val encryptedOut = ByteArrayOutputStream()
        BackupCrypto.encrypt(ByteArrayInputStream(input), encryptedOut, passphrase)

        val decryptedOut = ByteArrayOutputStream()
        val success = BackupCrypto.decrypt(ByteArrayInputStream(encryptedOut.toByteArray()), decryptedOut, passphrase)
        assertTrue("Exact 1 MiB boundary chunk decryption must succeed", success)
        assertArrayEquals(input, decryptedOut.toByteArray())
    }

    @Test
    fun testMultiChunkRandomDataRoundTrip_SEC06() {
        // 2.5 MiB triggers multiple chunks
        val input = ByteArray((2.5 * 1024 * 1024).toInt())
        Random(67890).nextBytes(input)

        val encryptedOut = ByteArrayOutputStream()
        BackupCrypto.encrypt(ByteArrayInputStream(input), encryptedOut, passphrase)

        val decryptedOut = ByteArrayOutputStream()
        val success = BackupCrypto.decrypt(ByteArrayInputStream(encryptedOut.toByteArray()), decryptedOut, passphrase)
        assertTrue("Multi-chunk decryption must succeed", success)
        assertArrayEquals(input, decryptedOut.toByteArray())
    }

    @Test
    fun testWrongPassphraseReturnsFalse_SEC06() {
        val input = "Shop sensitive records and customer balances".toByteArray(Charsets.UTF_8)
        val encryptedOut = ByteArrayOutputStream()
        BackupCrypto.encrypt(ByteArrayInputStream(input), encryptedOut, passphrase)

        val decryptedOut = ByteArrayOutputStream()
        val success = BackupCrypto.decrypt(ByteArrayInputStream(encryptedOut.toByteArray()), decryptedOut, wrongPassphrase)
        assertFalse("Decryption with wrong passphrase must return false", success)
        assertEquals("Output must remain empty on wrong passphrase", 0, decryptedOut.size())
    }

    @Test
    fun testFlippedByteInCiphertextReturnsFalse_SEC06() {
        val input = "Customer Name: Ali, Due: 50000".toByteArray(Charsets.UTF_8)
        val encryptedOut = ByteArrayOutputStream()
        BackupCrypto.encrypt(ByteArrayInputStream(input), encryptedOut, passphrase)

        val tamperedBytes = encryptedOut.toByteArray()
        // Flip a byte in the encrypted chunk
        val targetIdx = tamperedBytes.size - 5
        tamperedBytes[targetIdx] = (tamperedBytes[targetIdx].toInt() xor 0xFF).toByte()

        val decryptedOut = ByteArrayOutputStream()
        val success = BackupCrypto.decrypt(ByteArrayInputStream(tamperedBytes), decryptedOut, passphrase)
        assertFalse("Tampered ciphertext must fail authentication and return false", success)
        assertEquals(0, decryptedOut.size())
    }

    @Test
    fun testTruncatedFileReturnsFalse_SEC06() {
        val input = ByteArray(1024 * 100) // 100 KB
        Random(42).nextBytes(input)
        val encryptedOut = ByteArrayOutputStream()
        BackupCrypto.encrypt(ByteArrayInputStream(input), encryptedOut, passphrase)

        val truncatedBytes = encryptedOut.toByteArray().copyOf(encryptedOut.size() - 20)
        val decryptedOut = ByteArrayOutputStream()
        val success = BackupCrypto.decrypt(ByteArrayInputStream(truncatedBytes), decryptedOut, passphrase)
        assertFalse("Truncated backup file must fail and return false", success)
    }

    @Test
    fun testLegacyZipSignatureDetected_SEC06() {
        // Standard ZIP header: PK\x03\x04
        val zipHeader = byteArrayOf(0x50, 0x4B, 0x03, 0x04, 0x00, 0x00)
        assertFalse("Legacy ZIP must not be reported as ALIBK1 encrypted backup", BackupCrypto.isEncryptedBackup(ByteArrayInputStream(zipHeader)))

        val alibkHeader = "ALIBK1".toByteArray(Charsets.US_ASCII) + byteArrayOf(0x01)
        assertTrue("ALIBK1 header must be recognized", BackupCrypto.isEncryptedBackup(ByteArrayInputStream(alibkHeader)))
    }
}
