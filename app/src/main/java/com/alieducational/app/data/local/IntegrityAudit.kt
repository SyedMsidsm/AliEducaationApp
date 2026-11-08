package com.alieducational.app.data.local

import androidx.sqlite.db.SupportSQLiteDatabase
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IntegrityAudit @Inject constructor(
    private val db: AppDatabase
) {
    data class AuditReport(
        val orphanedSaleLines: Long,
        val invalidProductSaleLines: Long,
        val orphanedPurchaseLines: Long,
        val invalidProductStockMovements: Long,
        val negativeStockProducts: Long
    ) {
        val hasIntegrityIssues: Boolean
            get() = orphanedSaleLines > 0 ||
                    invalidProductSaleLines > 0 ||
                    orphanedPurchaseLines > 0 ||
                    invalidProductStockMovements > 0 ||
                    negativeStockProducts > 0
    }

    fun runAudit(): AuditReport {
        val readableDb = db.openHelper.readableDatabase
        return AuditReport(
            orphanedSaleLines = queryCount(readableDb, SQL_ORPHANED_SALE_LINES),
            invalidProductSaleLines = queryCount(readableDb, SQL_INVALID_PRODUCT_SALE_LINES),
            orphanedPurchaseLines = queryCount(readableDb, SQL_ORPHANED_PURCHASE_LINES),
            invalidProductStockMovements = queryCount(readableDb, SQL_INVALID_PRODUCT_STOCK_MOVEMENTS),
            negativeStockProducts = queryCount(readableDb, SQL_NEGATIVE_STOCK_PRODUCTS)
        )
    }

    private fun queryCount(database: SupportSQLiteDatabase, sql: String): Long {
        val cursor = database.query(sql)
        return cursor.use {
            if (it.moveToFirst()) it.getLong(0) else 0L
        }
    }

    companion object {
        const val SQL_ORPHANED_SALE_LINES = "SELECT COUNT(*) FROM sale_lines WHERE saleId NOT IN (SELECT id FROM sales);"
        const val SQL_INVALID_PRODUCT_SALE_LINES = "SELECT COUNT(*) FROM sale_lines WHERE productId IS NOT NULL AND productId NOT IN (SELECT id FROM products);"
        const val SQL_ORPHANED_PURCHASE_LINES = "SELECT COUNT(*) FROM purchase_lines WHERE purchaseId NOT IN (SELECT id FROM purchases);"
        const val SQL_INVALID_PRODUCT_STOCK_MOVEMENTS = "SELECT COUNT(*) FROM stock_movements WHERE productId NOT IN (SELECT id FROM products);"
        const val SQL_NEGATIVE_STOCK_PRODUCTS = "SELECT COUNT(*) FROM products WHERE currentSellableStock < 0;"
    }
}
