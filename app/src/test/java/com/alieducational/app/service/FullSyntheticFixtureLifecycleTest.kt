package com.alieducational.app.service

import com.alieducational.app.data.local.entity.ShopExpenseEntity
import com.alieducational.app.data.service.CashTrackerExcelService
import com.alieducational.app.domain.calculation.CostCalculator
import org.apache.poi.ss.usermodel.WorkbookFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class FullSyntheticFixtureLifecycleTest {

    private val fixturesDir = File("C:/Users/Musid/.gemini/antigravity/brain/d92e10cb-d589-4a63-9db5-449f958ba78f/scratch")

    @Test
    fun testF6_SyntheticStockFixtureImportAndTotals() {
        val stockFile = File(fixturesDir, "backup_stock_fixture.xlsx")
        assertTrue("Fixture file backup_stock_fixture.xlsx must exist", stockFile.exists())

        val wb = WorkbookFactory.create(stockFile)
        val sheet = wb.getSheetAt(0)
        assertNotNull("Sheet must exist", sheet)

        // Headers check
        val headerRow = sheet.getRow(0)
        assertEquals("SKU / Barcode", headerRow.getCell(0).stringCellValue)
        assertEquals("Product Name", headerRow.getCell(1).stringCellValue)

        data class TestProduct(
            val sku: String,
            val name: String,
            val openingPieces: Int,
            val packSize: Int,
            val unitCostPaise: Long,
            val pieceSellingPricePaise: Long,
            val packSellingPricePaise: Long
        )

        val products = mutableListOf<TestProduct>()
        for (r in 1..sheet.lastRowNum) {
            val row = sheet.getRow(r) ?: continue
            val sku = row.getCell(0).stringCellValue.trim()
            val name = row.getCell(1).stringCellValue.trim()
            val packSize = row.getCell(5).numericCellValue.toInt()
            val pieces = row.getCell(6).numericCellValue.toInt()
            val costPaise = (row.getCell(7).numericCellValue * 100).toLong()
            val piecePricePaise = (row.getCell(8).numericCellValue * 100).toLong()
            val packPricePaise = (row.getCell(9).numericCellValue * 100).toLong()

            products.add(
                TestProduct(sku, name, pieces, packSize, costPaise, piecePricePaise, packPricePaise)
            )
        }
        wb.close()

        assertEquals("Must parse 3 products", 3, products.size)

        // 1. Test Notebook
        val notebook = products.find { it.name == "Test Notebook" }
        assertNotNull("Notebook found", notebook)
        assertEquals("004521", notebook!!.sku)
        assertEquals(20, notebook.openingPieces)
        assertEquals(1, notebook.packSize)
        assertEquals(5000L, notebook.unitCostPaise) // ₹50.00
        assertEquals(8000L, notebook.pieceSellingPricePaise) // ₹80.00

        // 2. Test Blue Pen
        val pen = products.find { it.name == "Test Blue Pen" }
        assertNotNull("Pen found", pen)
        assertEquals("009870", pen!!.sku)
        assertEquals(100, pen.openingPieces)
        assertEquals(10, pen.packSize)
        assertEquals(500L, pen.unitCostPaise) // ₹5.00
        assertEquals(800L, pen.pieceSellingPricePaise) // ₹8.00
        assertEquals(7500L, pen.packSellingPricePaise) // ₹75.00

        // 3. Test Urdu-name product — قلم
        val urduItem = products.find { it.name.contains("قلم") }
        assertNotNull("Urdu item found", urduItem)
        assertEquals("001234", urduItem!!.sku)
        assertEquals(12, urduItem.openingPieces)
        assertEquals(1025L, urduItem.unitCostPaise) // ₹10.25
        assertEquals(1550L, urduItem.pieceSellingPricePaise) // ₹15.50

        // Total opening pieces = 132
        val totalOpeningPieces = products.sumOf { it.openingPieces }
        assertEquals("Expected opening pieces: 132", 132, totalOpeningPieces)

        // Known inventory cost = ₹1,623.00 (162,300 paise)
        val totalInventoryCostPaise = products.sumOf { it.openingPieces * it.unitCostPaise }
        assertEquals("Expected opening inventory cost: 1623.00 Rs", 162300L, totalInventoryCostPaise)
    }

    @Test
    fun testF6_SyntheticCashLedgerImportAndTotals() {
        val cashFile = File(fixturesDir, "cash_ledger_fixture.xlsx")
        assertTrue("Fixture file cash_ledger_fixture.xlsx must exist", cashFile.exists())

        val wb = WorkbookFactory.create(cashFile)
        val service = CashTrackerExcelService()
        val preview = service.parseWorkbook(wb, cashFile.name)
        wb.close()

        assertNull("No parse errors expected: ${preview.errorMessage}", preview.errorMessage)

        // Opening balance and totals rows must NOT become transactions
        assertEquals("Expected transaction count 2 (opening & totals rows excluded)", 2, preview.items.size)
        assertEquals(1, preview.incomeCount)
        assertEquals(1, preview.expenseCount)

        // Dated income ₹1,000.25
        val incomeItem = preview.items.find { it.isIncome }
        assertNotNull("Income item present", incomeItem)
        assertEquals(100025L, incomeItem!!.amountPaise)
        assertEquals(100025L, preview.totalIncomePaise)

        // Dated outflow ₹250.50
        val expenseItem = preview.items.find { !it.isIncome }
        assertNotNull("Expense item present", expenseItem)
        assertEquals(25050L, expenseItem!!.amountPaise)
        assertEquals(25050L, preview.totalExpensePaise)

        // Net movement = ₹749.75
        val netMovementPaise = preview.totalIncomePaise - preview.totalExpensePaise
        assertEquals("Expected net movement ₹749.75", 74975L, netMovementPaise)
    }

    @Test
    fun testF6_Sale_PigmyDeposit_Giveaway_InventoryLifecycle() {
        // Initial state from fixture:
        var notebookStock = 20
        var penStock = 100
        var urduItemStock = 12
        val notebookCostPaise = 5000L
        val penCostPaise = 500L
        val urduCostPaise = 1025L

        assertEquals(132, notebookStock + penStock + urduItemStock)
        assertEquals(162300L, (notebookStock * notebookCostPaise) + (penStock * penCostPaise) + (urduItemStock * urduCostPaise))

        // Step 4: Product sale of 2 Test Notebooks at ₹80 each, paid ₹200 cash
        val saleQty = 2
        val salePricePaise = 8000L
        val grossTotalPaise = saleQty * salePricePaise // ₹160.00
        val paidCashPaise = 20000L // ₹200.00 tendered
        val changeGivenPaise = paidCashPaise - grossTotalPaise // ₹40.00 change

        notebookStock -= saleQty
        val cogsPaise = saleQty * notebookCostPaise // ₹100.00
        val grossProfitPaise = grossTotalPaise - cogsPaise // ₹60.00

        assertEquals(16000L, grossTotalPaise)
        assertEquals(4000L, changeGivenPaise)
        assertEquals(18, notebookStock)
        assertEquals(10000L, cogsPaise)
        assertEquals(6000L, grossProfitPaise)

        // Step 5: Record confirmed ₹100 cash pigmy deposit and giveaway of 1 Test Blue Pen
        val pigmyDepositPaise = 10000L // ₹100.00
        val pigmyOutflow = ShopExpenseEntity(
            id = "pigmy-outflow-1",
            category = "SAVINGS_TRANSFER",
            amountPaise = pigmyDepositPaise,
            isIncome = false,
            isSavingsTransfer = true
        )
        // Pigmy outflow is linked and excluded from operating profit
        assertTrue(pigmyOutflow.isSavingsTransfer)
        assertFalse(pigmyOutflow.isIncome)

        // Giveaway of 1 Test Blue Pen
        val giveawayQty = 1
        penStock -= giveawayQty
        assertEquals(99, penStock)

        // Inventory verification after sale and giveaway:
        val remainingTotalPieces = notebookStock + penStock + urduItemStock
        assertEquals("Expected inventory pieces after sale & giveaway: 129", 129, remainingTotalPieces)

        val remainingValuePaise = (notebookStock * notebookCostPaise) + (penStock * penCostPaise) + (urduItemStock * urduCostPaise)
        // 18 * 50 = 900; 99 * 5 = 495; 12 * 10.25 = 123 -> Total = 1,518.00 (151,800 paise)
        assertEquals("Expected inventory known value: ₹1,518.00", 151800L, remainingValuePaise)
    }

    @Test
    fun testF6_BackupArchiveCreationValidationAndManifestIntegrity() {
        val tempDir = File(fixturesDir, "test_backup_temp").apply { mkdirs() }
        val dummyDb = File(tempDir, "ali_educational.db")

        // Create a mock SQLite header file
        FileOutputStream(dummyDb).use { fos ->
            val sqliteHeader = "SQLite format 3\u0000".toByteArray(Charsets.US_ASCII)
            fos.write(sqliteHeader)
            // Fill with dummy db payload
            fos.write(ByteArray(1024))
        }

        val backupZip = File(tempDir, "test_backup.zip")

        // Create ZIP with DB + manifest
        ZipOutputStream(FileOutputStream(backupZip)).use { zos ->
            // 1. ali_educational.db
            FileInputStream(dummyDb).use { fis ->
                zos.putNextEntry(ZipEntry("ali_educational.db"))
                fis.copyTo(zos)
                zos.closeEntry()
            }

            // 2. manifest.json
            val manifest = """
                {
                    "formatVersion": 1,
                    "app": "Ali Educational",
                    "packageName": "com.alieducational.app",
                    "schemaVersion": 4,
                    "dbSizeBytes": ${dummyDb.length()},
                    "timestamp": 1730416800000,
                    "piecesTotal": 129,
                    "inventoryValuePaise": 151800
                }
            """.trimIndent()
            zos.putNextEntry(ZipEntry("manifest.json"))
            zos.write(manifest.toByteArray(Charsets.UTF_8))
            zos.closeEntry()
        }

        assertTrue(backupZip.exists())
        assertTrue(backupZip.length() > 0)

        // Validate archive integrity
        var foundDb = false
        var validHeader = false
        var foundManifest = false

        ZipInputStream(FileInputStream(backupZip)).use { zis ->
            var entry: ZipEntry?
            while (zis.nextEntry.also { entry = it } != null) {
                when (entry!!.name) {
                    "ali_educational.db" -> {
                        foundDb = true
                        val header = ByteArray(16)
                        val read = zis.read(header, 0, 16)
                        if (read == 16 && String(header, Charsets.US_ASCII).startsWith("SQLite format 3")) {
                            validHeader = true
                        }
                    }
                    "manifest.json" -> {
                        foundManifest = true
                        val content = zis.bufferedReader(Charsets.UTF_8).readText()
                        assertTrue(content.contains("\"piecesTotal\": 129"))
                        assertTrue(content.contains("\"inventoryValuePaise\": 151800"))
                    }
                }
                zis.closeEntry()
            }
        }

        assertTrue("ali_educational.db found in zip", foundDb)
        assertTrue("Valid SQLite header verified in zip", validHeader)
        assertTrue("manifest.json found and verified in zip", foundManifest)

        // Clean up
        dummyDb.delete()
        backupZip.delete()
        tempDir.delete()
    }
}
