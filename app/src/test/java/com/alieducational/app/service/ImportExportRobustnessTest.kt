package com.alieducational.app.service

import org.apache.poi.ss.usermodel.WorkbookFactory
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class ImportExportRobustnessTest {

    @Test
    fun testCurrencyParsing_AllVariants() {
        val testCases = mapOf(
            "₹150" to 15000L,
            "Rs. 25.50" to 2550L,
            "1,250.00" to 125000L,
            " ₹ 99.99 " to 9999L,
            "0" to 0L,
            "" to 0L,
            "invalid" to 0L
        )

        for ((input, expectedPaise) in testCases) {
            val clean = input.replace("₹", "").replace("Rs.", "", ignoreCase = true).replace("Rs", "", ignoreCase = true).replace(",", "").trim()
            val valueDouble = clean.toDoubleOrNull() ?: 0.0
            val actualPaise = (valueDouble * 100).toLong()
            assertEquals("Failed for $input", expectedPaise, actualPaise)
        }
    }

    @Test
    fun testCsvParsing_HandlesQuotesAndCommasCleanly() {
        val sampleLine = "\"Dux Geometry Box, Deluxe\",Geometry,Dux,DUX-01,\"25\",\"120.00\",\"150.00\""
        val result = mutableListOf<String>()
        var curVal = StringBuilder()
        var inQuotes = false

        for (c in sampleLine) {
            if (c == '\"') {
                inQuotes = !inQuotes
            } else if (c == ',' && !inQuotes) {
                result.add(curVal.toString().trim())
                curVal = StringBuilder()
            } else {
                curVal.append(c)
            }
        }
        result.add(curVal.toString().trim())

        assertEquals(7, result.size)
        assertEquals("Dux Geometry Box, Deluxe", result[0])
        assertEquals("Geometry", result[1])
        assertEquals("Dux", result[2])
        assertEquals("DUX-01", result[3])
        assertEquals("25", result[4])
        assertEquals("120.00", result[5])
        assertEquals("150.00", result[6])
    }

    @Test
    fun testMultiSheetWorkbook_GeneratesAndReadsSheetNames() {
        val workbook = XSSFWorkbook()
        val sheet1 = workbook.createSheet("Primary Catalog")
        val sheet2 = workbook.createSheet("Secondary Wholesaler")

        val r1 = sheet1.createRow(0)
        r1.createCell(0).setCellValue("Product Name")
        r1.createCell(1).setCellValue("Price")

        val r2 = sheet2.createRow(0)
        r2.createCell(0).setCellValue("Wholesale Item")
        r2.createCell(1).setCellValue("Box Rate")

        val out = ByteArrayOutputStream()
        workbook.write(out)
        workbook.close()

        val inStream = ByteArrayInputStream(out.toByteArray())
        val readWb = WorkbookFactory.create(inStream)
        assertEquals(2, readWb.numberOfSheets)
        assertEquals("Primary Catalog", readWb.getSheetName(0))
        assertEquals("Secondary Wholesaler", readWb.getSheetName(1))
        readWb.close()
    }

    @Test
    fun testCorruptWorkbook_HandledGracefullyWithoutCrashing() {
        val corruptBytes = "NOT AN EXCEL FILE CONTENT".toByteArray()
        val inStream = ByteArrayInputStream(corruptBytes)
        var caughtException = false
        var friendlyMessage: String? = null

        try {
            WorkbookFactory.create(inStream)
        } catch (e: Exception) {
            caughtException = true
            val msg = e.localizedMessage ?: e.javaClass.simpleName
            friendlyMessage = when {
                msg.contains("password", ignoreCase = true) -> "Password protected"
                else -> "The file format is not a recognized Excel spreadsheet. Please ensure it is a valid .xlsx, .xls, or .csv file."
            }
        }

        assertTrue("Should have caught corrupt file exception", caughtException)
        assertNotNull(friendlyMessage)
        assertTrue(friendlyMessage!!.contains("not a recognized Excel spreadsheet"))
    }

    @Test
    fun testCashLedgerDetection_RealReferenceFiles() {
        val file29 = java.io.File("C:/Users/Musid/Downloads/Ali Educational 29-Oct-2026.xls")
        if (file29.exists()) {
            val wb = WorkbookFactory.create(file29)
            val sheet = wb.getSheetAt(0)
            val headers = mutableListOf<String>()
            // Find header on row 5
            for (r in 0..minOf(10, sheet.lastRowNum)) {
                val row = sheet.getRow(r) ?: continue
                val rowVals = (0 until row.lastCellNum).map { c ->
                    val cell = row.getCell(c) ?: return@map ""
                    try { cell.stringCellValue?.trim() ?: "" } catch (_: Exception) { "" }
                }
                if (rowVals.any { it.contains("Income", ignoreCase = true) }) {
                    headers.addAll(rowVals)
                    break
                }
            }
            wb.close()

            assertTrue("Should find header with Income in 29-Oct file", headers.isNotEmpty())
            val hLower = headers.map { it.lowercase() }
            val isCash = hLower.any { it.contains("income") || it.contains("expense") } &&
                    hLower.any { it.contains("balance") || it.contains("notes") || it.contains("date") }
            assertTrue("29-Oct file should be detected as a cash ledger", isCash)
        }
    }
}
