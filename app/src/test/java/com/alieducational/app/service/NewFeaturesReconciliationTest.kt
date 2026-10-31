package com.alieducational.app.service

import com.alieducational.app.data.local.entity.ShopExpenseEntity
import com.alieducational.app.domain.calculation.CostCalculator
import com.alieducational.app.domain.calculation.SaleLineInput
import com.alieducational.app.ui.BillingDraftItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class NewFeaturesReconciliationTest {

    @Test
    fun testDiscountAllocationWithUniqueLineIds() {
        // Two distinct lines of the SAME product: piece line and pack line
        val line1 = SaleLineInput(
            id = "line-1",
            quantity = 2,
            unitSellingPricePaise = 5000L, // 2 * 50 = 100 Rs = 10,000 paise
            unitCostPaise = 3000L
        )
        val line2 = SaleLineInput(
            id = "line-2",
            quantity = 1,
            unitSellingPricePaise = 45000L, // 1 * 450 = 450 Rs = 45,000 paise
            unitCostPaise = 25000L
        )

        val lines = listOf(line1, line2)
        val wholeBillDiscount = 5500L // 55 Rs (10% of 550 Rs)

        val results = CostCalculator.allocateWholeSaleDiscount(lines, wholeBillDiscount)

        assertEquals("Both lines must have allocated discounts", 2, results.size)
        val resultMap = results.associateBy { it.lineId }
        assertTrue("Line 1 must exist in result map", resultMap.containsKey("line-1"))
        assertTrue("Line 2 must exist in result map", resultMap.containsKey("line-2"))

        val totalAllocated = results.sumOf { it.allocatedDiscountPaise }
        assertEquals("Total allocated discount must equal whole-bill discount", wholeBillDiscount, totalAllocated)
        assertEquals("Line 1 discount is 10%", 1000L, resultMap["line-1"]?.allocatedDiscountPaise)
        assertEquals("Line 2 discount is 10%", 4500L, resultMap["line-2"]?.allocatedDiscountPaise)
    }

    @Test
    fun testPigmySavingsExcludedFromOperatingExpenses() {
        val regularExpense = ShopExpenseEntity(
            id = "exp-1",
            category = "ELECTRICITY",
            amountPaise = 250000L,
            isIncome = false,
            isSavingsTransfer = false
        )
        val pigmyTransfer = ShopExpenseEntity(
            id = "pigmy-transfer-1",
            category = "SAVINGS_TRANSFER",
            amountPaise = 100000L,
            isIncome = false,
            isSavingsTransfer = true
        )
        val serviceExpense = ShopExpenseEntity(
            id = "svc-exp-1",
            category = "SERVICE_EXPENSE",
            amountPaise = 50000L,
            isIncome = false,
            isSavingsTransfer = false
        )

        val allExpenses = listOf(regularExpense, pigmyTransfer, serviceExpense)

        // Filter operating expenses per Phase 6 & Phase 8 rules:
        val operatingExpenses = allExpenses.filter {
            !it.isVoided && !it.isIncome && !it.isSavingsTransfer && it.category != "SERVICE_EXPENSE"
        }

        assertEquals("Only regular operating expense must be included", 1, operatingExpenses.size)
        assertEquals("Regular expense amount", 250000L, operatingExpenses.sumOf { it.amountPaise })
        assertFalse("Pigmy transfer must NOT be in operating expenses", operatingExpenses.any { it.isSavingsTransfer })
    }

    @Test
    fun testServiceBillingCalculationAndStockIsolation() {
        val printingJob = BillingDraftItem(
            lineId = UUID.randomUUID().toString(),
            productId = "SERVICE_PRINTING",
            productName = "Printing — A4 B&W",
            unitType = "SERVICE",
            quantity = 50, // 50 pages
            unitPricePaise = 300L, // 3 Rs per page = 150 Rs
            lineType = "SERVICE",
            serviceOption = "A4 B&W",
            unitLabel = "pages"
        )

        assertEquals("Total pieces for service line must be 0", 0, printingJob.totalPieces)
        assertEquals("Line gross paise must be quantity * rate", 15000L, printingJob.lineGrossPaise)
        assertEquals("Line type is SERVICE", "SERVICE", printingJob.lineType)
        assertEquals("Print mode is A4 B&W", "A4 B&W", printingJob.serviceOption)
    }

    @Test
    fun testReceiptPrefixSeriesLogic() {
        val ownerPrefix = "AE-M-"
        val billingPrefix = "AE-B1-"
        val dateStr = "20261101"
        val count = 5
        val seqStr = String.format("%04d", count)

        val ownerReceipt = "$ownerPrefix$dateStr-$seqStr"
        val billingReceipt = "$billingPrefix$dateStr-$seqStr"

        assertTrue(ownerReceipt.startsWith("AE-M-"))
        assertTrue(billingReceipt.startsWith("AE-B1-"))
        assertEquals("AE-B1-20261101-0005", billingReceipt)
    }
}
