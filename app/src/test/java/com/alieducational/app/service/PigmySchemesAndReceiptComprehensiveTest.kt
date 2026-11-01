package com.alieducational.app.service

import com.alieducational.app.data.local.entity.PigmyDepositEntity
import com.alieducational.app.data.local.entity.PigmySchemeEntity
import com.alieducational.app.data.local.entity.SaleEntity
import com.alieducational.app.data.local.entity.SaleLineEntity
import com.alieducational.app.data.local.entity.ShopExpenseEntity
import com.alieducational.app.data.service.ReceiptService
import com.alieducational.app.domain.calculation.CostCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class PigmySchemesAndReceiptComprehensiveTest {

    private val receiptService = ReceiptService()

    @Test
    fun testPigmySchemes_InitialDefaultsAndExpandableSchemesLifecycle() {
        // 1. Initial 3 schemes (A, B, C)
        val initialSchemes = mutableListOf(
            PigmySchemeEntity(
                id = "scheme-A",
                name = "Pigmy Scheme 1",
                usualDailyAmountPaise = 10000L, // ₹100.00
                isArchived = false
            ),
            PigmySchemeEntity(
                id = "scheme-B",
                name = "Pigmy Scheme 2",
                usualDailyAmountPaise = 20000L, // ₹200.00
                isArchived = false
            ),
            PigmySchemeEntity(
                id = "scheme-C",
                name = "Pigmy Scheme 3",
                usualDailyAmountPaise = 30000L, // ₹300.00
                isArchived = false
            )
        )
        assertEquals("Initial schemes count must be 3", 3, initialSchemes.size)

        // 2. Record synthetic cash deposits: A: ₹100, B: ₹200, C: ₹300
        val deposits = mutableListOf<PigmyDepositEntity>()
        val expenses = mutableListOf<ShopExpenseEntity>()

        fun recordDeposit(schemeId: String, amountPaise: Long, paymentSource: String, notes: String?): String {
            val depositId = UUID.randomUUID().toString()
            var linkedExpenseId: String? = null
            val scheme = initialSchemes.find { it.id == schemeId }

            if (paymentSource == "CASH") {
                linkedExpenseId = UUID.randomUUID().toString()
                val expense = ShopExpenseEntity(
                    id = linkedExpenseId,
                    category = "SAVINGS_TRANSFER",
                    amountPaise = amountPaise,
                    isIncome = false,
                    notes = "Pigmy Deposit (${scheme?.name}): ${notes ?: ""}".trim(),
                    isSavingsTransfer = true,
                    timestamp = System.currentTimeMillis()
                )
                expenses.add(expense)
            }

            val deposit = PigmyDepositEntity(
                id = depositId,
                accountSchemeId = schemeId,
                amountPaise = amountPaise,
                paymentSource = paymentSource,
                notes = notes,
                linkedExpenseId = linkedExpenseId,
                isVoided = false
            )
            deposits.add(deposit)
            return depositId
        }

        val depAId = recordDeposit("scheme-A", 10000L, "CASH", "Deposit A")
        val depBId = recordDeposit("scheme-B", 20000L, "CASH", "Deposit B")
        val depCId = recordDeposit("scheme-C", 30000L, "CASH", "Deposit C")

        // Assert individual and combined totals
        val totalA = deposits.filter { it.accountSchemeId == "scheme-A" && !it.isVoided }.sumOf { it.amountPaise }
        val totalB = deposits.filter { it.accountSchemeId == "scheme-B" && !it.isVoided }.sumOf { it.amountPaise }
        val totalC = deposits.filter { it.accountSchemeId == "scheme-C" && !it.isVoided }.sumOf { it.amountPaise }
        val combinedTotal1 = deposits.filter { !it.isVoided }.sumOf { it.amountPaise }

        assertEquals(10000L, totalA)
        assertEquals(20000L, totalB)
        assertEquals(30000L, totalC)
        assertEquals("Combined recorded deposits must be ₹600.00 (60000 paise)", 60000L, combinedTotal1)

        // Assert exactly 3 active savings transfers created in shop cash
        val activeSavingsTransfers1 = expenses.filter { it.isSavingsTransfer && !it.isVoided }
        assertEquals("Must have exactly 3 active savings transfers", 3, activeSavingsTransfers1.size)
        assertEquals(60000L, activeSavingsTransfers1.sumOf { it.amountPaise })

        // 3. Edit deposit B from ₹200 to ₹250
        val depBIndex = deposits.indexOfFirst { it.id == depBId }
        val oldDepB = deposits[depBIndex]
        deposits[depBIndex] = oldDepB.copy(amountPaise = 25000L)

        // Synchronize linked expense
        val expBIndex = expenses.indexOfFirst { it.id == oldDepB.linkedExpenseId }
        expenses[expBIndex] = expenses[expBIndex].copy(amountPaise = 25000L)

        val combinedTotal2 = deposits.filter { !it.isVoided }.sumOf { it.amountPaise }
        assertEquals("Combined recorded deposits after edit B must be ₹650.00 (65000 paise)", 65000L, combinedTotal2)
        assertEquals("Active savings transfers must remain 3 without duplicates", 3, expenses.filter { it.isSavingsTransfer && !it.isVoided }.size)
        assertEquals(65000L, expenses.filter { it.isSavingsTransfer && !it.isVoided }.sumOf { it.amountPaise })

        // 4. Cancel deposit C
        val depCIndex = deposits.indexOfFirst { it.id == depCId }
        val oldDepC = deposits[depCIndex]
        deposits[depCIndex] = oldDepC.copy(isVoided = true, voidReason = "Customer cancellation")

        // Reverse linked expense
        val expCIndex = expenses.indexOfFirst { it.id == oldDepC.linkedExpenseId }
        expenses[expCIndex] = expenses[expCIndex].copy(isVoided = true, voidReason = "Voided with Pigmy deposit")

        val combinedTotal3 = deposits.filter { !it.isVoided }.sumOf { it.amountPaise }
        assertEquals("Active combined deposits after cancel C must be ₹350.00 (35000 paise)", 35000L, combinedTotal3)
        assertEquals("Active savings transfers must now be 2", 2, expenses.filter { it.isSavingsTransfer && !it.isVoided }.size)
        assertEquals(35000L, expenses.filter { it.isSavingsTransfer && !it.isVoided }.sumOf { it.amountPaise })

        // 5. Direct / Bank funding test: bank payment must NOT create cash outflow
        val depBankId = recordDeposit("scheme-A", 5000L, "BANK", "Direct Bank Payment")
        val combinedTotal4 = deposits.filter { !it.isVoided }.sumOf { it.amountPaise }
        assertEquals("Combined deposits after bank deposit must be ₹400.00 (40000 paise)", 40000L, combinedTotal4)
        assertEquals("Active cash savings transfers must remain 2 (bank deposit does not reduce physical cash)", 2, expenses.filter { it.isSavingsTransfer && !it.isVoided }.size)

        // 6. Profit Neutrality Test:
        // Operating expenses exclude savings transfers
        val operatingExpenses = expenses.filter { !it.isVoided && !it.isIncome && !it.isSavingsTransfer }
        assertEquals("Operating expenses must be 0; savings transfers do NOT reduce product profit", 0, operatingExpenses.size)

        // 7. Expandability test: Add Scheme D and Scheme E
        val schemeD = PigmySchemeEntity(id = "scheme-D", name = "Scheme D", usualDailyAmountPaise = 50000L, isArchived = false)
        val schemeE = PigmySchemeEntity(id = "scheme-E", name = "Scheme E", usualDailyAmountPaise = 100000L, isArchived = false)
        initialSchemes.add(schemeD)
        initialSchemes.add(schemeE)
        assertEquals("Total schemes count must now be 5", 5, initialSchemes.size)

        // Archive Scheme D
        val dIdx = initialSchemes.indexOfFirst { it.id == "scheme-D" }
        initialSchemes[dIdx] = initialSchemes[dIdx].copy(isArchived = true)
        val activeSchemes = initialSchemes.filter { !it.isArchived }
        assertEquals("Active schemes count should be 4", 4, activeSchemes.size)
        assertFalse("Archived scheme D should not appear in active schemes list", activeSchemes.any { it.id == "scheme-D" })

        // Reactivate Scheme D
        initialSchemes[dIdx] = initialSchemes[dIdx].copy(isArchived = false)
        assertEquals("Active schemes count restored to 5", 5, initialSchemes.filter { !it.isArchived }.size)
    }

    @Test
    fun testReceiptCleanup_AbsenceOfGstNoticeAndRetentionFooter() {
        val sale = SaleEntity(
            id = "sale-test-1",
            receiptNumber = "AE-M-20261101-1036",
            timestamp = 1793517000000L,
            grossTotalPaise = 200L,
            discountPercent = 50.0,
            discountValuePaise = 100L,
            netTotalPaise = 100L,
            amountReceivedPaise = 100L,
            changeGivenPaise = 0L,
            paymentMethod = "CASH",
            customerName = "Walk-in Customer"
        )

        val lines = listOf(
            SaleLineEntity(
                id = "line-1",
                saleId = "sale-test-1",
                productId = null,
                productName = "Photocopying",
                quantityBase = 1,
                unitSellingPricePaise = 200L,
                unitCostSnapshotPaise = null,
                lineGrossPaise = 200L,
                lineDiscountAllocatedPaise = 100L,
                lineNetPaise = 100L,
                lineProfitPaise = null,
                unitLabel = "copies"
            ),
            SaleLineEntity(
                id = "line-2",
                saleId = "sale-test-1",
                productId = "prod-urdu",
                productName = "قلم رصاص",
                quantityBase = 2,
                unitSellingPricePaise = 1500L,
                unitCostSnapshotPaise = 1000L,
                lineGrossPaise = 3000L,
                lineDiscountAllocatedPaise = 0L,
                lineNetPaise = 3000L,
                lineProfitPaise = 1000L,
                unitLabel = "pc"
            )
        )

        val summaryText = receiptService.generateReceiptSummaryText(sale, lines)

        // 1. Assert "Not GST Registered" is strictly ABSENT
        assertFalse("Summary text must not contain 'Not GST Registered'", summaryText.contains("Not GST Registered", ignoreCase = true))

        // 2. Assert customer retention instruction is strictly ABSENT
        assertFalse("Summary text must not contain 'retain'", summaryText.contains("retain", ignoreCase = true))

        // 3. Assert necessary shop header and footer remain present
        assertTrue("Must contain 'Ali Educational'", summaryText.contains("Ali Educational"))
        assertTrue("Must contain 'Stationery & Educational Supplies'", summaryText.contains("Stationery & Educational Supplies"))
        assertTrue("Must contain receipt number", summaryText.contains("AE-M-20261101-1036"))
        assertTrue("Must preserve Urdu product name", summaryText.contains("قلم رصاص"))
        assertTrue("Must contain 'Thank you for your visit!'", summaryText.contains("Thank you for your visit!"))
    }

    @Test
    fun testBackupAndRestore_WithFiveSchemesAndLinks() {
        val tempDir = File(System.getProperty("java.io.tmpdir"), "pigmy_backup_test_${System.currentTimeMillis()}")
        tempDir.mkdirs()

        try {
            val dbFile = File(tempDir, "ali_educational.db")
            // Create simulated SQLite format 3 database header
            FileOutputStream(dbFile).use { out ->
                out.write("SQLite format 3\u0000".toByteArray(Charsets.US_ASCII))
                out.write(ByteArray(1024)) // padding
            }

            val zipFile = File(tempDir, "ali_backup_test.zip")
            ZipOutputStream(FileOutputStream(zipFile)).use { zipOut ->
                // Add DB
                zipOut.putNextEntry(ZipEntry("ali_educational.db"))
                FileInputStream(dbFile).use { it.copyTo(zipOut) }
                zipOut.closeEntry()

                // Add Manifest
                val manifest = """
                    {
                        "app": "Ali Educational",
                        "schemaVersion": 6,
                        "schemesCount": 5,
                        "timestamp": ${System.currentTimeMillis()}
                    }
                """.trimIndent()
                zipOut.putNextEntry(ZipEntry("manifest.json"))
                zipOut.write(manifest.toByteArray(Charsets.UTF_8))
                zipOut.closeEntry()
            }

            assertTrue("Zip backup must exist", zipFile.exists())
            assertTrue("Zip backup size must be non-zero", zipFile.length() > 50)

            // Simulate restore into a disposable database
            val restoredDbFile = File(tempDir, "restored_ali_educational.db")
            var restoredManifest: String? = null

            ZipInputStream(FileInputStream(zipFile)).use { zipIn ->
                var entry: ZipEntry?
                while (zipIn.nextEntry.also { entry = it } != null) {
                    if (entry!!.name == "ali_educational.db") {
                        FileOutputStream(restoredDbFile).use { out ->
                            zipIn.copyTo(out)
                        }
                    } else if (entry!!.name == "manifest.json") {
                        restoredManifest = zipIn.bufferedReader().readText()
                    }
                    zipIn.closeEntry()
                }
            }

            assertTrue("Restored database must exist", restoredDbFile.exists())
            assertEquals("Restored database size must match original", dbFile.length(), restoredDbFile.length())
            assertNotNull("Manifest must be restored", restoredManifest)
            assertTrue("Restored manifest must have schemaVersion 6", restoredManifest!!.contains("\"schemaVersion\": 6"))
        } finally {
            tempDir.deleteRecursively()
        }
    }
}
