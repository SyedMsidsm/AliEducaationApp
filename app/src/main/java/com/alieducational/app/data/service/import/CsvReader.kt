package com.alieducational.app.data.service.`import`

import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.Charset
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets

object CsvReader {
    const val MAX_FILE_SIZE_BYTES = 10 * 1024 * 1024L // 10 MB
    const val MAX_ROWS = 20_000

    /**
     * Reads a CSV input stream with BOM detection, charset fallback (UTF-8 -> Windows-1252),
     * delimiter sniffing (comma, semicolon, tab), quoted multi-line fields, and escaped quotes.
     */
    fun read(inputStream: InputStream, maxRows: Int = MAX_ROWS): List<List<String>> {
        val bytes = inputStream.readBytes()
        if (bytes.size > MAX_FILE_SIZE_BYTES) {
            throw IllegalArgumentException("File exceeds the 10 MB limit (${bytes.size / (1024 * 1024)} MB). Please split into smaller files.")
        }

        val (text, _) = decodeText(bytes)
        val delimiter = sniffDelimiter(text)
        return parseCsvText(text, delimiter, maxRows)
    }

    private fun decodeText(rawBytes: ByteArray): Pair<String, Charset> {
        var offset = 0
        if (rawBytes.size >= 3 &&
            rawBytes[0] == 0xEF.toByte() &&
            rawBytes[1] == 0xBB.toByte() &&
            rawBytes[2] == 0xBF.toByte()
        ) {
            offset = 3
        } else if (rawBytes.size >= 2 &&
            rawBytes[0] == 0xFE.toByte() &&
            rawBytes[1] == 0xFF.toByte()
        ) {
            return Pair(String(rawBytes, 2, rawBytes.size - 2, StandardCharsets.UTF_16BE), StandardCharsets.UTF_16BE)
        } else if (rawBytes.size >= 2 &&
            rawBytes[0] == 0xFF.toByte() &&
            rawBytes[1] == 0xFE.toByte()
        ) {
            return Pair(String(rawBytes, 2, rawBytes.size - 2, StandardCharsets.UTF_16LE), StandardCharsets.UTF_16LE)
        }

        val slice = ByteBuffer.wrap(rawBytes, offset, rawBytes.size - offset)
        return try {
            val decoder = StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
            val charBuffer = decoder.decode(slice)
            Pair(charBuffer.toString(), StandardCharsets.UTF_8)
        } catch (_: CharacterCodingException) {
            val win1252 = Charset.forName("windows-1252")
            Pair(String(rawBytes, offset, rawBytes.size - offset, win1252), win1252)
        }
    }

    fun sniffDelimiter(text: String): Char {
        val sampleLines = mutableListOf<String>()
        var inQuotes = false
        val currentLine = StringBuilder()

        for (i in 0 until text.length.coerceAtMost(8192)) {
            val c = text[i]
            if (c == '"') {
                inQuotes = !inQuotes
                currentLine.append(c)
            } else if ((c == '\n' || c == '\r') && !inQuotes) {
                if (currentLine.isNotBlank()) {
                    sampleLines.add(currentLine.toString())
                    if (sampleLines.size >= 10) break
                }
                currentLine.clear()
            } else {
                currentLine.append(c)
            }
        }
        if (currentLine.isNotBlank() && sampleLines.size < 10) {
            sampleLines.add(currentLine.toString())
        }

        if (sampleLines.isEmpty()) return ','

        val candidates = listOf(',', ';', '\t')
        var bestDelimiter = ','
        var highestScore = -1

        for (cand in candidates) {
            val counts = sampleLines.map { line -> countCharOutsideQuotes(line, cand) }
            val total = counts.sum()
            val first = counts.firstOrNull() ?: 0
            val isConsistent = counts.all { it == first && it > 0 }
            val score = total + (if (isConsistent) 1000 else 0)
            if (score > highestScore && total > 0) {
                highestScore = score
                bestDelimiter = cand
            }
        }

        return bestDelimiter
    }

    private fun countCharOutsideQuotes(line: String, target: Char): Int {
        var count = 0
        var inQuotes = false
        for (c in line) {
            if (c == '"') inQuotes = !inQuotes
            else if (c == target && !inQuotes) count++
        }
        return count
    }

    fun parseCsvText(text: String, delimiter: Char, maxRows: Int = MAX_ROWS): List<List<String>> {
        val rows = ArrayList<List<String>>()
        var currentRow = ArrayList<String>()
        val currentField = StringBuilder()
        var inQuotes = false
        var i = 0
        val len = text.length

        while (i < len) {
            val c = text[i]

            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < len && text[i + 1] == '"') {
                        currentField.append('"')
                        i++
                    } else {
                        inQuotes = false
                    }
                } else {
                    currentField.append(c)
                }
            } else {
                when (c) {
                    '"' -> inQuotes = true
                    delimiter -> {
                        currentRow.add(currentField.toString().trim())
                        currentField.clear()
                    }
                    '\r' -> {
                        if (i + 1 < len && text[i + 1] == '\n') {
                            i++
                        }
                        currentRow.add(currentField.toString().trim())
                        currentField.clear()
                        if (currentRow.any { it.isNotEmpty() }) {
                            rows.add(currentRow)
                            if (rows.size > maxRows) {
                                throw IllegalArgumentException("File exceeds the $maxRows row limit. Please split into smaller files.")
                            }
                        }
                        currentRow = ArrayList()
                    }
                    '\n' -> {
                        currentRow.add(currentField.toString().trim())
                        currentField.clear()
                        if (currentRow.any { it.isNotEmpty() }) {
                            rows.add(currentRow)
                            if (rows.size > maxRows) {
                                throw IllegalArgumentException("File exceeds the $maxRows row limit. Please split into smaller files.")
                            }
                        }
                        currentRow = ArrayList()
                    }
                    else -> currentField.append(c)
                }
            }
            i++
        }

        if (currentField.isNotEmpty() || currentRow.isNotEmpty()) {
            currentRow.add(currentField.toString().trim())
            if (currentRow.any { it.isNotEmpty() }) {
                rows.add(currentRow)
                if (rows.size > maxRows) {
                    throw IllegalArgumentException("File exceeds the $maxRows row limit. Please split into smaller files.")
                }
            }
        }

        return rows
    }
}
