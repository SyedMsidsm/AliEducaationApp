package com.alieducational.app.data

import androidx.sqlite.db.SupportSQLiteDatabase
import com.alieducational.app.data.local.AppDatabase
import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy

class MigrationSqlTest {

    @Test
    fun testMigration4To5StatementsRecorded() {
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

        AppDatabase.MIGRATION_4_5.migrate(proxyDb)

        // Verify alter tables
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

        // Verify new tables
        assertTrue(statements.any { it.contains("CREATE TABLE IF NOT EXISTS `pigmy_deposits`") })
        assertTrue(statements.any { it.contains("CREATE TABLE IF NOT EXISTS `product_giveaways`") })
        assertTrue(statements.any { it.contains("CREATE TABLE IF NOT EXISTS `service_definitions`") })
        assertTrue(statements.any { it.contains("CREATE TABLE IF NOT EXISTS `sync_events`") })
    }

    @Test
    fun testMigration8To9IndicesStatementsRecorded() {
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

        AppDatabase.MIGRATION_8_9.migrate(proxyDb)

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
}
