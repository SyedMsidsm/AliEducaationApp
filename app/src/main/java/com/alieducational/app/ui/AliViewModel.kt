package com.alieducational.app.ui

import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alieducational.app.data.local.entity.CustomerEntity
import com.alieducational.app.data.local.entity.HeldBillEntity
import com.alieducational.app.data.local.entity.ProductEntity
import com.alieducational.app.data.local.entity.SaleEntity
import com.alieducational.app.data.local.entity.SaleLineEntity
import com.alieducational.app.data.local.entity.ServiceDefinitionEntity
import com.alieducational.app.data.local.entity.StockMovementEntity
import com.alieducational.app.data.local.entity.ShopExpenseEntity
import com.alieducational.app.data.local.entity.SupplierEntity
import com.alieducational.app.data.preferences.AppPreferences
import com.alieducational.app.domain.calculation.CostCalculator
import com.alieducational.app.data.repository.AppRepository
import com.alieducational.app.data.repository.BarcodeLookupResult
import com.alieducational.app.data.repository.PurchaseItemParam
import com.alieducational.app.data.repository.SaleItemParam
import com.alieducational.app.data.service.BackupMetadata
import com.alieducational.app.data.service.BackupRestoreService
import com.alieducational.app.data.service.CashTrackerExcelService
import com.alieducational.app.data.service.CashTrackerImportPreview
import com.alieducational.app.data.service.ColumnMapping
import com.alieducational.app.data.service.ImportExportService
import com.alieducational.app.data.service.ImportResult
import com.alieducational.app.data.service.ReceiptService
import com.alieducational.app.data.service.SpreadsheetPreview
import com.alieducational.app.data.service.SyncConnectionState
import com.alieducational.app.data.service.TwoPhoneSyncService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import javax.inject.Inject

data class BillingDraftItem(
    val lineId: String = UUID.randomUUID().toString(),
    val productId: String,
    val productName: String,
    val urduName: String? = null,
    val unitType: String = "PIECE", // "PIECE", "PACK", "SERVICE"
    val packSize: Int = 1,
    val quantity: Int = 1, // number of chosen units (pieces, packs, sides, copies, sheets)
    val unitPricePaise: Long = 0L, // selling price per chosen unit
    val movingAvgCostPaise: Long = 0L,
    val lineType: String = "PRODUCT", // "PRODUCT" or "SERVICE"
    val serviceId: String? = null,
    val serviceName: String? = null,
    val serviceOption: String? = null, // e.g. "A4 B&W", "A4 Colour"
    val unitLabel: String? = null // e.g. "sides", "copies", "sheets"
) {
    val totalPieces: Int
        get() = if (lineType == "SERVICE") 0 else if (unitType == "PACK") quantity * packSize else quantity

    val lineGrossPaise: Long
        get() = quantity.toLong() * unitPricePaise
}

data class BillingState(
    val items: List<BillingDraftItem> = emptyList(),
    val customer: CustomerEntity? = null,
    val discountType: String = "NONE", // NONE, PERCENT, FIXED
    val discountValuePaise: Long = 0L,
    val discountPercent: Double = 0.0,
    val paymentMethod: String = "CASH", // CASH, UPI, CREDIT, OTHER
    val cashReceivedStr: String = "",
    val receiptAction: String = "NONE", // NONE, PREVIEW, PRINT, SHARE
    val lastAddedItem: BillingDraftItem? = null,
    val lastScannedBarcode: String? = null,
    val unknownScannedBarcode: String? = null,
    val isTorchOn: Boolean = false,
    val lastSavedSale: SaleEntity? = null
) {
    val grossTotalPaise: Long
        get() = items.sumOf { it.lineGrossPaise }

    val actualDiscountPaise: Long
        get() = when (discountType) {
            "PERCENT" -> ((grossTotalPaise * discountPercent) / 100.0).toLong().coerceAtMost(grossTotalPaise)
            "FIXED" -> discountValuePaise.coerceAtMost(grossTotalPaise)
            else -> 0L
        }

    val netTotalPaise: Long
        get() = (grossTotalPaise - actualDiscountPaise).coerceAtLeast(0L)

    val totalItemCount: Int
        get() = items.sumOf { it.quantity }

    val cashReceivedPaise: Long
        get() = CostCalculator.parseRupeesToPaise(cashReceivedStr) ?: 0L

    val changeToGivePaise: Long
        get() = if (paymentMethod == "CASH" && cashReceivedPaise > netTotalPaise) cashReceivedPaise - netTotalPaise else 0L

    val remainingCreditPaise: Long
        get() = if (paymentMethod == "CREDIT" || paymentMethod == "OTHER") {
            (netTotalPaise - cashReceivedPaise).coerceAtLeast(0L)
        } else 0L
}

@HiltViewModel
class AliViewModel @Inject constructor(
    private val repository: AppRepository,
    private val preferences: AppPreferences,
    private val importExportService: ImportExportService,
    private val backupService: BackupRestoreService,
    val receiptService: ReceiptService,
    val cashTrackerExcelService: CashTrackerExcelService,
    val twoPhoneSyncService: TwoPhoneSyncService
) : ViewModel() {

    val syncConnectionState: StateFlow<SyncConnectionState> = twoPhoneSyncService.connectionState

    fun startSyncServer(port: Int = 8080) {
        twoPhoneSyncService.startOwnerServer(port)
    }

    fun stopSyncServer() {
        twoPhoneSyncService.stopOwnerServer()
    }

    fun syncWithMainPhone(host: String, port: Int = 8080, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val success = twoPhoneSyncService.syncWithMainPhone(host, port)
            onResult(success)
        }
    }

    // ── Preferences ────────────────────────────────────────────────────
    val isDarkTheme: StateFlow<Boolean> = preferences.themeMode
        .combine(preferences.isFirstLaunch) { mode, _ -> mode == "dark" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val appLanguage: StateFlow<String> = preferences.appLanguage
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "en")

    val isLockEnabled: StateFlow<Boolean> = preferences.isLockEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val lockTimeoutSeconds: StateFlow<Int> = preferences.lockTimeoutSeconds
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 120)

    val lastBackupTime: StateFlow<Long> = preferences.lastBackupTime
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    val isFirstLaunch: StateFlow<Boolean> = preferences.isFirstLaunch
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val shopName: StateFlow<String> = preferences.shopName
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Ali Educational")

    val pigmyUsualAmountPaise: StateFlow<Long> = preferences.pigmyUsualAmountPaise
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 100000L)

    val deviceRole: StateFlow<String> = preferences.deviceRole
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "OWNER")

    val deviceId: StateFlow<String> = preferences.deviceId
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "device-main")

    val pairedOwnerHost: StateFlow<String> = preferences.pairedOwnerHost
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

        val allPigmySchemes = repository.allPigmySchemes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activePigmySchemes = repository.activePigmySchemes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val lastExternalSaveTime = preferences.lastExternalSaveTime
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    val lastExternalSaveDestination = preferences.lastExternalSaveDestination
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val allPigmyDeposits = repository.allPigmyDeposits
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activePigmyDeposits = repository.activePigmyDeposits
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalPigmyDepositsPaise = repository.totalPigmyDepositsPaise
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    val allGiveaways = repository.allGiveaways
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeGiveaways = repository.activeGiveaways
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeServices = repository.activeServices
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pendingSyncCount = repository.pendingSyncCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    init {
        viewModelScope.launch { repository.ensureDefaultPigmySchemes() }
        viewModelScope.launch {
            preferences.setAppLanguage("en")
            repository.ensurePredefinedServices()
        }
    }

    // ── Dashboard Metrics ──────────────────────────────────────────────
    val totalStockValuePaise = repository.totalStockValuePaise
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    val lowStockCount = repository.lowStockCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val totalProductCount = repository.totalProductCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val totalWeOweSuppliersPaise = repository.totalWeOweSuppliersPaise
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    val totalCustomersOweUsPaise = repository.totalCustomersOweUsPaise
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    val recentMovements = repository.recentStockMovements
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ── Inventory ──────────────────────────────────────────────────────
    val allProducts = repository.allProducts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val lowStockProducts = repository.lowStockProducts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    val allSaleLines = repository.allSaleLines
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addOrUpdateProductWithStock(
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
        paymentMethod: String = "CASH",
        onDone: (ProductEntity) -> Unit = {}
    ) {
        viewModelScope.launch {
            val result = repository.addOrUpdateProductWithStock(
                existingId = existingId,
                name = name,
                barcode = barcode,
                category = category,
                brand = brand,
                packSize = packSize,
                boxesBrought = boxesBrought,
                loosePiecesBrought = loosePiecesBrought,
                costPerPiecePaise = costPerPiecePaise,
                sellingPricePiecePaise = sellingPricePiecePaise,
                wholesalePricePiecePaise = wholesalePricePiecePaise,
                lowStockThreshold = lowStockThreshold,
                supplierName = supplierName,
                billNo = billNo,
                transportChargesPaise = transportChargesPaise,
                isPaidNow = isPaidNow,
                paymentMethod = paymentMethod
            )
            onDone(result)
        }
    }

    fun getPurchasesForSupplier(supplierId: String) = repository.getPurchasesForSupplier(supplierId)

    fun addProduct(product: ProductEntity, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            repository.addProduct(product)
            onDone()
        }
    }

    fun updateProduct(product: ProductEntity, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            repository.updateProduct(product)
            onDone()
        }
    }

    fun recordStockAdjustment(
        productId: String,
        type: String,
        quantityChange: Int,
        countedTotal: Int? = null,
        reason: String?,
        onDone: () -> Unit = {}
    ) {
        viewModelScope.launch {
            repository.recordStockAdjustment(productId, type, quantityChange, countedTotal, reason)
            onDone()
        }
    }

    // ── Suppliers & Goods Received ─────────────────────────────────────
    val allSuppliers = repository.allSuppliers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addSupplier(supplier: SupplierEntity, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            repository.addSupplier(supplier)
            onDone()
        }
    }

    fun receiveGoods(
        supplierId: String,
        invoiceNumber: String?,
        items: List<PurchaseItemParam>,
        totalDeliveryPaise: Long,
        deliveryPaidTo: String,
        amountPaidPaise: Long,
        paymentMethod: String,
        notes: String?,
        onDone: () -> Unit = {}
    ) {
        viewModelScope.launch {
            repository.receiveGoods(
                supplierId = supplierId,
                invoiceNumber = invoiceNumber,
                items = items,
                totalDeliveryPaise = totalDeliveryPaise,
                deliveryPaidTo = deliveryPaidTo,
                amountPaidPaise = amountPaidPaise,
                paymentMethod = paymentMethod,
                notes = notes
            )
            onDone()
        }
    }

    // ── Customers & Credit ─────────────────────────────────────────────
    val allCustomers = repository.allCustomers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addCustomer(customer: CustomerEntity, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            repository.addCustomer(customer)
            onDone()
        }
    }

    fun recordAccountPayment(
        accountType: String,
        accountId: String,
        amountPaise: Long,
        paymentMethod: String,
        notes: String?,
        receiptRef: String?,
        onDone: () -> Unit = {}
    ) {
        viewModelScope.launch {
            repository.recordAccountPayment(
                accountType = accountType,
                accountId = accountId,
                amountPaise = amountPaise,
                paymentMethod = paymentMethod,
                notes = notes,
                receiptRef = receiptRef
            )
            onDone()
        }
    }

    // ── Sales ──────────────────────────────────────────────────────────
    val allSales = repository.allSales
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val unresolvedStockSales = repository.unresolvedStockSales
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allExpenses = repository.allExpenses
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun recordSale(
        customerId: String?,
        customerName: String?,
        items: List<SaleItemParam>,
        discountType: String,
        discountInput: Double,
        amountReceivedPaise: Long,
        paymentMethod: String,
        notes: String?,
        onSuccess: (SaleEntity) -> Unit = {}
    ) {
        viewModelScope.launch {
            val sale = repository.recordSale(
                customerId = customerId,
                customerName = customerName,
                items = items,
                discountType = discountType,
                discountInput = discountInput,
                amountReceivedPaise = amountReceivedPaise,
                paymentMethod = paymentMethod,
                notes = notes
            )
            onSuccess(sale)
        }
    }

    // ── Counter Fast Billing & Barcode Scanning ────────────────────────

    val favoriteProducts = repository.favoriteProducts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allCategories = repository.allCategories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allHeldBills = repository.allHeldBills
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _billingState = MutableStateFlow(BillingState())
    val billingState: StateFlow<BillingState> = _billingState.asStateFlow()

    private val _selectedBillingCategory = MutableStateFlow<String?>(null)
    val selectedBillingCategory: StateFlow<String?> = _selectedBillingCategory.asStateFlow()

    fun selectBillingCategory(category: String?) {
        _selectedBillingCategory.value = category
    }

    fun toggleFavorite(productId: String, isFav: Boolean) {
        viewModelScope.launch {
            repository.setFavorite(productId, isFav)
        }
    }

    fun addItemToBill(product: ProductEntity, unitType: String = "PIECE", quantity: Int = 1) {
        val currentItems = _billingState.value.items.toMutableList()
        val price = if (unitType == "PACK") {
            if (product.sellingPricePackPaise > 0) product.sellingPricePackPaise else product.sellingPricePiecePaise * product.packSize
        } else {
            product.sellingPricePiecePaise
        }

        val existingIndex = currentItems.indexOfFirst { it.productId == product.id && it.unitType == unitType }
        val addedItem: BillingDraftItem
        if (existingIndex >= 0) {
            val existing = currentItems[existingIndex]
            addedItem = existing.copy(quantity = existing.quantity + quantity)
            currentItems[existingIndex] = addedItem
        } else {
            addedItem = BillingDraftItem(
                productId = product.id,
                productName = product.name,
                urduName = product.urduName,
                unitType = unitType,
                packSize = product.packSize,
                quantity = quantity,
                unitPricePaise = price,
                movingAvgCostPaise = product.movingAvgCostPaise
            )
            currentItems.add(addedItem)
        }

        _billingState.value = _billingState.value.copy(
            items = currentItems,
            lastAddedItem = addedItem,
            lastScannedBarcode = null
        )
    }

    fun handleScannedBarcode(barcode: String, onUnknown: (String) -> Unit = {}) {
        val code = barcode.trim()
        if (code.isEmpty()) return

        viewModelScope.launch {
            val result = repository.lookupBarcode(code)
            if (result != null) {
                addItemToBill(result.product, result.unitType, 1)
                _billingState.value = _billingState.value.copy(lastScannedBarcode = code)
            } else {
                _billingState.value = _billingState.value.copy(unknownScannedBarcode = code)
                onUnknown(code)
            }
        }
    }

    fun clearUnknownBarcode() {
        _billingState.value = _billingState.value.copy(unknownScannedBarcode = null)
    }

    fun linkAndAddUnknownBarcode(
        barcode: String,
        productId: String,
        unitType: String = "PIECE",
        packQty: Int = 1,
        onDone: () -> Unit = {}
    ) {
        viewModelScope.launch {
            repository.linkBarcode(barcode, productId, unitType, packQty)
            val product = repository.getProductById(productId)
            if (product != null) {
                addItemToBill(product, unitType, 1)
            }
            clearUnknownBarcode()
            onDone()
        }
    }

    fun undoLastAddition() {
        val lastItem = _billingState.value.lastAddedItem ?: return
        val currentItems = _billingState.value.items.toMutableList()
        val index = currentItems.indexOfFirst { it.productId == lastItem.productId && it.unitType == lastItem.unitType }
        if (index >= 0) {
            val existing = currentItems[index]
            if (existing.quantity > 1) {
                currentItems[index] = existing.copy(quantity = existing.quantity - 1)
            } else {
                currentItems.removeAt(index)
            }
            _billingState.value = _billingState.value.copy(
                items = currentItems,
                lastAddedItem = null
            )
        }
    }

    fun addAgainLastItem() {
        val lastItem = _billingState.value.lastAddedItem ?: return
        val currentItems = _billingState.value.items.toMutableList()
        val index = currentItems.indexOfFirst { it.productId == lastItem.productId && it.unitType == lastItem.unitType }
        if (index >= 0) {
            val existing = currentItems[index]
            val updated = existing.copy(quantity = existing.quantity + 1)
            currentItems[index] = updated
            _billingState.value = _billingState.value.copy(
                items = currentItems,
                lastAddedItem = updated
            )
        }
    }

    fun updateDraftItemQuantity(productId: String, unitType: String, delta: Int) {
        val currentItems = _billingState.value.items.toMutableList()
        val index = currentItems.indexOfFirst { it.productId == productId && it.unitType == unitType }
        if (index >= 0) {
            val existing = currentItems[index]
            val newQty = existing.quantity + delta
            if (newQty > 0) {
                currentItems[index] = existing.copy(quantity = newQty)
            } else {
                currentItems.removeAt(index)
            }
            _billingState.value = _billingState.value.copy(items = currentItems)
        }
    }

    fun updateDraftItemUnit(productId: String, currentUnit: String, newUnit: String) {
        val currentItems = _billingState.value.items.toMutableList()
        val index = currentItems.indexOfFirst { it.productId == productId && it.unitType == currentUnit }
        if (index >= 0) {
            viewModelScope.launch {
                val product = repository.getProductById(productId)
                if (product != null) {
                    val newPrice = if (newUnit == "PACK") {
                        if (product.sellingPricePackPaise > 0) product.sellingPricePackPaise else product.sellingPricePiecePaise * product.packSize
                    } else {
                        product.sellingPricePiecePaise
                    }
                    val existing = currentItems[index]
                    currentItems[index] = existing.copy(
                        unitType = newUnit,
                        unitPricePaise = newPrice
                    )
                    _billingState.value = _billingState.value.copy(items = currentItems)
                }
            }
        }
    }

    fun removeDraftItem(productId: String, unitType: String) {
        val currentItems = _billingState.value.items.filterNot { it.productId == productId && it.unitType == unitType }
        _billingState.value = _billingState.value.copy(items = currentItems)
    }

    fun setWholeBillDiscountPercent(percent: Double) {
        _billingState.value = _billingState.value.copy(
            discountType = "PERCENT",
            discountPercent = percent,
            discountValuePaise = 0L
        )
    }

    fun setWholeBillDiscountFixed(amountPaise: Long) {
        _billingState.value = _billingState.value.copy(
            discountType = "FIXED",
            discountValuePaise = amountPaise,
            discountPercent = 0.0
        )
    }

    fun clearBillDiscount() {
        _billingState.value = _billingState.value.copy(
            discountType = "NONE",
            discountValuePaise = 0L,
            discountPercent = 0.0
        )
    }

    fun setBillingCustomer(customer: CustomerEntity?) {
        _billingState.value = _billingState.value.copy(customer = customer)
    }

    fun setBillingPaymentMethod(method: String) {
        _billingState.value = _billingState.value.copy(paymentMethod = method)
    }

    fun setCashReceived(text: String) {
        _billingState.value = _billingState.value.copy(cashReceivedStr = text)
    }

    fun setReceiptAction(action: String) {
        _billingState.value = _billingState.value.copy(receiptAction = action)
    }

    fun toggleTorch() {
        _billingState.value = _billingState.value.copy(isTorchOn = !_billingState.value.isTorchOn)
    }

    fun resetBilling() {
        _billingState.value = BillingState()
    }

    fun holdCurrentBill(note: String? = null, onDone: () -> Unit = {}) {
        val state = _billingState.value
        if (state.items.isEmpty()) return

        val json = itemsToJson(state.items)
        val heldBill = HeldBillEntity(
            note = note,
            customerId = state.customer?.id,
            customerName = state.customer?.name,
            discountType = state.discountType,
            discountValuePaise = state.discountValuePaise,
            discountPercent = state.discountPercent,
            draftJson = json,
            itemCount = state.totalItemCount,
            totalPaise = state.netTotalPaise
        )

        viewModelScope.launch {
            repository.holdBill(heldBill)
            resetBilling()
            onDone()
        }
    }

    fun resumeHeldBill(bill: HeldBillEntity, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            val restoredItems = jsonToItems(bill.draftJson)
            val customer = if (bill.customerId != null) repository.getCustomerById(bill.customerId) else null

            _billingState.value = BillingState(
                items = restoredItems,
                customer = customer,
                discountType = bill.discountType,
                discountValuePaise = bill.discountValuePaise,
                discountPercent = bill.discountPercent
            )
            repository.deleteHeldBill(bill.id)
            onDone()
        }
    }

    fun discardHeldBill(billId: String) {
        viewModelScope.launch {
            repository.deleteHeldBill(billId)
        }
    }

    suspend fun getSaleLines(saleId: String): List<SaleLineEntity> =
        repository.getLinesForSale(saleId)

    private var isPostingBill = false

    fun postCurrentBill(onSuccess: (SaleEntity) -> Unit, onError: (String) -> Unit) {
        if (isPostingBill) return
        val state = _billingState.value
        if (state.items.isEmpty()) {
            onError("Cannot save an empty bill")
            return
        }

        if ((state.paymentMethod == "CREDIT" || state.paymentMethod == "OTHER") && state.remainingCreditPaise > 0 && state.customer == null) {
            onError("Customer account is required when credit or unpaid balance remains")
            return
        }

        isPostingBill = true
        viewModelScope.launch {
            try {
                val saleItems = state.items.map { item ->
                    SaleItemParam(
                        productId = if (item.lineType == "SERVICE") null else item.productId,
                        productName = item.productName,
                        quantityBase = if (item.lineType == "SERVICE") item.quantity else item.totalPieces,
                        isPack = item.unitType == "PACK",
                        unitSellingPricePaise = item.unitPricePaise,
                        lineId = item.lineId,
                        lineType = item.lineType,
                        serviceId = item.serviceId,
                        serviceName = item.serviceName,
                        serviceOption = item.serviceOption,
                        unitLabel = item.unitLabel
                    )
                }

                val amountPaid = if (state.paymentMethod == "CASH") {
                    if (state.cashReceivedPaise > 0L) {
                        state.cashReceivedPaise.coerceAtMost(state.netTotalPaise)
                    } else {
                        state.netTotalPaise
                    }
                } else if (state.paymentMethod == "UPI" || state.paymentMethod == "CARD") {
                    state.netTotalPaise
                } else {
                    state.cashReceivedPaise.coerceAtMost(state.netTotalPaise)
                }

                val discountInput = if (state.discountType == "PERCENT") {
                    state.discountPercent
                } else {
                    state.discountValuePaise / 100.0
                }

                val devId = deviceId.value
                val sale = repository.recordSale(
                    customerId = state.customer?.id,
                    customerName = state.customer?.name,
                    items = saleItems,
                    discountType = state.discountType,
                    discountInput = discountInput,
                    amountReceivedPaise = amountPaid,
                    paymentMethod = state.paymentMethod,
                    notes = null,
                    originDeviceId = devId
                )

                _billingState.value = _billingState.value.copy(lastSavedSale = sale)
                onSuccess(sale)
            } catch (e: Exception) {
                onError(e.message ?: "Failed to post bill")
            } finally {
                isPostingBill = false
            }
        }
    }

    private fun itemsToJson(items: List<BillingDraftItem>): String {
        val array = JSONArray()
        for (item in items) {
            val obj = JSONObject()
            obj.put("lineId", item.lineId)
            obj.put("productId", item.productId)
            obj.put("productName", item.productName)
            obj.put("urduName", item.urduName ?: "")
            obj.put("unitType", item.unitType)
            obj.put("packSize", item.packSize)
            obj.put("quantity", item.quantity)
            obj.put("unitPricePaise", item.unitPricePaise)
            obj.put("movingAvgCostPaise", item.movingAvgCostPaise)
            obj.put("lineType", item.lineType)
            obj.put("serviceId", item.serviceId ?: "")
            obj.put("serviceName", item.serviceName ?: "")
            obj.put("serviceOption", item.serviceOption ?: "")
            obj.put("unitLabel", item.unitLabel ?: "")
            array.put(obj)
        }
        return array.toString()
    }

    private fun jsonToItems(json: String): List<BillingDraftItem> {
        val list = mutableListOf<BillingDraftItem>()
        try {
            val array = JSONArray(json)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    BillingDraftItem(
                        lineId = obj.optString("lineId").ifBlank { UUID.randomUUID().toString() },
                        productId = obj.getString("productId"),
                        productName = obj.getString("productName"),
                        urduName = obj.optString("urduName").ifBlank { null },
                        unitType = obj.optString("unitType", "PIECE"),
                        packSize = obj.optInt("packSize", 1),
                        quantity = obj.optInt("quantity", 1),
                        unitPricePaise = obj.optLong("unitPricePaise", 0L),
                        movingAvgCostPaise = obj.optLong("movingAvgCostPaise", 0L),
                        lineType = obj.optString("lineType", "PRODUCT"),
                        serviceId = obj.optString("serviceId").ifBlank { null },
                        serviceName = obj.optString("serviceName").ifBlank { null },
                        serviceOption = obj.optString("serviceOption").ifBlank { null },
                        unitLabel = obj.optString("unitLabel").ifBlank { null }
                    )
                )
            }
        } catch (_: Exception) {}
        return list
    }

    fun recordDailyMonetaryTotal(
        dateTimestamp: Long,
        amountPaise: Long,
        isFullDayTotal: Boolean,
        notes: String?,
        onDone: () -> Unit = {}
    ) {
        viewModelScope.launch {
            repository.recordDailyMonetaryTotal(dateTimestamp, amountPaise, isFullDayTotal, notes)
            onDone()
        }
    }

    fun recordShopExpense(
        category: String,
        amountPaise: Long,
        isIncome: Boolean,
        notes: String?,
        timestamp: Long = System.currentTimeMillis(),
        onDone: () -> Unit = {}
    ) {
        viewModelScope.launch {
            repository.recordShopExpense(category, amountPaise, isIncome, notes, timestamp)
            onDone()
        }
    }

    fun voidSale(
        saleId: String,
        stockCondition: String, // SELLABLE, DAMAGED, NONE
        voidReason: String,
        onDone: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                repository.voidSale(saleId, stockCondition, voidReason)
                onDone()
            } catch (e: Exception) {
                onError(e.message ?: "Failed to void sale")
            }
        }
    }

    fun voidExpense(expenseId: String, reason: String, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            repository.voidExpense(expenseId, reason)
            onDone()
        }
    }

    fun parseCashTrackerExcel(
        uri: Uri,
        fileName: String,
        onResult: (CashTrackerImportPreview) -> Unit
    ) {
        viewModelScope.launch {
            val preview = cashTrackerExcelService.parseExcelFile(uri, fileName)
            onResult(preview)
        }
    }

    fun confirmCashTrackerImport(
        items: List<ShopExpenseEntity>,
        onDone: () -> Unit
    ) {
        viewModelScope.launch {
            repository.recordShopExpensesBatch(items)
            onDone()
        }
    }

    fun exportCashTrackerExcel(
        items: List<ShopExpenseEntity>,
        periodTitle: String,
        onIntentReady: (Intent) -> Unit
    ) {
        viewModelScope.launch {
            val intent = cashTrackerExcelService.exportCashTrackerExcel(items, periodTitle)
            onIntentReady(intent)
        }
    }

    fun archiveCustomer(customerId: String, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            repository.archiveCustomer(customerId)
            onDone()
        }
    }

    fun archiveSupplier(supplierId: String, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            repository.archiveSupplier(supplierId)
            onDone()
        }
    }

    // ── Backup & Restore ───────────────────────────────────────────────
    val backupFolderPath: String
        get() = backupService.backupFolderPath

    fun getBackupShareIntent(filePath: String): Intent {
        return backupService.getShareIntent(filePath)
    }

    private val _backupStatus = MutableStateFlow<String?>(null)
    val backupStatus: StateFlow<String?> = _backupStatus.asStateFlow()

    private val _availableBackups = MutableStateFlow<List<BackupMetadata>>(emptyList())
    val availableBackups: StateFlow<List<BackupMetadata>> = _availableBackups.asStateFlow()

    private val _isCreatingBackup = MutableStateFlow(false)
    val isCreatingBackup: StateFlow<Boolean> = _isCreatingBackup.asStateFlow()

    fun loadBackups() {
        viewModelScope.launch {
            _availableBackups.value = backupService.getAvailableBackups()
        }
    }

    fun createBackup(onDone: () -> Unit = {}) {
        if (_isCreatingBackup.value) return // Prevent overlapping jobs
        _isCreatingBackup.value = true
        _backupStatus.value = "Creating backup..."
        viewModelScope.launch {
            try {
                val meta = backupService.createBackup()
                _backupStatus.value = "Backup created: ${meta.fileName}"
                loadBackups()
                onDone()
            } catch (e: Exception) {
                _backupStatus.value = "Backup failed: ${e.localizedMessage ?: "Unknown error"}"
            } finally {
                _isCreatingBackup.value = false
            }
        }
    }

    fun restoreBackup(file: File, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            val success = backupService.restoreBackup(file)
            _backupStatus.value = if (success) "Restored successfully" else "Restore failed"
            onDone()
        }
    }

    fun restoreFromUri(uri: Uri, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            val success = backupService.restoreFromUri(uri)
            _backupStatus.value = if (success) "Restored successfully" else "Restore failed"
            onDone()
        }
    }

    fun exportBackupToUri(backupFile: File, targetUri: Uri, onDone: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val success = backupService.exportBackupToUri(backupFile, targetUri)
            _backupStatus.value = if (success) "Saved to Google Drive / Files successfully" else "Failed to save backup to destination"
            onDone(success)
        }
    }

    // ── Import / Export ────────────────────────────────────────────────
    private val _previewData = MutableStateFlow<SpreadsheetPreview?>(null)
    val previewData: StateFlow<SpreadsheetPreview?> = _previewData.asStateFlow()

    private val _importResult = MutableStateFlow<ImportResult?>(null)
    val importResult: StateFlow<ImportResult?> = _importResult.asStateFlow()

    private val _isImporting = MutableStateFlow(false)
    val isImporting: StateFlow<Boolean> = _isImporting.asStateFlow()

    fun previewSpreadsheet(uri: Uri, fileName: String, sheetIndex: Int = 0) {
        viewModelScope.launch {
            _previewData.value = importExportService.previewFile(uri, fileName, sheetIndex)
        }
    }

    fun executeImport(
        uri: Uri,
        fileName: String,
        sheetIndex: Int = 0,
        mapping: ColumnMapping,
        updateExisting: Boolean = false,
        onDone: () -> Unit = {}
    ) {
        if (_isImporting.value) return
        viewModelScope.launch {
            _isImporting.value = true
            try {
                val result = importExportService.importProducts(uri, fileName, sheetIndex, mapping, updateExisting)
                _importResult.value = result
                onDone()
            } finally {
                _isImporting.value = false
            }
        }
    }

    // ── Settings ───────────────────────────────────────────────────────
    fun setTheme(mode: String) {
        viewModelScope.launch { preferences.setThemeMode(mode) }
    }

    fun setLanguage(lang: String) {
        viewModelScope.launch { preferences.setAppLanguage(lang) }
    }

    fun setLockEnabled(enabled: Boolean) {
        viewModelScope.launch { preferences.setLockEnabled(enabled) }
    }

    fun setLockTimeout(seconds: Int) {
        viewModelScope.launch { preferences.setLockTimeoutSeconds(seconds) }
    }

    fun completeSetup(shopName: String, language: String, lockEnabled: Boolean) {
        viewModelScope.launch {
            preferences.setShopName(shopName)
            preferences.setAppLanguage(language)
            preferences.setLockEnabled(lockEnabled)
            preferences.setFirstLaunch(false)
        }
    }

    // ── Pigmy Savings Actions ──────────────────────────────────────────
    fun setPigmyUsualAmount(amountPaise: Long) {
        viewModelScope.launch {
            preferences.setPigmyUsualAmountPaise(amountPaise)
        }
    }

    fun createPigmyScheme(name: String, usualDailyAmountPaise: Long, onDone: (String) -> Unit = {}) {
        viewModelScope.launch {
            val id = repository.createPigmyScheme(name, usualDailyAmountPaise)
            onDone(id)
        }
    }

    fun updatePigmyScheme(id: String, name: String, usualDailyAmountPaise: Long, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            repository.updatePigmyScheme(id, name, usualDailyAmountPaise)
            onDone()
        }
    }

    fun setPigmySchemeArchived(id: String, isArchived: Boolean, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            repository.setPigmySchemeArchived(id, isArchived)
            onDone()
        }
    }

    fun recordPigmyDeposit(
        schemeId: String = "DEFAULT_PIGMY",
        amountPaise: Long,
        paymentSource: String = "CASH",
        notes: String? = null,
        paidTimestamp: Long = System.currentTimeMillis(),
        onDone: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            val id = repository.recordPigmyDeposit(schemeId, amountPaise, paymentSource, notes, paidTimestamp)
            onDone(id)
        }
    }

    fun updatePigmyDeposit(
        depositId: String,
        schemeId: String,
        amountPaise: Long,
        paymentSource: String,
        notes: String?,
        paidTimestamp: Long,
        onDone: () -> Unit = {}
    ) {
        viewModelScope.launch {
            repository.updatePigmyDeposit(depositId, schemeId, amountPaise, paymentSource, notes, paidTimestamp)
            onDone()
        }
    }

    fun voidPigmyDeposit(depositId: String, reason: String, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            repository.voidPigmyDeposit(depositId, reason)
            onDone()
        }
    }

    // ── Product Giveaways & Donations Actions ──────────────────────────
    fun recordProductGiveaway(
        productId: String,
        quantityBase: Int,
        isPack: Boolean = false,
        packSizeSnapshot: Int = 1,
        reason: String = "FREE_ITEM",
        recipientNote: String? = null,
        onDone: () -> Unit = {}
    ) {
        viewModelScope.launch {
            repository.recordProductGiveaway(
                productId = productId,
                quantityBase = quantityBase,
                isPack = isPack,
                packSizeSnapshot = packSizeSnapshot,
                reason = reason,
                recipientNote = recipientNote
            )
            onDone()
        }
    }

    fun cancelProductGiveaway(giveawayId: String, reason: String? = null, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            repository.cancelProductGiveaway(giveawayId, reason)
            onDone()
        }
    }

    // ── Services Actions ───────────────────────────────────────────────
    fun addCustomService(name: String, unitLabel: String, onDone: (ServiceDefinitionEntity) -> Unit = {}) {
        viewModelScope.launch {
            val svc = repository.addCustomService(name, unitLabel)
            onDone(svc)
        }
    }

    fun archiveService(serviceId: String, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            repository.archiveService(serviceId)
            onDone()
        }
    }

    fun addServiceToBill(
        serviceName: String,
        option: String?,
        unitLabel: String,
        quantity: Int,
        unitRatePaise: Long,
        serviceId: String? = null
    ) {
        val currentItems = _billingState.value.items.toMutableList()
        val displayName = if (!option.isNullOrBlank()) "$serviceName — $option" else serviceName
        val serviceLine = BillingDraftItem(
            lineId = java.util.UUID.randomUUID().toString(),
            productId = serviceId ?: "SERVICE_${serviceName.uppercase().replace(" ", "_")}",
            productName = displayName,
            urduName = null,
            unitType = "SERVICE",
            packSize = 1,
            quantity = quantity,
            unitPricePaise = unitRatePaise,
            movingAvgCostPaise = 0L,
            lineType = "SERVICE",
            serviceId = serviceId,
            serviceName = serviceName,
            serviceOption = option,
            unitLabel = unitLabel
        )
        currentItems.add(serviceLine)
        _billingState.value = _billingState.value.copy(
            items = currentItems,
            lastAddedItem = serviceLine
        )
    }

    fun updateItemPrice(lineId: String, newPricePaise: Long) {
        val currentItems = _billingState.value.items.toMutableList()
        val index = currentItems.indexOfFirst { it.lineId == lineId }
        if (index >= 0) {
            val item = currentItems[index]
            currentItems[index] = item.copy(unitPricePaise = newPricePaise)
            _billingState.value = _billingState.value.copy(items = currentItems)
        }
    }

    fun updateItemQuantity(lineId: String, newQuantity: Int) {
        val currentItems = _billingState.value.items.toMutableList()
        val index = currentItems.indexOfFirst { it.lineId == lineId }
        if (index >= 0) {
            if (newQuantity <= 0) {
                currentItems.removeAt(index)
            } else {
                currentItems[index] = currentItems[index].copy(quantity = newQuantity)
            }
            _billingState.value = _billingState.value.copy(items = currentItems)
        }
    }

    fun removeItemFromBill(lineId: String) {
        val currentItems = _billingState.value.items.toMutableList()
        currentItems.removeAll { it.lineId == lineId }
        _billingState.value = _billingState.value.copy(items = currentItems)
    }

    fun recordServiceExpense(
        category: String,
        amountPaise: Long,
        serviceTarget: String,
        notes: String? = null,
        attachmentUri: String? = null,
        timestamp: Long = System.currentTimeMillis(),
        onDone: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            val id = repository.recordServiceExpense(
                category = category,
                amountPaise = amountPaise,
                serviceTarget = serviceTarget,
                notes = notes,
                attachmentUri = attachmentUri,
                timestamp = timestamp
            )
            onDone(id)
        }
    }

    fun getServiceExpensesInRange(startTime: Long, endTime: Long) =
        repository.getServiceExpensesInRange(startTime, endTime)

    fun getServiceLinesInRange(startTime: Long, endTime: Long) =
        repository.getServiceLinesInRange(startTime, endTime)

    fun setDeviceRole(role: String) {
        viewModelScope.launch {
            preferences.setDeviceRole(role)
        }
    }

    fun setPairedOwnerHost(host: String) {
        viewModelScope.launch {
            preferences.setPairedOwnerHost(host)
        }
    }
}