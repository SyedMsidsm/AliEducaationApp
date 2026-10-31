package com.alieducational.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val urduName: String? = null,
    val category: String,
    val subcategory: String? = null,
    val brand: String? = null,
    val sku: String? = null,
    val barcode: String? = null, // primary piece barcode if assigned
    val isFavorite: Boolean = false, // quick tap on counter billing
    val photoUri: String? = null,
    val notes: String? = null,
    val baseUnit: String = "piece", // e.g. "piece", "packet"
    val packSize: Int = 1,          // e.g. 10 pieces per box
    val movingAvgCostPaise: Long = 0L,     // weighted-average cost in paise
    val sellingPricePiecePaise: Long = 0L, // selling price per piece
    val sellingPricePackPaise: Long = 0L,  // selling price per box/pack
    val wholesalePricePiecePaise: Long = 0L, // wholesale price per piece (optional)
    val currentSellableStock: Int = 0,     // current sellable quantity in base units
    val currentDamagedStock: Int = 0,      // current damaged quantity in base units
    val lowStockThreshold: Int = 5,        // alert threshold in base units
    val isArchived: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "product_barcodes",
    indices = [Index(value = ["barcode"], unique = true)]
)
data class ProductBarcodeEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val barcode: String, // String preserves leading zeros, never numeric
    val productId: String,
    val unitType: String = "PIECE", // PIECE, PACK
    val packQuantity: Int = 1,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "stock_movements")
data class StockMovementEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val productId: String,
    val type: String, // RECEIVE, SALE, SHOP_USE, GIVEAWAY, DAMAGE, RETURN, CORRECTION
    val quantityChangeBase: Int, // positive for addition, negative for deduction
    val countedTotalBase: Int? = null, // for CORRECTION
    val unitCostPaise: Long = 0L, // unit cost at time of movement
    val reason: String? = null,
    val referenceId: String? = null, // linked purchaseId or saleId
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "suppliers")
data class SupplierEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val phone: String? = null,
    val address: String? = null,
    val taxDetails: String? = null,
    val bankDetails: String? = null,
    val notes: String? = null,
    val currentBalancePaise: Long = 0L, // positive means shop owes supplier
    val isArchived: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "customers")
data class CustomerEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val phone: String? = null,
    val notes: String? = null,
    val currentBalancePaise: Long = 0L, // positive means customer owes shop
    val isArchived: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "purchases")
data class PurchaseEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val supplierId: String,
    val invoiceNumber: String? = null,
    val invoiceDate: Long = System.currentTimeMillis(),
    val totalGoodsValuePaise: Long,
    val deliveryChargePaise: Long = 0L,
    val deliveryPaidTo: String = "SUPPLIER", // SUPPLIER, CARRIER, SETTLED
    val amountPaidPaise: Long = 0L,
    val remainingPayablePaise: Long = 0L,
    val paymentMethod: String = "CASH", // CASH, UPI, BANK_TRANSFER, CHEQUE
    val notes: String? = null,
    val photoUri: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "purchase_lines")
data class PurchaseLineEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val purchaseId: String,
    val productId: String,
    val quantityBase: Int,
    val unitPurchasePricePaise: Long,
    val allocatedDeliveryPaise: Long = 0L,
    val effectiveUnitCostPaise: Long
)

@Entity(tableName = "sales")
data class SaleEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val receiptNumber: String,
    val customerId: String? = null, // null for anonymous cash sale
    val customerName: String? = null,
    val grossTotalPaise: Long,
    val discountType: String = "NONE", // NONE, PERCENT, FIXED
    val discountValuePaise: Long = 0L,
    val discountPercent: Double = 0.0,
    val netTotalPaise: Long,
    val amountReceivedPaise: Long,
    val changeGivenPaise: Long = 0L, // change returned to customer
    val remainingCreditPaise: Long = 0L,
    val paymentMethod: String = "CASH", // CASH, UPI, CARD, CREDIT
    val isSummaryEntry: Boolean = false, // true for money-only daily summaries
    val unresolvedStockDetails: Boolean = false,
    val notes: String? = null,
    val isVoided: Boolean = false,
    val voidReason: String? = null,
    val voidTimestamp: Long? = null,
    val refundPayoutPaise: Long = 0L,
    val stockRestoredCondition: String? = null, // SELLABLE, DAMAGED, NONE
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "sale_lines")
data class SaleLineEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val saleId: String,
    val productId: String?, // null for money-only daily summaries
    val productName: String,
    val quantityBase: Int,
    val isPack: Boolean = false,
    val unitSellingPricePaise: Long,
    val unitCostSnapshotPaise: Long?, // cost at time of sale; null if unknown
    val lineGrossPaise: Long,
    val lineDiscountAllocatedPaise: Long = 0L,
    val lineNetPaise: Long,
    val lineProfitPaise: Long? // null if cost unknown
)

@Entity(tableName = "account_payments")
data class AccountPaymentEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val accountType: String, // SUPPLIER or CUSTOMER
    val accountId: String,
    val amountPaise: Long,
    val paymentMethod: String = "CASH",
    val notes: String? = null,
    val receiptReference: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "shop_expenses")
data class ShopExpenseEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val category: String, // RENT, ELECTRICITY, SALARY, OTHER_EXPENSE, OTHER_INCOME
    val amountPaise: Long,
    val isIncome: Boolean = false,
    val notes: String? = null,
    val isVoided: Boolean = false,
    val voidReason: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "import_batches")
data class ImportBatchEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val fileName: String,
    val totalRows: Int,
    val importedCount: Int,
    val skippedCount: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "held_bills")
data class HeldBillEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val note: String? = null,
    val customerId: String? = null,
    val customerName: String? = null,
    val discountType: String = "NONE",
    val discountValuePaise: Long = 0L,
    val discountPercent: Double = 0.0,
    val draftJson: String, // serialized lines for recovery across app restart/lock
    val itemCount: Int = 0,
    val totalPaise: Long = 0L,
    val createdAt: Long = System.currentTimeMillis()
)