package com.alieducational.app.data

import androidx.sqlite.db.SupportSQLiteDatabase
import com.alieducational.app.data.local.AppDatabase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy

class MigrationSqlTest {

    private fun captureStatements(block: (SupportSQLiteDatabase) -> Unit): List<String> {
        val statements = mutableListOf<String>()
        val handler = InvocationHandler { _, method: Method, args: Array<out Any>? ->
            if (method.name == "execSQL" && args != null && args.isNotEmpty()) {
                statements.add(args[0] as String)
            }
            null
        }
        val proxyDb = Proxy.newProxyInstance(
            SupportSQLiteDatabase::class.java.classLoader,
            arrayOf(SupportSQLiteDatabase::class.java),
            handler
        ) as SupportSQLiteDatabase
        block(proxyDb)
        return statements
    }

    @Test
    fun testMigration1To2StatementsRecorded() {
        val statements = captureStatements { AppDatabase.MIGRATION_1_2.migrate(it) }
        assertTrue(statements.any { it.contains("ALTER TABLE products ADD COLUMN isFavorite") })
        assertTrue(statements.any { it.contains("ALTER TABLE products ADD COLUMN barcode") })
        assertTrue(statements.any { it.contains("ALTER TABLE sales ADD COLUMN changeGivenPaise") })
        assertTrue(statements.any { it.contains("CREATE TABLE IF NOT EXISTS product_barcodes") })
        assertTrue(statements.any { it.contains("CREATE UNIQUE INDEX IF NOT EXISTS index_product_barcodes_barcode") })
        assertTrue(statements.any { it.contains("CREATE TABLE IF NOT EXISTS held_bills") })
    }

    @Test
    fun testMigration2To3StatementsRecorded() {
        val statements = captureStatements { AppDatabase.MIGRATION_2_3.migrate(it) }
        assertTrue(statements.any { it.contains("ALTER TABLE sales ADD COLUMN isVoided") })
        assertTrue(statements.any { it.contains("ALTER TABLE sales ADD COLUMN voidReason") })
        assertTrue(statements.any { it.contains("ALTER TABLE sales ADD COLUMN voidTimestamp") })
        assertTrue(statements.any { it.contains("ALTER TABLE sales ADD COLUMN refundPayoutPaise") })
        assertTrue(statements.any { it.contains("ALTER TABLE sales ADD COLUMN stockRestoredCondition") })
        assertTrue(statements.any { it.contains("ALTER TABLE shop_expenses ADD COLUMN isVoided") })
        assertTrue(statements.any { it.contains("ALTER TABLE shop_expenses ADD COLUMN voidReason") })
    }

    @Test
    fun testMigration3To4StatementsRecorded() {
        val statements = captureStatements { AppDatabase.MIGRATION_3_4.migrate(it) }
        assertTrue(statements.any { it.contains("ALTER TABLE products ADD COLUMN wholesalePricePiecePaise") })
    }

    @Test
    fun testMigration4To5StatementsRecorded() {
        val statements = captureStatements { AppDatabase.MIGRATION_4_5.migrate(it) }
        assertTrue(statements.any { it.contains("ALTER TABLE sales ADD COLUMN originDeviceId") })
        assertTrue(statements.any { it.contains("ALTER TABLE sales ADD COLUMN syncStatus") })
        assertTrue(statements.any { it.contains("ALTER TABLE sale_lines ADD COLUMN lineType") })
        assertTrue(statements.any { it.contains("ALTER TABLE sale_lines ADD COLUMN serviceId") })
        assertTrue(statements.any { it.contains("ALTER TABLE sale_lines ADD COLUMN serviceName") })
        assertTrue(statements.any { it.contains("ALTER TABLE sale_lines ADD COLUMN serviceOption") })
        assertTrue(statements.any { it.contains("ALTER TABLE sale_lines ADD COLUMN unitLabel") })
        assertTrue(statements.any { it.contains("ALTER TABLE shop_expenses ADD COLUMN expenseCategory") })
        assertTrue(statements.any { it.contains("ALTER TABLE shop_expenses ADD COLUMN serviceTarget") })
        assertTrue(statements.any { it.contains("ALTER TABLE shop_expenses ADD COLUMN attachmentUri") })
        assertTrue(statements.any { it.contains("ALTER TABLE shop_expenses ADD COLUMN isSavingsTransfer") })
        assertTrue(statements.any { it.contains("CREATE TABLE IF NOT EXISTS `pigmy_deposits`") })
        assertTrue(statements.any { it.contains("CREATE TABLE IF NOT EXISTS `product_giveaways`") })
        assertTrue(statements.any { it.contains("CREATE TABLE IF NOT EXISTS `service_definitions`") })
        assertTrue(statements.any { it.contains("CREATE TABLE IF NOT EXISTS `sync_events`") })
    }

    @Test
    fun testMigration5To6StatementsRecorded() {
        val statements = captureStatements { AppDatabase.MIGRATION_5_6.migrate(it) }
        assertTrue(statements.any { it.contains("CREATE TABLE IF NOT EXISTS `pigmy_schemes`") })
        assertTrue(statements.any { it.contains("DEFAULT_PIGMY") })
    }

    @Test
    fun testMigration6To7StatementsRecorded() {
        val statements = captureStatements { AppDatabase.MIGRATION_6_7.migrate(it) }
        assertTrue(statements.any { it.contains("CREATE TABLE IF NOT EXISTS `cash_categories`") })
        assertTrue(statements.any { it.contains("cat_general") })
        assertTrue(statements.any { it.contains("ALTER TABLE shop_expenses ADD COLUMN categoryId") })
        assertTrue(statements.any { it.contains("CREATE TABLE IF NOT EXISTS `pigmy_withdrawals`") })
        assertTrue(statements.any { it.contains("ALTER TABLE pigmy_schemes ADD COLUMN openingBalancePaise") })
        assertTrue(statements.any { it.contains("ALTER TABLE pigmy_schemes ADD COLUMN openingBalanceDate") })
    }

    @Test
    fun testMigration7To8StatementsRecorded() {
        val statements = captureStatements { AppDatabase.MIGRATION_7_8.migrate(it) }
        assertTrue(statements.any { it.contains("ALTER TABLE shop_expenses ADD COLUMN isExpenseRefund") })
        assertTrue(statements.any { it.contains("ALTER TABLE shop_expenses ADD COLUMN linkedExpenseId") })
        assertTrue(statements.any { it.contains("ALTER TABLE shop_expenses ADD COLUMN classification") })
        assertTrue(statements.any { it.contains("ALTER TABLE shop_expenses ADD COLUMN paymentMethod") })
        assertTrue(statements.any { it.contains("UPDATE shop_expenses SET classification = 'SERVICES'") })
    }

    @Test
    fun testMigration8To9IndicesStatementsRecorded() {
        val statements = captureStatements { AppDatabase.MIGRATION_8_9.migrate(it) }
        assertTrue(statements.any { it.contains("CREATE INDEX IF NOT EXISTS `index_sales_timestamp`") })
        assertTrue(statements.any { it.contains("CREATE INDEX IF NOT EXISTS `index_sales_customerId`") })
        assertTrue(statements.any { it.contains("CREATE INDEX IF NOT EXISTS `index_sales_syncStatus`") })
        assertTrue(statements.any { it.contains("CREATE INDEX IF NOT EXISTS `index_sales_receiptNumber`") })
        assertTrue(statements.any { it.contains("CREATE INDEX IF NOT EXISTS `index_sale_lines_saleId`") })
        assertTrue(statements.any { it.contains("CREATE INDEX IF NOT EXISTS `index_sale_lines_productId`") })
        assertTrue(statements.any { it.contains("CREATE INDEX IF NOT EXISTS `index_stock_movements_productId_timestamp`") })
        assertTrue(statements.any { it.contains("CREATE INDEX IF NOT EXISTS `index_account_payments_accountType_accountId_timestamp`") })
        assertTrue(statements.any { it.contains("CREATE INDEX IF NOT EXISTS `index_purchase_lines_purchaseId`") })
        assertTrue(statements.any { it.contains("CREATE INDEX IF NOT EXISTS `index_sync_events_entityId`") })
        assertTrue(statements.any { it.contains("CREATE INDEX IF NOT EXISTS `index_shop_expenses_timestamp`") })
        assertTrue(statements.any { it.contains("CREATE INDEX IF NOT EXISTS `index_shop_expenses_isVoided`") })
    }

    @Test
    fun testMigrationChainContinuousSequenceFrom1To9() {
        val migrations = listOf(
            AppDatabase.MIGRATION_1_2,
            AppDatabase.MIGRATION_2_3,
            AppDatabase.MIGRATION_3_4,
            AppDatabase.MIGRATION_4_5,
            AppDatabase.MIGRATION_5_6,
            AppDatabase.MIGRATION_6_7,
            AppDatabase.MIGRATION_7_8,
            AppDatabase.MIGRATION_8_9
        )

        for (i in 0 until migrations.size) {
            val m = migrations[i]
            assertEquals("Migration start version at index $i", i + 1, m.startVersion)
            assertEquals("Migration end version at index $i", i + 2, m.endVersion)
        }
    }
}
