package com.alieducational.app.data.repository

import androidx.room.withTransaction
import com.alieducational.app.data.local.AppDatabase
import com.alieducational.app.data.local.dao.AccountPaymentDao
import com.alieducational.app.data.local.dao.CustomerDao
import com.alieducational.app.data.local.dao.HeldBillDao
import com.alieducational.app.data.local.dao.ImportBatchDao
import com.alieducational.app.data.local.dao.PigmyDepositDao
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

data class PurchaseItemParam(
    val productId: String,
    val quantityBase: Int,
    val unitPurchasePricePaise: Long
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
    val unitLabel: String? = null
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
    private val productGiveawayDao: ProductGiveawayDao,
    private val serviceDefinitionDao: ServiceDefinitionDao,
    private val syncEventDao: SyncEventDao
) {

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
            if (prod != null) {
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
    suspend fun getHeldBillById(id: String): HeldBillEntity? = heldBillDao.getHeldBillById(id)

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
        val qtyBrought = (boxesBrought * packSize) + loosePiecesBrought

        // Calculate landed cost including allocated transportation
        val transportPerPiecePaise = if (qtyBrought > 0 && transportChargesPaise > 0L) {
            transportChargesPaise / qtyBrought
        } else 0L
        val effectiveLandedCostPaise = costPerPiecePaise + transportPerPiecePaise

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
            val incomingTotalLandedCostPaise = qtyBrought.toLong() * effectiveLandedCostPaise
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
                sellingPricePackPaise = sellingPricePiecePaise * packSize,
                wholesalePricePiecePaise = wholesalePricePiecePaise,
                lowStockThreshold = lowStockThreshold,
                currentSellableStock = existingProduct.currentSellableStock + qtyBrought,
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
                sellingPricePackPaise = sellingPricePiecePaise * packSize,
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
                    purchaseDao.insertPurchase(
                        PurchaseEntity(
                            supplierId = supplierId,
                            invoiceNumber = billNo?.trim()?.ifBlank { null },
                            totalGoodsValuePaise = totalGoodsCostPaise,
                            deliveryChargePaise = transportChargesPaise,
                            amountPaidPaise = totalBillAmountPaise,
                            remainingPayablePaise = 0L,
                            paymentMethod = paymentMethod,
                            notes = "Bill #${billNo?.trim() ?: "N/A"}: ${targetProduct.name} ($qtyBrought pcs) - Paid ($paymentMethod)"
                        )
                    )
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
                    purchaseDao.insertPurchase(
                        PurchaseEntity(
                            supplierId = supplierId,
                            invoiceNumber = billNo?.trim()?.ifBlank { null },
                            totalGoodsValuePaise = totalGoodsCostPaise,
                            deliveryChargePaise = transportChargesPaise,
                            amountPaidPaise = 0L,
                            remainingPayablePaise = totalBillAmountPaise,
                            paymentMethod = paymentMethod,
                            notes = "Bill #${billNo?.trim() ?: "N/A"}: ${targetProduct.name} ($qtyBrought pcs) - Unpaid Credit"
                        )
                    )
                }
            }
        }

        targetProduct
    }

    suspend fun getProductById(id: String): ProductEntity? = productDao.getProductById(id)

    suspend fun addProduct(product: ProductEntity) {
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
        productDao.updateProduct(product)
    }

    suspend fun recordStockAdjustment(
        productId: String,
        type: String, // SHOP_USE, GIVEAWAY, DAMAGE, RETURN, CORRECTION
        quantityChange: Int,
        countedTotal: Int? = null,
        reason: String?
    ) {
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
    val allPurchases: Flow<List<PurchaseEntity>> = purchaseDao.getAllPurchases()

    suspend fun addSupplier(supplier: SupplierEntity) {
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
        val purchaseId = UUID.randomUUID().toString()
        val lineInputs = items.map {
            PurchaseLineInput(
                id = it.productId,
                quantity = it.quantityBase,
                unitPricePaise = it.unitPurchasePricePaise
            )
        }

        val allocatedDelivery = CostCalculator.allocateDelivery(lineInputs, totalDeliveryPaise)
        val totalGoodsValuePaise = items.sumOf { it.quantityBase.toLong() * it.unitPurchasePricePaise }

        val purchaseLines = items.map { item ->
            val alloc = allocatedDelivery.first { it.lineId == item.productId }
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
            val alloc = allocatedDelivery.first { it.lineId == item.productId }

            val newAvgCost = CostCalculator.calculateMovingWeightedAverage(
                existingStockQuantity = product.currentSellableStock,
                existingAverageCostPaise = product.movingAvgCostPaise,
                incomingQuantity = item.quantityBase,
                incomingTotalLandedCostPaise = alloc.totalLandedCostPaise
            )

            productDao.updateWeightedAverageCost(item.productId, newAvgCost)
            productDao.adjustSellableStock(item.productId, item.quantityBase)

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
        paymentDao.insertPayment(
            AccountPaymentEntity(
                accountType = accountType,
                accountId = accountId,
                amountPaise = amountPaise,
                paymentMethod = paymentMethod,
                notes = notes,
                receiptReference = receiptRef
            )
        )

        if (accountType == "SUPPLIER") {
            // We paid supplier: our debt decreases
            supplierDao.adjustBalance(accountId, -amountPaise)
        } else {
            // Customer paid us: their debt decreases
            customerDao.adjustBalance(accountId, -amountPaise)
        }
    }

    fun getPaymentsForAccount(accountType: String, accountId: String): Flow<List<AccountPaymentEntity>> =
        paymentDao.getPaymentsForAccount(accountType, accountId)

    // ── Sales, Discounts & Profit ──────────────────────────────────────

    val allSales: Flow<List<SaleEntity>> = saleDao.getAllSales()
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
        val saleId = UUID.randomUUID().toString()
        val dateStr = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
        val randomSuffix = (1000..9999).random()
        val devicePrefix = if (originDeviceId != null && originDeviceId != "device-main") "AE-B1" else "AE-M"
        val receiptNumber = "$devicePrefix-$dateStr-$randomSuffix"

        val grossTotalPaise = items.sumOf { it.quantityBase.toLong() * it.unitSellingPricePaise }

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
        val changeGivenPaise = (amountReceivedPaise - netTotalPaise).coerceAtLeast(0L)

        // Allocate whole-sale discount proportionally across items using lineId (not productId)
        val saleLineInputs = items.map { item ->
            val costSnapshot = if (item.lineType == "PRODUCT" && item.productId != null) {
                val product = productDao.getProductById(item.productId)
                product?.movingAvgCostPaise
            } else null

            SaleLineInput(
                id = item.lineId,
                quantity = item.quantityBase,
                unitSellingPricePaise = item.unitSellingPricePaise,
                unitCostPaise = costSnapshot
            )
        }

        val allocatedDiscountLines = CostCalculator.allocateWholeSaleDiscount(saleLineInputs, discountValuePaise)

        val saleLines = items.map { item ->
            val alloc = allocatedDiscountLines.first { it.lineId == item.lineId }
            val isProduct = item.lineType == "PRODUCT" && item.productId != null
            val costSnapshot = if (isProduct) {
                val product = productDao.getProductById(item.productId!!)
                product?.movingAvgCostPaise
            } else null

            SaleLineEntity(
                id = item.lineId,
                saleId = saleId,
                productId = item.productId,
                productName = item.productName,
                quantityBase = item.quantityBase,
                isPack = item.isPack,
                unitSellingPricePaise = item.unitSellingPricePaise,
                unitCostSnapshotPaise = costSnapshot,
                lineGrossPaise = alloc.grossValuePaise,
                lineDiscountAllocatedPaise = alloc.allocatedDiscountPaise,
                lineNetPaise = alloc.netValuePaise,
                lineProfitPaise = if (isProduct) alloc.actualProfitPaise else null,
                lineType = item.lineType,
                serviceId = item.serviceId,
                serviceName = item.serviceName,
                serviceOption = item.serviceOption,
                unitLabel = item.unitLabel
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
            unresolvedStockDetails = false,
            notes = notes,
            originDeviceId = originDeviceId,
            syncStatus = "LOCAL"
        )
        saleDao.insertSale(sale)

        // If customer owed credit, update customer debt
        if (customerId != null && remainingCreditPaise > 0) {
            customerDao.adjustBalance(customerId, remainingCreditPaise)
        }

        sale
    }

    // ── Pigmy Daily Savings Tracker ─────────────────────────────────────

    val allPigmyDeposits: Flow<List<PigmyDepositEntity>> = pigmyDepositDao.getAllDeposits()
    val activePigmyDeposits: Flow<List<PigmyDepositEntity>> = pigmyDepositDao.getActiveDeposits()
    val totalPigmyDepositsPaise: Flow<Long?> = pigmyDepositDao.getTotalRecordedDepositsPaise()

    fun getPigmyDepositsInRange(startTime: Long, endTime: Long): Flow<List<PigmyDepositEntity>> =
        pigmyDepositDao.getDepositsInRange(startTime, endTime)

    suspend fun recordPigmyDeposit(
        amountPaise: Long,
        paymentSource: String = "CASH",
        notes: String? = null,
        paidTimestamp: Long = System.currentTimeMillis()
    ): String = db.withTransaction {
        val depositId = UUID.randomUUID().toString()
        var linkedExpenseId: String? = null

        // If paid from shop cash, create a linked savings-transfer expense entry atomically
        if (paymentSource == "CASH") {
            linkedExpenseId = UUID.randomUUID().toString()
            val expense = ShopExpenseEntity(
                id = linkedExpenseId,
                category = "SAVINGS_TRANSFER",
                amountPaise = amountPaise,
                isIncome = false,
                notes = "Pigmy Deposit: ${notes ?: ""}".trim(),
                isSavingsTransfer = true,
                timestamp = paidTimestamp
            )
            expenseDao.insertExpense(expense)
        }

        val deposit = PigmyDepositEntity(
            id = depositId,
            accountSchemeId = "DEFAULT_PIGMY",
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

    suspend fun voidPigmyDeposit(depositId: String, reason: String) = db.withTransaction {
        val deposit = pigmyDepositDao.getDepositById(depositId) ?: return@withTransaction
        pigmyDepositDao.voidDeposit(depositId, reason)
        if (deposit.linkedExpenseId != null) {
            expenseDao.voidExpense(deposit.linkedExpenseId, "Voided with Pigmy deposit: $reason")
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
        val service = ServiceDefinitionEntity(
            name = name.trim(),
            unitLabel = unitLabel.trim().ifBlank { "job" },
            isPredefined = false
        )
        serviceDefinitionDao.insertService(service)
        return service
    }

    suspend fun archiveService(serviceId: String) {
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

    suspend fun recordShopExpense(
        category: String,
        amountPaise: Long,
        isIncome: Boolean,
        notes: String?,
        timestamp: Long = System.currentTimeMillis()
    ) {
        expenseDao.insertExpense(
            ShopExpenseEntity(
                category = category,
                amountPaise = amountPaise,
                isIncome = isIncome,
                notes = notes,
                timestamp = timestamp
            )
        )
    }

    suspend fun recordShopExpensesBatch(expenses: List<ShopExpenseEntity>) {
        expenseDao.insertExpenses(expenses)
    }

    fun getSalesByDateRange(startTime: Long, endTime: Long): Flow<List<SaleEntity>> =
        saleDao.getSalesByDateRange(startTime, endTime)

    suspend fun getLinesForSale(saleId: String): List<SaleLineEntity> =
        saleDao.getLinesForSale(saleId)

    suspend fun voidSale(
        saleId: String,
        stockCondition: String, // SELLABLE, DAMAGED, NONE
        voidReason: String
    ) = db.withTransaction {
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
                        productDao.moveToDamagedStock(pId, line.quantityBase)
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

    suspend fun voidExpense(expenseId: String, reason: String) {
        expenseDao.voidExpense(expenseId, reason)
    }

    suspend fun archiveCustomer(customerId: String) {
        customerDao.archiveCustomer(customerId)
    }

    suspend fun archiveSupplier(supplierId: String) {
        supplierDao.archiveSupplier(supplierId)
    }

    fun getPurchasesForSupplier(supplierId: String): kotlinx.coroutines.flow.Flow<List<com.alieducational.app.data.local.entity.PurchaseEntity>> =
        purchaseDao.getPurchasesForSupplier(supplierId)
}