package com.alieducational.inventory.data;
import androidx.annotation.NonNull;
import androidx.room.*;
@Entity(tableName="stock_movements",foreignKeys={@ForeignKey(entity=ProductRecord.class,parentColumns="id",childColumns="productId",onDelete=ForeignKey.RESTRICT)},indices={@Index("productId")})
public class StockMovement {
    @PrimaryKey @NonNull public String id="";
    @NonNull public String productId="";
    @NonNull public String reason="";
    public long sellableDelta;
    public long damagedDelta;
    public int packSizeSnapshot;
    public long createdAt;
    @NonNull public String note="";
    public Long openingCostPaise;
}
