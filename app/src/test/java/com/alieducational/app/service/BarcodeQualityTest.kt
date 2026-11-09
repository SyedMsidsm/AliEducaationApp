package com.alieducational.app.service

import com.alieducational.app.domain.barcode.ShopBarcode
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.MultiFormatReader
import com.google.zxing.MultiFormatWriter
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import org.junit.Assert.*
import org.junit.Test
import java.security.SecureRandom

class BarcodeQualityTest {

    @Test
    fun testShopBarcodeUniqueness_10000Codes() {
        val count = 10_000
        val set = HashSet<String>(count)
        val rng = SecureRandom()
        for (i in 0 until count) {
            val code = ShopBarcode.newPayload("PIECE", 1, rng)
            assertTrue("Collision detected at iteration $i: $code", set.add(code))
        }
        assertEquals(count, set.size)
    }

    @Test
    fun testShopBarcodeFormat_PieceAndPack() {
        val pieceCode = ShopBarcode.newPayload("PIECE", 1)
        assertEquals("Piece code must be 12 characters", 12, pieceCode.length)
        assertTrue("Piece code must start with AE-P", pieceCode.startsWith("AE-P"))

        // Crockford alphabet: 0-9, A-Z excluding I, L, O, U
        val crockfordChars = "0123456789ABCDEFGHJKMNPQRSTVWXYZ".toSet()
        val payloadChars = pieceCode.removePrefix("AE-P")
        assertEquals(8, payloadChars.length)
        assertTrue("Payload must use Crockford base-32", payloadChars.all { it in crockfordChars })

        val packCode = ShopBarcode.newPayload("PACK", 12)
        assertTrue("Pack code must start with AE-P", packCode.startsWith("AE-P"))
        assertTrue("Pack code must end with -PK12", packCode.endsWith("-PK12"))
    }

    @Test
    fun testModuleWidth_StandardPresets() {
        val pieceCode = "AE-P12345678" // 12 chars
        // Code 128: 11 * 14 + 13 = 167 modules
        // On 50x25 mm thermal (136 pt printable width)
        val widthMmThermal50 = ShopBarcode.moduleWidthMm(pieceCode, 136f)
        assertTrue("Module width on 50x25 must be >= 0.25 mm, got $widthMmThermal50", widthMmThermal50 >= 0.25f)

        // On A4 3x8 (180 pt printable width)
        val widthMmA4 = ShopBarcode.moduleWidthMm(pieceCode, 180f)
        assertTrue("Module width on A4 3x8 must be >= 0.30 mm, got $widthMmA4", widthMmA4 >= 0.30f)
    }

    @Test
    fun testEan13ChecksumValidation() {
        assertTrue("Valid EAN-13 should pass", ShopBarcode.isEan13Valid("8901234567890"))
        assertTrue("Valid EAN-13 should pass", ShopBarcode.isEan13Valid("4006381333931"))
        assertFalse("Invalid check digit must fail", ShopBarcode.isEan13Valid("8901234567891"))
        assertFalse("Non-13 digits must fail", ShopBarcode.isEan13Valid("123456"))
    }

    @Test
    fun testZxingDecodeRoundtrip_200RandomCodes() {
        val writer = MultiFormatWriter()
        val reader = MultiFormatReader()
        val rng = SecureRandom()

        for (i in 0 until 200) {
            val payload = ShopBarcode.newPayload("PIECE", 1, rng)
            val width = 360
            val height = 100
            val bitMatrix = writer.encode(payload, BarcodeFormat.CODE_128, width, height)

            // Convert BitMatrix to RGBLuminanceSource without Android Bitmap
            val pixels = IntArray(width * height)
            for (y in 0 until height) {
                for (x in 0 until width) {
                    pixels[y * width + x] = if (bitMatrix.get(x, y)) 0xFF000000.toInt() else 0xFFFFFFFF.toInt()
                }
            }

            val source = RGBLuminanceSource(width, height, pixels)
            val bitmap = BinaryBitmap(HybridBinarizer(source))
            val result = reader.decode(bitmap)

            assertEquals("Decoded barcode must match generated payload on iteration $i", payload, result.text)
            reader.reset()
        }
    }
}
