package com.alieducational.app.service

import com.alieducational.app.data.local.entity.SaleEntity
import com.alieducational.app.data.local.entity.SaleLineEntity
import com.alieducational.app.data.service.CashTrackerExcelService
import com.alieducational.app.data.service.ImportExportService
import com.alieducational.app.data.service.ReceiptService
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class ReceiptPrintingAndImportLimitsTest {

    private lateinit var cashService: CashTrackerExcelService
    private lateinit var importService: ImportExportService
    private lateinit var receiptService: ReceiptService

    @Before
    fun setup() {
        cashService = CashTrackerExcelService()
        importService = ImportExportService()
        receiptService = ReceiptService()
    }

    @Test
    fun testCashTrackerExcel_RowLimitExceeded() {
        val workbook = XSSFWorkbook()
        val sheet = workbook.createSheet("Transactions")
        val header = sheet.createRow(0)
        header.createCell(0).setCellValue("Date")
        header.createCell(1).setCellValue("Description")
        header.createCell(2).setCellValue("Category")
        header.createCell(3).setCellValue("Income")
        header.createCell(4).setCellValue("Expense")

        // Set row 20005 to trigger row limit
        sheet.createRow(20005).createCell(1).setCellValue("Overflow entry")

        val preview = cashService.parseWorkbook(workbook, "large_cash.xlsx")
        assertNotNull(preview.errorMessage)
        assertTrue(
            "Expected row limit error, got: ${preview.errorMessage}",
            preview.errorMessage?.contains("exceeds the 20,000 row limit") == true
        )
    }

    @Test
    fun testCashTrackerExcel_NormalSheetAllowed() {
        val workbook = XSSFWorkbook()
        val sheet = workbook.createSheet("Transactions")
        val header = sheet.createRow(0)
        header.createCell(0).setCellValue("Date")
        header.createCell(1).setCellValue("Description")
        header.createCell(2).setCellValue("Category")
        header.createCell(3).setCellValue("Expense")

        for (i in 1..5) {
            val r = sheet.createRow(i)
            r.createCell(0).setCellValue("01/11/2026")
            r.createCell(1).setCellValue("Shop Expense $i")
            r.createCell(2).setCellValue("Supplies")
            r.createCell(3).setCellValue("100.00")
        }

        val preview = cashService.parseWorkbook(workbook, "normal_cash.xlsx")
        assertNull("Expected no error, got: ${preview.errorMessage}", preview.errorMessage)
        assertEquals(5, preview.items.size)
    }

    @Test
    fun testImportExport_ExcelRowLimitExceeded_Preview() {
        val workbook = XSSFWorkbook()
        val sheet = workbook.createSheet("Products")
        val header = sheet.createRow(0)
        header.createCell(0).setCellValue("Product Name")
        header.createCell(1).setCellValue("Sale Price")
        sheet.createRow(20005).createCell(0).setCellValue("Overflow item")

        val out = ByteArrayOutputStream()
        workbook.write(out)
        val inStream = ByteArrayInputStream(out.toByteArray())

        val preview = importService.previewExcel(inStream, "large_catalog.xlsx", 0)
        assertNotNull(preview.errorMessage)
        assertTrue(
            "Expected row limit error, got: ${preview.errorMessage}",
            preview.errorMessage?.contains("exceeds the 20,000 row limit") == true
        )
    }

    @Test
    fun testImportExport_ExcelRowLimitExceeded_ReadAll() {
        val workbook = XSSFWorkbook()
        val sheet = workbook.createSheet("Products")
        val header = sheet.createRow(0)
        header.createCell(0).setCellValue("Product Name")
        header.createCell(1).setCellValue("Sale Price")
        sheet.createRow(20005).createCell(0).setCellValue("Overflow item")

        val out = ByteArrayOutputStream()
        workbook.write(out)
        val inStream = ByteArrayInputStream(out.toByteArray())

        try {
            importService.readAllExcel(inStream, 0)
            fail("Expected IllegalArgumentException for exceeding 20,000 rows")
        } catch (e: IllegalArgumentException) {
            assertTrue(
                "Expected row limit error message, got: ${e.message}",
                e.message?.contains("exceeds the 20,000 row limit") == true
            )
        }
    }

    @Test
    fun testImportExport_CsvRowLimitExceeded_ReadAll() {
        val sb = java.lang.StringBuilder()
        sb.append("Product Name,Category,Price\n")
        for (i in 1..20005) {
            sb.append("Item $i,Books,50.00\n")
        }
        val inStream = ByteArrayInputStream(sb.toString().toByteArray())

        try {
            importService.readAllCsv(inStream)
            fail("Expected IllegalArgumentException for CSV exceeding 20,000 rows")
        } catch (e: IllegalArgumentException) {
            assertTrue(
                "Expected row limit error message, got: ${e.message}",
                e.message?.contains("exceeds the 20,000 row limit") == true
            )
        }
    }

    @Test
    fun testReceiptService_SummaryText() {
        val sale = SaleEntity(
            receiptNumber = "ALI-20261109-001",
            timestamp = System.currentTimeMillis(),
            grossTotalPaise = 25000L,
            netTotalPaise = 25000L,
            amountReceivedPaise = 25000L,
            paymentMethod = "CASH",
            customerName = "Test Customer"
        )
        val line = SaleLineEntity(
            saleId = sale.id,
            productId = "prod-1",
            productName = "Register 200 Pages",
            unitLabel = "pcs",
            quantityBase = 2,
            unitSellingPricePaise = 12500L,
            unitCostSnapshotPaise = 5000L,
            lineGrossPaise = 25000L,
            lineNetPaise = 25000L,
            lineProfitPaise = 15000L
        )

        val summary = receiptService.generateReceiptSummaryText(sale, listOf(line))
        assertTrue(summary.contains("Ali Educational"))
        assertTrue(summary.contains("ALI-20261109-001"))
        assertTrue(summary.contains("Register 200 Pages"))
        assertTrue(summary.contains("Cash"))
    }
}
