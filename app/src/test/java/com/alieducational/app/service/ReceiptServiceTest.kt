package com.alieducational.app.service

import com.alieducational.app.data.local.entity.SaleEntity
import com.alieducational.app.data.local.entity.SaleLineEntity
import com.alieducational.app.data.service.ReceiptService
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReceiptServiceTest {

    @Test
    fun testGenerateReceiptSummaryText_SuppliedPhotocopyingExample() {
        // Supplied Photocopying example: 1 copy @ ₹2.00, 50% discount = ₹1.00
        val sale = SaleEntity(
            receiptNumber = "AE-M-20261101-8261",
            customerName = "Walk-in Customer",
            grossTotalPaise = 200L, // ₹2.00
            discountType = "PERCENT",
            discountValuePaise = 100L, // ₹1.00
            discountPercent = 50.0,
            netTotalPaise = 100L, // ₹1.00
            amountReceivedPaise = 100L, // ₹1.00
            changeGivenPaise = 0L,
            remainingCreditPaise = 0L,
            paymentMethod = "CASH",
            timestamp = 1762002120000L
        )

        val lines = listOf(
            SaleLineEntity(
                saleId = sale.id,
                productId = null,
                productName = "Photocopying",
                quantityBase = 1,
                unitSellingPricePaise = 200L,
                unitCostSnapshotPaise = null,
                lineGrossPaise = 200L,
                lineDiscountAllocatedPaise = 100L,
                lineNetPaise = 100L,
                lineProfitPaise = null,
                lineType = "SERVICE",
                serviceId = "svc_copy",
                serviceName = "Photocopying",
                unitLabel = "copies"
            )
        )

        val service = ReceiptService()
        val text = service.generateReceiptSummaryText(sale, lines)

        // Verify discount shown ONCE:
        // Item row: 1 copy @ ₹2.00 = ₹2.00 (gross)
        assertTrue(text.contains("Photocopying: 1 copy @ ₹2.00 = ₹2.00"))
        assertTrue(text.contains("Subtotal: ₹2.00"))
        assertTrue(text.contains("Discount (50%): -₹1.00"))
        assertTrue(text.contains("TOTAL PAYABLE: ₹1.00"))
        assertTrue(text.contains("Amount paid: ₹1.00"))
        assertTrue(text.contains("Balance due: ₹0.00"))
        assertTrue(text.contains("Status: PAID"))

        // Assert strictly absent
        assertFalse(text.contains("Not GST registered"))
        assertFalse(text.contains("Please retain"))
    }

    @Test
    fun testGenerateReceiptSummaryText_PartialPaymentCreditSale() {
        val sale = SaleEntity(
            receiptNumber = "AE-M-20261101-9002",
            customerName = "Rashid Khan",
            grossTotalPaise = 25000L, // ₹250.00
            discountType = "NONE",
            discountValuePaise = 0L,
            discountPercent = 0.0,
            netTotalPaise = 25000L, // ₹250.00
            amountReceivedPaise = 10000L, // ₹100.00
            changeGivenPaise = 0L,
            remainingCreditPaise = 15000L, // ₹150.00 balance due
            paymentMethod = "CREDIT",
            timestamp = 1762002120000L
        )

        val lines = listOf(
            SaleLineEntity(
                saleId = sale.id,
                productId = "prod_pen",
                productName = "Parker Vector Pen (قلم)",
                quantityBase = 1,
                isPack = true,
                unitSellingPricePaise = 25000L,
                unitCostSnapshotPaise = 18000L,
                lineGrossPaise = 25000L,
                lineDiscountAllocatedPaise = 0L,
                lineNetPaise = 25000L,
                lineProfitPaise = 7000L
            )
        )

        val service = ReceiptService()
        val text = service.generateReceiptSummaryText(sale, lines)

        assertTrue(text.contains("Receipt No: AE-M-20261101-9002"))
        assertTrue(text.contains("Customer: Rashid Khan"))
        assertTrue(text.contains("Payment: Credit"))
        assertTrue(text.contains("Parker Vector Pen (قلم)"))
        assertTrue(text.contains("Subtotal: ₹250.00"))
        assertTrue(text.contains("TOTAL PAYABLE: ₹250.00"))
        assertTrue(text.contains("Amount paid: ₹100.00"))
        assertTrue(text.contains("Balance due: ₹150.00"))
        assertTrue(text.contains("Status: PARTIAL"))
    }

    @Test
    fun testGenerateReceiptSummaryText_CashChangeSale() {
        val sale = SaleEntity(
            receiptNumber = "AE-M-20261101-9003",
            customerName = "Zaid Ali",
            grossTotalPaise = 17000L, // ₹170.00
            discountType = "NONE",
            discountValuePaise = 0L,
            discountPercent = 0.0,
            netTotalPaise = 17000L, // ₹170.00
            amountReceivedPaise = 20000L, // ₹200.00 tendered
            changeGivenPaise = 3000L, // ₹30.00 change
            remainingCreditPaise = 0L,
            paymentMethod = "CASH",
            timestamp = 1762002120000L
        )

        val lines = listOf(
            SaleLineEntity(
                saleId = sale.id,
                productId = "prod_reg",
                productName = "Classmate Register 200 Pgs",
                quantityBase = 2,
                isPack = false,
                unitSellingPricePaise = 8500L,
                unitCostSnapshotPaise = 6000L,
                lineGrossPaise = 17000L,
                lineDiscountAllocatedPaise = 0L,
                lineNetPaise = 17000L,
                lineProfitPaise = 5000L
            )
        )

        val service = ReceiptService()
        val text = service.generateReceiptSummaryText(sale, lines)

        assertTrue(text.contains("Subtotal: ₹170.00"))
        assertTrue(text.contains("TOTAL PAYABLE: ₹170.00"))
        assertTrue(text.contains("Amount paid: ₹200.00"))
        assertTrue(text.contains("Change: ₹30.00"))
        assertTrue(text.contains("Balance due: ₹0.00"))
        assertTrue(text.contains("Status: PAID"))
    }
}
