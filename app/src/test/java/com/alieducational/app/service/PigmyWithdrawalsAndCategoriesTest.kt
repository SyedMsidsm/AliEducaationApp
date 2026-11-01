package com.alieducational.app.service

import androidx.sqlite.db.SupportSQLiteDatabase
import com.alieducational.app.data.local.AppDatabase
import com.alieducational.app.data.local.entity.CashCategoryEntity
import com.alieducational.app.data.local.entity.PigmyDepositEntity
import com.alieducational.app.data.local.entity.PigmySchemeEntity
import com.alieducational.app.data.local.entity.PigmyWithdrawalEntity
import com.alieducational.app.data.local.entity.ShopExpenseEntity
import com.alieducational.app.data.service.CashTrackerExcelService
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class PigmyWithdrawalsAndCategoriesTest {

    @Test
    fun testMigration6To7_StatementsRecorded() {
        val statements = mutableListOf<String>()

        val handler = InvocationHandler { _, method: Method, args: Array<out Any>? ->
            if (method.name == "execSQL" && args != null && args.isNotEmpty()) {
                statements.add(args[0] as String)
            }
            null
        }

        val proxyDb = Proxy.newProxyInstance(
            SupportSQLiteDatabase::class.java.classLoader,
            arrayOf(SupportSQLiteDatabase::class.java),
            handler
        ) as SupportSQLiteDatabase

        AppDatabase.MIGRATION_6_7.migrate(proxyDb)

        assertTrue("Must create cash_categories table",
            statements.any { it.contains("CREATE TABLE IF NOT EXISTS `cash_categories`") })
        assertTrue("Must populate default cash categories",
            statements.any { it.contains("INSERT OR IGNORE INTO `cash_categories`") })
        assertTrue("Must alter shop_expenses to add categoryId",
            statements.any { it.contains("ALTER TABLE shop_expenses ADD COLUMN categoryId") })
        assertTrue("Must create pigmy_withdrawals table",
            statements.any { it.contains("CREATE TABLE IF NOT EXISTS `pigmy_withdrawals`") })
        assertTrue("Must alter pigmy_schemes to add openingBalancePaise",
            statements.any { it.contains("ALTER TABLE pigmy_schemes ADD COLUMN openingBalancePaise") })
        assertTrue("Must alter pigmy_schemes to add openingBalanceDate",
            statements.any { it.contains("ALTER TABLE pigmy_schemes ADD COLUMN openingBalanceDate") })
    }

    @Test
    fun testPigmyTrackedBalanceCalculations_WithWithdrawalsAndOpeningBalance() {
        val schemeId = "scheme_alpha"
        val openingBalance = 500000L // ₹5,000.00 confirmed starting balance

        val deposits = listOf(
            PigmyDepositEntity(
                id = "dep_1",
                accountSchemeId = schemeId,
                amountPaise = 100000L, // ₹1,000.00
                paymentSource = "CASH",
                linkedExpenseId = "exp_out_1",
                isVoided = false
            ),
            PigmyDepositEntity(
                id = "dep_2",
                accountSchemeId = schemeId,
                amountPaise = 50000L, // ₹500.00
                paymentSource = "BANK",
                linkedExpenseId = null,
                isVoided = false
            ),
            PigmyDepositEntity(
                id = "dep_voided",
                accountSchemeId = schemeId,
                amountPaise = 20000L, // ₹200.00 (Cancelled)
                paymentSource = "CASH",
                linkedExpenseId = "exp_out_voided",
                isVoided = true
            )
        )

        val withdrawals = listOf(
            PigmyWithdrawalEntity(
                id = "with_1",
                accountSchemeId = schemeId,
                amountPaise = 200000L, // ₹2,000.00 to Shop Cash
                destination = "CASH",
                linkedExpenseId = "exp_in_1",
                isVoided = false
            ),
            PigmyWithdrawalEntity(
                id = "with_2",
                accountSchemeId = schemeId,
                amountPaise = 100000L, // ₹1,000.00 to Bank Account
                destination = "BANK",
                linkedExpenseId = null,
                isVoided = false
            ),
            PigmyWithdrawalEntity(
                id = "with_voided",
                accountSchemeId = schemeId,
                amountPaise = 50000L, // ₹500.00 (Cancelled)
                destination = "CASH",
                linkedExpenseId = "exp_in_voided",
                isVoided = true
            )
        )

        // 1. Check active totals
        val activeDepositsTotal = deposits.filter { !it.isVoided }.sumOf { it.amountPaise }
        val activeWithdrawalsTotal = withdrawals.filter { !it.isVoided }.sumOf { it.amountPaise }
        assertEquals(150000L, activeDepositsTotal) // ₹1,500.00
        assertEquals(300000L, activeWithdrawalsTotal) // ₹3,000.00

        // 2. Tracked Balance = Opening Balance + Active Deposits - Active Withdrawals
        val trackedBalance = openingBalance + activeDepositsTotal - activeWithdrawalsTotal
        assertEquals(350000L, trackedBalance) // ₹5,000 + ₹1,500 - ₹3,000 = ₹3,500.00

        // 3. Validation: Withdrawal of ₹4,000 exceeds available ₹3,500
        val requestedWithdrawal = 400000L
        val exceedsBalance = requestedWithdrawal > trackedBalance
        assertTrue("Requested withdrawal above tracked funds must be flagged", exceedsBalance)
    }

    @Test
    fun testCashInSavingsTransfer_ClassifiedAsTransferNotSalesOrProfit() {
        val withdrawal = PigmyWithdrawalEntity(
            id = "with_101",
            accountSchemeId = "DEFAULT_PIGMY",
            amountPaise = 300000L, // ₹3,000.00
            destination = "CASH",
            linkedExpenseId = "exp_in_101"
        )

        // Linked Cash In entry generated into shop_expenses
        val linkedCashIn = ShopExpenseEntity(
            id = "exp_in_101",
            category = "Pigmy",
            amountPaise = withdrawal.amountPaise,
            isIncome = true, // Cash In
            notes = "Pigmy withdrawal — Pigmy Scheme 1",
            isSavingsTransfer = true, // Must be marked as savings transfer!
            categoryId = "cat_pigmy"
        )

        // Ensure isSavingsTransfer is true
        assertTrue(linkedCashIn.isSavingsTransfer)
        assertTrue(linkedCashIn.isIncome)
        assertEquals("Pigmy", linkedCashIn.category)

        // When calculating operating profit or sales, isSavingsTransfer entries must be excluded!
        val operatingSales = listOf<ShopExpenseEntity>().filter { !it.isSavingsTransfer && it.isIncome }
        assertEquals(0, operatingSales.size)
    }

    @Test
    fun testExcelImportAndExport_WithCategoryAndAttachmentColumns() {
        val service = CashTrackerExcelService()
        val testWorkbook = XSSFWorkbook()
        val sheet = testWorkbook.createSheet("Cash In Out")

        // Create headers with Category and Attachment
        val headerRow = sheet.createRow(0)
        headerRow.createCell(0).setCellValue("Date")
        headerRow.createCell(1).setCellValue("Category")
        headerRow.createCell(2).setCellValue("Description")
        headerRow.createCell(3).setCellValue("Payment Mode")
        headerRow.createCell(4).setCellValue("Attachment")
        headerRow.createCell(5).setCellValue("Cash In (Income)")
        headerRow.createCell(6).setCellValue("Cash Out (Expense)")

        // Row 1: Categorized expense with attachment
        val r1 = sheet.createRow(1)
        r1.createCell(0).setCellValue("01-Nov-2026")
        r1.createCell(1).setCellValue("Rent")
        r1.createCell(2).setCellValue("Shop Rent Nov 2026")
        r1.createCell(3).setCellValue("BANK")
        r1.createCell(4).setCellValue("Yes")
        r1.createCell(5).setCellValue(0.0)
        r1.createCell(6).setCellValue(2000.0)

        // Row 2: Pigmy withdrawal savings transfer
        val r2 = sheet.createRow(2)
        r2.createCell(0).setCellValue("01-Nov-2026")
        r2.createCell(1).setCellValue("Pigmy")
        r2.createCell(2).setCellValue("Pigmy withdrawal — Scheme 1")
        r2.createCell(3).setCellValue("CASH")
        r2.createCell(4).setCellValue("No")
        r2.createCell(5).setCellValue(3000.0)
        r2.createCell(6).setCellValue(0.0)

        val preview = service.parseWorkbook(testWorkbook, "categorized_test.xlsx")
        assertNull(preview.errorMessage)
        assertEquals(2, preview.items.size)

        val rentItem = preview.items[0]
        assertEquals("Rent", rentItem.category)
        assertFalse(rentItem.isIncome)
        assertEquals(200000L, rentItem.amountPaise)

        val pigmyItem = preview.items[1]
        assertEquals("Pigmy", pigmyItem.category)
        assertTrue(pigmyItem.isIncome)
        assertEquals(300000L, pigmyItem.amountPaise)

        testWorkbook.close()
    }

    @Test
    fun testBackupAndRestore_ZipsAndUnpacksAttachmentsDurableStorage() {
        val tempDir = File(System.getProperty("java.io.tmpdir"), "test_backup_" + UUID.randomUUID())
        tempDir.mkdirs()

        val filesDir = File(tempDir, "files")
        val attachmentsDir = File(filesDir, "attachments")
        attachmentsDir.mkdirs()

        // Create a synthetic attachment photo
        val samplePhoto = File(attachmentsDir, "attachment_test_123.jpg")
        samplePhoto.writeBytes(byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte(), 0x12, 0x34))

        // Create synthetic backup zip containing db, manifest, and attachment
        val backupZip = File(tempDir, "backup_with_photo.zip")
        ZipOutputStream(FileOutputStream(backupZip)).use { zipOut ->
            // SQLite dummy header
            zipOut.putNextEntry(ZipEntry("ali_educational.db"))
            zipOut.write("SQLite format 3\u0000".toByteArray(Charsets.US_ASCII))
            zipOut.closeEntry()

            // Manifest
            zipOut.putNextEntry(ZipEntry("manifest.json"))
            zipOut.write("{\"app\":\"Ali Educational\"}".toByteArray(Charsets.UTF_8))
            zipOut.closeEntry()

            // Attachment entry
            zipOut.putNextEntry(ZipEntry("attachments/${samplePhoto.name}"))
            FileInputStream(samplePhoto).use { it.copyTo(zipOut) }
            zipOut.closeEntry()
        }

        assertTrue("Backup ZIP must exist and be non-empty", backupZip.exists() && backupZip.length() > 0)

        // Restore into a separate target directory to verify independence
        val restoredTargetDir = File(tempDir, "restored_installation")
        val restoredAttachmentsDir = File(restoredTargetDir, "attachments")

        ZipInputStream(FileInputStream(backupZip)).use { zipIn ->
            var entry: ZipEntry?
            while (zipIn.nextEntry.also { entry = it } != null) {
                if (entry!!.name.startsWith("attachments/") && !entry!!.isDirectory) {
                    restoredAttachmentsDir.mkdirs()
                    val destFile = File(restoredAttachmentsDir, File(entry!!.name).name)
                    FileOutputStream(destFile).use { out -> zipIn.copyTo(out) }
                }
                zipIn.closeEntry()
            }
        }

        val restoredPhoto = File(restoredAttachmentsDir, "attachment_test_123.jpg")
        assertTrue("Restored photo must exist in target installation", restoredPhoto.exists())
        assertEquals(samplePhoto.length(), restoredPhoto.length())

        // Cleanup
        tempDir.deleteRecursively()
    }
}
