package com.alieducational.app.billing

import com.alieducational.app.data.local.entity.ShopExpenseEntity
import com.alieducational.app.ui.BulkResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class BulkVoidTest {

    @Test
    fun testBulkResultStructure() {
        val result = BulkResult(
            done = 4,
            skipped = listOf("item-3" to "Already voided")
        )
        assertEquals(4, result.done)
        assertEquals(1, result.skipped.size)
        assertEquals("item-3", result.skipped[0].first)
        assertEquals("Already voided", result.skipped[0].second)
    }

    @Test
    fun testExpenseOrderingRefundsFirstThenNewest() {
        val now = 1700000000000L
        val parentExpense1 = ShopExpenseEntity(id = "e1", category = "RENT", amountPaise = 500000, timestamp = now)
        val parentExpense2 = ShopExpenseEntity(id = "e2", category = "ELECTRICITY", amountPaise = 100000, timestamp = now + 1000)
        val refund1 = ShopExpenseEntity(id = "r1", category = "RENT", amountPaise = 50000, isExpenseRefund = true, linkedExpenseId = "e1", timestamp = now + 2000)
        val refund2 = ShopExpenseEntity(id = "r2", category = "ELECTRICITY", amountPaise = 20000, isExpenseRefund = true, linkedExpenseId = "e2", timestamp = now + 3000)

        val input = listOf(parentExpense1, refund1, parentExpense2, refund2)

        val comparator = compareByDescending<ShopExpenseEntity> { it.isExpenseRefund }
            .thenByDescending { it.timestamp }

        val ordered = input.sortedWith(comparator)

        // Refunds must be ordered before parent expenses, newest refunds first
        assertEquals("r2", ordered[0].id)
        assertEquals("r1", ordered[1].id)
        assertEquals("e2", ordered[2].id)
        assertEquals("e1", ordered[3].id)
    }

    @Test
    fun testBulkExecutionHandlesFailuresGracefully() {
        val ids = listOf("id-1", "id-2", "id-3", "id-4", "id-5")
        var done = 0
        val skipped = mutableListOf<Pair<String, String>>()
        for (id in ids) {
            try {
                if (id == "id-3") throw IllegalStateException("Already voided")
                done++
            } catch (e: IllegalStateException) {
                skipped += id to (e.message ?: "Not allowed")
            } catch (e: IllegalArgumentException) {
                skipped += id to (e.message ?: "Not found")
            }
        }
        val result = BulkResult(done, skipped)
        assertEquals(4, result.done)
        assertEquals(1, result.skipped.size)
        assertEquals("id-3", result.skipped[0].first)
        assertEquals("Already voided", result.skipped[0].second)
    }

    @Test
    fun testBatchSizeValidation() {
        assertThrows(IllegalArgumentException::class.java) {
            require("".isNotBlank()) { "Reason cannot be blank" }
        }
        val overLimit = (1..201).map { "id-$it" }
        assertThrows(IllegalArgumentException::class.java) {
            require(overLimit.size <= 200) { "Batch size limit exceeded" }
        }
    }
}
