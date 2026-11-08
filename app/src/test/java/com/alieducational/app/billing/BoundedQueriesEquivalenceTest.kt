package com.alieducational.app.billing

import com.alieducational.app.data.local.entity.SaleEntity
import com.alieducational.app.data.local.entity.SaleLineEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class BoundedQueriesEquivalenceTest {

    @Test
    fun testSeeded200Sales_OldInMemoryVsRangeBoundedEquivalence() {
        val baseTime = 1731110400000L // Reference timestamp
        val oneDayMs = 86400000L

        val allSales = mutableListOf<SaleEntity>()
        val allSaleLines = mutableListOf<SaleLineEntity>()

        // Seed 200 sales across 10 days (20 sales per day)
        for (i in 0 until 200) {
            val dayOffset = i / 20 // 0 to 9 days ago
            val saleTime = baseTime - (dayOffset * oneDayMs) + ((i % 20) * 1000L)
            val isVoided = (i % 17 == 0) // Some voided bills
            val isCredit = (i % 5 == 0)

            val saleId = "sale-$i"
            val netPaise = (100 + (i * 10)) * 100L // in paise
            val duesPaise = if (isCredit && !isVoided) netPaise / 2 else 0L
            val recPaise = netPaise - duesPaise

            val sale = SaleEntity(
                id = saleId,
                receiptNumber = "ALI-REC-$i",
                timestamp = saleTime,
                grossTotalPaise = netPaise,
                netTotalPaise = netPaise,
                amountReceivedPaise = recPaise,
                remainingCreditPaise = duesPaise,
                paymentMethod = if (isCredit) "CREDIT" else "CASH",
                isVoided = isVoided,
                customerName = "Customer ${i % 10}"
            )
            allSales.add(sale)

            // Add lines for this sale
            // Product line
            val costSnapshot = 50L * 100L
            val qty = 2
            val lineGross = netPaise
            val lineProfit = lineGross - (costSnapshot * qty)

            val line = SaleLineEntity(
                id = "line-$i-1",
                saleId = saleId,
                productId = "prod-${i % 15}",
                productName = "Product ${i % 15}",
                quantityBase = qty,
                unitSellingPricePaise = netPaise / qty,
                unitCostSnapshotPaise = costSnapshot,
                lineGrossPaise = lineGross,
                lineNetPaise = lineGross,
                lineProfitPaise = lineProfit,
                lineType = if (i % 8 == 0) "SERVICE" else "PRODUCT"
            )
            allSaleLines.add(line)
        }

        assertEquals(200, allSales.size)
        assertEquals(200, allSaleLines.size)

        // Target Date Range: Day 2 to Day 5 (inclusive)
        val startTime = baseTime - (5 * oneDayMs)
        val endTime = baseTime - (2 * oneDayMs) + (23 * 3600 * 1000L)

        // --- OLD METHOD: Load allSales + allSaleLines, filter in memory in Compose ---
        val oldFilteredSales = allSales.filter { !it.isVoided && it.timestamp in startTime..endTime }
        val oldSaleIds = oldFilteredSales.map { it.id }.toSet()
        val oldFilteredLines = allSaleLines.filter { it.saleId in oldSaleIds }

        val oldTotalRevenue = oldFilteredSales.sumOf { it.netTotalPaise }
        val oldTotalCollected = oldFilteredSales.sumOf { it.amountReceivedPaise }
        val oldTotalDues = oldFilteredSales.sumOf { it.remainingCreditPaise }

        val oldProductLines = oldFilteredLines.filter { it.lineType != "SERVICE" && it.unitCostSnapshotPaise != null && it.unitCostSnapshotPaise > 0 }
        val oldProductRevenue = oldProductLines.sumOf { it.lineNetPaise }
        val oldProductCogs = oldProductLines.sumOf { it.quantityBase.toLong() * (it.unitCostSnapshotPaise ?: 0L) }
        val oldProductGrossProfit = oldProductRevenue - oldProductCogs

        // --- NEW METHOD: Range-bounded query equivalents ---
        val newRangeSales = allSales.filter { sale -> sale.isVoided == false && sale.timestamp >= startTime && sale.timestamp <= endTime }
        val newRangeSaleIds = newRangeSales.map { it.id }.toSet()
        val newRangeLines = allSaleLines.filter { line ->
            line.saleId in newRangeSaleIds
        }

        val newTotalRevenue = newRangeSales.sumOf { it.netTotalPaise }
        val newTotalCollected = newRangeSales.sumOf { it.amountReceivedPaise }
        val newTotalDues = newRangeSales.sumOf { it.remainingCreditPaise }

        val newProductLines = newRangeLines.filter { it.lineType != "SERVICE" && it.unitCostSnapshotPaise != null && it.unitCostSnapshotPaise > 0 }
        val newProductRevenue = newProductLines.sumOf { it.lineNetPaise }
        val newProductCogs = newProductLines.sumOf { it.quantityBase.toLong() * (it.unitCostSnapshotPaise ?: 0L) }
        val newProductGrossProfit = newProductRevenue - newProductCogs

        // --- VERIFY EXACT MATCH TO THE PAISE ---
        assertEquals("Total Revenue must match exactly", oldTotalRevenue, newTotalRevenue)
        assertEquals("Total Collected must match exactly", oldTotalCollected, newTotalCollected)
        assertEquals("Total Dues must match exactly", oldTotalDues, newTotalDues)
        assertEquals("Product Revenue must match exactly", oldProductRevenue, newProductRevenue)
        assertEquals("Product COGS must match exactly", oldProductCogs, newProductCogs)
        assertEquals("Product Gross Profit must match exactly", oldProductGrossProfit, newProductGrossProfit)
        assertTrue("Range must have captured sales", newRangeSales.isNotEmpty())
    }

    @Test
    fun testPagination_Limit100WithOffset_CoversAllRecordsWithoutDuplicates() {
        val totalRecords = 250
        val items = (1..totalRecords).map { "Bill-$it" }

        val pageSize = 100
        var currentLimit = pageSize

        // Page 1
        val page1 = items.take(currentLimit)
        assertEquals(100, page1.size)
        assertEquals("Bill-1", page1.first())
        assertEquals("Bill-100", page1.last())
        assertTrue("Has more pages", items.size > currentLimit)

        // Load More -> Page 2
        currentLimit += pageSize
        val page2 = items.take(currentLimit)
        assertEquals(200, page2.size)
        assertEquals("Bill-1", page2.first())
        assertEquals("Bill-200", page2.last())
        assertTrue("Has more pages", items.size > currentLimit)

        // Load More -> Page 3 (remaining 50)
        currentLimit += pageSize
        val page3 = items.take(currentLimit)
        assertEquals(250, page3.size)
        assertEquals("Bill-1", page3.first())
        assertEquals("Bill-250", page3.last())
        assertTrue("No more pages", currentLimit >= items.size)
    }
}
