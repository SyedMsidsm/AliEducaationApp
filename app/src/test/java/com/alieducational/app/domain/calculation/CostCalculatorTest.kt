package com.alieducational.app.domain.calculation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CostCalculatorTest {

    @Test
    fun testCost01_ProportionalDeliveryAllocation() {
        // Spec COST-01 / CORE-05:
        // 20 pens at ₹10 = ₹200 (20000 paise)
        // 10 notebooks at ₹30 = ₹300 (30000 paise)
        // Delivery = ₹50 (5000 paise)
        // Total stock cost = ₹550 (55000 paise)
        // Expected: Pens get ₹20 (2000 paise), notebooks get ₹30 (3000 paise)
        // Pens unit cost = ₹11 (1100 paise), Notebooks unit cost = ₹33 (3300 paise)
        val lines = listOf(
            PurchaseLineInput(id = "pens", quantity = 20, unitPricePaise = 1000L),
            PurchaseLineInput(id = "notebooks", quantity = 10, unitPricePaise = 3000L)
        )
        val results = CostCalculator.allocateDelivery(lines, totalDeliveryPaise = 5000L)

        assertEquals(2, results.size)

        val penResult = results.first { it.lineId == "pens" }
        assertEquals(20000L, penResult.goodsValuePaise)
        assertEquals(2000L, penResult.allocatedDeliveryPaise)
        assertEquals(22000L, penResult.totalLandedCostPaise)
        assertEquals(1100L, penResult.effectiveUnitCostPaise) // ₹11

        val nbResult = results.first { it.lineId == "notebooks" }
        assertEquals(30000L, nbResult.goodsValuePaise)
        assertEquals(3000L, nbResult.allocatedDeliveryPaise)
        assertEquals(33000L, nbResult.totalLandedCostPaise)
        assertEquals(3300L, nbResult.effectiveUnitCostPaise) // ₹33

        // Total delivery allocated must equal total delivery charge exactly
        assertEquals(5000L, penResult.allocatedDeliveryPaise + nbResult.allocatedDeliveryPaise)
    }

    @Test
    fun testCost02_MovingWeightedAverage() {
        // Spec COST-02:
        // 10 pieces cost ₹100 (10000 paise, avg ₹10 = 1000 paise)
        // Receive 10 with goods ₹140 (14000 paise) + delivery ₹20 (2000 paise) = total landed ₹160 (16000 paise)
        // Combined quantity = 20, stock value = ₹260 (26000 paise), moving average = ₹13 (1300 paise)
        val newAvg = CostCalculator.calculateMovingWeightedAverage(
            existingStockQuantity = 10,
            existingAverageCostPaise = 1000L,
            incomingQuantity = 10,
            incomingTotalLandedCostPaise = 16000L
        )
        assertEquals(1300L, newAvg)
    }

    @Test
    fun testCost03_SaleWithDiscountAndProfit() {
        // Spec COST-03 / CORE-06:
        // Sell 5 from COST-02 for ₹100 gross (selling price ₹20 = 2000 paise each)
        // 10% discount = ₹10 (1000 paise) -> net sale = ₹90 (9000 paise)
        // Cost of 5 items at ₹13 (1300 paise) = ₹65 (6500 paise)
        // Actual gross profit = ₹90 - ₹65 = ₹25 (2500 paise)
        val lines = listOf(
            SaleLineInput(
                id = "item1",
                quantity = 5,
                unitSellingPricePaise = 2000L,
                unitCostPaise = 1300L
            )
        )
        val results = CostCalculator.allocateWholeSaleDiscount(lines, totalDiscountPaise = 1000L)

        assertEquals(1, results.size)
        val result = results[0]
        assertEquals(10000L, result.grossValuePaise)
        assertEquals(1000L, result.allocatedDiscountPaise)
        assertEquals(9000L, result.netValuePaise)
        assertEquals(2500L, result.actualProfitPaise) // ₹25 gross profit
    }

    @Test
    fun testCost04_HistoricalSaleNotAffectedByLaterReceipts() {
        // Spec COST-04:
        // Initial stock: 15 pieces at ₹13 (1300 paise)
        // Receive 10 more at ₹18 (1800 paise) landed cost
        // New avg = (15 * 1300 + 10 * 1800) / 25 = (19500 + 18000) / 25 = 37500 / 25 = 1500 paise (₹15)
        val updatedAvg = CostCalculator.calculateMovingWeightedAverage(
            existingStockQuantity = 15,
            existingAverageCostPaise = 1300L,
            incomingQuantity = 10,
            incomingTotalLandedCostPaise = 18000L
        )
        assertEquals(1500L, updatedAvg)
    }

    @Test
    fun testCore01AndFinal01_BoxesAndPieces() {
        // Spec FINAL-01: Stock is 23 pieces, 10 per box
        // Result: 2 boxes + 3 pieces
        val (boxes, pieces) = CostCalculator.formatBoxesAndPieces(23, 10)
        assertEquals(2, boxes)
        assertEquals(3, pieces)
    }

    @Test
    fun testZeroGoodsValueDeliveryAllocation() {
        // Edge case: All lines are zero-value (e.g. promotional free samples)
        // Delivery = ₹30 (3000 paise) across 2 lines -> ₹15 each
        val lines = listOf(
            PurchaseLineInput(id = "sample1", quantity = 5, unitPricePaise = 0L),
            PurchaseLineInput(id = "sample2", quantity = 5, unitPricePaise = 0L)
        )
        val results = CostCalculator.allocateDelivery(lines, totalDeliveryPaise = 3000L)
        assertEquals(1500L, results[0].allocatedDeliveryPaise)
        assertEquals(1500L, results[1].allocatedDeliveryPaise)
        assertEquals(3000L, results.sumOf { it.allocatedDeliveryPaise })
    }

    @Test
    fun testMissingCostProfitIsUnavailable() {
        // Spec CORE-12: When cost is unknown (null), profit must be null, not zero!
        val lines = listOf(
            SaleLineInput(
                id = "item_no_cost",
                quantity = 1,
                unitSellingPricePaise = 5000L,
                unitCostPaise = null
            )
        )
        val results = CostCalculator.allocateWholeSaleDiscount(lines, totalDiscountPaise = 0L)
        assertNull(results[0].actualProfitPaise)
    }

    @Test
    fun testCurrencyFormatting() {
        assertEquals("₹100.50", CostCalculator.formatPaiseToRupees(10050L))
        assertEquals("₹0.00", CostCalculator.formatPaiseToRupees(0L))
        assertEquals("-₹540.74", CostCalculator.formatPaiseToRupees(-54074L))
    }

    @Test
    fun testIndianDigitGrouping_Section23() {
        // Section 23 table:
        // 999 rupees -> ₹999.00
        assertEquals("₹999.00", CostCalculator.formatPaiseToRupees(99900L))
        // 1000 rupees -> ₹1,000.00
        assertEquals("₹1,000.00", CostCalculator.formatPaiseToRupees(100000L))
        // 12235 rupees -> ₹12,235.00
        assertEquals("₹12,235.00", CostCalculator.formatPaiseToRupees(1223500L))
        // 100000 rupees -> ₹1,00,000.00
        assertEquals("₹1,00,000.00", CostCalculator.formatPaiseToRupees(10000000L))
        // 1234567.50 rupees -> ₹12,34,567.50
        assertEquals("₹12,34,567.50", CostCalculator.formatPaiseToRupees(123456750L))
        // Negative 12197.72 rupees -> -₹12,197.72
        assertEquals("-₹12,197.72", CostCalculator.formatPaiseToRupees(-1219772L))
        // 123456 pieces -> 1,23,456 pcs
        assertEquals("1,23,456 pcs", CostCalculator.formatQuantityWithGrouping(123456, "pcs"))
    }

    @Test
    fun testGrossMarginAndMarkupCalculations_Section8() {
        val costPaise = 10000L // ₹100.00

        // 1. Markup 20% -> P = 100 * (1 + 0.20) = ₹120.00
        val markupPrice = CostCalculator.calculatePriceFromPercentage(costPaise, 20.0, CostCalculator.PricingMode.MARKUP)
        assertEquals(12000L, markupPrice)

        // 2. Gross Margin 20% -> P = 100 / (1 - 0.20) = ₹125.00
        val marginPrice = CostCalculator.calculatePriceFromPercentage(costPaise, 20.0, CostCalculator.PricingMode.GROSS_MARGIN)
        assertEquals(12500L, marginPrice)

        // 3. Percentage from Price: Cost ₹100, Selling ₹125 -> Markup 25%, Gross Margin 20%
        val markupPct = CostCalculator.calculatePercentageFromPrice(costPaise, 12500L, CostCalculator.PricingMode.MARKUP)
        assertEquals(25.0, markupPct!!, 0.001)

        val marginPct = CostCalculator.calculatePercentageFromPrice(costPaise, 12500L, CostCalculator.PricingMode.GROSS_MARGIN)
        assertEquals(20.0, marginPct!!, 0.001)

        // 4. Invalid margin >= 100% returns null
        assertNull(CostCalculator.calculatePriceFromPercentage(costPaise, 100.0, CostCalculator.PricingMode.GROSS_MARGIN))
        assertNull(CostCalculator.calculatePriceFromPercentage(costPaise, 120.0, CostCalculator.PricingMode.GROSS_MARGIN))
    }

    @Test
    fun testTransportProportionalAllocation_Section7() {
        // Section 7 example: goods A ₹600 and B ₹400, transport ₹100 -> shares ₹60 and ₹40
        val lines = listOf(
            PurchaseLineInput(id = "item_A", quantity = 1, unitPricePaise = 60000L),
            PurchaseLineInput(id = "item_B", quantity = 1, unitPricePaise = 40000L)
        )
        val results = CostCalculator.allocateDelivery(lines, totalDeliveryPaise = 10000L)
        assertEquals(6000L, results[0].allocatedDeliveryPaise)
        assertEquals(4000L, results[1].allocatedDeliveryPaise)
        assertEquals(66000L, results[0].totalLandedCostPaise)
        assertEquals(44000L, results[1].totalLandedCostPaise)
        assertEquals(10000L, results.sumOf { it.allocatedDeliveryPaise })
    }
}
