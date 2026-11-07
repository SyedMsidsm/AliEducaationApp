package com.alieducational.inventory.data;
import androidx.room.*;
import java.util.List;
@Dao public interface InventoryDao {
    @Insert void insertProduct(ProductRecord product);
    @Update int updateProduct(ProductRecord product);
    @Insert void insertMovement(StockMovement movement);
    @Query("SELECT * FROM stock_movements WHERE id=:id") StockMovement movement(String id);
    @Query("SELECT * FROM products WHERE id=:id") ProductRecord product(String id);
    @Query("SELECT p.*, COALESCE(SUM(m.sellableDelta),0) AS sellable, COALESCE(SUM(m.damagedDelta),0) AS damaged FROM products p LEFT JOIN stock_movements m ON m.productId=p.id GROUP BY p.id ORDER BY p.name COLLATE NOCASE") List<StockRow> stock();
    @Query("SELECT COALESCE(SUM(sellableDelta),0) FROM stock_movements WHERE productId=:id") long sellable(String id);
    @Query("SELECT * FROM stock_movements WHERE productId=:id ORDER BY createdAt DESC,rowid DESC") List<StockMovement> history(String id);
}
