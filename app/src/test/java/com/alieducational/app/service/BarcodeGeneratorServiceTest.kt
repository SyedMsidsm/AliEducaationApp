package com.alieducational.app.service

import com.alieducational.app.data.service.BarcodeGeneratorService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BarcodeGeneratorServiceTest {

    private val service = BarcodeGeneratorService()

    @Test
    fun testPieceAndPackBarcodesAreDistinct_Section16() {
        val productId = "prod-abc-12345"
        val pieceBarcode = service.createBarcodePayload(productId, "PIECE", 1)
        val packBarcode = service.createBarcodePayload(productId, "PACK", 10)

        // Must start with AE-
        assertTrue(pieceBarcode.startsWith("AE-"))
        assertTrue(packBarcode.startsWith("AE-"))

        // Must have distinct piece and pack identifiers
        assertTrue(pieceBarcode.endsWith("-PC"))
        assertTrue(packBarcode.endsWith("-PK10"))

        assertNotEquals(pieceBarcode, packBarcode)
    }

    @Test
    fun testBarcodePayloadIsDeterministic() {
        val productId = "stationery-pen-blue"
        val code1 = service.createBarcodePayload(productId, "PIECE", 1)
        val code2 = service.createBarcodePayload(productId, "PIECE", 1)
        assertEquals(code1, code2)
    }
}
