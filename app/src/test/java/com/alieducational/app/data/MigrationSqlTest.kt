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
}
