package com.alieducational.app.domain.barcode

import java.security.SecureRandom

object ShopBarcode {
    private const val ALPHABET = "0123456789ABCDEFGHJKMNPQRSTVWXYZ" // Crockford Base-32 (no I, L, O, U)

    /**
     * Generates a short, random shop barcode payload.
     * Piece code: "AE-P" + 8 Base-32 chars (12 chars total).
     * Pack code: "AE-P" + 8 Base-32 chars + "-PK" + packQty.
     */
    fun newPayload(
        unitType: String = "PIECE",
        packQty: Int = 1,
        rng: SecureRandom = SecureRandom()
    ): String {
        val b = ByteArray(5).also(rng::nextBytes)
        var v = 0L
        for (x in b) {
            v = (v shl 8) or (x.toLong() and 0xFF)
        }
        val core = "AE-P" + (0 until 8).map { ALPHABET[((v shr (5 * (7 - it))) and 31).toInt()] }.joinToString("")
        return if (unitType.equals("PACK", ignoreCase = true) && packQty > 1) {
            "$core-PK$packQty"
        } else {
            core
        }
    }

    /**
     * Calculates the module width in millimeters for a given payload and printable width in points.
     * Code 128 has: Start (11) + data ((len) * 11) + Check (11) + Stop (13) = (len + 2) * 11 + 13 modules.
     */
    fun moduleWidthMm(payload: String, printableWidthPt: Float): Float {
        val totalModules = (payload.length + 2) * 11 + 13
        val printableWidthMm = printableWidthPt * (25.4f / 72.0f)
        return printableWidthMm / totalModules.toFloat()
    }

    /**
     * Validates a 13-digit EAN-13 barcode using the standard modulo-10 algorithm.
     */
    fun isEan13Valid(code: String): Boolean {
        if (!Regex("^\\d{13}$").matches(code)) return false
        var sum = 0
        for (i in 0 until 12) {
            val digit = code[i].digitToInt()
            sum += if (i % 2 == 0) digit else digit * 3
        }
        val checkDigit = (10 - (sum % 10)) % 10
        return checkDigit == code[12].digitToInt()
    }
}
