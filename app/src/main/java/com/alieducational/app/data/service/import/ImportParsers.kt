package com.alieducational.app.data.service.`import`

import java.math.BigDecimal
import java.math.RoundingMode

sealed interface Parsed<out T> {
    data class Ok<T>(val v: T) : Parsed<T>
    data class Err(val msg: String) : Parsed<Nothing>
}

object ImportParsers {

    private val unitSuffixRegex = Regex("(?i)\\s*(pcs|pc|pieces|piece|units|unit|pkts|pkt|boxes|box)\\s*$")
    private val currencyRegex = Regex("(?i)(₹|Rs\\.?|INR|/-)")

    /**
     * Parses a quantity string (e.g. "12 pcs", "1,200", "12.0", "12").
     * Rejects negatives and non-numeric text without silent fallback to zero.
     */
    fun parseQty(raw: String?, defaultIfEmpty: Int? = null): Parsed<Int> {
        if (raw.isNullOrBlank()) {
            return if (defaultIfEmpty != null) Parsed.Ok(defaultIfEmpty)
            else Parsed.Err("Quantity is required")
        }

        var cleaned = raw.trim()
            .replace(",", "")
            .replace(unitSuffixRegex, "")
            .trim()

        if (cleaned.startsWith("-")) {
            return Parsed.Err("Quantity cannot be negative: '$raw'")
        }

        if (cleaned.endsWith(".0") || cleaned.endsWith(".00")) {
            cleaned = cleaned.substringBeforeLast(".")
        }

        val asLong = cleaned.toLongOrNull()
        if (asLong != null) {
            if (asLong < 0) return Parsed.Err("Quantity cannot be negative: '$raw'")
            if (asLong > Int.MAX_VALUE) return Parsed.Err("Quantity exceeds maximum limit: '$raw'")
            return Parsed.Ok(asLong.toInt())
        }

        // Check if fractional
        val asDouble = cleaned.toDoubleOrNull()
        if (asDouble != null) {
            if (asDouble < 0) return Parsed.Err("Quantity cannot be negative: '$raw'")
            if (asDouble == asDouble.toLong().toDouble()) {
                val intVal = asDouble.toLong()
                if (intVal in 0..Int.MAX_VALUE) return Parsed.Ok(intVal.toInt())
            }
            return Parsed.Err("Quantity must be a whole number: '$raw'")
        }

        return Parsed.Err("Invalid quantity: '$raw'")
    }

    /**
     * Parses money into Long paise (e.g. "₹ 1,20,000", "12.50", ".50", "19.999", "12 /-", "12.0").
     * Rejects negatives and non-numeric strings.
     */
    fun parseMoney(raw: String?, defaultIfEmpty: Long? = null): Parsed<Long> {
        if (raw.isNullOrBlank()) {
            return if (defaultIfEmpty != null) Parsed.Ok(defaultIfEmpty)
            else Parsed.Err("Price is required")
        }

        var cleaned = raw.replace(currencyRegex, "")
            .replace(",", "")
            .trim()

        if (cleaned.startsWith("-")) {
            return Parsed.Err("Price cannot be negative: '$raw'")
        }

        if (cleaned.startsWith(".")) {
            cleaned = "0$cleaned"
        }

        if (!Regex("^[0-9]+(\\.[0-9]+)?$").matches(cleaned)) {
            return Parsed.Err("Invalid price format: '$raw'")
        }

        return try {
            val bd = BigDecimal(cleaned)
            val paise = bd.movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValueExact()
            if (paise < 0) Parsed.Err("Price cannot be negative: '$raw'")
            else Parsed.Ok(paise)
        } catch (e: Exception) {
            Parsed.Err("Invalid price: '$raw'")
        }
    }

    /**
     * Parses pack size (must be >= 1).
     */
    fun parsePackSize(raw: String?, defaultIfEmpty: Int = 1): Parsed<Int> {
        if (raw.isNullOrBlank()) return Parsed.Ok(defaultIfEmpty)
        when (val qty = parseQty(raw, defaultIfEmpty)) {
            is Parsed.Ok -> {
                return if (qty.v <= 0) Parsed.Err("Pack size must be at least 1: '$raw'")
                else Parsed.Ok(qty.v)
            }
            is Parsed.Err -> return qty
        }
    }

    /**
     * Parses a barcode: strips control/whitespace, preserves leading zeros.
     */
    fun parseBarcode(raw: String?): Parsed<String?> {
        if (raw.isNullOrBlank()) return Parsed.Ok(null)
        val cleaned = raw.replace(Regex("[\\p{Cntrl}\\s]"), "").trim()
        return if (cleaned.isEmpty()) Parsed.Ok(null) else Parsed.Ok(cleaned)
    }
}
