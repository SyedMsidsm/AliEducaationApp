package com.alieducational.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.alieducational.app.data.local.dao.AccountPaymentDao
import com.alieducational.app.data.local.dao.CustomerDao
import com.alieducational.app.data.local.dao.HeldBillDao
import com.alieducational.app.data.local.dao.ImportBatchDao
import com.alieducational.app.data.local.dao.PigmyDepositDao
import com.alieducational.app.data.local.dao.CashCategoryDao
import com.alieducational.app.data.local.dao.PigmySchemeDao
import com.alieducational.app.data.local.dao.PigmyWithdrawalDao
import com.alieducational.app.data.local.entity.CashCategoryEntity
import com.alieducational.app.data.local.entity.PigmyWithdrawalEntity
import com.alieducational.app.data.local.dao.ProductBarcodeDao
import com.alieducational.app.data.local.dao.ProductDao
import com.alieducational.app.data.local.dao.ProductGiveawayDao
import com.alieducational.app.data.local.dao.PurchaseDao
import com.alieducational.app.data.local.dao.SaleDao
import com.alieducational.app.data.local.dao.ServiceDefinitionDao
import com.alieducational.app.data.local.dao.ShopExpenseDao
import com.alieducational.app.data.local.dao.StockMovementDao
import com.alieducational.app.data.local.dao.SupplierDao
import com.alieducational.app.data.local.dao.SyncEventDao
import com.alieducational.app.data.local.entity.AccountPaymentEntity
import com.alieducational.app.data.local.entity.CustomerEntity
import com.alieducational.app.data.local.entity.HeldBillEntity
import com.alieducational.app.data.local.entity.ImportBatchEntity
import com.alieducational.app.data.local.entity.PigmyDepositEntity
import com.alieducational.app.data.local.entity.PigmySchemeEntity
import com.alieducational.app.data.local.entity.ProductBarcodeEntity
import com.alieducational.app.data.local.entity.ProductEntity
import com.alieducational.app.data.local.entity.ProductGiveawayEntity
import com.alieducational.app.data.local.entity.PurchaseEntity
import com.alieducational.app.data.local.entity.PurchaseLineEntity
import com.alieducational.app.data.local.entity.SaleEntity
import com.alieducational.app.data.local.entity.SaleLineEntity
import com.alieducational.app.data.local.entity.ServiceDefinitionEntity
import com.alieducational.app.data.local.entity.ShopExpenseEntity
import com.alieducational.app.data.local.entity.StockMovementEntity
import com.alieducational.app.data.local.entity.SupplierEntity
import com.alieducational.app.data.local.entity.SyncEventEntity

@Database(
    entities = [
        ProductEntity::class,
        ProductBarcodeEntity::class,
        StockMovementEntity::class,
        SupplierEntity::class,
        CustomerEntity::class,
        PurchaseEntity::class,
        PurchaseLineEntity::class,
        SaleEntity::class,
        SaleLineEntity::class,
        AccountPaymentEntity::class,
        ShopExpenseEntity::class,
        CashCategoryEntity::class,
        ImportBatchEntity::class,
        HeldBillEntity::class,
        PigmyDepositEntity::class,
        PigmyWithdrawalEntity::class,
        PigmySchemeEntity::class,
        ProductGiveawayEntity::class,
        ServiceDefinitionEntity::class,
        SyncEventEntity::class
    ],
    version = 8,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun productBarcodeDao(): ProductBarcodeDao
    abstract fun stockMovementDao(): StockMovementDao
    abstract fun supplierDao(): SupplierDao
    abstract fun customerDao(): CustomerDao
    abstract fun purchaseDao(): PurchaseDao
    abstract fun saleDao(): SaleDao
    abstract fun accountPaymentDao(): AccountPaymentDao
    abstract fun shopExpenseDao(): ShopExpenseDao
    abstract fun importBatchDao(): ImportBatchDao
    abstract fun heldBillDao(): HeldBillDao
    abstract fun pigmyDepositDao(): PigmyDepositDao
    abstract fun pigmyWithdrawalDao(): PigmyWithdrawalDao
    abstract fun pigmySchemeDao(): PigmySchemeDao
    abstract fun cashCategoryDao(): CashCategoryDao
    abstract fun productGiveawayDao(): ProductGiveawayDao
    abstract fun serviceDefinitionDao(): ServiceDefinitionDao
    abstract fun syncEventDao(): SyncEventDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE products ADD COLUMN isFavorite INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE products ADD COLUMN barcode TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE sales ADD COLUMN changeGivenPaise INTEGER NOT NULL DEFAULT 0")
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS product_barcodes (
                        id TEXT NOT NULL PRIMARY KEY,
                        barcode TEXT NOT NULL,
                        productId TEXT NOT NULL,
                        unitType TEXT NOT NULL DEFAULT 'PIECE',
                        packQuantity INTEGER NOT NULL DEFAULT 1,
                        createdAt INTEGER NOT NULL
                    )
                """.trimIndent())
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_product_barcodes_barcode ON product_barcodes(barcode)")
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS held_bills (
                        id TEXT NOT NULL PRIMARY KEY,
                        note TEXT,
                        customerId TEXT,
                        customerName TEXT,
                        discountType TEXT NOT NULL DEFAULT 'NONE',
                        discountValuePaise INTEGER NOT NULL DEFAULT 0,
                        discountPercent REAL NOT NULL DEFAULT 0.0,
                        draftJson TEXT NOT NULL,
                        itemCount INTEGER NOT NULL DEFAULT 0,
                        totalPaise INTEGER NOT NULL DEFAULT 0,
                        createdAt INTEGER NOT NULL
                    )
                """.trimIndent())
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE sales ADD COLUMN isVoided INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE sales ADD COLUMN voidReason TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE sales ADD COLUMN voidTimestamp INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE sales ADD COLUMN refundPayoutPaise INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE sales ADD COLUMN stockRestoredCondition TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE shop_expenses ADD COLUMN isVoided INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE shop_expenses ADD COLUMN voidReason TEXT DEFAULT NULL")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE products ADD COLUMN wholesalePricePiecePaise INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. Alter sales
                db.execSQL("ALTER TABLE sales ADD COLUMN originDeviceId TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE sales ADD COLUMN syncStatus TEXT NOT NULL DEFAULT 'LOCAL'")

                // 2. Alter sale_lines
                db.execSQL("ALTER TABLE sale_lines ADD COLUMN lineType TEXT NOT NULL DEFAULT 'PRODUCT'")
                db.execSQL("ALTER TABLE sale_lines ADD COLUMN serviceId TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE sale_lines ADD COLUMN serviceName TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE sale_lines ADD COLUMN serviceOption TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE sale_lines ADD COLUMN unitLabel TEXT DEFAULT NULL")

                // 3. Alter shop_expenses
                db.execSQL("ALTER TABLE shop_expenses ADD COLUMN expenseCategory TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE shop_expenses ADD COLUMN serviceTarget TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE shop_expenses ADD COLUMN attachmentUri TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE shop_expenses ADD COLUMN isSavingsTransfer INTEGER NOT NULL DEFAULT 0")

                // 4. Create pigmy_deposits table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `pigmy_deposits` (
                        `id` TEXT NOT NULL,
                        `accountSchemeId` TEXT NOT NULL,
                        `paidTimestamp` INTEGER NOT NULL,
                        `amountPaise` INTEGER NOT NULL,
                        `paymentSource` TEXT NOT NULL,
                        `notes` TEXT,
                        `linkedExpenseId` TEXT,
                        `isVoided` INTEGER NOT NULL,
                        `voidReason` TEXT,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                """.trimIndent())

                // 5. Create product_giveaways table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `product_giveaways` (
                        `id` TEXT NOT NULL,
                        `productId` TEXT NOT NULL,
                        `productName` TEXT NOT NULL,
                        `quantityBase` INTEGER NOT NULL,
                        `isPack` INTEGER NOT NULL,
                        `packSizeSnapshot` INTEGER NOT NULL,
                        `unitCostSnapshotPaise` INTEGER,
                        `reason` TEXT NOT NULL,
                        `recipientNote` TEXT,
                        `timestamp` INTEGER NOT NULL,
                        `isCancelled` INTEGER NOT NULL,
                        `cancelledTimestamp` INTEGER,
                        PRIMARY KEY(`id`)
                    )
                """.trimIndent())

                // 6. Create service_definitions table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `service_definitions` (
                        `id` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `unitLabel` TEXT NOT NULL,
                        `isPredefined` INTEGER NOT NULL,
                        `isArchived` INTEGER NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                """.trimIndent())

                // 7. Create sync_events table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `sync_events` (
                        `id` TEXT NOT NULL,
                        `eventType` TEXT NOT NULL,
                        `entityId` TEXT NOT NULL,
                        `payloadJson` TEXT NOT NULL,
                        `originDeviceId` TEXT NOT NULL,
                        `status` TEXT NOT NULL,
                        `retryCount` INTEGER NOT NULL,
                        `timestamp` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                """.trimIndent())
            }
        }
    
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `pigmy_schemes` (
                        `id` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `usualDailyAmountPaise` INTEGER NOT NULL,
                        `isArchived` INTEGER NOT NULL DEFAULT 0,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                """.trimIndent())

                val now = System.currentTimeMillis()
                db.execSQL("""
                    INSERT OR IGNORE INTO `pigmy_schemes` (`id`, `name`, `usualDailyAmountPaise`, `isArchived`, `createdAt`, `updatedAt`)
                    VALUES 
                        ('DEFAULT_PIGMY', 'Pigmy Scheme 1', 10000, 0, $now, $now),
                        ('scheme_2', 'Pigmy Scheme 2', 20000, 0, $now + 1, $now + 1),
                        ('scheme_3', 'Pigmy Scheme 3', 30000, 0, $now + 2, $now + 2)
                """.trimIndent())
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. Create cash_categories table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `cash_categories` (
                        `id` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `isSystem` INTEGER NOT NULL DEFAULT 0,
                        `isArchived` INTEGER NOT NULL DEFAULT 0,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                """.trimIndent())

                // 2. Populate default categories
                val now = System.currentTimeMillis()
                db.execSQL("""
                    INSERT OR IGNORE INTO `cash_categories` (`id`, `name`, `isSystem`, `isArchived`, `createdAt`, `updatedAt`)
                    VALUES
                        ('cat_general', 'General', 1, 0, $now, $now),
                        ('cat_pigmy', 'Pigmy', 1, 0, $now + 1, $now + 1),
                        ('cat_rent', 'Rent', 0, 0, $now + 2, $now + 2),
                        ('cat_electricity', 'Electricity', 0, 0, $now + 3, $now + 3),
                        ('cat_transport', 'Transport', 0, 0, $now + 4, $now + 4),
                        ('cat_salary', 'Salary', 0, 0, $now + 5, $now + 5),
                        ('cat_tea_snacks', 'Tea & Snacks', 0, 0, $now + 6, $now + 6)
                """.trimIndent())

                // 3. Alter shop_expenses to add categoryId
                db.execSQL("ALTER TABLE shop_expenses ADD COLUMN categoryId TEXT DEFAULT NULL")

                // 4. Create pigmy_withdrawals table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `pigmy_withdrawals` (
                        `id` TEXT NOT NULL,
                        `accountSchemeId` TEXT NOT NULL,
                        `withdrawalTimestamp` INTEGER NOT NULL,
                        `amountPaise` INTEGER NOT NULL,
                        `destination` TEXT NOT NULL,
                        `notes` TEXT,
                        `linkedExpenseId` TEXT,
                        `isVoided` INTEGER NOT NULL DEFAULT 0,
                        `voidReason` TEXT,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                """.trimIndent())

                // 5. Alter pigmy_schemes to add openingBalancePaise and openingBalanceDate
                db.execSQL("ALTER TABLE pigmy_schemes ADD COLUMN openingBalancePaise INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE pigmy_schemes ADD COLUMN openingBalanceDate INTEGER DEFAULT NULL")
            }
        }

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE shop_expenses ADD COLUMN isExpenseRefund INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE shop_expenses ADD COLUMN linkedExpenseId TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE shop_expenses ADD COLUMN classification TEXT NOT NULL DEFAULT 'SHARED'")
                db.execSQL("ALTER TABLE shop_expenses ADD COLUMN paymentMethod TEXT NOT NULL DEFAULT 'CASH'")
                db.execSQL("UPDATE shop_expenses SET classification = 'SERVICES' WHERE category = 'SERVICE_EXPENSE' OR serviceTarget IS NOT NULL")
            }
        }

    }
}