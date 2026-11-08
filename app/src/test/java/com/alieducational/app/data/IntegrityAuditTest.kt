package com.alieducational.app.data

import com.alieducational.app.data.local.IntegrityAudit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class IntegrityAuditTest {

    @Test
    fun testIntegrityAuditSqlQueriesAreReadOnly() {
        val queries = listOf(
            IntegrityAudit.SQL_ORPHANED_SALE_LINES,
            IntegrityAudit.SQL_INVALID_PRODUCT_SALE_LINES,
            IntegrityAudit.SQL_ORPHANED_PURCHASE_LINES,
            IntegrityAudit.SQL_INVALID_PRODUCT_STOCK_MOVEMENTS,
            IntegrityAudit.SQL_NEGATIVE_STOCK_PRODUCTS
        )

        for (q in queries) {
            assertTrue("Query must be SELECT: $q", q.trim().uppercase().startsWith("SELECT"))
            assertFalse("Query must never contain INSERT: $q", q.uppercase().contains("INSERT"))
            assertFalse("Query must never contain UPDATE: $q", q.uppercase().contains("UPDATE"))
            assertFalse("Query must never contain DELETE: $q", q.uppercase().contains("DELETE"))
            assertFalse("Query must never contain DROP: $q", q.uppercase().contains("DROP"))
        }
    }

    @Test
    fun testAuditReportHasIntegrityIssuesLogic() {
        val clean = IntegrityAudit.AuditReport(
            orphanedSaleLines = 0,
            invalidProductSaleLines = 0,
            orphanedPurchaseLines = 0,
            invalidProductStockMovements = 0,
            negativeStockProducts = 0
        )
        assertFalse(clean.hasIntegrityIssues)

        val badSaleLine = clean.copy(orphanedSaleLines = 2)
        assertTrue(badSaleLine.hasIntegrityIssues)

        val negativeStock = clean.copy(negativeStockProducts = 1)
        assertTrue(negativeStock.hasIntegrityIssues)
    }
}
