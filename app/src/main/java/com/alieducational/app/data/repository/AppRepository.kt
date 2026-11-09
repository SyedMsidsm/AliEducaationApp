package com.alieducational.app.data.repository

import androidx.room.withTransaction
import com.alieducational.app.data.local.AppDatabase
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
import com.alieducational.app.domain.calculation.CostCalculator
import com.alieducational.app.domain.calculation.PurchaseLineInput
import com.alieducational.app.domain.calculation.SaleLineInput
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

data class PurchaseItemParam(
    val productId: String,
    val quantityBase: Int,
    val unitPurchasePricePaise: Long,
    val newSellingPricePiecePaise: Long? = null,
    val lineId: String = UUID.randomUUID().toString()
)

data class SaleItemParam(
    val productId: String?,
    val productName: String,
    val quantityBase: Int,
    val isPack: Boolean = false,
    val unitSellingPricePaise: Long,
    val lineId: String = UUID.randomUUID().toString(),
    val lineType: String = "PRODUCT", // "PRODUCT", "SERVICE"
    val serviceId: String? = null,
    val serviceName: String? = null,
    val serviceOption: String? = null,
    val unitLabel: String? = null,
    val packSizeSnapshot: Int? = null
)

data class BarcodeLookupResult(
    val product: ProductEntity,
    val unitType: String, // "PIECE" or "PACK"
    val packQuantity: Int,
    val barcode: String
)

@Singleton
class AppRepository @Inject constructor(
    private val db: AppDatabase,
    private val productDao: ProductDao,
    private val productBarcodeDao: ProductBarcodeDao,
    private val stockMovementDao: StockMovementDao,
    private val supplierDao: SupplierDao,
    private val customerDao: CustomerDao,
    private val purchaseDao: PurchaseDao,
    private val saleDao: SaleDao,
    private val paymentDao: AccountPaymentDao,
    private val expenseDao: ShopExpenseDao,
    private val heldBillDao: HeldBillDao,
    private val pigmyDepositDao: PigmyDepositDao,
    private val pigmyWithdrawalDao: PigmyWithdrawalDao,
    private val pigmySchemeDao: PigmySchemeDao,
    private val cashCategoryDao: CashCategoryDao,
    private val productGiveawayDao: ProductGiveawayDao,
    private val serviceDefinitionDao: ServiceDefinitionDao,
    private val syncEventDao: SyncEventDao,
    private val barcodeGeneratorService: com.alieducational.app.data.service.BarcodeGeneratorService = com.alieducational.app.data.service.BarcodeGeneratorService(),
    private val preferences: com.alieducational.app.data.preferences.AppPreferences? = null
) {

    private suspend fun requireOwner() {
        require(preferences == null || preferences.deviceRole.first() == "OWNER") { "Owner authorization required" }
    }

    // ── Stock & Inventory ──────────────────────────────────────────────

    val allProducts: Flow<List<ProductEntity>> = productDao.getAllActiveProducts()
    val favoriteProducts: Flow<List<ProductEntity>> = productDao.getFavoriteProducts()
    val allCategories: Flow<List<String>> = productDao.getAllCategories()
    val lowStockProducts: Flow<List<ProductEntity>> = productDao.getLowStockProducts()
    val totalStockValuePaise: Flow<Long?> = productDao.getTotalStockValuePaise()
    val lowStockCount: Flow<Int> = productDao.getLowStockCount()
    val totalProductCount: Flow<Int> = productDao.getTotalProductCount()
    val recentStockMovements: Flow<List<StockMovementEntity>> = stockMovementDao.getRecentMovements()
    val allHeldBills: Flow<List<HeldBillEntity>> = heldBillDao.getAllHeldBills()

    fun searchProducts(query: String): Flow<List<ProductEntity>> = productDao.searchProducts(query)
    fun getProductsByCategory(category: String): Flow<List<ProductEntity>> = productDao.getProductsByCategory(category)
    suspend fun setFavorite(productId: String, isFav: Boolean) = productDao.setFavorite(productId, isFav)

    // ── Barcode Resolution & Linking ───────────────────────────────────

    suspend fun lookupBarcode(rawBarcode: String): BarcodeLookupResult? {
        val code = rawBarcode.trim()
        if (code.isEmpty()) return null

        // 1. Check mapped barcodes table (supports distinct piece and pack barcodes)
        val mapping = productBarcodeDao.findByBarcode(code)
        if (mapping != null) {
            val prod = productDao.getProductById(mapping.productId)
            if (prod != null && !prod.isArchived) {
                return BarcodeLookupResult(
                    product = prod,
                    unitType = mapping.unitType,
                    packQuantity = mapping.packQuantity,
                    barcode = code
                )
            }
        }

        // 2. Direct fallback to product's primary barcode or SKU
        val directProd = productDao.getProductByBarcode(code) ?: productDao.getProductBySku(code)
        if (directProd != null) {
            return BarcodeLookupResult(
                product = directProd,
                unitType = "PIECE",
                packQuantity = 1,
                barcode = code
            )
        }

        return null
    }

    suspend fun linkBarcode(barcode: String, productId: String, unitType: String = "PIECE", packQty: Int = 1) {
        val code = barcode.trim()
        productBarcodeDao.insertBarcode(
            ProductBarcodeEntity(
                barcode = code,
                productId = productId,
                unitType = unitType,
                packQuantity = packQty
            )
        )
        if (unitType == "PIECE") {
            val p = productDao.getProductById(productId)
            if (p != null && p.barcode.isNullOrBlank()) {
                productDao.updateProduct(p.copy(barcode = code))
            }
        }
    }

    // ── Held Bills (Draft parking) ─────────────────────────────────────

    suspend fun holdBill(bill: HeldBillEntity) = heldBillDao.insertHeldBill(bill)
    suspend fun deleteHeldBill(id: String) = heldBillDao.deleteHeldBill(id)
    suspend fun deleteHeldBills(ids: List<String>) = heldBillDao.deleteHeldBills(ids)
    suspend fun getHeldBillById(id: String): HeldBillEntity? = heldBillDao.getHeldBillById(id)
    suspend fun getExpenseById(id: String): ShopExpenseEntity? = expenseDao.getExpenseById(id)

    val allSaleLines: Flow<List<SaleLineEntity>> = saleDao.getAllSaleLines()

    suspend fun addOrUpdateProductWithStock(
        existingId: String?,
        name: String,
        barcode: String?,
        category: String,
        brand: String?,
        packSize: Int,
        boxesBrought: Int,
        loosePiecesBrought: Int,
        costPerPiecePaise: Long,
        sellingPricePiecePaise: Long,
        wholesalePricePiecePaise: Long,
        lowStockThreshold: Int,
        supplierName: String?,
        billNo: String? = null,
        transportChargesPaise: Long = 0L,
        isPaidNow: Boolean = false,
        paymentMethod: String = "CASH"
    ): ProductEntity = db.withTransaction {
        requireOwner()
        require(packSize > 0 && boxesBrought >= 0 && loosePiecesBrought >= 0 && costPerPiecePaise >= 0 && transportChargesPaise >= 0 && sellingPricePiecePaise >= 0) { "Invalid product quantity or price" }
        val qtyBrought = Math.addExact(Math.multiplyExact(boxesBrought, packSize), loosePiecesBrought)

        // Calculate landed cost including allocated transportation
        val transportPerPiecePaise = if (qtyBrought > 0 && transportChargesPaise > 0L) {
            transportChargesPaise / qtyBrought
        } else 0L
        val effectiveLandedCostPaise = if (qtyBrought > 0) CostCalculator.calculateMovingWeightedAverage(0, 0, qtyBrought, Math.addExact(Math.multiplyExact(qtyBrought.toLong(), costPerPiecePaise), transportChargesPaise)) else costPerPiecePaise

        // 1. Resolve or create supplier if provided
        var supplierId: String? = null
        val cleanSupplierName = supplierName?.trim()
        if (!cleanSupplierName.isNullOrBlank()) {
            val existingSupplier = supplierDao.getSupplierByName(cleanSupplierName)
            if (existingSupplier != null) {
                supplierId = existingSupplier.id
            } else {
                val newSupplier = SupplierEntity(name = cleanSupplierName)
                supplierDao.insertSupplier(newSupplier)
                supplierId = newSupplier.id
            }
        }

        // 2. Resolve existing product or create new
        val existingProduct = if (!existingId.isNullOrBlank()) {
            productDao.getProductById(existingId)
        } else if (!barcode.isNullOrBlank()) {
            productDao.getProductByBarcode(barcode)
        } else {
            productDao.getProductByName(name.trim())
        }

        val targetProduct: ProductEntity
        if (existingProduct != null) {
            val incomingTotalLandedCostPaise = Math.addExact(Math.multiplyExact(qtyBrought.toLong(), costPerPiecePaise), transportChargesPaise)
            val newAvgCost = if (qtyBrought > 0) {
                CostCalculator.calculateMovingWeightedAverage(
                    existingStockQuantity = existingProduct.currentSellableStock,
                    existingAverageCostPaise = existingProduct.movingAvgCostPaise,
                    incomingQuantity = qtyBrought,
                    incomingTotalLandedCostPaise = incomingTotalLandedCostPaise
                )
            } else {
                if (effectiveLandedCostPaise > 0L) effectiveLandedCostPaise else existingProduct.movingAvgCostPaise
            }

            targetProduct = existingProduct.copy(
                name = name.trim(),
                barcode = barcode?.trim()?.ifBlank { null } ?: existingProduct.barcode,
                category = category.trim().ifBlank { "Stationery" },
                brand = brand?.trim()?.ifBlank { null },
                packSize = packSize,
                movingAvgCostPaise = newAvgCost,
                sellingPricePiecePaise = sellingPricePiecePaise,
                sellingPricePackPaise = Math.multiplyExact(sellingPricePiecePaise, packSize.toLong()),
                wholesalePricePiecePaise = wholesalePricePiecePaise,
                lowStockThreshold = lowStockThreshold,
                currentSellableStock = Math.addExact(existingProduct.currentSellableStock, qtyBrought),
                updatedAt = System.currentTimeMillis()
            )
            productDao.updateProduct(targetProduct)
        } else {
            targetProduct = ProductEntity(
                name = name.trim(),
                barcode = barcode?.trim()?.ifBlank { null },
                category = category.trim().ifBlank { "Stationery" },
                brand = brand?.trim()?.ifBlank { null },
                packSize = packSize,
                movingAvgCostPaise = effectiveLandedCostPaise,
                sellingPricePiecePaise = sellingPricePiecePaise,
                sellingPricePackPaise = Math.multiplyExact(sellingPricePiecePaise, packSize.toLong()),
                wholesalePricePiecePaise = wholesalePricePiecePaise,
                lowStockThreshold = lowStockThreshold,
                currentSellableStock = qtyBrought
            )
            productDao.insertProduct(targetProduct)
        }

        // 3. Register barcode in product_barcodes table if provided
        val cleanBarcode = barcode?.trim()
        if (!cleanBarcode.isNullOrBlank()) {
            val existingBarcodeMapping = productBarcodeDao.findByBarcode(cleanBarcode)
            if (existingBarcodeMapping == null) {
                productBarcodeDao.insertBarcode(
                    ProductBarcodeEntity(
                        barcode = cleanBarcode,
                        productId = targetProduct.id,
                        unitType = "PIECE",
                        packQuantity = 1
                    )
                )
            }
        }

        // 4. Record stock movement if qtyBrought > 0
        if (qtyBrought > 0) {
            val movementType = if (supplierId != null) "RECEIVE" else (if (existingProduct != null) "RECEIVE" else "OPENING_STOCK")
            val cleanBillNo = billNo?.trim()
            val reason = when {
                !cleanBillNo.isNullOrBlank() && transportChargesPaise > 0L ->
                    "Bill #$cleanBillNo (incl. ${CostCalculator.formatPaiseToRupees(transportChargesPaise)} transport)"
                !cleanBillNo.isNullOrBlank() -> "Bill #$cleanBillNo"
                !cleanSupplierName.isNullOrBlank() -> "Received from $cleanSupplierName"
                existingProduct != null -> "Stock Restocked"
                else -> "Opening Stock"
            }

            stockMovementDao.insertMovement(
                StockMovementEntity(
                    productId = targetProduct.id,
                    type = movementType,
                    quantityChangeBase = qtyBrought,
                    unitCostPaise = effectiveLandedCostPaise,
                    reason = reason,
                    referenceId = supplierId
                )
            )
        }

        // 5. Record supplier bill / purchase & handle payment
        if (supplierId != null && qtyBrought > 0) {
            val totalGoodsCostPaise = qtyBrought.toLong() * costPerPiecePaise
            val totalBillAmountPaise = totalGoodsCostPaise + transportChargesPaise

            if (totalBillAmountPaise > 0L) {
                if (isPaidNow) {
                    val purchase = PurchaseEntity(
                            supplierId = supplierId,
                            invoiceNumber = billNo?.trim()?.ifBlank { null },
                            totalGoodsValuePaise = totalGoodsCostPaise,
                            deliveryChargePaise = transportChargesPaise,
                            amountPaidPaise = totalBillAmountPaise,
                            remainingPayablePaise = 0L,
                            paymentMethod = paymentMethod,
                            notes = "Bill #${billNo?.trim() ?: "N/A"}: ${targetProduct.name} ($qtyBrought pcs) - Paid ($paymentMethod)"
                        )
                    purchaseDao.insertPurchase(purchase)
                    purchaseDao.insertPurchaseLines(listOf(PurchaseLineEntity(purchaseId = purchase.id, productId = targetProduct.id,
                        quantityBase = qtyBrought, unitPurchasePricePaise = costPerPiecePaise, allocatedDeliveryPaise = transportChargesPaise, effectiveUnitCostPaise = effectiveLandedCostPaise)))
                    paymentDao.insertPayment(
                        AccountPaymentEntity(
                            accountType = "SUPPLIER",
                            accountId = supplierId,
                            amountPaise = totalBillAmountPaise,
                            paymentMethod = paymentMethod,
                            notes = "Paid Bill #${billNo?.trim() ?: "N/A"} for ${targetProduct.name} ($qtyBrought pcs)",
                            receiptReference = billNo?.trim()?.ifBlank { null }
                        )
                    )
                } else {
                    supplierDao.adjustBalance(supplierId, totalBillAmountPaise)
                    val purchase = PurchaseEntity(
                            supplierId = supplierId,
                            invoiceNumber = billNo?.trim()?.ifBlank { null },
                            totalGoodsValuePaise = totalGoodsCostPaise,
                            deliveryChargePaise = transportChargesPaise,
                            amountPaidPaise = 0L,
                            remainingPayablePaise = totalBillAmountPaise,
                            paymentMethod = paymentMethod,
                            notes = "Bill #${billNo?.trim() ?: "N/A"}: ${targetProduct.name} ($qtyBrought pcs) - Unpaid Credit"
                        )
                    purchaseDao.insertPurchase(purchase)
                    purchaseDao.insertPurchaseLines(listOf(PurchaseLineEntity(purchaseId = purchase.id, productId = targetProduct.id,
                        quantityBase = qtyBrought, unitPurchasePricePaise = costPerPiecePaise, allocatedDeliveryPaise = transportChargesPaise, effectiveUnitCostPaise = effectiveLandedCostPaise)))
                }
            }
        }

        targetProduct
    }

    suspend fun getProductById(id: String): ProductEntity? = productDao.getProductById(id)

    suspend fun addProduct(product: ProductEntity) {
        requireOwner()
        db.withTransaction {
            productDao.insertProduct(product)
            if (product.currentSellableStock > 0) {
                stockMovementDao.insertMovement(
                    StockMovementEntity(
                        productId = product.id,
                        type = "OPENING_STOCK",
                        quantityChangeBase = product.currentSellableStock,
                        unitCostPaise = product.movingAvgCostPaise,
                        reason = "Opening Stock"
                    )
                )
            }
        }
    }

    suspend fun updateProduct(product: ProductEntity) {
        requireOwner()
        productDao.updateProduct(product)
    }

    suspend fun recordStockAdjustment(
        productId: String,
        type: String, // SHOP_USE, GIVEAWAY, DAMAGE, RETURN, CORRECTION
        quantityChange: Int,
        countedTotal: Int? = null,
        reason: String?
    ) {
        requireOwner()
        db.withTransaction {
            val product = productDao.getProductById(productId) ?: return@withTransaction
            val actualChange = if (type == "CORRECTION" && countedTotal != null) {
                countedTotal - product.currentSellableStock
            } else {
                quantityChange
            }

            if (type == "DAMAGE") {
                productDao.moveToDamagedStock(productId, kotlin.math.abs(actualChange))
            } else {
                productDao.adjustSellableStock(productId, actualChange)
            }

            stockMovementDao.insertMovement(
                StockMovementEntity(
                    productId = productId,
                    type = type,
                    quantityChangeBase = actualChange,
                    countedTotalBase = countedTotal,
                    unitCostPaise = product.movingAvgCostPaise,
                    reason = reason
                )
            )
        }
    }

    // ── Purchases & Goods Received ─────────────────────────────────────

    val allSuppliers: Flow<List<SupplierEntity>> = supplierDao.getAllSuppliers()
    val totalWeOweSuppliersPaise: Flow<Long?> = supplierDao.getTotalWeOweSuppliersPaise()
    val distinctCreditorsCount: Flow<Int> = supplierDao.getDistinctCreditorsCount()
    val allPurchases: Flow<List<PurchaseEntity>> = purchaseDao.getAllPurchases()

    suspend fun addSupplier(supplier: SupplierEntity) {
        requireOwner()
        supplierDao.insertSupplier(supplier)
    }

    suspend fun getSupplierById(id: String): SupplierEntity? = supplierDao.getSupplierById(id)

    suspend fun receiveGoods(
        supplierId: String,
        invoiceNumber: String?,
        items: List<PurchaseItemParam>,
        totalDeliveryPaise: Long,
        deliveryPaidTo: String, // SUPPLIER, CARRIER, SETTLED
        amountPaidPaise: Long,
        paymentMethod: String,
        notes: String?
    ): String = db.withTransaction {
        requireOwner()
        require(supplierDao.getSupplierById(supplierId)?.isArchived == false) { "Select an active supplier" }
        require(items.isNotEmpty() && items.all { it.quantityBase > 0 && it.unitPurchasePricePaise >= 0 }) { "Invalid purchase items" }
        require(items.map { it.lineId }.distinct().size == items.size) { "Duplicate purchase line" }
        items.forEach { require(productDao.getProductById(it.productId)?.isArchived == false) { "Unknown product" } }
        require(totalDeliveryPaise >= 0 && amountPaidPaise >= 0) { "Invalid payment or transport" }
        val billed = items.fold(0L) { total, item -> Math.addExact(total, Math.multiplyExact(item.quantityBase.toLong(), item.unitPurchasePricePaise)) }
        require(amountPaidPaise <= Math.addExact(billed, if (deliveryPaidTo == "SUPPLIER") totalDeliveryPaise else 0)) { "Payment exceeds invoice" }
        val purchaseId = UUID.randomUUID().toString()
        val lineInputs = items.map {
            PurchaseLineInput(
                id = it.lineId,
                quantity = it.quantityBase,
                unitPricePaise = it.unitPurchasePricePaise
            )
        }

        val allocatedDelivery = CostCalculator.allocateDelivery(lineInputs, totalDeliveryPaise)
        val totalGoodsValuePaise = items.sumOf { it.quantityBase.toLong() * it.unitPurchasePricePaise }

        val purchaseLines = items.map { item ->
            val alloc = allocatedDelivery.first { it.lineId == item.lineId }
            PurchaseLineEntity(
                purchaseId = purchaseId,
                productId = item.productId,
                quantityBase = item.quantityBase,
                unitPurchasePricePaise = item.unitPurchasePricePaise,
                allocatedDeliveryPaise = alloc.allocatedDeliveryPaise,
                effectiveUnitCostPaise = alloc.effectiveUnitCostPaise
            )
        }

        // Update product stock and moving weighted-average cost
        items.forEach { item ->
            val product = productDao.getProductById(item.productId) ?: return@forEach
            val alloc = allocatedDelivery.first { it.lineId == item.lineId }

            val newAvgCost = CostCalculator.calculateMovingWeightedAverage(
                existingStockQuantity = product.currentSellableStock,
                existingAverageCostPaise = product.movingAvgCostPaise,
                incomingQuantity = item.quantityBase,
                incomingTotalLandedCostPaise = alloc.totalLandedCostPaise
            )

            productDao.updateWeightedAverageCost(item.productId, newAvgCost)
            productDao.adjustSellableStock(item.productId, item.quantityBase)

            if (item.newSellingPricePiecePaise != null && item.newSellingPricePiecePaise > 0L) {
                val updatedProd = (productDao.getProductById(item.productId) ?: error("Product disappeared")).copy(
                    sellingPricePiecePaise = item.newSellingPricePiecePaise,
                    sellingPricePackPaise = item.newSellingPricePiecePaise * product.packSize,
                    updatedAt = System.currentTimeMillis()
                )
                productDao.updateProduct(updatedProd)
            }

            stockMovementDao.insertMovement(
                StockMovementEntity(
                    productId = item.productId,
                    type = "RECEIVE",
                    quantityChangeBase = item.quantityBase,
                    unitCostPaise = alloc.effectiveUnitCostPaise,
                    reason = if (!invoiceNumber.isNullOrBlank()) "Invoice $invoiceNumber" else "Purchase received",
                    referenceId = purchaseId
                )
            )
        }

        purchaseDao.insertPurchaseLines(purchaseLines)

        // Calculate liability to supplier
        val deliveryOwedToSupplier = if (deliveryPaidTo == "SUPPLIER") totalDeliveryPaise else 0L
        val totalBilledBySupplier = totalGoodsValuePaise + deliveryOwedToSupplier
        val remainingPayablePaise = totalBilledBySupplier - amountPaidPaise

        val purchase = PurchaseEntity(
            id = purchaseId,
            supplierId = supplierId,
            invoiceNumber = invoiceNumber,
            totalGoodsValuePaise = totalGoodsValuePaise,
            deliveryChargePaise = totalDeliveryPaise,
            deliveryPaidTo = deliveryPaidTo,
            amountPaidPaise = amountPaidPaise,
            remainingPayablePaise = remainingPayablePaise,
            paymentMethod = paymentMethod,
            notes = notes
        )
        purchaseDao.insertPurchase(purchase)

        // Adjust supplier running balance: + billed - paid = + remainingPayable
        supplierDao.adjustBalance(supplierId, remainingPayablePaise)

        if (amountPaidPaise > 0) {
            paymentDao.insertPayment(
                AccountPaymentEntity(
                    accountType = "SUPPLIER",
                    accountId = supplierId,
                    amountPaise = amountPaidPaise,
                    paymentMethod = paymentMethod,
                    notes = "Payment for purchase invoice ${invoiceNumber ?: ""}",
                    receiptReference = invoiceNumber
                )
            )
        }

        return@withTransaction purchaseId
    }

    // ── Customers & Credit ─────────────────────────────────────────────

    val allCustomers: Flow<List<CustomerEntity>> = customerDao.getAllCustomers()
    val totalCustomersOweUsPaise: Flow<Long?> = customerDao.getTotalCustomersOweUsPaise()
    val distinctDebtorsCount: Flow<Int> = customerDao.getDistinctDebtorsCount()

    suspend fun addCustomer(customer: CustomerEntity) {
        customerDao.insertCustomer(customer)
    }

    suspend fun getCustomerById(id: String): CustomerEntity? = customerDao.getCustomerById(id)

    suspend fun recordAccountPayment(
        accountType: String, // SUPPLIER or CUSTOMER
        accountId: String,
        amountPaise: Long,
        paymentMethod: String,
        notes: String?,
        receiptRef: String?
    ) = db.withTransaction {
        requireOwner()
        require(accountType in setOf("CUSTOMER", "SUPPLIER") && amountPaise > 0) { "Invalid payment" }
        val balance = if (accountType == "CUSTOMER") {
            val account = customerDao.getCustomerById(accountId) ?: error("Customer missing")
            require(!account.isArchived) { "Account archived" }
            account.currentBalancePaise
        } else {
            val account = supplierDao.getSupplierById(accountId) ?: error("Supplier missing")
            require(!account.isArchived) { "Account archived" }
            account.currentBalancePaise
        }
        require(amountPaise <= balance) { "Payment exceeds outstanding balance" }
        val payment = AccountPaymentEntity(accountType = accountType, accountId = accountId,
            amountPaise = amountPaise, paymentMethod = paymentMethod, notes = notes, receiptReference = receiptRef)
        var left = amountPaise
        val allocations = org.json.JSONArray()
        if (accountType == "CUSTOMER") {
            val bills = saleDao.getSalesForCustomer(accountId).first().filter { !it.isVoided && it.remainingCreditPaise > 0 }
                .sortedWith(compareBy<SaleEntity> { if (it.receiptNumber == receiptRef) 0 else 1 }.thenBy { it.timestamp })
            for (bill in bills) {
                if (left == 0L) break
                val allocated = minOf(left, bill.remainingCreditPaise)
                saleDao.updateSale(bill.copy(amountReceivedPaise = Math.addExact(bill.amountReceivedPaise, allocated), remainingCreditPaise = bill.remainingCreditPaise - allocated))
                allocations.put(org.json.JSONObject().put("saleId", bill.id).put("amountPaise", allocated))
                left -= allocated
            }
            customerDao.adjustBalance(accountId, -amountPaise)
        } else {
            val invoices = purchaseDao.getPurchasesForSupplier(accountId).first().filter { it.remainingPayablePaise > 0 }.sortedBy { it.timestamp }
            for (invoice in invoices) {
                if (left == 0L) break
                val allocated = minOf(left, invoice.remainingPayablePaise)
                purchaseDao.updatePurchase(invoice.copy(amountPaidPaise = Math.addExact(invoice.amountPaidPaise, allocated), remainingPayablePaise = invoice.remainingPayablePaise - allocated))
                allocations.put(org.json.JSONObject().put("purchaseId", invoice.id).put("amountPaise", allocated))
                left -= allocated
            }
            supplierDao.adjustBalance(accountId, -amountPaise)
        }
        paymentDao.insertPayment(payment)
        syncEventDao.insertEvent(SyncEventEntity(eventType = "PAYMENT_ALLOCATED", entityId = payment.id,
            payloadJson = org.json.JSONObject().put("allocations", allocations).put("openingBalancePaymentPaise", left).toString(),
            originDeviceId = "OWNER", status = "ACKNOWLEDGED"))
    }

    fun getPaymentsForAccount(accountType: String, accountId: String): Flow<List<AccountPaymentEntity>> =
        paymentDao.getPaymentsForAccount(accountType, accountId)

    // ── Sales, Discounts & Profit ──────────────────────────────────────

    val allSales: Flow<List<SaleEntity>> = saleDao.getAllSalesWithVoided()
    val unresolvedStockSales: Flow<List<SaleEntity>> = saleDao.getUnresolvedStockSales()

    suspend fun recordSale(
        customerId: String?,
        customerName: String?,
        items: List<SaleItemParam>,
        discountType: String, // NONE, PERCENT, FIXED
        discountInput: Double, // percent value e.g. 10.0 or fixed rupee value e.g. 50.0
        amountReceivedPaise: Long,
        paymentMethod: String,
        notes: String?,
        originDeviceId: String? = null
    ): SaleEntity = db.withTransaction {
        require(items.isNotEmpty()) { "Empty bill" }
        require(discountType in setOf("NONE", "FIXED", "PERCENT")) { "Invalid discount type" }
        require(items.all { it.lineType in setOf("PRODUCT", "SERVICE") }) { "Invalid sale line type" }
        require(discountInput.isFinite() && discountInput >= 0 && (discountType != "PERCENT" || discountInput <= 100)) { "Invalid discount" }
        require(amountReceivedPaise >= 0) { "Invalid received amount" }
        require(items.all { it.quantityBase > 0 && it.unitSellingPricePaise >= 0 }) { "Invalid quantity or price" }
        require(items.map { it.lineId }.distinct().size == items.size) { "Duplicate sale line" }
        require(items.none { saleDao.getLineById(it.lineId) != null }) { "This bill has already been posted; open Bill History" }
        if (customerId != null) require(customerDao.getCustomerById(customerId)?.isArchived == false) { "Unknown customer" }
        val products = items.filter { it.lineType == "PRODUCT" }.groupBy { it.productId }
        products.forEach { (id, group) ->
            require(id != null) { "Product required" }
            val product = productDao.getProductById(id) ?: error("Unknown product")
            require(!product.isArchived && group.sumOf { it.quantityBase.toLong() } <= product.currentSellableStock.toLong()) { "Insufficient stock for ${product.name}" }
        }
        val unitPrices = items.associate { item ->
            val pack = if (item.isPack) item.packSizeSnapshot ?: productDao.getProductById(item.productId ?: "")?.packSize ?: 1 else 1
            require(pack > 0 && item.quantityBase % pack == 0) { "Invalid pack quantity" }
            // Sale lines persist base-unit pricing. Exact line totals are calculated separately below.
            item.lineId to (item.unitSellingPricePaise / pack)
        }
        val grossValues = items.associate { item ->
            val pack = if (item.isPack) item.packSizeSnapshot ?: productDao.getProductById(item.productId ?: "")?.packSize ?: 1 else 1
            item.lineId to Math.multiplyExact((item.quantityBase / pack).toLong(), item.unitSellingPricePaise)
        }
        val saleId = UUID.randomUUID().toString()
        val dateStr = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
        val randomSuffix = UUID.randomUUID().toString().take(12)
        val devicePrefix = if (originDeviceId != null && originDeviceId != "device-main") "AE-B1" else "AE-M"
        val receiptNumber = "$devicePrefix-$dateStr-$randomSuffix"

        val grossTotalPaise = grossValues.values.fold(0L) { a, b -> Math.addExact(a, b) }

        val discountValuePaise: Long = when (discountType) {
            "PERCENT" -> ((grossTotalPaise * discountInput) / 100.0).toLong()
            "FIXED" -> (discountInput * 100.0).toLong().coerceAtMost(grossTotalPaise)
            else -> 0L
        }

        val discountPercent: Double = if (grossTotalPaise > 0) {
            (discountValuePaise.toDouble() / grossTotalPaise.toDouble()) * 100.0
        } else 0.0

        val netTotalPaise = grossTotalPaise - discountValuePaise
        val remainingCreditPaise = (netTotalPaise - amountReceivedPaise).coerceAtLeast(0L)
        require(remainingCreditPaise == 0L || customerId != null) { "Customer is required for unpaid balance" }
        val changeGivenPaise = (amountReceivedPaise - netTotalPaise).coerceAtLeast(0L)

        // Allocate whole-sale discount proportionally across items using lineId (not productId)
        val saleLineInputs = items.map { item ->
            val costSnapshot = if (item.lineType == "PRODUCT" && item.productId != null) {
                val product = productDao.getProductById(item.productId)
                product?.movingAvgCostPaise?.takeIf { it > 0 }
            } else null

            SaleLineInput(
                id = item.lineId,
                quantity = 1,
                unitSellingPricePaise = grossValues.getValue(item.lineId),
                unitCostPaise = costSnapshot?.let { Math.multiplyExact(it, item.quantityBase.toLong()) }
            )
        }

        val allocatedDiscountLines = CostCalculator.allocateWholeSaleDiscount(saleLineInputs, discountValuePaise)

        val saleLines = items.map { item ->
            val alloc = allocatedDiscountLines.first { it.lineId == item.lineId }
            val isProduct = item.lineType == "PRODUCT" && item.productId != null
            val costSnapshot = if (isProduct) {
                val product = productDao.getProductById(item.productId!!)
                product?.movingAvgCostPaise?.takeIf { it > 0 }
            } else null

            SaleLineEntity(
                id = item.lineId,
                saleId = saleId,
                productId = item.productId,
                productName = item.productName,
                quantityBase = item.quantityBase,
                isPack = item.isPack,
                unitSellingPricePaise = unitPrices.getValue(item.lineId),
                unitCostSnapshotPaise = costSnapshot,
                lineGrossPaise = alloc.grossValuePaise,
                lineDiscountAllocatedPaise = alloc.allocatedDiscountPaise,
                lineNetPaise = alloc.netValuePaise,
                lineProfitPaise = if (isProduct) alloc.actualProfitPaise else null,
                lineType = item.lineType,
                serviceId = item.serviceId,
                serviceName = item.serviceName,
                serviceOption = item.serviceOption,
                unitLabel = if (item.isPack) "pack:${item.packSizeSnapshot ?: productDao.getProductById(item.productId ?: "")?.packSize ?: 1}" else item.unitLabel
            )
        }

        saleDao.insertSaleLines(saleLines)

        // Deduct stock ONLY for products. Services DO NOT deduct product inventory.
        items.filter { it.lineType == "PRODUCT" && it.productId != null }.forEach { item ->
            val pId = item.productId!!
            productDao.adjustSellableStock(pId, -item.quantityBase)
            val product = productDao.getProductById(pId)
            stockMovementDao.insertMovement(
                StockMovementEntity(
                    productId = pId,
                    type = "SALE",
                    quantityChangeBase = -item.quantityBase,
                    unitCostPaise = product?.movingAvgCostPaise ?: 0L,
                    reason = "Sale $receiptNumber",
                    referenceId = saleId
                )
            )
        }

        val sale = SaleEntity(
            id = saleId,
            receiptNumber = receiptNumber,
            customerId = customerId,
            customerName = customerName,
            grossTotalPaise = grossTotalPaise,
            discountType = discountType,
            discountValuePaise = discountValuePaise,
            discountPercent = discountPercent,
            netTotalPaise = netTotalPaise,
            amountReceivedPaise = amountReceivedPaise,
            changeGivenPaise = changeGivenPaise,
            remainingCreditPaise = remainingCreditPaise,
            paymentMethod = paymentMethod,
            isSummaryEntry = false,
            unresolvedStockDetails = saleLines.any { it.lineType == "PRODUCT" && it.unitCostSnapshotPaise == null },
            notes = notes,
            originDeviceId = originDeviceId,
            syncStatus = if (preferences?.deviceRole?.first() == "BILLING") "PENDING_SYNC" else "LOCAL"
        )
        saleDao.insertSale(sale)
        if (sale.syncStatus == "PENDING_SYNC") {
            items.filter { it.productId != null }.forEach { item ->
                val quote = syncEventDao.getEventByEntityId("catalog-quote-${item.productId}")?.payloadJson ?: ""
                syncEventDao.insertEvent(SyncEventEntity(id = "sale-quote-${item.lineId}", eventType = "SALE_QUOTE", entityId = "sale-quote-${item.lineId}", payloadJson = quote, originDeviceId = originDeviceId ?: "BILLING", status = "ACKNOWLEDGED"))
            }
        }

        // If customer owed credit, update customer debt
        if (customerId != null && remainingCreditPaise > 0) {
            customerDao.adjustBalance(customerId, remainingCreditPaise)
        }

        sale
    }

    // ── Cash In/Out Categories ──────────────────────────────────────────

    val allCashCategories: Flow<List<CashCategoryEntity>> = cashCategoryDao.getAllCategories()
    val activeCashCategories: Flow<List<CashCategoryEntity>> = cashCategoryDao.getActiveCategories()

    suspend fun ensureDefaultCashCategories() {
        if (cashCategoryDao.getCategoryCount() == 0) {
            val now = System.currentTimeMillis()
            val defaults = listOf(
                CashCategoryEntity(id = "cat_general", name = "General", isSystem = true, isArchived = false, createdAt = now, updatedAt = now),
                CashCategoryEntity(id = "cat_pigmy", name = "Pigmy", isSystem = true, isArchived = false, createdAt = now + 1, updatedAt = now + 1),
                CashCategoryEntity(id = "cat_rent", name = "Rent", isSystem = false, isArchived = false, createdAt = now + 2, updatedAt = now + 2),
                CashCategoryEntity(id = "cat_electricity", name = "Electricity", isSystem = false, isArchived = false, createdAt = now + 3, updatedAt = now + 3),
                CashCategoryEntity(id = "cat_transport", name = "Transport", isSystem = false, isArchived = false, createdAt = now + 4, updatedAt = now + 4),
                CashCategoryEntity(id = "cat_salary", name = "Salary", isSystem = false, isArchived = false, createdAt = now + 5, updatedAt = now + 5),
                CashCategoryEntity(id = "cat_tea_snacks", name = "Tea & Snacks", isSystem = false, isArchived = false, createdAt = now + 6, updatedAt = now + 6)
            )
            cashCategoryDao.insertCategories(defaults)
        }
    }

    suspend fun createCashCategory(name: String): String {
        requireOwner()
        val trimmed = name.trim()
        require(trimmed.isNotBlank()) { "Category name cannot be blank" }
        val existing = cashCategoryDao.getCategoryByName(trimmed)
        if (existing != null) {
            if (existing.isArchived) {
                cashCategoryDao.setArchived(existing.id, false)
            }
            return existing.id
        }
        val id = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        val cat = CashCategoryEntity(id = id, name = trimmed, isSystem = false, isArchived = false, createdAt = now, updatedAt = now)
        cashCategoryDao.insertCategory(cat)
        return id
    }

    suspend fun renameCashCategory(id: String, newName: String) {
        requireOwner()
        val trimmed = newName.trim()
        require(trimmed.isNotBlank()) { "Category name cannot be blank" }
        cashCategoryDao.renameCategory(id, trimmed)
    }

    suspend fun setCashCategoryArchived(id: String, isArchived: Boolean) {
        requireOwner()
        cashCategoryDao.setArchived(id, isArchived)
    }

    // ── Pigmy Daily Savings Tracker ─────────────────────────────────────

    val allPigmySchemes: Flow<List<PigmySchemeEntity>> = pigmySchemeDao.getAllSchemes()
    val activePigmySchemes: Flow<List<PigmySchemeEntity>> = pigmySchemeDao.getActiveSchemes()

    val allPigmyDeposits: Flow<List<PigmyDepositEntity>> = pigmyDepositDao.getAllDeposits()
    val activePigmyDeposits: Flow<List<PigmyDepositEntity>> = pigmyDepositDao.getActiveDeposits()
    val totalPigmyDepositsPaise: Flow<Long?> = pigmyDepositDao.getTotalRecordedDepositsPaise()

    fun getPigmyDepositsInRange(startTime: Long, endTime: Long): Flow<List<PigmyDepositEntity>> =
        pigmyDepositDao.getDepositsInRange(startTime, endTime)

    fun getPigmyDepositsForScheme(schemeId: String): Flow<List<PigmyDepositEntity>> =
        pigmyDepositDao.getDepositsForScheme(schemeId)

    fun getTotalPigmyDepositsForScheme(schemeId: String): Flow<Long?> =
        pigmyDepositDao.getTotalForScheme(schemeId)

    suspend fun ensureDefaultPigmySchemes() {
        if (pigmySchemeDao.getSchemeCount() == 0) {
            val now = System.currentTimeMillis()
            val defaults = listOf(
                PigmySchemeEntity(
                    id = "DEFAULT_PIGMY",
                    name = "Pigmy Scheme 1",
                    usualDailyAmountPaise = 10000L,
                    isArchived = false,
                    createdAt = now,
                    updatedAt = now
                ),
                PigmySchemeEntity(
                    id = "scheme_2",
                    name = "Pigmy Scheme 2",
                    usualDailyAmountPaise = 20000L,
                    isArchived = false,
                    createdAt = now + 1,
                    updatedAt = now + 1
                ),
                PigmySchemeEntity(
                    id = "scheme_3",
                    name = "Pigmy Scheme 3",
                    usualDailyAmountPaise = 30000L,
                    isArchived = false,
                    createdAt = now + 2,
                    updatedAt = now + 2
                )
            )
            pigmySchemeDao.insertSchemes(defaults)
        }
    }

    suspend fun createPigmyScheme(name: String, usualDailyAmountPaise: Long): String {
        requireOwner()
        val id = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        val scheme = PigmySchemeEntity(
            id = id,
            name = name.trim(),
            usualDailyAmountPaise = maxOf(100L, usualDailyAmountPaise),
            isArchived = false,
            createdAt = now,
            updatedAt = now
        )
        pigmySchemeDao.insertScheme(scheme)
        return id
    }

    suspend fun updatePigmyScheme(id: String, name: String, usualDailyAmountPaise: Long) {
        requireOwner()
        pigmySchemeDao.updateSchemeDetails(id, name.trim(), maxOf(100L, usualDailyAmountPaise))
    }

    suspend fun setPigmySchemeArchived(id: String, isArchived: Boolean) {
        requireOwner()
        pigmySchemeDao.setArchived(id, isArchived)
    }

    suspend fun recordPigmyDeposit(
        schemeId: String = "DEFAULT_PIGMY",
        amountPaise: Long,
        paymentSource: String = "CASH",
        notes: String? = null,
        paidTimestamp: Long = System.currentTimeMillis(),
        attachmentUri: String? = null
    ): String = db.withTransaction {
        requireOwner()
        val scheme = pigmySchemeDao.getSchemeById(schemeId)
        val schemeName = scheme?.name ?: "Pigmy Savings"
        val depositId = UUID.randomUUID().toString()
        var linkedExpenseId: String? = null

        // If paid from shop cash, create a linked savings-transfer expense entry atomically
        if (paymentSource == "CASH") {
            linkedExpenseId = UUID.randomUUID().toString()
            val noteText = if (!notes.isNullOrBlank()) "Pigmy Deposit ($schemeName): $notes" else "Pigmy Deposit ($schemeName)"
            val expense = ShopExpenseEntity(
                id = linkedExpenseId,
                category = schemeName,
                amountPaise = amountPaise,
                isIncome = false,
                notes = noteText,
                isSavingsTransfer = true,
                categoryId = schemeId,
                timestamp = paidTimestamp,
                attachmentUri = attachmentUri
            )
            expenseDao.insertExpense(expense)
        }

        val deposit = PigmyDepositEntity(
            id = depositId,
            accountSchemeId = schemeId,
            paidTimestamp = paidTimestamp,
            amountPaise = amountPaise,
            paymentSource = paymentSource,
            notes = notes,
            linkedExpenseId = linkedExpenseId,
            isVoided = false,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        pigmyDepositDao.insertDeposit(deposit)
        depositId
    }

    suspend fun updatePigmyDeposit(
        depositId: String,
        schemeId: String,
        amountPaise: Long,
        paymentSource: String,
        notes: String?,
        paidTimestamp: Long
    ) = db.withTransaction {
        requireOwner()
        val oldDeposit = pigmyDepositDao.getDepositById(depositId) ?: return@withTransaction
        require(!oldDeposit.isVoided && amountPaise > 0) { "Invalid or cancelled deposit" }
        val removed = if (schemeId == oldDeposit.accountSchemeId) oldDeposit.amountPaise - amountPaise else oldDeposit.amountPaise
        require(pigmyBalance(oldDeposit.accountSchemeId) >= removed) { "Deposit edit would make savings negative" }
        require(pigmySchemeDao.getSchemeById(schemeId)?.isArchived == false) { "Active scheme required" }
        val scheme = pigmySchemeDao.getSchemeById(schemeId)
        val schemeName = scheme?.name ?: "Pigmy Savings"
        val noteText = if (!notes.isNullOrBlank()) "Pigmy Deposit ($schemeName): $notes" else "Pigmy Deposit ($schemeName)"

        var linkedExpenseId = oldDeposit.linkedExpenseId

        if (paymentSource == "CASH") {
            if (linkedExpenseId != null) {
                // Update existing linked cash expense atomically
                val existingExpense = expenseDao.getExpenseById(linkedExpenseId)
                if (existingExpense != null) {
                    expenseDao.insertExpense(
                        existingExpense.copy(
                            amountPaise = amountPaise,
                            notes = noteText,
                            timestamp = paidTimestamp,
                            isVoided = false
                        )
                    )
                }
            } else {
                // Old was bank or missing; create new linked cash expense
                linkedExpenseId = UUID.randomUUID().toString()
                val expense = ShopExpenseEntity(
                    id = linkedExpenseId,
                    category = schemeName,
                    amountPaise = amountPaise,
                    isIncome = false,
                    notes = noteText,
                    isSavingsTransfer = true,
                    categoryId = schemeId,
                    timestamp = paidTimestamp
                )
                expenseDao.insertExpense(expense)
            }
        } else {
            // Source is BANK: reverse/void the linked cash expense if one existed
            if (linkedExpenseId != null) {
                expenseDao.voidExpense(linkedExpenseId, "Payment source changed to BANK")
                linkedExpenseId = null
            }
        }

        val updated = oldDeposit.copy(
            accountSchemeId = schemeId,
            amountPaise = amountPaise,
            paymentSource = paymentSource,
            notes = notes,
            paidTimestamp = paidTimestamp,
            linkedExpenseId = linkedExpenseId,
            updatedAt = System.currentTimeMillis()
        )
        pigmyDepositDao.updateDeposit(updated)
    }

    suspend fun voidPigmyDeposit(depositId: String, reason: String) = db.withTransaction {
        requireOwner()
        val deposit = pigmyDepositDao.getDepositById(depositId) ?: return@withTransaction
        require(!deposit.isVoided && pigmyBalance(deposit.accountSchemeId) >= deposit.amountPaise) { "Deposit cancellation would make savings negative" }
        pigmyDepositDao.voidDeposit(depositId, reason)
        if (deposit.linkedExpenseId != null) {
            expenseDao.voidExpense(deposit.linkedExpenseId, "Voided with Pigmy deposit: $reason")
        }
    }

    // ── Pigmy Withdrawals ────────────────────────────────────────────────

    val allPigmyWithdrawals: Flow<List<PigmyWithdrawalEntity>> = pigmyWithdrawalDao.getAllWithdrawals()
    val activePigmyWithdrawals: Flow<List<PigmyWithdrawalEntity>> = pigmyWithdrawalDao.getActiveWithdrawals()
    val totalPigmyWithdrawalsPaise: Flow<Long?> = pigmyWithdrawalDao.getTotalRecordedWithdrawalsPaise()

    fun getPigmyWithdrawalsForScheme(schemeId: String): Flow<List<PigmyWithdrawalEntity>> =
        pigmyWithdrawalDao.getWithdrawalsForScheme(schemeId)

    fun getTotalPigmyWithdrawalsForScheme(schemeId: String): Flow<Long?> =
        pigmyWithdrawalDao.getTotalForScheme(schemeId)

    private suspend fun pigmyBalance(schemeId: String): Long {
        val scheme = pigmySchemeDao.getSchemeById(schemeId) ?: error("Savings scheme missing")
        return Math.subtractExact(Math.addExact(scheme.openingBalancePaise, pigmyDepositDao.getTotalForScheme(schemeId).first() ?: 0L), pigmyWithdrawalDao.getTotalForScheme(schemeId).first() ?: 0L)
    }
    suspend fun updatePigmySchemeOpeningBalance(schemeId: String, openingBalancePaise: Long, date: Long? = null) = db.withTransaction {
        requireOwner()
        val scheme = pigmySchemeDao.getSchemeById(schemeId) ?: error("Savings scheme missing")
        require(openingBalancePaise >= 0 && pigmyBalance(schemeId) - scheme.openingBalancePaise + openingBalancePaise >= 0) { "Opening balance would make savings negative" }
        pigmySchemeDao.updateOpeningBalance(schemeId, openingBalancePaise, date)
    }

    suspend fun recordPigmyWithdrawal(
        schemeId: String,
        amountPaise: Long,
        destination: String = "CASH", // CASH, BANK
        notes: String? = null,
        withdrawalTimestamp: Long = System.currentTimeMillis(),
        attachmentUri: String? = null
    ): String = db.withTransaction {
        requireOwner()
        require(amountPaise > 0 && amountPaise <= pigmyBalance(schemeId)) { "Withdrawal must be positive and within the savings balance" }
        val scheme = pigmySchemeDao.getSchemeById(schemeId)
        val schemeName = scheme?.name ?: "Pigmy Savings"

        val withdrawalId = UUID.randomUUID().toString()
        var linkedExpenseId: String? = null

        // If received into shop cash, create a linked savings-transfer income entry atomically
        if (destination == "CASH") {
            linkedExpenseId = UUID.randomUUID().toString()
            val noteText = if (!notes.isNullOrBlank()) "Pigmy withdrawal — $schemeName: $notes" else "Pigmy withdrawal — $schemeName"
            val expense = ShopExpenseEntity(
                id = linkedExpenseId,
                category = schemeName,
                amountPaise = amountPaise,
                isIncome = true, // Cash In!
                notes = noteText,
                isSavingsTransfer = true, // Returned savings, NOT sales revenue or operating profit!
                categoryId = schemeId,
                timestamp = withdrawalTimestamp,
                attachmentUri = attachmentUri
            )
            expenseDao.insertExpense(expense)
        }

        val withdrawal = PigmyWithdrawalEntity(
            id = withdrawalId,
            accountSchemeId = schemeId,
            withdrawalTimestamp = withdrawalTimestamp,
            amountPaise = amountPaise,
            destination = destination,
            notes = notes,
            linkedExpenseId = linkedExpenseId,
            isVoided = false,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        pigmyWithdrawalDao.insertWithdrawal(withdrawal)
        withdrawalId
    }

    suspend fun updatePigmyWithdrawal(
        withdrawalId: String,
        schemeId: String,
        amountPaise: Long,
        destination: String,
        notes: String?,
        withdrawalTimestamp: Long
    ) = db.withTransaction {
        requireOwner()
        val oldWithdrawal = pigmyWithdrawalDao.getWithdrawalById(withdrawalId) ?: return@withTransaction
        require(!oldWithdrawal.isVoided && amountPaise > 0) { "Invalid or cancelled withdrawal" }
        val available = pigmyBalance(schemeId) + if (schemeId == oldWithdrawal.accountSchemeId) oldWithdrawal.amountPaise else 0L
        require(amountPaise <= available) { "Withdrawal exceeds savings balance" }
        val scheme = pigmySchemeDao.getSchemeById(schemeId)
        val schemeName = scheme?.name ?: "Pigmy Savings"
        val noteText = if (!notes.isNullOrBlank()) "Pigmy withdrawal — $schemeName: $notes" else "Pigmy withdrawal — $schemeName"

        var linkedExpenseId = oldWithdrawal.linkedExpenseId

        if (destination == "CASH") {
            if (linkedExpenseId != null) {
                // Update existing linked cash expense atomically
                val existingExpense = expenseDao.getExpenseById(linkedExpenseId)
                if (existingExpense != null) {
                    expenseDao.insertExpense(
                        existingExpense.copy(
                            amountPaise = amountPaise,
                            category = schemeName,
                            categoryId = schemeId,
                            notes = noteText,
                            timestamp = withdrawalTimestamp,
                            isVoided = false
                        )
                    )
                }
            } else {
                // Old was bank or missing; create new linked cash expense
                linkedExpenseId = UUID.randomUUID().toString()
                val expense = ShopExpenseEntity(
                    id = linkedExpenseId,
                    category = schemeName,
                    amountPaise = amountPaise,
                    isIncome = true,
                    notes = noteText,
                    isSavingsTransfer = true,
                    categoryId = schemeId,
                    timestamp = withdrawalTimestamp
                )
                expenseDao.insertExpense(expense)
            }
        } else {
            // Destination is BANK: reverse/void the linked cash expense if one existed
            if (linkedExpenseId != null) {
                expenseDao.voidExpense(linkedExpenseId, "Withdrawal destination changed to BANK")
                linkedExpenseId = null
            }
        }

        val updated = oldWithdrawal.copy(
            accountSchemeId = schemeId,
            amountPaise = amountPaise,
            destination = destination,
            notes = notes,
            withdrawalTimestamp = withdrawalTimestamp,
            linkedExpenseId = linkedExpenseId,
            updatedAt = System.currentTimeMillis()
        )
        pigmyWithdrawalDao.updateWithdrawal(updated)
    }

    suspend fun voidPigmyWithdrawal(withdrawalId: String, reason: String) = db.withTransaction {
        requireOwner()
        val withdrawal = pigmyWithdrawalDao.getWithdrawalById(withdrawalId) ?: return@withTransaction
        pigmyWithdrawalDao.voidWithdrawal(withdrawalId, reason)
        if (withdrawal.linkedExpenseId != null) {
            expenseDao.voidExpense(withdrawal.linkedExpenseId, "Voided with Pigmy withdrawal: $reason")
        }
    }

    // ── Product Giveaways & Donations ───────────────────────────────────

    val allGiveaways: Flow<List<ProductGiveawayEntity>> = productGiveawayDao.getAllGiveaways()
    val activeGiveaways: Flow<List<ProductGiveawayEntity>> = productGiveawayDao.getActiveGiveaways()

    fun getGiveawaysInRange(startTime: Long, endTime: Long): Flow<List<ProductGiveawayEntity>> =
        productGiveawayDao.getGiveawaysInRange(startTime, endTime)

    suspend fun recordProductGiveaway(
        productId: String,
        quantityBase: Int,
        isPack: Boolean = false,
        packSizeSnapshot: Int = 1,
        reason: String = "FREE_ITEM", // FREE_ITEM or DONATION
        recipientNote: String? = null,
        timestamp: Long = System.currentTimeMillis()
    ): String = db.withTransaction {
        requireOwner()
        val product = productDao.getProductById(productId)
            ?: throw IllegalArgumentException("Product not found")

        val giveawayId = UUID.randomUUID().toString()
        val costSnapshot = if (product.movingAvgCostPaise > 0) product.movingAvgCostPaise else null

        // 1. Deduct sellable stock
        productDao.adjustSellableStock(productId, -quantityBase, timestamp)

        // 2. Insert audit stock movement
        stockMovementDao.insertMovement(
            StockMovementEntity(
                productId = productId,
                type = "GIVEAWAY",
                quantityChangeBase = -quantityBase,
                unitCostPaise = product.movingAvgCostPaise,
                reason = "${if (reason == "DONATION") "Donation" else "Free item"}: ${recipientNote ?: ""}".trim(),
                referenceId = giveawayId,
                timestamp = timestamp
            )
        )

        // 3. Record giveaway record
        val giveaway = ProductGiveawayEntity(
            id = giveawayId,
            productId = productId,
            productName = product.name,
            quantityBase = quantityBase,
            isPack = isPack,
            packSizeSnapshot = packSizeSnapshot,
            unitCostSnapshotPaise = costSnapshot,
            reason = reason,
            recipientNote = recipientNote,
            timestamp = timestamp,
            isCancelled = false
        )
        productGiveawayDao.insertGiveaway(giveaway)
        giveawayId
    }

    suspend fun cancelProductGiveaway(giveawayId: String, reason: String? = null) = db.withTransaction {
        requireOwner()
        val giveaway = productGiveawayDao.getGiveawayById(giveawayId) ?: return@withTransaction
        if (giveaway.isCancelled) return@withTransaction

        // 1. Mark cancelled
        productGiveawayDao.cancelGiveaway(giveawayId)

        // 2. Restore sellable stock once
        productDao.adjustSellableStock(giveaway.productId, giveaway.quantityBase)

        // 3. Insert audit reversal movement
        stockMovementDao.insertMovement(
            StockMovementEntity(
                productId = giveaway.productId,
                type = "CORRECTION",
                quantityChangeBase = giveaway.quantityBase,
                unitCostPaise = giveaway.unitCostSnapshotPaise ?: 0L,
                reason = "Cancelled giveaway: ${reason ?: ""}".trim(),
                referenceId = giveawayId
            )
        )
    }

    // ── Services & Owner Expenses ───────────────────────────────────────

    val activeServices: Flow<List<ServiceDefinitionEntity>> = serviceDefinitionDao.getAllActiveServices()
    val allServices: Flow<List<ServiceDefinitionEntity>> = serviceDefinitionDao.getAllServices()

    suspend fun ensurePredefinedServices() {
        val existing = serviceDefinitionDao.getServiceByName("Printing")
        if (existing == null) {
            val predefined = listOf(
                ServiceDefinitionEntity(
                    name = "Printing",
                    unitLabel = "sides",
                    isPredefined = true
                ),
                ServiceDefinitionEntity(
                    name = "Photocopying",
                    unitLabel = "copies",
                    isPredefined = true
                ),
                ServiceDefinitionEntity(
                    name = "Sticker printing",
                    unitLabel = "sheets",
                    isPredefined = true
                )
            )
            serviceDefinitionDao.insertServices(predefined)
        }
    }

    suspend fun addCustomService(name: String, unitLabel: String): ServiceDefinitionEntity {
        requireOwner()
        val service = ServiceDefinitionEntity(
            name = name.trim(),
            unitLabel = unitLabel.trim().ifBlank { "job" },
            isPredefined = false
        )
        serviceDefinitionDao.insertService(service)
        return service
    }

    suspend fun archiveService(serviceId: String) {
        requireOwner()
        serviceDefinitionDao.archiveService(serviceId)
    }

    suspend fun recordServiceExpense(
        category: String, // PAPER, INK_TONER, STICKER_SHEETS, MACHINE_REPAIR, OTHER
        amountPaise: Long,
        serviceTarget: String, // PRINTING, PHOTOCOPYING, STICKER_PRINTING, SHARED
        notes: String? = null,
        attachmentUri: String? = null,
        timestamp: Long = System.currentTimeMillis()
    ): String {
        requireOwner()
        val expenseId = UUID.randomUUID().toString()
        val expense = ShopExpenseEntity(
            id = expenseId,
            category = "SERVICE_EXPENSE",
            amountPaise = amountPaise,
            isIncome = false,
            notes = notes,
            expenseCategory = category,
            serviceTarget = serviceTarget,
            attachmentUri = attachmentUri,
            isSavingsTransfer = false,
            timestamp = timestamp
        )
        expenseDao.insertExpense(expense)
        return expenseId
    }

    fun getServiceExpensesInRange(startTime: Long, endTime: Long): Flow<List<ShopExpenseEntity>> =
        expenseDao.getServiceExpensesInRange(startTime, endTime)

    fun getServiceLinesInRange(startTime: Long, endTime: Long): Flow<List<SaleLineEntity>> =
        saleDao.getServiceLinesInRange(startTime, endTime)

    // ── Two-Phone Sync Foundations ─────────────────────────────────────

    val pendingSyncCount: Flow<Int> = syncEventDao.getPendingCount()
    val rejectedSyncCount: Flow<Int> = saleDao.getRejectedSyncSalesCount()
    suspend fun retryRejectedSyncSales() = saleDao.retryRejectedSyncSales()

    suspend fun getPendingSyncEvents(): List<SyncEventEntity> =
        syncEventDao.getPendingEvents()

    suspend fun markEventSynced(eventId: String) {
        syncEventDao.updateEventStatus(eventId, "ACKNOWLEDGED")
    }

    suspend fun insertSyncEvent(eventType: String, entityId: String, payloadJson: String, originDeviceId: String) {
        syncEventDao.insertEvent(
            SyncEventEntity(
                eventType = eventType,
                entityId = entityId,
                payloadJson = payloadJson,
                originDeviceId = originDeviceId,
                status = "PENDING"
            )
        )
    }

    suspend fun recordDailyMonetaryTotal(
        dateTimestamp: Long,
        amountPaise: Long,
        isFullDayTotal: Boolean,
        notes: String?
    ): SaleEntity = db.withTransaction {
        requireOwner()
        require(amountPaise > 0) { "Enter a positive total" }
        val saleId = UUID.randomUUID().toString()
        val dateStr = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date(dateTimestamp))
        val receiptNumber = "AE-DAILY-$dateStr"

        val sale = SaleEntity(
            id = saleId,
            receiptNumber = receiptNumber,
            customerId = null,
            customerName = "Daily Cash Summary",
            grossTotalPaise = amountPaise,
            discountType = "NONE",
            discountValuePaise = 0L,
            discountPercent = 0.0,
            netTotalPaise = amountPaise,
            amountReceivedPaise = amountPaise,
            remainingCreditPaise = 0L,
            paymentMethod = "CASH",
            isSummaryEntry = true,
            unresolvedStockDetails = true,
            notes = if (isFullDayTotal) "Full Day Net Sales Summary. $notes" else "Additional Unrecorded Sales Summary. $notes",
            timestamp = dateTimestamp
        )

        saleDao.insertSale(sale)
        return@withTransaction sale
    }

    // ── Shop Expenses & Other Income ───────────────────────────────────

    val allExpenses: Flow<List<ShopExpenseEntity>> = expenseDao.getAllExpenses()

    val refundableExpenses: Flow<List<ShopExpenseEntity>> = expenseDao.getRefundableExpenses()

    fun getSalesForCustomer(customerId: String): Flow<List<SaleEntity>> =
        saleDao.getSalesForCustomer(customerId)

    suspend fun updateShopExpense(
        id: String,
        amountPaise: Long,
        category: String,
        paymentMethod: String,
        notes: String?,
        timestamp: Long,
        attachmentUri: String? = null
    ): Boolean = db.withTransaction {
        requireOwner()
        val existing = expenseDao.getExpenseById(id) ?: return@withTransaction false
        require(!existing.isVoided && amountPaise > 0) { "Invalid or cancelled expense" }
        val refunds = expenseDao.getActiveRefundsForExpense(id)
        require(amountPaise >= refunds.sumOf { it.amountPaise }) { "Amount cannot be below existing refunds" }
        if (existing.isExpenseRefund) {
            val parent = expenseDao.getExpenseById(existing.linkedExpenseId ?: "") ?: error("Refund source missing")
            require(!parent.isVoided && amountPaise + expenseDao.getActiveRefundsForExpense(parent.id).filter { it.id != id }.sumOf { it.amountPaise } <= parent.amountPaise) { "Refund exceeds original expense" }
        }
        if (existing.isSavingsTransfer) {
            require(paymentMethod == existing.paymentMethod) { "Change savings payment source from Pigmy Savings" }
            pigmyDepositDao.getByExpense(id)?.let {
                require(pigmyBalance(it.accountSchemeId) - it.amountPaise + amountPaise >= 0) { "Deposit edit would make savings negative" }
                pigmyDepositDao.updateDeposit(it.copy(amountPaise = amountPaise, notes = notes, paidTimestamp = timestamp, updatedAt = System.currentTimeMillis()))
            }
            pigmyWithdrawalDao.getByExpense(id)?.let {
                val available = (pigmySchemeDao.getSchemeById(it.accountSchemeId)?.openingBalancePaise ?: 0) + (pigmyDepositDao.getTotalForScheme(it.accountSchemeId).first() ?: 0)
                val other = (pigmyWithdrawalDao.getTotalForScheme(it.accountSchemeId).first() ?: 0) - it.amountPaise
                require(amountPaise <= available - other) { "Withdrawal exceeds savings" }
                pigmyWithdrawalDao.updateWithdrawal(it.copy(amountPaise = amountPaise, notes = notes, withdrawalTimestamp = timestamp, updatedAt = System.currentTimeMillis()))
            }
        }
        val updated = existing.copy(
            amountPaise = amountPaise,
            category = category,
            paymentMethod = paymentMethod,
            notes = notes,
            timestamp = timestamp,
            attachmentUri = attachmentUri ?: existing.attachmentUri
        )
        expenseDao.updateExpense(updated)
        val payload = org.json.JSONObject().apply {
            put("action", "UPDATE_EXPENSE")
            put("expenseId", id)
            put("oldAmountPaise", existing.amountPaise)
            put("newAmountPaise", amountPaise)
            put("oldCategory", existing.category)
            put("newCategory", category)
            put("oldPaymentMethod", existing.paymentMethod)
            put("newPaymentMethod", paymentMethod)
            put("oldNotes", existing.notes ?: "")
            put("newNotes", notes ?: "")
            put("oldTimestamp", existing.timestamp)
            put("newTimestamp", timestamp)
        }.toString()
        val event = SyncEventEntity(
            eventType = "EXPENSE_UPDATED",
            entityId = id,
            payloadJson = payload,
            originDeviceId = "DEVICE",
            status = "ACKNOWLEDGED",
            timestamp = System.currentTimeMillis()
        )
        syncEventDao.insertEvent(event)
        true
    }

    suspend fun voidShopExpense(id: String, reason: String): Boolean {
        voidExpense(id, reason)
        return true
    }

    suspend fun getNoteSuggestions(isIncome: Boolean, query: String = ""): List<String> {
        val raw = expenseDao.getRecentRawNotes(isIncome)
        val cleanQuery = query.trim().lowercase()
        val seen = mutableSetOf<String>()
        val result = mutableListOf<String>()
        for (note in raw) {
            val trimmed = note.trim()
            val lower = trimmed.lowercase()
            if (lower.isNotEmpty() && !seen.contains(lower)) {
                seen.add(lower)
                if (cleanQuery.isEmpty() || lower.contains(cleanQuery)) {
                    result.add(trimmed)
                    if (result.size >= 8) break
                }
            }
        }
        return result
    }

    suspend fun suggestShopBarcode(
        unitType: String = "PIECE",
        packQty: Int = 1
    ): String {
        for (attempt in 1..5) {
            val candidate = com.alieducational.app.domain.barcode.ShopBarcode.newPayload(unitType, packQty)
            val existingInProducts = productDao.getProductByBarcode(candidate)
            val existingInAux = productBarcodeDao.findByBarcode(candidate)
            if (existingInProducts == null && existingInAux == null) {
                return candidate
            }
        }
        return com.alieducational.app.domain.barcode.ShopBarcode.newPayload(unitType, packQty)
    }

    suspend fun generateAndRegisterBarcode(
        productId: String,
        unitType: String = "PIECE",
        packQty: Int = 1
    ): ProductBarcodeEntity = db.withTransaction {
        requireOwner()
        val product = productDao.getProductById(productId)
            ?: throw IllegalArgumentException("Product $productId not found")

        require(!product.isArchived && unitType in setOf("PIECE", "PACK") && (unitType != "PACK" || packQty > 1)) { "Invalid product or pack" }
        require(product.barcode.isNullOrBlank() || product.barcode.startsWith("AE-P")) { "Keep the manufacturer barcode; shop generation is only for items without one" }

        val barcodesForProduct = productBarcodeDao.getBarcodesForProduct(productId)
            .filter { it.unitType == unitType && (unitType != "PACK" || it.packQuantity == packQty) && it.barcode.startsWith("AE-P") }

        val shortPrior = barcodesForProduct.firstOrNull { it.barcode.length <= 16 }
        if (shortPrior != null) return@withTransaction shortPrior

        val code = suggestShopBarcode(unitType, packQty)
        val existing = productBarcodeDao.findByBarcode(code)
        if (existing != null) {
            require(existing.productId == productId) { "Barcode collision; generation aborted" }
            return@withTransaction existing
        }

        val entity = ProductBarcodeEntity(
            barcode = code,
            productId = productId,
            unitType = unitType,
            packQuantity = if (unitType == "PACK") packQty else 1
        )
        productBarcodeDao.insertBarcode(entity)

        if (unitType == "PIECE" && (product.barcode.isNullOrBlank() || product.barcode.startsWith("AE-P"))) {
            productDao.updateProduct(product.copy(barcode = code, updatedAt = System.currentTimeMillis()))
        }

        entity
    }

    suspend fun recordShopExpense(
        category: String,
        amountPaise: Long,
        isIncome: Boolean,
        notes: String?,
        timestamp: Long = System.currentTimeMillis(),
        categoryId: String? = null,
        attachmentUri: String? = null,
        classification: String = "SHARED",
        paymentMethod: String = "CASH",
        isExpenseRefund: Boolean = false,
        linkedExpenseId: String? = null
    ) {
        requireOwner()
        expenseDao.insertExpense(
            ShopExpenseEntity(
                category = category,
                amountPaise = amountPaise,
                isIncome = isIncome,
                notes = notes,
                categoryId = categoryId,
                attachmentUri = attachmentUri,
                classification = classification,
                paymentMethod = paymentMethod,
                isExpenseRefund = isExpenseRefund,
                linkedExpenseId = linkedExpenseId,
                timestamp = timestamp
            )
        )
    }

    suspend fun recordExpenseRefund(
        originalExpenseId: String,
        amountPaise: Long,
        paymentMethod: String = "CASH",
        notes: String? = null,
        timestamp: Long = System.currentTimeMillis(),
        attachmentUri: String? = null
    ) = db.withTransaction {
        requireOwner()
        val original = expenseDao.getExpenseById(originalExpenseId)
            ?: throw IllegalArgumentException("Original expense $originalExpenseId not found")
        if (amountPaise <= 0L) {
            throw IllegalArgumentException("Refund amount must be positive")
        }
        require(!original.isVoided && !original.isIncome && !original.isSavingsTransfer) { "Select an active operating expense" }
        val activeRefunds = expenseDao.getActiveRefundsForExpense(originalExpenseId)
        val priorTotal = activeRefunds.sumOf { it.amountPaise }
        val remainingRefundable = original.amountPaise - priorTotal
        if (amountPaise > remainingRefundable) {
            throw IllegalArgumentException("Refund amount of ₹${amountPaise/100.0} exceeds remaining refundable amount of ₹${remainingRefundable/100.0}")
        }

        val refundExpense = ShopExpenseEntity(
            category = original.category,
            amountPaise = amountPaise,
            isIncome = true,
            isExpenseRefund = true,
            linkedExpenseId = original.id,
            classification = original.classification,
            expenseCategory = original.expenseCategory,
            serviceTarget = original.serviceTarget,
            notes = notes ?: "Refund of ${original.category}",
            paymentMethod = paymentMethod,
            attachmentUri = attachmentUri,
            timestamp = timestamp
        )
        expenseDao.insertExpense(refundExpense)
    }

    suspend fun updateExpenseAttachment(expenseId: String, attachmentUri: String?) {
        requireOwner()
        val existing = expenseDao.getExpenseById(expenseId) ?: return
        expenseDao.updateExpense(existing.copy(attachmentUri = attachmentUri))
    }

    suspend fun recordShopExpensesBatch(expenses: List<ShopExpenseEntity>) {
        requireOwner()
        db.withTransaction {
            expenses.forEach { expense ->
                require(expense.amountPaise > 0) { "Invalid imported amount" }
                val key = java.util.UUID.nameUUIDFromBytes("${expense.timestamp}|${expense.category}|${expense.amountPaise}|${expense.isIncome}|${expense.notes}".toByteArray()).toString()
                if (expenseDao.getExpenseById("import-$key") == null) expenseDao.insertExpense(expense.copy(id = "import-$key"))
            }
        }
    }

    fun getSalesByDateRange(startTime: Long, endTime: Long): Flow<List<SaleEntity>> =
        saleDao.getSalesByDateRange(startTime, endTime)

    fun getSaleLinesInRange(startTime: Long, endTime: Long): Flow<List<SaleLineEntity>> =
        saleDao.getSaleLinesInRange(startTime, endTime)

    fun getOperatingExpensesInRange(startTime: Long, endTime: Long): Flow<List<ShopExpenseEntity>> =
        expenseDao.getOperatingExpensesInRange(startTime, endTime)

    suspend fun getSaleByReceipt(number: String): SaleEntity? = saleDao.getSaleByReceipt(number)

    suspend fun getLinesForSale(saleId: String): List<SaleLineEntity> =
        saleDao.getLinesForSale(saleId)

    suspend fun voidSale(
        saleId: String,
        stockCondition: String, // SELLABLE, DAMAGED, NONE
        voidReason: String
    ) = db.withTransaction {
        requireOwner()
        val sale = saleDao.getSaleById(saleId) ?: throw IllegalArgumentException("Sale not found: $saleId")
        if (sale.isVoided) throw IllegalStateException("Sale $saleId is already voided")

        val lines = saleDao.getLinesForSale(saleId)
        val timestamp = System.currentTimeMillis()

        // 1. Inventory Return (policy: ask owner whether sellable or damaged)
        lines.forEach { line ->
            val pId = line.productId
            if (pId != null) {
                when (stockCondition) {
                    "SELLABLE" -> {
                        productDao.adjustSellableStock(pId, line.quantityBase)
                        stockMovementDao.insertMovement(
                            StockMovementEntity(
                                productId = pId,
                                type = "SALE_RETURN",
                                quantityChangeBase = line.quantityBase,
                                unitCostPaise = line.unitCostSnapshotPaise ?: 0L,
                                reason = "Voided sale #${sale.receiptNumber}: Restored to sellable stock. Reason: $voidReason",
                                referenceId = saleId
                            )
                        )
                    }
                    "DAMAGED" -> {
                        val product = productDao.getProductById(pId) ?: error("Return product missing")
                        productDao.updateProduct(product.copy(currentDamagedStock = Math.addExact(product.currentDamagedStock, line.quantityBase)))
                        stockMovementDao.insertMovement(
                            StockMovementEntity(
                                productId = pId,
                                type = "DAMAGED",
                                quantityChangeBase = line.quantityBase,
                                unitCostPaise = line.unitCostSnapshotPaise ?: 0L,
                                reason = "Voided sale #${sale.receiptNumber}: Returned damaged/unusable. Reason: $voidReason",
                                referenceId = saleId
                            )
                        )
                    }
                    else -> {
                        // NONE / No stock restoration
                    }
                }
            }
        }

        // 2. Refund Payout (policy: refund the payout)
        val refundPayoutPaise = minOf(sale.amountReceivedPaise, sale.netTotalPaise)

        if (refundPayoutPaise > 0) {
            paymentDao.insertPayment(
                AccountPaymentEntity(
                    accountType = if (sale.customerId != null) "CUSTOMER" else "CASH_REFUND",
                    accountId = sale.customerId ?: "ANONYMOUS_COUNTER",
                    amountPaise = -refundPayoutPaise,
                    paymentMethod = sale.paymentMethod,
                    notes = "Refund payout for voided sale #${sale.receiptNumber}",
                    receiptReference = sale.receiptNumber
                )
            )
        }

        // 3. Customer balance adjustment: if customer owed remaining credit, reverse that credit debt
        if (sale.customerId != null && sale.remainingCreditPaise > 0) {
            customerDao.adjustBalance(sale.customerId, -sale.remainingCreditPaise)
        }

        // 4. Mark sale as voided with full audit trail
        saleDao.voidSale(
            saleId = saleId,
            reason = voidReason,
            timestamp = timestamp,
            refundPayout = refundPayoutPaise,
            condition = stockCondition
        )
    }

    suspend fun voidExpense(expenseId: String, reason: String) = db.withTransaction {
        requireOwner()
        val refunds = expenseDao.getActiveRefundsForExpense(expenseId)
        if (refunds.isNotEmpty()) {
            throw IllegalStateException("Cannot cancel an expense with active linked refunds. Please cancel linked refunds first.")
        }
        pigmyDepositDao.getByExpense(expenseId)?.takeUnless { it.isVoided }?.let {
            require(pigmyBalance(it.accountSchemeId) >= it.amountPaise) { "Deposit cancellation would make savings negative" }
        }
        expenseDao.voidExpense(expenseId, reason)
        pigmyDepositDao.voidByLinkedExpenseId(expenseId, reason)
        pigmyWithdrawalDao.voidByLinkedExpenseId(expenseId, reason)
    }

    suspend fun deleteCashCategory(id: String) {
        requireOwner()
        cashCategoryDao.deleteCategory(id)
    }

    suspend fun archiveCustomer(customerId: String) {
        requireOwner()
        require(customerDao.getCustomerById(customerId)?.currentBalancePaise == 0L) { "Settle outstanding balance before archiving" }
        customerDao.archiveCustomer(customerId)
    }

    suspend fun deleteCustomer(customerId: String) {
        requireOwner()
        require((customerDao.getCustomerById(customerId)?.currentBalancePaise ?: 0) == 0L) { "Settle account before archiving" }
        customerDao.archiveCustomer(customerId)
    }

    suspend fun archiveSupplier(supplierId: String) {
        requireOwner()
        require(supplierDao.getSupplierById(supplierId)?.currentBalancePaise == 0L) { "Settle outstanding balance before archiving" }
        supplierDao.archiveSupplier(supplierId)
    }

    suspend fun deleteSupplier(supplierId: String) {
        requireOwner()
        require((supplierDao.getSupplierById(supplierId)?.currentBalancePaise ?: 0) == 0L) { "Settle account before archiving" }
        supplierDao.archiveSupplier(supplierId)
    }

    fun getPurchasesForSupplier(supplierId: String): kotlinx.coroutines.flow.Flow<List<com.alieducational.app.data.local.entity.PurchaseEntity>> =
        purchaseDao.getPurchasesForSupplier(supplierId)
}