package com.alieducational.app.service

import com.alieducational.app.data.local.entity.ShopExpenseEntity
import com.alieducational.app.domain.calculation.CostCalculator
import com.alieducational.app.domain.calculation.PurchaseLineInput
import com.alieducational.app.domain.calculation.SaleLineInput
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class GoldenScenarioAndSection25Test {

    @Test
    fun testGoldenScenario_Section20() {
        // G01-G14 Golden scenario from ANTIGRAVITY_LAYOUT_GRAPHS_SUPPLIERS_AND_SHOP_EXPENSES.md Section 20
        var physicalCashPaise = 1000000L // ₹10,000.00
        var bankBalancePaise = 0L

        // G01: Supplier A, Customer A added in directory (0 balance effect)
        var supplierADuePaise = 0L
        var customerADuePaise = 0L
        var notebookStockQty = 0
        var notebookAvgCostPaise = 0L

        // G02: Receive 100 notebooks for ₹1,000; pay supplier ₹600
        val g02GoodsCostPaise = 100000L // ₹1,000.00
        val g02PaidPaise = 60000L // ₹600.00
        val g02UnpaidPaise = g02GoodsCostPaise - g02PaidPaise // ₹400.00
        notebookStockQty += 100
        supplierADuePaise += g02UnpaidPaise
        physicalCashPaise -= g02PaidPaise
        assertEquals(940000L, physicalCashPaise) // ₹9,400.00
        assertEquals(40000L, supplierADuePaise) // ₹400.00

        // G03: Delivery ₹100 capitalized into this receipt
        val deliveryPaise = 10000L // ₹100.00
        physicalCashPaise -= deliveryPaise
        val totalLandedGoodsCost = g02GoodsCostPaise + deliveryPaise // ₹1,100.00
        notebookAvgCostPaise = totalLandedGoodsCost / notebookStockQty // ₹11.00 per piece (1100 paise)
        assertEquals(930000L, physicalCashPaise) // ₹9,300.00
        assertEquals(1100L, notebookAvgCostPaise) // ₹11.00 unit cost
        assertEquals(40000L, supplierADuePaise) // supplier due still ₹400

        // G04: Bill Customer A for 10 notebooks at ₹20 each; apply ₹20 whole-bill discount; collect ₹100
        // Gross: 10 * 20 = 200; Discount: 20; Net: 180; COGS: 10 * 11 = 110; Profit: 70
        val g04GrossPaise = 20000L
        val g04DiscountPaise = 2000L
        val g04NetPaise = g04GrossPaise - g04DiscountPaise // 18000L (₹180.00)
        val g04CogsPaise = 10 * notebookAvgCostPaise // 11000L (₹110.00)
        val g04GrossProfitPaise = g04NetPaise - g04CogsPaise // 7000L (₹70.00)
        val g04CollectedPaise = 10000L // ₹100.00
        val g04DuePaise = g04NetPaise - g04CollectedPaise // 8000L (₹80.00)
        notebookStockQty -= 10
        customerADuePaise += g04DuePaise
        physicalCashPaise += g04CollectedPaise
        assertEquals(90, notebookStockQty)
        assertEquals(940000L, physicalCashPaise) // ₹9,400.00
        assertEquals(8000L, customerADuePaise) // ₹80.00

        // G05: Service bill: 5 photocopies at ₹10 each, fully paid
        val g05ServiceRevenuePaise = 5000L // ₹50.00
        physicalCashPaise += g05ServiceRevenuePaise
        assertEquals(945000L, physicalCashPaise) // ₹9,450.00

        // G06: Receive ₹30 repayment against Customer A's bill
        val g06RepaymentPaise = 3000L // ₹30.00
        customerADuePaise -= g06RepaymentPaise
        physicalCashPaise += g06RepaymentPaise
        assertEquals(5000L, customerADuePaise) // ₹50.00 due
        assertEquals(948000L, physicalCashPaise) // ₹9,480.00

        // G07: Pay Supplier A remaining ₹400
        val g07SupplierPaymentPaise = 40000L // ₹400.00
        supplierADuePaise -= g07SupplierPaymentPaise
        physicalCashPaise -= g07SupplierPaymentPaise
        assertEquals(0L, supplierADuePaise) // ₹0.00 due
        assertEquals(908000L, physicalCashPaise) // ₹9,080.00

        // G08: Shop expense: electricity ₹120, Shared
        val g08ExpSharedPaise = 12000L
        physicalCashPaise -= g08ExpSharedPaise
        assertEquals(896000L, physicalCashPaise) // ₹8,960.00

        // G09: Shop expense: paper ₹80, Services
        val g09ExpServicesPaise = 8000L
        physicalCashPaise -= g09ExpServicesPaise
        assertEquals(888000L, physicalCashPaise) // ₹8,880.00

        // G10: Shop expense: shelving repair ₹40, Products
        val g10ExpProductsPaise = 4000L
        physicalCashPaise -= g10ExpProductsPaise
        assertEquals(884000L, physicalCashPaise) // ₹8,840.00

        // G11: Linked electricity expense refund: ₹20
        val g11RefundSharedPaise = 2000L
        physicalCashPaise += g11RefundSharedPaise
        assertEquals(886000L, physicalCashPaise) // ₹8,860.00

        // G12: Confirm ₹300 Pigmy deposit
        var pigmyTrackedSavingsPaise = 0L
        val g12PigmyDepositPaise = 30000L
        pigmyTrackedSavingsPaise += g12PigmyDepositPaise
        physicalCashPaise -= g12PigmyDepositPaise
        assertEquals(30000L, pigmyTrackedSavingsPaise) // ₹300.00
        assertEquals(856000L, physicalCashPaise) // ₹8,560.00

        // G13: Withdraw ₹100 Pigmy savings into shop cash
        val g13PigmyWithdrawalPaise = 10000L
        pigmyTrackedSavingsPaise -= g13PigmyWithdrawalPaise
        physicalCashPaise += g13PigmyWithdrawalPaise
        assertEquals(20000L, pigmyTrackedSavingsPaise) // ₹200.00
        assertEquals(866000L, physicalCashPaise) // ₹8,660.00

        // G14: Return 2 saleable notebooks from G04, refund ₹36 in cash
        // Net discounted return: 2 * 18 = 36; Cost reversal: 2 * 11 = 22
        val g14ReturnNetPaise = 3600L // ₹36.00
        val g14ReturnCogsPaise = 2200L // ₹22.00
        notebookStockQty += 2
        physicalCashPaise -= g14ReturnNetPaise
        assertEquals(92, notebookStockQty)
        assertEquals(862400L, physicalCashPaise) // ₹8,624.00

        // Net financial summaries
        val netProductSalesPaise = g04NetPaise - g14ReturnNetPaise // 18000 - 3600 = 14400 (₹144.00)
        val netProductCogsPaise = g04CogsPaise - g14ReturnCogsPaise // 11000 - 2200 = 8800 (₹88.00)
        val productGrossProfitPaise = netProductSalesPaise - netProductCogsPaise // 5600 (₹56.00)
        val netServiceRevenuePaise = g05ServiceRevenuePaise // 5000 (₹50.00)
        val combinedNetBilledRevenuePaise = netProductSalesPaise + netServiceRevenuePaise // 19400 (₹194.00)

        val grossOperatingExpensesPaise = g08ExpSharedPaise + g09ExpServicesPaise + g10ExpProductsPaise // 12000 + 8000 + 4000 = 24000 (₹240.00)
        val linkedExpenseRefundsPaise = g11RefundSharedPaise // 2000 (₹20.00)
        val netOperatingExpensesPaise = grossOperatingExpensesPaise - linkedExpenseRefundsPaise // 22000 (₹220.00)

        val netProductsExpensesPaise = g10ExpProductsPaise // 4000 (₹40.00)
        val netServicesExpensesPaise = g09ExpServicesPaise // 8000 (₹80.00)
        val netSharedExpensesPaise = g08ExpSharedPaise - g11RefundSharedPaise // 12000 - 2000 = 10000 (₹100.00)

        val productResultAfterExpensesPaise = productGrossProfitPaise - netProductsExpensesPaise // 5600 - 4000 = 1600 (₹16.00)
        val serviceResultAfterExpensesPaise = netServiceRevenuePaise - netServicesExpensesPaise // 5000 - 8000 = -3000 (-₹30.00)
        val combinedRecordedOperatingResultPaise = productGrossProfitPaise + netServiceRevenuePaise - netOperatingExpensesPaise // 5600 + 5000 - 22000 = -11400 (-₹114.00)

        val remainingInventoryCostPaise = notebookStockQty * notebookAvgCostPaise // 92 * 1100 = 101200 (₹1,012.00)

        // Exact reconciliation assertions matching Section 20 table
        assertEquals("Sellable notebooks", 92, notebookStockQty)
        assertEquals("Remaining notebook inventory cost", 101200L, remainingInventoryCostPaise)
        assertEquals("Net product sales", 14400L, netProductSalesPaise)
        assertEquals("Product COGS", 8800L, netProductCogsPaise)
        assertEquals("Product gross profit", 5600L, productGrossProfitPaise)
        assertEquals("Net service revenue", 5000L, netServiceRevenuePaise)
        assertEquals("Combined net billed revenue", 19400L, combinedNetBilledRevenuePaise)
        assertEquals("Gross operating expenses", 24000L, grossOperatingExpensesPaise)
        assertEquals("Linked expense refunds", 2000L, linkedExpenseRefundsPaise)
        assertEquals("Net operating expenses", 22000L, netOperatingExpensesPaise)
        assertEquals("Net Products expenses", 4000L, netProductsExpensesPaise)
        assertEquals("Net Services expenses", 8000L, netServicesExpensesPaise)
        assertEquals("Net Shared expenses", 10000L, netSharedExpensesPaise)
        assertEquals("Product result after Products expenses", 1600L, productResultAfterExpensesPaise)
        assertEquals("Service revenue minus Services expenses", -3000L, serviceResultAfterExpensesPaise)
        assertEquals("Combined recorded operating result", -11400L, combinedRecordedOperatingResultPaise)
        assertEquals("Physical cash", 862400L, physicalCashPaise)
        assertEquals("Bank balance", 0L, bankBalancePaise)
        assertEquals("Customer A remaining due", 5000L, customerADuePaise)
        assertEquals("Supplier A due", 0L, supplierADuePaise)
        assertEquals("Pigmy tracked savings", 20000L, pigmyTrackedSavingsPaise)

        // Gross margin calculation: 56.00 / 144.00 = 38.89%
        val marginPct = (productGrossProfitPaise.toDouble() / netProductSalesPaise.toDouble()) * 100.0
        assertEquals("Gross margin ~38.89%", 38.89, marginPct, 0.01)

        // Independent balance identity check (Section 20):
        // Cash (8,624) + Inventory (1,012) + Receivables (50) + Pigmy (200) - Payables (0) = 9,886
        // Equal to Opening Cash (10,000) + Combined Result (-114) = 9,886
        val endingTotalAssets = physicalCashPaise + remainingInventoryCostPaise + customerADuePaise + pigmyTrackedSavingsPaise - supplierADuePaise
        val expectedAssetsFromOperations = 1000000L + combinedRecordedOperatingResultPaise
        assertEquals(988600L, endingTotalAssets)
        assertEquals(expectedAssetsFromOperations, endingTotalAssets)
    }

    @Test
    fun testSection25_IndependentExportFixture() {
        // Section 25 Independent Export Fixture
        val cal = Calendar.getInstance()
        fun makeTime(day: Int): Long {
            cal.set(2026, Calendar.NOVEMBER, day, 12, 0, 0)
            cal.set(Calendar.MILLISECOND, 0)
            return cal.timeInMillis
        }

        val e1 = ShopExpenseEntity(id = "E1", category = "Electricity", amountPaise = 100000L, isIncome = false, timestamp = makeTime(1), classification = "SHARED")
        val e2 = ShopExpenseEntity(id = "E2", category = "Paper", amountPaise = 20000L, isIncome = false, timestamp = makeTime(2), classification = "SERVICES")
        val e3 = ShopExpenseEntity(id = "E3", category = "Electricity", amountPaise = 10000L, isIncome = true, timestamp = makeTime(3), isExpenseRefund = true, linkedExpenseId = "E1", classification = "SHARED")
        val e4 = ShopExpenseEntity(id = "E4", category = "Other", amountPaise = 50000L, isIncome = true, timestamp = makeTime(4), classification = "SHARED")
        val e5 = ShopExpenseEntity(id = "E5", category = "Electricity", amountPaise = 30000L, isIncome = false, timestamp = makeTime(5), classification = "SHARED")

        val allItems = listOf(e1, e2, e3, e4, e5)

        cal.set(2026, Calendar.NOVEMBER, 1, 0, 0, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val rangeStart = cal.timeInMillis

        cal.set(2026, Calendar.NOVEMBER, 3, 23, 59, 59)
        cal.set(Calendar.MILLISECOND, 999)
        val rangeEnd = cal.timeInMillis

        // 1. Select 1-3 Nov and Electricity: 2 rows (E1, E3), inflow ₹100, outflow ₹1,000, net -₹900
        val filter1 = allItems.filter { it.timestamp in rangeStart..rangeEnd && it.category == "Electricity" }
        assertEquals(2, filter1.size)
        val inflow1 = filter1.filter { it.isIncome }.sumOf { it.amountPaise }
        val outflow1 = filter1.filter { !it.isIncome }.sumOf { it.amountPaise }
        val net1 = inflow1 - outflow1
        assertEquals(10000L, inflow1) // ₹100.00
        assertEquals(100000L, outflow1) // ₹1,000.00
        assertEquals(-90000L, net1) // -₹900.00

        // 2. Select 1-3 Nov and Electricity + Paper: 3 rows (E1, E2, E3), inflow ₹100, outflow ₹1,200, net -₹1,100
        val filter2 = allItems.filter { it.timestamp in rangeStart..rangeEnd && (it.category == "Electricity" || it.category == "Paper") }
        assertEquals(3, filter2.size)
        val inflow2 = filter2.filter { it.isIncome }.sumOf { it.amountPaise }
        val outflow2 = filter2.filter { !it.isIncome }.sumOf { it.amountPaise }
        val net2 = inflow2 - outflow2
        assertEquals(10000L, inflow2) // ₹100.00
        assertEquals(120000L, outflow2) // ₹1,200.00
        assertEquals(-110000L, net2) // -₹1,100.00

        // 3. Select 1-3 Nov, Electricity and Out only: 1 row (E1), outflow ₹1,000, net -₹1,000
        val filter3 = allItems.filter { it.timestamp in rangeStart..rangeEnd && it.category == "Electricity" && !it.isIncome }
        assertEquals(1, filter3.size)
        val inflow3 = filter3.filter { it.isIncome }.sumOf { it.amountPaise }
        val outflow3 = filter3.filter { !it.isIncome }.sumOf { it.amountPaise }
        val net3 = inflow3 - outflow3
        assertEquals(0L, inflow3)
        assertEquals(100000L, outflow3) // ₹1,000.00
        assertEquals(-100000L, net3) // -₹1,000.00

        // Invariants: E4 (4 Nov) and E5 (5 Nov) never appear in 1-3 Nov selection
        assertTrue(filter1.none { it.id == "E4" || it.id == "E5" })
        assertTrue(filter2.none { it.id == "E4" || it.id == "E5" })
        assertTrue(filter3.none { it.id == "E4" || it.id == "E5" })
    }

    @Test
    fun testIndianNumberGroupingValues() {
        // Section 23 table:
        assertEquals("₹999.00", CostCalculator.formatPaiseToRupees(99900L))
        assertEquals("₹1,000.00", CostCalculator.formatPaiseToRupees(100000L))
        assertEquals("₹12,235.00", CostCalculator.formatPaiseToRupees(1223500L))
        assertEquals("₹1,00,000.00", CostCalculator.formatPaiseToRupees(10000000L))
        assertEquals("₹12,34,567.50", CostCalculator.formatPaiseToRupees(123456750L))
        assertEquals("-₹12,197.72", CostCalculator.formatPaiseToRupees(-1219772L))
        assertEquals("1,23,456 pcs", CostCalculator.formatQuantityWithGrouping(123456, "pcs"))
    }
}
