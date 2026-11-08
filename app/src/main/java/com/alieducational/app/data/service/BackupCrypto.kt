package com.alieducational.app.data.service

import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.io.PushbackInputStream
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Streaming AES-256-GCM chunked encryption for shop database backups (Section SEC-06).
 *
 * Wire format:
 * - Magic "ALIBK1" (6 bytes ASCII)
 * - Version (1 byte: 0x01)
 * - Salt (16 bytes random)
 * - Base IV (12 bytes random)
 * - Consecutive chunks:
 *     - chunkLength: Int (4 bytes)
 *     - isLastFlag: Byte (1 byte: 1 if final chunk, 0 otherwise)
 *     - cipherChunk: ByteArray (chunkLength bytes: ciphertext + 128-bit GCM tag)
 *
 * Security:
 * - Key derivation: PBKDF2WithHmacSHA256, 210,000 rounds, 256-bit key.
 * - Nonce: baseIv (12 bytes) with bytes 4..11 XORed with chunkIndex (Long).
 * - AAD: 8-byte chunkIndex + 1-byte isLastFlag. Protects against chunk deletion, reordering, and truncation.
 */
object BackupCrypto {

    private val MAGIC = "ALIBK1".toByteArray(Charsets.US_ASCII)
    private const val VERSION: Byte = 0x01
    private const val PBKDF2_ITERATIONS = 210_000
    private const val KEY_BITS = 256
    private const val CHUNK_BUFFER_SIZE = 1024 * 1024 // 1 MiB chunk payload
    private const val GCM_TAG_LENGTH_BITS = 128
    private const val GCM_TAG_LENGTH_BYTES = 16

    private fun deriveKey(passphrase: CharArray, salt: ByteArray): SecretKey {
        val spec = PBEKeySpec(passphrase, salt, PBKDF2_ITERATIONS, KEY_BITS)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val keyBytes = factory.generateSecret(spec).encoded
        return SecretKeySpec(keyBytes, "AES")
    }

    private fun computeNonce(baseIv: ByteArray, chunkIndex: Long): ByteArray {
        val nonce = baseIv.copyOf()
        for (i in 0..7) {
            val shift = (7 - i) * 8
            val b = ((chunkIndex ushr shift) and 0xFF).toByte()
            nonce[4 + i] = (nonce[4 + i].toInt() xor b.toInt()).toByte()
        }
        return nonce
    }

    private fun computeAad(chunkIndex: Long, isLast: Boolean): ByteArray {
        val aad = ByteArray(9)
        for (i in 0..7) {
            val shift = (7 - i) * 8
            aad[i] = ((chunkIndex ushr shift) and 0xFF).toByte()
        }
        aad[8] = if (isLast) 1 else 0
        return aad
    }

    /**
     * Checks if the given input stream begins with the "ALIBK1" magic bytes without consuming
     * the rest of the stream.
     */
    fun isEncryptedBackup(input: InputStream): Boolean {
        if (!input.markSupported()) {
            val buf = ByteArray(MAGIC.size)
            var read = 0
            while (read < MAGIC.size) {
                val r = input.read(buf, read, MAGIC.size - read)
                if (r == -1) return false
                read += r
            }
            return buf.contentEquals(MAGIC)
        }
        input.mark(MAGIC.size + 1)
        val buf = ByteArray(MAGIC.size)
        var read = 0
        while (read < MAGIC.size) {
            val r = input.read(buf, read, MAGIC.size - read)
            if (r == -1) {
                input.reset()
                return false
            }
            read += r
        }
        input.reset()
        return buf.contentEquals(MAGIC)
    }

    /**
     * Encrypts input stream to output stream using streaming chunked AES-256-GCM.
     */
    fun encrypt(input: InputStream, out: OutputStream, passphrase: CharArray) {
        val random = SecureRandom()
        val salt = ByteArray(16).also { random.nextBytes(it) }
        val baseIv = ByteArray(12).also { random.nextBytes(it) }

        val key = deriveKey(passphrase, salt)
        val dataOut = DataOutputStream(out)
        val pushbackInput = if (input is PushbackInputStream) input else PushbackInputStream(input, 1)

        // 1. Write Header
        dataOut.write(MAGIC)
        dataOut.writeByte(VERSION.toInt())
        dataOut.write(salt)
        dataOut.write(baseIv)

        // 2. Stream and encrypt chunks
        val buffer = ByteArray(CHUNK_BUFFER_SIZE)
        var chunkIndex = 0L
        var done = false

        while (!done) {
            var bytesRead = 0
            while (bytesRead < CHUNK_BUFFER_SIZE) {
                val r = pushbackInput.read(buffer, bytesRead, CHUNK_BUFFER_SIZE - bytesRead)
                if (r == -1) break
                bytesRead += r
            }

            // Check if this is the last chunk
            var isLast = false
            if (bytesRead < CHUNK_BUFFER_SIZE) {
                isLast = true
                done = true
            } else {
                val nextByte = pushbackInput.read()
                if (nextByte == -1) {
                    isLast = true
                    done = true
                } else {
                    pushbackInput.unread(nextByte)
                    isLast = false
                }
            }

            val nonce = computeNonce(baseIv, chunkIndex)
            val aad = computeAad(chunkIndex, isLast)

            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(GCM_TAG_LENGTH_BITS, nonce))
            cipher.updateAAD(aad)
            val cipherChunk = cipher.doFinal(buffer, 0, bytesRead)

            dataOut.writeInt(cipherChunk.size)
            dataOut.writeByte(if (isLast) 1 else 0)
            dataOut.write(cipherChunk)

            chunkIndex++
        }
        dataOut.flush()
    }

    /**
     * Decrypts an encrypted backup stream. Returns true on successful verified decryption;
     * returns false on incorrect passphrase, corrupted data, or tampering.
     */
    fun decrypt(input: InputStream, out: OutputStream, passphrase: CharArray): Boolean = runCatching {
        val dataIn = DataInputStream(input)

        // 1. Validate Magic
        val magicBuf = ByteArray(MAGIC.size)
        dataIn.readFully(magicBuf)
        if (!magicBuf.contentEquals(MAGIC)) return false

        // 2. Validate Version
        val version = dataIn.readByte()
        if (version != VERSION) return false

        // 3. Read Salt and Base IV
        val salt = ByteArray(16)
        dataIn.readFully(salt)
        val baseIv = ByteArray(12)
        dataIn.readFully(baseIv)

        val key = deriveKey(passphrase, salt)

        // 4. Decrypt chunks sequentially
        var chunkIndex = 0L
        var isLast = false

        while (!isLast) {
            val chunkLength = dataIn.readInt()
            if (chunkLength < GCM_TAG_LENGTH_BYTES || chunkLength > CHUNK_BUFFER_SIZE + GCM_TAG_LENGTH_BYTES) {
                return false
            }

            val isLastFlag = dataIn.readByte()
            isLast = (isLastFlag.toInt() == 1)

            val cipherChunk = ByteArray(chunkLength)
            dataIn.readFully(cipherChunk)

            val nonce = computeNonce(baseIv, chunkIndex)
            val aad = computeAad(chunkIndex, isLast)

            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_LENGTH_BITS, nonce))
            cipher.updateAAD(aad)
            val plainChunk = cipher.doFinal(cipherChunk)

            out.write(plainChunk)
            chunkIndex++
        }
        out.flush()
        true
    }.getOrDefault(false)
}
