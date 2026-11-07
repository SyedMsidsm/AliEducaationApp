package com.alieducational.inventory.data;
import androidx.room.Database;
import androidx.room.RoomDatabase;

@Database(entities={SettingsRecord.class,DraftRecord.class,ProductRecord.class,StockMovement.class},version=3,exportSchema=true)
public abstract class ShopDatabase extends RoomDatabase {
    public abstract FoundationDao foundation();
    public abstract InventoryDao inventory();
}
