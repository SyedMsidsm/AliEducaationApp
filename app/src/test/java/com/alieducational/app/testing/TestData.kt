package com.alieducational.app.testing

import com.alieducational.app.data.local.entity.CustomerEntity
import com.alieducational.app.data.local.entity.ProductEntity
import com.alieducational.app.data.local.entity.SaleEntity
import com.alieducational.app.data.local.entity.SaleLineEntity
import com.alieducational.app.data.local.entity.ShopExpenseEntity
import java.util.UUID

object TestData {

    fun product(
        id: String = UUID.randomUUID().toString(),
        name: String = "Test Product",
        urduName: String? = null,
        category: String = "Stationery",
        subcategory: String? = null,
        brand: String? = "Classmate",
        sku: String? = null,
        barcode: String? = null,
        isFavorite: Boolean = false,
        photoUri: String? = null,
        notes: String? = null,
        baseUnit: String = "piece",
        packSize: Int = 1,
        movingAvgCostPaise: Long = 5000L,
        sellingPricePiecePaise: Long = 10000L,
        sellingPricePackPaise: Long = 0L,
        wholesalePricePiecePaise: Long = 0L,
        currentSellableStock: Int = 100,
        currentDamagedStock: Int = 0,
        lowStockThreshold: Int = 5,
        isArchived: Boolean = false,
        createdAt: Long = 1731110400000L,
        updatedAt: Long = 1731110400000L
    ) = ProductEntity(
        id = id,
        name = name,
        urduName = urduName,
        category = category,
        subcategory = subcategory,
        brand = brand,
        sku = sku,
        barcode = barcode,
        isFavorite = isFavorite,
        photoUri = photoUri,
        notes = notes,
        baseUnit = baseUnit,
        packSize = packSize,
        movingAvgCostPaise = movingAvgCostPaise,
        sellingPricePiecePaise = sellingPricePiecePaise,
        sellingPricePackPaise = sellingPricePackPaise,
        wholesalePricePiecePaise = wholesalePricePiecePaise,
        currentSellableStock = currentSellableStock,
        currentDamagedStock = currentDamagedStock,
        lowStockThreshold = lowStockThreshold,
        isArchived = isArchived,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    fun customer(
        id: String = UUID.randomUUID().toString(),
        name: String = "Test Customer",
        phone: String? = "9876543210",
        notes: String? = null,
        currentBalancePaise: Long = 0L,
        isArchived: Boolean = false,
        createdAt: Long = 1731110400000L
    ) = CustomerEntity(
        id = id,
        name = name,
        phone = phone,
        notes = notes,
        currentBalancePaise = currentBalancePaise,
        isArchived = isArchived,
        createdAt = createdAt
    )

    fun sale(
        id: String = UUID.randomUUID().toString(),
        receiptNumber: String = "ALI-REC-001",
        customerId: String? = null,
        customerName: String? = "Walk-in Customer",
        grossTotalPaise: Long = 20000L,
        discountType: String = "NONE",
        discountValuePaise: Long = 0L,
        discountPercent: Double = 0.0,
        netTotalPaise: Long = 20000L,
        amountReceivedPaise: Long = 20000L,
        changeGivenPaise: Long = 0L,
        remainingCreditPaise: Long = 0L,
        paymentMethod: String = "CASH",
        isSummaryEntry: Boolean = false,
        unresolvedStockDetails: Boolean = false,
        notes: String? = null,
        isVoided: Boolean = false,
        voidReason: String? = null,
        voidTimestamp: Long? = null,
        refundPayoutPaise: Long = 0L,
        stockRestoredCondition: String? = null,
        originDeviceId: String? = null,
        syncStatus: String = "LOCAL",
        timestamp: Long = 1731110400000L
    ) = SaleEntity(
        id = id,
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
        isSummaryEntry = isSummaryEntry,
        unresolvedStockDetails = unresolvedStockDetails,
        notes = notes,
        isVoided = isVoided,
        voidReason = voidReason,
        voidTimestamp = voidTimestamp,
        refundPayoutPaise = refundPayoutPaise,
        stockRestoredCondition = stockRestoredCondition,
        originDeviceId = originDeviceId,
        syncStatus = syncStatus,
        timestamp = timestamp
    )

    fun saleLine(
        id: String = UUID.randomUUID().toString(),
        saleId: String,
        productId: String? = "prod-1",
        productName: String = "Register 200 Pages",
        quantityBase: Int = 2,
        isPack: Boolean = false,
        unitSellingPricePaise: Long = 10000L,
        unitCostSnapshotPaise: Long? = 5000L,
        lineGrossPaise: Long = 20000L,
        lineDiscountAllocatedPaise: Long = 0L,
        lineNetPaise: Long = 20000L,
        lineProfitPaise: Long? = 10000L,
        lineType: String = "PRODUCT",
        serviceId: String? = null,
        serviceName: String? = null,
        serviceOption: String? = null,
        unitLabel: String? = "pcs"
    ) = SaleLineEntity(
        id = id,
        saleId = saleId,
        productId = productId,
        productName = productName,
        quantityBase = quantityBase,
        isPack = isPack,
        unitSellingPricePaise = unitSellingPricePaise,
        unitCostSnapshotPaise = unitCostSnapshotPaise,
        lineGrossPaise = lineGrossPaise,
        lineDiscountAllocatedPaise = lineDiscountAllocatedPaise,
        lineNetPaise = lineNetPaise,
        lineProfitPaise = lineProfitPaise,
        lineType = lineType,
        serviceId = serviceId,
        serviceName = serviceName,
        serviceOption = serviceOption,
        unitLabel = unitLabel
    )

    fun expense(
        id: String = UUID.randomUUID().toString(),
        category: String = "ELECTRICITY",
        amountPaise: Long = 50000L,
        isIncome: Boolean = false,
        notes: String? = "Shop bill",
        isVoided: Boolean = false,
        voidReason: String? = null,
        expenseCategory: String? = null,
        serviceTarget: String? = null,
        attachmentUri: String? = null,
        isSavingsTransfer: Boolean = false,
        categoryId: String? = null,
        isExpenseRefund: Boolean = false,
        linkedExpenseId: String? = null,
        classification: String = "SHARED",
        paymentMethod: String = "CASH",
        timestamp: Long = 1731110400000L
    ) = ShopExpenseEntity(
        id = id,
        category = category,
        amountPaise = amountPaise,
        isIncome = isIncome,
        notes = notes,
        isVoided = isVoided,
        voidReason = voidReason,
        expenseCategory = expenseCategory,
        serviceTarget = serviceTarget,
        attachmentUri = attachmentUri,
        isSavingsTransfer = isSavingsTransfer,
        categoryId = categoryId,
        isExpenseRefund = isExpenseRefund,
        linkedExpenseId = linkedExpenseId,
        classification = classification,
        paymentMethod = paymentMethod,
        timestamp = timestamp
    )
}
