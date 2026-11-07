package com.alieducational.inventory.data;
import androidx.annotation.NonNull;
import androidx.room.*;
@Entity(tableName="products",indices={@Index(value={"sku"},unique=true)})
public class ProductRecord {
    @PrimaryKey @NonNull public String id="";
    @NonNull public String name="";
    public String sku;
    @NonNull public String category="";
    @NonNull public String brand="";
    @NonNull public String variant="";
    public int packSize=1;
    public long sellingPaise=0;
    public Long packSellingPaise;
    public long lowStock=5;
    public boolean archived=false;
}
