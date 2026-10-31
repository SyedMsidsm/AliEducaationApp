package com.alieducational.app.service

import com.alieducational.app.data.local.entity.SaleEntity
import com.alieducational.app.data.local.entity.SaleLineEntity
import com.alieducational.app.data.service.ReceiptService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReceiptServiceTest {

    @Test
    fun testGenerateReceiptSummaryText_FullBreakdown() {
        // Construct standard sale
        val sale = SaleEntity(
            receiptNumber = "AE-00123",
            customerName = "Tariq Ahmad",
            grossTotalPaise = 20000L, // ₹200.00
            discountType = "PERCENT",
            discountValuePaise = 2000L, // ₹20.00
            discountPercent = 10.0,
            netTotalPaise = 18000L, // ₹180.00
            amountReceivedPaise = 20000L, // ₹200.00 tendered
            changeGivenPaise = 2000L, // ₹20.00 change
            remainingCreditPaise = 0L,
            paymentMethod = "CASH",
            timestamp = 1730416800000L
        )

        val lines = listOf(
            SaleLineEntity(
                saleId = sale.id,
                productId = "prod1",
                productName = "Classmate Notebook (اردو نوٹ بک)",
                quantityBase = 2,
                isPack = false,
                unitSellingPricePaise = 8000L, // ₹80.00
                unitCostSnapshotPaise = 5000L,
                lineGrossPaise = 16000L,
                lineDiscountAllocatedPaise = 1600L,
                lineNetPaise = 14400L,
                lineProfitPaise = 4400L
            ),
            SaleLineEntity(
                saleId = sale.id,
                productId = null,
                productName = "Printing - A4 B&W",
                quantityBase = 20,
                isPack = false,
                unitSellingPricePaise = 200L, // ₹2.00
                unitCostSnapshotPaise = null,
                lineGrossPaise = 4000L,
                lineDiscountAllocatedPaise = 400L,
                lineNetPaise = 3600L,
                lineProfitPaise = null
            )
        )

        val service = ReceiptService()
        val text = service.generateReceiptSummaryText(sale, lines)

        assertTrue(text.contains("Receipt: AE-00123"))
        assertTrue(text.contains("Customer: Tariq Ahmad"))
        assertTrue(text.contains("Classmate Notebook (اردو نوٹ بک)"))
        assertTrue(text.contains("Printing - A4 B&W"))
        assertTrue(text.contains("Subtotal: ₹200.00"))
        assertTrue(text.contains("Discount: -₹20.00 (10.0%)"))
        assertTrue(text.contains("Total: ₹180.00"))
        assertTrue(text.contains("Change Given: ₹20.00"))
        assertTrue(text.contains("Payment Mode: CASH"))
    }
}
