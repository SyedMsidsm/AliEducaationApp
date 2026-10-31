package com.alieducational.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
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
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {
    @Query("SELECT * FROM products WHERE isArchived = 0 ORDER BY name ASC")
    fun getAllActiveProducts(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE isArchived = 0 AND (name LIKE '%' || :query || '%' OR sku LIKE '%' || :query || '%' OR barcode LIKE '%' || :query || '%' OR brand LIKE '%' || :query || '%' OR category LIKE '%' || :query || '%') ORDER BY name ASC")
    fun searchProducts(query: String): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE isArchived = 0 AND currentSellableStock <= lowStockThreshold ORDER BY currentSellableStock ASC")
    fun getLowStockProducts(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE id = :id")
    suspend fun getProductById(id: String): ProductEntity?

    @Query("SELECT * FROM products WHERE sku = :sku AND isArchived = 0 LIMIT 1")
    suspend fun getProductBySku(sku: String): ProductEntity?

    @Query("SELECT * FROM products WHERE barcode = :barcode AND isArchived = 0 LIMIT 1")
    suspend fun getProductByBarcode(barcode: String): ProductEntity?

    @Query("SELECT * FROM products WHERE LOWER(name) = LOWER(:name) AND isArchived = 0 LIMIT 1")
    suspend fun getProductByName(name: String): ProductEntity?

    @Query("SELECT * FROM products WHERE isArchived = 0 AND isFavorite = 1 ORDER BY name ASC")
    fun getFavoriteProducts(): Flow<List<ProductEntity>>

    @Query("UPDATE products SET isFavorite = :isFav WHERE id = :productId")
    suspend fun setFavorite(productId: String, isFav: Boolean)

    @Query("SELECT DISTINCT category FROM products WHERE isArchived = 0 ORDER BY category ASC")
    fun getAllCategories(): Flow<List<String>>

    @Query("SELECT * FROM products WHERE isArchived = 0 AND category = :category ORDER BY name ASC")
    fun getProductsByCategory(category: String): Flow<List<ProductEntity>>

    @Query("SELECT SUM(currentSellableStock * movingAvgCostPaise) FROM products WHERE isArchived = 0")
    fun getTotalStockValuePaise(): Flow<Long?>

    @Query("SELECT COUNT(*) FROM products WHERE isArchived = 0 AND currentSellableStock <= lowStockThreshold")
    fun getLowStockCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM products WHERE isArchived = 0")
    fun getTotalProductCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: ProductEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProducts(products: List<ProductEntity>)

    @Update
    suspend fun updateProduct(product: ProductEntity)

    @Query("UPDATE products SET currentSellableStock = currentSellableStock + :changeBase, updatedAt = :timestamp WHERE id = :productId")
    suspend fun adjustSellableStock(productId: String, changeBase: Int, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE products SET currentSellableStock = currentSellableStock - :qty, currentDamagedStock = currentDamagedStock + :qty, updatedAt = :timestamp WHERE id = :productId")
    suspend fun moveToDamagedStock(productId: String, qty: Int, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE products SET movingAvgCostPaise = :newCostPaise, updatedAt = :timestamp WHERE id = :productId")
    suspend fun updateWeightedAverageCost(productId: String, newCostPaise: Long, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE products SET isArchived = 1, updatedAt = :timestamp WHERE id = :productId")
    suspend fun archiveProduct(productId: String, timestamp: Long = System.currentTimeMillis())
}

@Dao
interface StockMovementDao {
    @Query("SELECT * FROM stock_movements WHERE productId = :productId ORDER BY timestamp DESC")
    fun getMovementsForProduct(productId: String): Flow<List<StockMovementEntity>>

    @Query("SELECT * FROM stock_movements ORDER BY timestamp DESC LIMIT 50")
    fun getRecentMovements(): Flow<List<StockMovementEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMovement(movement: StockMovementEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMovements(movements: List<StockMovementEntity>)
}

@Dao
interface SupplierDao {
    @Query("SELECT * FROM suppliers WHERE isArchived = 0 ORDER BY name ASC")
    fun getAllSuppliers(): Flow<List<SupplierEntity>>

    @Query("SELECT * FROM suppliers WHERE id = :id")
    suspend fun getSupplierById(id: String): SupplierEntity?

    @Query("SELECT * FROM suppliers WHERE LOWER(name) = LOWER(:name) AND isArchived = 0 LIMIT 1")
    suspend fun getSupplierByName(name: String): SupplierEntity?

    @Query("SELECT SUM(currentBalancePaise) FROM suppliers WHERE isArchived = 0 AND currentBalancePaise > 0")
    fun getTotalWeOweSuppliersPaise(): Flow<Long?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSupplier(supplier: SupplierEntity)

    @Update
    suspend fun updateSupplier(supplier: SupplierEntity)

    @Query("UPDATE suppliers SET currentBalancePaise = currentBalancePaise + :amountChangePaise WHERE id = :supplierId")
    suspend fun adjustBalance(supplierId: String, amountChangePaise: Long)

    @Query("UPDATE suppliers SET isArchived = 1 WHERE id = :supplierId")
    suspend fun archiveSupplier(supplierId: String)
}

@Dao
interface CustomerDao {
    @Query("SELECT * FROM customers WHERE isArchived = 0 ORDER BY name ASC")
    fun getAllCustomers(): Flow<List<CustomerEntity>>

    @Query("SELECT * FROM customers WHERE id = :id")
    suspend fun getCustomerById(id: String): CustomerEntity?

    @Query("SELECT SUM(currentBalancePaise) FROM customers WHERE isArchived = 0 AND currentBalancePaise > 0")
    fun getTotalCustomersOweUsPaise(): Flow<Long?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomer(customer: CustomerEntity)

    @Update
    suspend fun updateCustomer(customer: CustomerEntity)

    @Query("UPDATE customers SET currentBalancePaise = currentBalancePaise + :amountChangePaise WHERE id = :customerId")
    suspend fun adjustBalance(customerId: String, amountChangePaise: Long)

    @Query("UPDATE customers SET isArchived = 1 WHERE id = :customerId")
    suspend fun archiveCustomer(customerId: String)
}

@Dao
interface PurchaseDao {
    @Query("SELECT * FROM purchases ORDER BY timestamp DESC")
    fun getAllPurchases(): Flow<List<PurchaseEntity>>

    @Query("SELECT * FROM purchases WHERE id = :id")
    suspend fun getPurchaseById(id: String): PurchaseEntity?

    @Query("SELECT * FROM purchases WHERE supplierId = :supplierId ORDER BY timestamp DESC")
    fun getPurchasesForSupplier(supplierId: String): Flow<List<PurchaseEntity>>

    @Query("SELECT * FROM purchase_lines WHERE purchaseId = :purchaseId")
    suspend fun getLinesForPurchase(purchaseId: String): List<PurchaseLineEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPurchase(purchase: PurchaseEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPurchaseLines(lines: List<PurchaseLineEntity>)
}

@Dao
interface SaleDao {
    @Query("SELECT * FROM sales WHERE isVoided = 0 ORDER BY timestamp DESC")
    fun getAllSales(): Flow<List<SaleEntity>>

    @Query("SELECT * FROM sales ORDER BY timestamp DESC")
    fun getAllSalesWithVoided(): Flow<List<SaleEntity>>

    @Query("SELECT * FROM sales WHERE id = :id")
    suspend fun getSaleById(id: String): SaleEntity?

    @Query("SELECT * FROM sale_lines WHERE saleId = :saleId")
    suspend fun getLinesForSale(saleId: String): List<SaleLineEntity>

    @Query("SELECT * FROM sale_lines")
    fun getAllSaleLines(): Flow<List<SaleLineEntity>>

    @Query("SELECT * FROM sales WHERE isVoided = 0 AND timestamp >= :startTime AND timestamp <= :endTime ORDER BY timestamp DESC")
    fun getSalesByDateRange(startTime: Long, endTime: Long): Flow<List<SaleEntity>>

    @Query("SELECT SUM(netTotalPaise) FROM sales WHERE isVoided = 0 AND timestamp >= :startTime AND timestamp <= :endTime")
    fun getTotalSalesInRange(startTime: Long, endTime: Long): Flow<Long?>

    @Query("SELECT * FROM sales WHERE unresolvedStockDetails = 1 AND isVoided = 0 ORDER BY timestamp DESC")
    fun getUnresolvedStockSales(): Flow<List<SaleEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSale(sale: SaleEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSaleLines(lines: List<SaleLineEntity>)

    @Query("UPDATE sales SET isSummaryEntry = 0, unresolvedStockDetails = 0 WHERE id = :saleId")
    suspend fun markStockResolved(saleId: String)

    @Query("UPDATE sales SET isVoided = 1, voidReason = :reason, voidTimestamp = :timestamp, refundPayoutPaise = :refundPayout, stockRestoredCondition = :condition WHERE id = :saleId")
    suspend fun voidSale(saleId: String, reason: String, timestamp: Long, refundPayout: Long, condition: String)
}

@Dao
interface AccountPaymentDao {
    @Query("SELECT * FROM account_payments WHERE accountType = :accountType AND accountId = :accountId ORDER BY timestamp DESC")
    fun getPaymentsForAccount(accountType: String, accountId: String): Flow<List<AccountPaymentEntity>>

    @Query("SELECT * FROM account_payments ORDER BY timestamp DESC LIMIT 50")
    fun getRecentPayments(): Flow<List<AccountPaymentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(payment: AccountPaymentEntity)
}

@Dao
interface ShopExpenseDao {
    @Query("SELECT * FROM shop_expenses WHERE isVoided = 0 ORDER BY timestamp DESC")
    fun getAllExpenses(): Flow<List<ShopExpenseEntity>>

    @Query("SELECT * FROM shop_expenses WHERE isVoided = 0 AND timestamp >= :startTime AND timestamp <= :endTime ORDER BY timestamp DESC")
    fun getExpensesInRange(startTime: Long, endTime: Long): Flow<List<ShopExpenseEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: ShopExpenseEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpenses(expenses: List<ShopExpenseEntity>)

    @Query("UPDATE shop_expenses SET isVoided = 1, voidReason = :reason WHERE id = :expenseId")
    suspend fun voidExpense(expenseId: String, reason: String)
}

@Dao
interface ImportBatchDao {
    @Query("SELECT * FROM import_batches ORDER BY timestamp DESC")
    fun getAllBatches(): Flow<List<ImportBatchEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBatch(batch: ImportBatchEntity)
}

@Dao
interface ProductBarcodeDao {
    @Query("SELECT * FROM product_barcodes WHERE barcode = :barcode LIMIT 1")
    suspend fun findByBarcode(barcode: String): ProductBarcodeEntity?

    @Query("SELECT * FROM product_barcodes WHERE productId = :productId")
    suspend fun getBarcodesForProduct(productId: String): List<ProductBarcodeEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertBarcode(barcode: ProductBarcodeEntity)

    @Query("DELETE FROM product_barcodes WHERE id = :id")
    suspend fun deleteBarcode(id: String)
}

@Dao
interface HeldBillDao {
    @Query("SELECT * FROM held_bills ORDER BY createdAt DESC")
    fun getAllHeldBills(): Flow<List<HeldBillEntity>>

    @Query("SELECT * FROM held_bills WHERE id = :id")
    suspend fun getHeldBillById(id: String): HeldBillEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHeldBill(bill: HeldBillEntity)

    @Query("DELETE FROM held_bills WHERE id = :id")
    suspend fun deleteHeldBill(id: String)
}