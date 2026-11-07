package com.alieducational.inventory.data;
import androidx.room.Embedded;
public class StockRow {
    @Embedded public ProductRecord product;
    public long sellable;
    public long damaged;
}
