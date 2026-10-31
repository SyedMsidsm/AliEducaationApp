package com.alieducational.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.alieducational.app.data.local.dao.AccountPaymentDao
import com.alieducational.app.data.local.dao.CustomerDao
import com.alieducational.app.data.local.dao.HeldBillDao
import com.alieducational.app.data.local.dao.ImportBatchDao
import com.alieducational.app.data.local.dao.ProductBarcodeDao
import com.alieducational.app.data.local.dao.ProductDao
import com.alieducational.app.data.local.dao.PurchaseDao
import com.alieducational.app.data.local.dao.SaleDao
import com.alieducational.app.data.local.dao.ShopExpenseDao
import com.alieducational.app.data.local.dao.StockMovementDao
import com.alieducational.app.data.local.dao.SupplierDao
import com.alieducational.app.data.local.entity.AccountPaymentEntity
import com.alieducational.app.data.local.entity.CustomerEntity
import com.alieducational.app.data.local.entity.HeldBillEntity
import com.alieducational.app.data.local.entity.ImportBatchEntity
import com.alieducational.app.data.local.entity.ProductBarcodeEntity
import com.alieducational.app.data.local.entity.ProductEntity
import com.alieducational.app.data.local.entity.PurchaseEntity
import com.alieducational.app.data.local.entity.PurchaseLineEntity
import com.alieducational.app.data.local.entity.SaleEntity
import com.alieducational.app.data.local.entity.SaleLineEntity
import com.alieducational.app.data.local.entity.ShopExpenseEntity
import com.alieducational.app.data.local.entity.StockMovementEntity
import com.alieducational.app.data.local.entity.SupplierEntity

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
        ImportBatchEntity::class,
        HeldBillEntity::class
    ],
    version = 4,
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
    }
}