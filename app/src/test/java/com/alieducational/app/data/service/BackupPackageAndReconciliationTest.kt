package com.alieducational.app.data.service

import com.alieducational.app.data.local.entity.AccountPaymentEntity
import com.alieducational.app.data.local.entity.CashCategoryEntity
import com.alieducational.app.data.local.entity.CustomerEntity
import com.alieducational.app.data.local.entity.PigmyDepositEntity
import com.alieducational.app.data.local.entity.PigmySchemeEntity
import com.alieducational.app.data.local.entity.PigmyWithdrawalEntity
import com.alieducational.app.data.local.entity.ProductEntity
import com.alieducational.app.data.local.entity.PurchaseEntity
import com.alieducational.app.data.local.entity.PurchaseLineEntity
import com.alieducational.app.data.local.entity.SaleEntity
import com.alieducational.app.data.local.entity.SaleLineEntity
import com.alieducational.app.data.local.entity.ServiceDefinitionEntity
import com.alieducational.app.data.local.entity.ShopExpenseEntity
import com.alieducational.app.data.local.entity.StockMovementEntity
import com.alieducational.app.data.local.entity.SupplierEntity
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class BackupPackageAndReconciliationTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun testExcelGeneratorGeneratesAll11SheetsWithNumericData() {
        val generator = ShopRecordsExcelGenerator()
        val outFile = tempFolder.newFile("test_shop_records.xlsx")

        val snapshotData = ShopSnapshotData(
            snapshotId = "SNAP_2026-11-01_20-24-23",
            timestamp = 1761999863000L,
            shopName = "Ali Educational",
            products = listOf(
                ProductEntity(
                    id = "PROD-001",
                    name = "Classmate Notebook 200p / رجسٹر",
                    category = "Notebooks",
                    brand = "Classmate",
                    sku = "CN-200",
                    barcode = "8901234567890",
                    baseUnit = "piece",
                    packSize = 10,
                    currentSellableStock = 50,
                    movingAvgCostPaise = 3500L, // ₹35.00
                    sellingPricePiecePaise = 5000L, // ₹50.00
                    sellingPricePackPaise = 48000L, // ₹480.00
                    lowStockThreshold = 10
                )
            ),
            movements = listOf(
                StockMovementEntity(
                    id = "MOV-001",
                    productId = "PROD-001",
                    type = "RECEIVE",
                    quantityChangeBase = 50,
                    unitCostPaise = 3500L,
                    reason = "Initial Stock"
                )
            ),
            purchases = emptyList(),
            purchaseLines = emptyList(),
            sales = listOf(
                SaleEntity(
                    id = "SALE-001",
                    receiptNumber = "AE-M-20261101-0001",
                    customerId = "CUST-001",
                    customerName = "Ahmed Walk-in",
                    paymentMethod = "CASH",
                    grossTotalPaise = 10000L,
                    discountValuePaise = 1000L,
                    netTotalPaise = 9000L, // ₹90.00
                    amountReceivedPaise = 9000L,
                    remainingCreditPaise = 0L
                )
            ),
            saleLines = listOf(
                SaleLineEntity(
                    id = "SL-001",
                    saleId = "SALE-001",
                    productId = "PROD-001",
                    productName = "Classmate Notebook 200p / رجسٹر",
                    lineType = "PRODUCT",
                    quantityBase = 2,
                    unitSellingPricePaise = 5000L,
                    unitCostSnapshotPaise = 3500L,
                    lineGrossPaise = 10000L,
                    lineNetPaise = 9000L,
                    lineProfitPaise = 2000L
                )
            ),
            accountPayments = listOf(
                AccountPaymentEntity(
                    id = "PAY-001",
                    accountType = "CUSTOMER",
                    accountId = "CUST-001",
                    amountPaise = 5000L,
                    paymentMethod = "UPI",
                    notes = "Advance cleared"
                )
            ),
            customers = listOf(
                CustomerEntity(
                    id = "CUST-001",
                    name = "Ahmed Walk-in",
                    phone = "9876543210",
                    currentBalancePaise = 0L
                )
            ),
            suppliers = listOf(
                SupplierEntity(
                    id = "SUPP-001",
                    name = "Star Paper Distributors",
                    phone = "9123456780",
                    currentBalancePaise = 150000L // ₹1,500.00
                )
            ),
            cashExpenses = listOf(
                ShopExpenseEntity(
                    id = "EXP-001",
                    isIncome = false,
                    amountPaise = 20000L,
                    category = "Tea & Refreshment",
                    notes = "Evening staff tea",
                    isSavingsTransfer = false
                ),
                ShopExpenseEntity(
                    id = "INC-001",
                    isIncome = true,
                    amountPaise = 50000L,
                    category = "Other Income",
                    notes = "Paper scrap sold",
                    isSavingsTransfer = false
                )
            ),
            pigmySchemes = listOf(
                PigmySchemeEntity(
                    id = "SCHEME-001",
                    name = "Daily 100 Savings",
                    usualDailyAmountPaise = 10000L
                )
            ),
            pigmyDeposits = listOf(
                PigmyDepositEntity(
                    id = "DEP-001",
                    accountSchemeId = "SCHEME-001",
                    amountPaise = 10000L,
                    paymentSource = "CASH",
                    notes = "Deposit by Musid"
                )
            ),
            pigmyWithdrawals = emptyList(),
            services = listOf(
                ServiceDefinitionEntity(
                    id = "SVC-001",
                    name = "Photocopying B&W",
                    unitLabel = "copies"
                )
            ),
            cashCategories = listOf(
                CashCategoryEntity(id = "CAT-001", name = "Tea & Refreshment"),
                CashCategoryEntity(id = "CAT-002", name = "Other Income")
            ),
            photoCount = 2
        )

        // Generate the workbook
        generator.generateWorkbook(snapshotData, outFile)
        assertTrue("Workbook file must exist", outFile.exists())
        assertTrue("Workbook size must be > 1000 bytes", outFile.length() > 1000)

        // Inspect the generated workbook with Apache POI
        FileInputStream(outFile).use { fis ->
            val wb = XSSFWorkbook(fis)
            assertEquals("Must have exactly 11 sheets", 11, wb.numberOfSheets)

            val sheetNames = (0 until wb.numberOfSheets).map { wb.getSheetName(it) }
            assertTrue(sheetNames.contains("Summary"))
            assertTrue(sheetNames.contains("Products and Stock"))
            assertTrue(sheetNames.contains("Stock Movements"))
            assertTrue(sheetNames.contains("Bills and Items"))
            assertTrue(sheetNames.contains("Payments and Dues"))
            assertTrue(sheetNames.contains("Customers and Suppliers"))
            assertTrue(sheetNames.contains("Cash In Out"))
            assertTrue(sheetNames.contains("Expenses and Refunds"))
            assertTrue(sheetNames.contains("Services"))
            assertTrue(sheetNames.contains("Pigmy Savings"))
            assertTrue(sheetNames.contains("Categories"))

            // Verify Products and Stock sheet has our notebook with exact Urdu name preserved
            val prodSheet = wb.getSheet("Products and Stock")
            val dataRow = prodSheet.getRow(1)
            assertNotNull(dataRow)
            assertEquals("PROD-001", dataRow.getCell(0).stringCellValue)
            assertEquals("Classmate Notebook 200p / رجسٹر", dataRow.getCell(2).stringCellValue)
            assertEquals(50.0, dataRow.getCell(7).numericCellValue, 0.001) // Sellable stock units
            assertEquals(35.0, dataRow.getCell(8).numericCellValue, 0.001) // Cost ₹35.00
            assertEquals(1750.0, dataRow.getCell(11).numericCellValue, 0.001) // Valuation ₹1750.00

            wb.close()
        }
    }

    @Test
    fun testOuterPackageBundleAndAutoDetectInnerCompleteZip() {
        // Create synthetic Complete_Backup.zip
        val completeBackupFile = tempFolder.newFile("Complete_Backup.zip")
        ZipOutputStream(FileOutputStream(completeBackupFile)).use { zos ->
            zos.putNextEntry(ZipEntry("ali_educational.db"))
            zos.write("SQLite format 3\u0000fake-db-bytes".toByteArray(Charsets.US_ASCII))
            zos.closeEntry()

            zos.putNextEntry(ZipEntry("manifest.json"))
            zos.write("{\"formatVersion\":1,\"schemaVersion\":8}".toByteArray(Charsets.UTF_8))
            zos.closeEntry()
        }

        // Create outer package ZIP
        val packageFile = tempFolder.newFile("Ali_Educational_Backup_Package_2026-11-01_20-24-23.zip")
        ZipOutputStream(FileOutputStream(packageFile)).use { zos ->
            // Put Complete_Backup.zip inside
            FileInputStream(completeBackupFile).use { fis ->
                zos.putNextEntry(ZipEntry("Complete_Backup.zip"))
                fis.copyTo(zos)
                zos.closeEntry()
            }

            // Put Shop_Records.xlsx inside
            zos.putNextEntry(ZipEntry("Shop_Records.xlsx"))
            zos.write("dummy excel bytes".toByteArray())
            zos.closeEntry()

            // Put Read_Me.txt inside
            zos.putNextEntry(ZipEntry("Read_Me.txt"))
            zos.write("ReadMe instructions".toByteArray())
            zos.closeEntry()
        }

        // Verify that scanning packageFile finds Complete_Backup.zip
        var foundCompleteZip = false
        var foundExcel = false
        var foundReadMe = false

        ZipInputStream(FileInputStream(packageFile)).use { zis ->
            var entry: ZipEntry?
            while (zis.nextEntry.also { entry = it } != null) {
                when (entry!!.name) {
                    "Complete_Backup.zip" -> foundCompleteZip = true
                    "Shop_Records.xlsx" -> foundExcel = true
                    "Read_Me.txt" -> foundReadMe = true
                }
                zis.closeEntry()
            }
        }

        assertTrue("Must contain Complete_Backup.zip", foundCompleteZip)
        assertTrue("Must contain Shop_Records.xlsx", foundExcel)
        assertTrue("Must contain Read_Me.txt", foundReadMe)
    }

    @Test
    fun testZipSlipPathTraversalRejection() {
        val maliciousFile = tempFolder.newFile("malicious.zip")
        ZipOutputStream(FileOutputStream(maliciousFile)).use { zos ->
            zos.putNextEntry(ZipEntry("../../../etc/passwd"))
            zos.write("malicious payload".toByteArray())
            zos.closeEntry()
        }

        var threwSecurityException = false
        try {
            ZipInputStream(FileInputStream(maliciousFile)).use { zis ->
                var entry: ZipEntry?
                while (zis.nextEntry.also { entry = it } != null) {
                    val entryName = entry!!.name
                    if (entryName.contains("..") || entryName.startsWith("/") || entryName.startsWith("\\")) {
                        throw SecurityException("Malicious path detected: $entryName")
                    }
                    zis.closeEntry()
                }
            }
        } catch (e: SecurityException) {
            threwSecurityException = true
        }

        assertTrue("Path traversal entry must be rejected with SecurityException", threwSecurityException)
    }
}
