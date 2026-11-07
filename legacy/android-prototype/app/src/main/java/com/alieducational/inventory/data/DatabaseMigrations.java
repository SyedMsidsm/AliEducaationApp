package com.alieducational.inventory.data;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;
public final class DatabaseMigrations {
    public static final Migration V2_TO_V3=new Migration(2,3) {
        @Override public void migrate(SupportSQLiteDatabase db) {
            db.execSQL("ALTER TABLE products ADD COLUMN packSellingPaise INTEGER");
        }
    };
    public static final Migration V1_TO_V2=new Migration(1,2) {
        @Override public void migrate(SupportSQLiteDatabase db) {
            db.execSQL("CREATE TABLE IF NOT EXISTS products (id TEXT NOT NULL PRIMARY KEY, name TEXT NOT NULL, sku TEXT, category TEXT NOT NULL, brand TEXT NOT NULL, variant TEXT NOT NULL, packSize INTEGER NOT NULL, sellingPaise INTEGER NOT NULL, lowStock INTEGER NOT NULL, archived INTEGER NOT NULL)");
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_products_sku ON products(sku)");
            db.execSQL("CREATE TABLE IF NOT EXISTS stock_movements (id TEXT NOT NULL PRIMARY KEY, productId TEXT NOT NULL, reason TEXT NOT NULL, sellableDelta INTEGER NOT NULL, damagedDelta INTEGER NOT NULL, packSizeSnapshot INTEGER NOT NULL, createdAt INTEGER NOT NULL, note TEXT NOT NULL, openingCostPaise INTEGER, FOREIGN KEY(productId) REFERENCES products(id) ON UPDATE NO ACTION ON DELETE RESTRICT)");
            db.execSQL("CREATE INDEX IF NOT EXISTS index_stock_movements_productId ON stock_movements(productId)");
        }
    };
}
