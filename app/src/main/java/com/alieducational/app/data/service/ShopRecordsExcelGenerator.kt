package com.alieducational.app.data.service

import com.alieducational.app.data.local.entity.AccountPaymentEntity
import com.alieducational.app.data.local.entity.CashCategoryEntity
import com.alieducational.app.data.local.entity.CustomerEntity
import com.alieducational.app.data.local.entity.PigmyDepositEntity
import com.alieducational.app.data.local.entity.PigmySchemeEntity
import com.alieducational.app.data.local.entity.PigmyWithdrawalEntity
import com.alieducational.app.data.local.entity.ProductEntity
import com.alieducational.app.data.local.entity.PurchaseEntity
import com.alieducational.app.data.local.entity.PurchaseLineEntity
import com.alieducational.app.data.local.entity.SaleEntity
import com.alieducational.app.data.local.entity.SaleLineEntity
import com.alieducational.app.data.local.entity.ServiceDefinitionEntity
import com.alieducational.app.data.local.entity.ShopExpenseEntity
import com.alieducational.app.data.local.entity.StockMovementEntity
import com.alieducational.app.data.local.entity.SupplierEntity
import org.apache.poi.ss.usermodel.BorderStyle
import org.apache.poi.ss.usermodel.FillPatternType
import org.apache.poi.ss.usermodel.HorizontalAlignment
import org.apache.poi.ss.usermodel.IndexedColors
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

data class ShopSnapshotData(
    val snapshotId: String,
    val timestamp: Long,
    val shopName: String,
    val products: List<ProductEntity>,
    val movements: List<StockMovementEntity>,
    val purchases: List<PurchaseEntity>,
    val purchaseLines: List<PurchaseLineEntity>,
    val sales: List<SaleEntity>,
    val saleLines: List<SaleLineEntity>,
    val accountPayments: List<AccountPaymentEntity>,
    val customers: List<CustomerEntity>,
    val suppliers: List<SupplierEntity>,
    val cashExpenses: List<ShopExpenseEntity>,
    val pigmySchemes: List<PigmySchemeEntity>,
    val pigmyDeposits: List<PigmyDepositEntity>,
    val pigmyWithdrawals: List<PigmyWithdrawalEntity>,
    val services: List<ServiceDefinitionEntity>,
    val cashCategories: List<CashCategoryEntity>,
    val photoCount: Int
)

@Singleton
class ShopRecordsExcelGenerator @Inject constructor() {

    fun generateWorkbook(data: ShopSnapshotData, outputFile: File) {
        val workbook = XSSFWorkbook()
        val dateFormat = SimpleDateFormat("dd-MMM-yyyy hh:mm a", Locale.US)

        // Styles
        val headerStyle = workbook.createCellStyle().apply {
            val font = workbook.createFont().apply {
                bold = true
                color = IndexedColors.WHITE.index
            }
            setFont(font)
            fillForegroundColor = IndexedColors.GREY_80_PERCENT.index
            fillPattern = FillPatternType.SOLID_FOREGROUND
            alignment = HorizontalAlignment.CENTER
            borderBottom = BorderStyle.THIN
            borderTop = BorderStyle.THIN
            borderLeft = BorderStyle.THIN
            borderRight = BorderStyle.THIN
        }

        val normalStyle = workbook.createCellStyle().apply {
            borderBottom = BorderStyle.THIN
            borderTop = BorderStyle.THIN
            borderLeft = BorderStyle.THIN
            borderRight = BorderStyle.THIN
        }

        val currencyFormat = workbook.createDataFormat().getFormat("#,##0.00")
        val numberStyle = workbook.createCellStyle().apply {
            setDataFormat(currencyFormat)
            alignment = HorizontalAlignment.RIGHT
            borderBottom = BorderStyle.THIN
            borderTop = BorderStyle.THIN
            borderLeft = BorderStyle.THIN
            borderRight = BorderStyle.THIN
        }

        val integerStyle = workbook.createCellStyle().apply {
            alignment = HorizontalAlignment.RIGHT
            borderBottom = BorderStyle.THIN
            borderTop = BorderStyle.THIN
            borderLeft = BorderStyle.THIN
            borderRight = BorderStyle.THIN
        }

        fun createHeaderRow(sheet: org.apache.poi.ss.usermodel.Sheet, rowIdx: Int, headers: List<String>) {
            val row = sheet.createRow(rowIdx)
            headers.forEachIndexed { i, title ->
                val cell = row.createCell(i)
                cell.setCellValue(title)
                cell.cellStyle = headerStyle
            }
        }

        fun writeString(row: Row, col: Int, value: String?) {
            val cell = row.createCell(col)
            cell.setCellValue(value ?: "")
            cell.cellStyle = normalStyle
        }

        fun writeNumber(row: Row, col: Int, rupees: Double) {
            val cell = row.createCell(col)
            cell.setCellValue(rupees)
            cell.cellStyle = numberStyle
        }

        fun writeInt(row: Row, col: Int, value: Int) {
            val cell = row.createCell(col)
            cell.setCellValue(value.toDouble())
            cell.cellStyle = integerStyle
        }

        // ══════════════════════════════════════════════════════════════
        // SHEET 1: Summary
        // ══════════════════════════════════════════════════════════════
        val summarySheet = workbook.createSheet("Summary")
        var sRow = 0

        val titleRow = summarySheet.createRow(sRow++)
        titleRow.createCell(0).apply {
            setCellValue("${data.shopName} - Complete Shop Records")
            cellStyle = workbook.createCellStyle().apply {
                val f = workbook.createFont().apply { bold = true; fontHeightInPoints = 14.toShort() }
                setFont(f)
            }
        }

        val noticeRow = summarySheet.createRow(sRow++)
        noticeRow.createCell(0).setCellValue("Readable Auditing Workbook (Generated from Snapshot: ${data.snapshotId})")

        sRow++ // Blank line

        fun addSummaryLine(label: String, value: Any) {
            val r = summarySheet.createRow(sRow++)
            r.createCell(0).apply { setCellValue(label); cellStyle = normalStyle }
            val c = r.createCell(1)
            when (value) {
                is Double -> {
                    c.setCellValue(value)
                    c.cellStyle = numberStyle
                }
                is Number -> {
                    c.setCellValue(value.toDouble())
                    c.cellStyle = integerStyle
                }
                else -> {
                    c.setCellValue(value.toString())
                    c.cellStyle = normalStyle
                }
            }
        }

        addSummaryLine("Snapshot Identifier:", data.snapshotId)
        addSummaryLine("Snapshot Timestamp:", dateFormat.format(Date(data.timestamp)))
        addSummaryLine("Total Products Count:", data.products.size)
        val totalSellableStock = data.products.sumOf { it.currentSellableStock }
        addSummaryLine("Total Sellable Stock Units:", totalSellableStock)
        val stockValuationRupees = data.products.filter { !it.isArchived && it.currentSellableStock > 0 }.sumOf { it.currentSellableStock * it.movingAvgCostPaise } / 100.0
        addSummaryLine("Total Stock Inventory Valuation (₹):", stockValuationRupees)

        val totalCustomersDueRupees = data.customers.sumOf { it.currentBalancePaise } / 100.0
        addSummaryLine("Total Outstanding Customers Owe Us (₹):", totalCustomersDueRupees)
        val totalSuppliersDueRupees = data.suppliers.sumOf { it.currentBalancePaise } / 100.0
        addSummaryLine("Total Outstanding We Owe Suppliers (₹):", totalSuppliersDueRupees)

        val completedBills = data.sales.filter { !it.isVoided }
        val totalSalesRupees = completedBills.sumOf { it.netTotalPaise } / 100.0
        addSummaryLine("Total Completed Bills Count:", completedBills.size)
        addSummaryLine("Total Recorded Sales Revenue (₹):", totalSalesRupees)

        val activeExpenses = data.cashExpenses.filter { !it.isVoided }
        val totalInflowRupees = activeExpenses.filter { it.isIncome }.sumOf { it.amountPaise } / 100.0
        val totalOutflowRupees = activeExpenses.filter { !it.isIncome }.sumOf { it.amountPaise } / 100.0
        addSummaryLine("Total Recorded Cash Inflow (₹):", totalInflowRupees)
        addSummaryLine("Total Recorded Cash Outflow (₹):", totalOutflowRupees)

        val activePigmyDeposits = data.pigmyDeposits.filter { !it.isVoided }.sumOf { it.amountPaise }
        val activePigmyWithdrawals = data.pigmyWithdrawals.filter { !it.isVoided }.sumOf { it.amountPaise }
        val trackedPigmyBalanceRupees = (activePigmyDeposits - activePigmyWithdrawals) / 100.0
        addSummaryLine("Total Pigmy Schemes Count:", data.pigmySchemes.size)
        addSummaryLine("Net Tracked Pigmy Savings Balance (₹):", trackedPigmyBalanceRupees)
        addSummaryLine("Photo & File Attachments Count:", data.photoCount)

        summarySheet.setColumnWidth(0, 36 * 256)
        summarySheet.setColumnWidth(1, 24 * 256)

        // ══════════════════════════════════════════════════════════════
        // SHEET 2: Products and Stock
        // ══════════════════════════════════════════════════════════════
        val prodSheet = workbook.createSheet("Products and Stock")
        val prodHeaders = listOf(
            "Product ID", "Barcode", "Product Name", "Category", "Brand",
            "Base Unit", "Pack Size", "Sellable Stock", "Avg Cost (₹)",
            "Sell Price Piece (₹)", "Sell Price Pack (₹)", "Stock Value (₹)",
            "Low Stock Threshold", "Status"
        )
        createHeaderRow(prodSheet, 0, prodHeaders)
        data.products.forEachIndexed { idx, p ->
            val row = prodSheet.createRow(idx + 1)
            writeString(row, 0, p.id)
            writeString(row, 1, p.barcode ?: "")
            writeString(row, 2, p.name)
            writeString(row, 3, p.category)
            writeString(row, 4, p.brand ?: "")
            writeString(row, 5, p.baseUnit)
            writeInt(row, 6, p.packSize)
            writeInt(row, 7, p.currentSellableStock)
            writeNumber(row, 8, p.movingAvgCostPaise / 100.0)
            writeNumber(row, 9, p.sellingPricePiecePaise / 100.0)
            writeNumber(row, 10, p.sellingPricePackPaise / 100.0)
            writeNumber(row, 11, (p.currentSellableStock * p.movingAvgCostPaise) / 100.0)
            writeInt(row, 12, p.lowStockThreshold)
            writeString(row, 13, if (p.isArchived) "Archived" else "Active")
        }

        // ══════════════════════════════════════════════════════════════
        // SHEET 3: Stock Movements / Purchases
        // ══════════════════════════════════════════════════════════════
        val movSheet = workbook.createSheet("Stock Movements")
        val movHeaders = listOf(
            "Movement ID", "Date & Time", "Product Name", "Type",
            "Quantity Change", "Unit Cost (₹)", "Total Cost (₹)", "Reason / Notes", "Reference ID"
        )
        createHeaderRow(movSheet, 0, movHeaders)
        val productMap = data.products.associateBy { it.id }
        data.movements.forEachIndexed { idx, m ->
            val row = movSheet.createRow(idx + 1)
            writeString(row, 0, m.id)
            writeString(row, 1, dateFormat.format(Date(m.timestamp)))
            writeString(row, 2, productMap[m.productId]?.name ?: m.productId)
            writeString(row, 3, m.type)
            writeInt(row, 4, m.quantityChangeBase)
            writeNumber(row, 5, m.unitCostPaise / 100.0)
            writeNumber(row, 6, (m.quantityChangeBase * m.unitCostPaise) / 100.0)
            writeString(row, 7, m.reason ?: "")
            writeString(row, 8, m.referenceId ?: "")
        }

        // ══════════════════════════════════════════════════════════════
        // SHEET 4: Bills / Bill Items
        // ══════════════════════════════════════════════════════════════
        val billsSheet = workbook.createSheet("Bills and Items")
        val billHeaders = listOf(
            "Receipt No", "Date & Time", "Customer Name", "Line Type",
            "Item Name", "Quantity", "Rate (₹)", "Line Total (₹)",
            "Bill Subtotal (₹)", "Discount (₹)", "Payable (₹)", "Paid (₹)",
            "Balance Due (₹)", "Payment Mode", "Status"
        )
        createHeaderRow(billsSheet, 0, billHeaders)
        val customerMap = data.customers.associateBy { it.id }
        val saleLinesGrouped = data.saleLines.groupBy { it.saleId }

        var billRowIdx = 1
        data.sales.forEach { sale ->
            val lines = saleLinesGrouped[sale.id] ?: emptyList()
            val custName = sale.customerName ?: sale.customerId?.let { customerMap[it]?.name } ?: "Walk-in Customer"
            val status = if (sale.isVoided) "CANCELLED" else "COMPLETED"
            val balanceDue = sale.remainingCreditPaise

            if (lines.isEmpty()) {
                val row = billsSheet.createRow(billRowIdx++)
                writeString(row, 0, sale.receiptNumber)
                writeString(row, 1, dateFormat.format(Date(sale.timestamp)))
                writeString(row, 2, custName)
                writeString(row, 3, "GENERAL")
                writeString(row, 4, "-")
                writeInt(row, 5, 0)
                writeNumber(row, 6, 0.0)
                writeNumber(row, 7, 0.0)
                writeNumber(row, 8, sale.grossTotalPaise / 100.0)
                writeNumber(row, 9, sale.discountValuePaise / 100.0)
                writeNumber(row, 10, sale.netTotalPaise / 100.0)
                writeNumber(row, 11, sale.amountReceivedPaise / 100.0)
                writeNumber(row, 12, balanceDue / 100.0)
                writeString(row, 13, sale.paymentMethod)
                writeString(row, 14, status)
            } else {
                lines.forEach { line ->
                    val row = billsSheet.createRow(billRowIdx++)
                    writeString(row, 0, sale.receiptNumber)
                    writeString(row, 1, dateFormat.format(Date(sale.timestamp)))
                    writeString(row, 2, custName)
                    writeString(row, 3, line.lineType)
                    writeString(row, 4, line.productName)
                    writeInt(row, 5, line.quantityBase)
                    writeNumber(row, 6, line.unitSellingPricePaise / 100.0)
                    writeNumber(row, 7, line.lineNetPaise / 100.0)
                    writeNumber(row, 8, sale.grossTotalPaise / 100.0)
                    writeNumber(row, 9, sale.discountValuePaise / 100.0)
                    writeNumber(row, 10, sale.netTotalPaise / 100.0)
                    writeNumber(row, 11, sale.amountReceivedPaise / 100.0)
                    writeNumber(row, 12, balanceDue / 100.0)
                    writeString(row, 13, sale.paymentMethod)
                    writeString(row, 14, status)
                }
            }
        }

        // ══════════════════════════════════════════════════════════════
        // SHEET 5: Payments / Allocations
        // ══════════════════════════════════════════════════════════════
        val paymentsSheet = workbook.createSheet("Payments and Dues")
        val paymentHeaders = listOf(
            "Payment ID", "Date & Time", "Party Type", "Party Name",
            "Amount (₹)", "Payment Mode", "Notes / Reference"
        )
        createHeaderRow(paymentsSheet, 0, paymentHeaders)
        val supplierMap = data.suppliers.associateBy { it.id }
        data.accountPayments.forEachIndexed { idx, p ->
            val row = paymentsSheet.createRow(idx + 1)
            val partyName = if (p.accountType == "CUSTOMER") {
                customerMap[p.accountId]?.name ?: p.accountId
            } else {
                supplierMap[p.accountId]?.name ?: p.accountId
            }
            writeString(row, 0, p.id)
            writeString(row, 1, dateFormat.format(Date(p.timestamp)))
            writeString(row, 2, p.accountType)
            writeString(row, 3, partyName)
            writeNumber(row, 4, p.amountPaise / 100.0)
            writeString(row, 5, p.paymentMethod)
            writeString(row, 6, p.notes ?: "")
        }

        // ══════════════════════════════════════════════════════════════
        // SHEET 6: Customers & Suppliers
        // ══════════════════════════════════════════════════════════════
        val partiesSheet = workbook.createSheet("Customers and Suppliers")
        val partyHeaders = listOf(
            "Account Type", "Party ID", "Name", "Phone", "Outstanding Balance (₹)", "Status"
        )
        createHeaderRow(partiesSheet, 0, partyHeaders)
        var pRowIdx = 1
        data.customers.forEach { c ->
            val row = partiesSheet.createRow(pRowIdx++)
            writeString(row, 0, "CUSTOMER")
            writeString(row, 1, c.id)
            writeString(row, 2, c.name)
            writeString(row, 3, c.phone ?: "")
            writeNumber(row, 4, c.currentBalancePaise / 100.0)
            writeString(row, 5, if (c.isArchived) "Archived" else "Active")
        }
        data.suppliers.forEach { s ->
            val row = partiesSheet.createRow(pRowIdx++)
            writeString(row, 0, "SUPPLIER")
            writeString(row, 1, s.id)
            writeString(row, 2, s.name)
            writeString(row, 3, s.phone ?: "")
            writeNumber(row, 4, s.currentBalancePaise / 100.0)
            writeString(row, 5, if (s.isArchived) "Archived" else "Active")
        }

        // ══════════════════════════════════════════════════════════════
        // SHEET 7: Cash In Out
        // ══════════════════════════════════════════════════════════════
        val cashSheet = workbook.createSheet("Cash In Out")
        val cashHeaders = listOf(
            "Entry ID", "Date & Time", "Direction", "Category",
            "Notes", "Amount (₹)", "Payment Method", "Linked ID", "Status"
        )
        createHeaderRow(cashSheet, 0, cashHeaders)
        data.cashExpenses.forEachIndexed { idx, ce ->
            val row = cashSheet.createRow(idx + 1)
            writeString(row, 0, ce.id)
            writeString(row, 1, dateFormat.format(Date(ce.timestamp)))
            writeString(row, 2, if (ce.isIncome) "CASH IN" else "CASH OUT")
            writeString(row, 3, ce.category)
            writeString(row, 4, ce.notes ?: "")
            writeNumber(row, 5, ce.amountPaise / 100.0)
            writeString(row, 6, ce.paymentMethod)
            writeString(row, 7, ce.linkedExpenseId ?: "")
            writeString(row, 8, if (ce.isVoided) "CANCELLED" else "ACTIVE")
        }

        // ══════════════════════════════════════════════════════════════
        // SHEET 8: Expenses / Refunds
        // ══════════════════════════════════════════════════════════════
        val expSheet = workbook.createSheet("Expenses and Refunds")
        val expHeaders = listOf(
            "ID", "Date & Time", "Classification", "Category",
            "Amount (₹)", "Notes", "Linked Refund ID", "Has Attachment"
        )
        createHeaderRow(expSheet, 0, expHeaders)
        val actualExpenses = data.cashExpenses.filter { !it.isVoided && !it.isSavingsTransfer && (!it.isIncome || it.isExpenseRefund) }
        actualExpenses.forEachIndexed { idx, ex ->
            val row = expSheet.createRow(idx + 1)
            val classification = if (ex.isSavingsTransfer) "SAVINGS_TRANSFER" else ex.classification
            writeString(row, 0, ex.id)
            writeString(row, 1, dateFormat.format(Date(ex.timestamp)))
            writeString(row, 2, classification)
            writeString(row, 3, ex.category)
            writeNumber(row, 4, (if (ex.isExpenseRefund) -ex.amountPaise else ex.amountPaise) / 100.0)
            writeString(row, 5, ex.notes ?: "")
            writeString(row, 6, ex.linkedExpenseId ?: "")
            writeString(row, 7, if (!ex.attachmentUri.isNullOrBlank()) "YES" else "NO")
        }

        // ══════════════════════════════════════════════════════════════
        // SHEET 9: Services
        // ══════════════════════════════════════════════════════════════
        val svcSheet = workbook.createSheet("Services")
        val svcHeaders = listOf("Service ID", "Service Name", "Unit Label", "Predefined", "Status")
        createHeaderRow(svcSheet, 0, svcHeaders)
        data.services.forEachIndexed { idx, svc ->
            val row = svcSheet.createRow(idx + 1)
            writeString(row, 0, svc.id)
            writeString(row, 1, svc.name)
            writeString(row, 2, svc.unitLabel)
            writeString(row, 3, if (svc.isPredefined) "YES" else "NO")
            writeString(row, 4, if (svc.isArchived) "Archived" else "Active")
        }

        // ══════════════════════════════════════════════════════════════
        // SHEET 10: Pigmy Daily Savings
        // ══════════════════════════════════════════════════════════════
        val pigmySheet = workbook.createSheet("Pigmy Savings")
        val pigmyHeaders = listOf(
            "Scheme Name", "Transaction Type", "Date & Time", "Amount (₹)",
            "Payment Source / Destination", "Notes", "Linked ID"
        )
        createHeaderRow(pigmySheet, 0, pigmyHeaders)
        val schemeMap = data.pigmySchemes.associateBy { it.id }

        var pigmyRowIdx = 1
        data.pigmyDeposits.forEach { dep ->
            val row = pigmySheet.createRow(pigmyRowIdx++)
            val schemeName = schemeMap[dep.accountSchemeId]?.name ?: dep.accountSchemeId
            writeString(row, 0, schemeName)
            writeString(row, 1, "DEPOSIT")
            writeString(row, 2, dateFormat.format(Date(dep.paidTimestamp)))
            writeNumber(row, 3, dep.amountPaise / 100.0)
            writeString(row, 4, dep.paymentSource)
            writeString(row, 5, dep.notes ?: "")
            writeString(row, 6, dep.linkedExpenseId ?: "")
        }
        data.pigmyWithdrawals.forEach { w ->
            val row = pigmySheet.createRow(pigmyRowIdx++)
            val schemeName = schemeMap[w.accountSchemeId]?.name ?: w.accountSchemeId
            writeString(row, 0, schemeName)
            writeString(row, 1, "WITHDRAWAL")
            writeString(row, 2, dateFormat.format(Date(w.withdrawalTimestamp)))
            writeNumber(row, 3, w.amountPaise / 100.0)
            writeString(row, 4, w.destination)
            writeString(row, 5, w.notes ?: "")
            writeString(row, 6, w.linkedExpenseId ?: "")
        }

        // ══════════════════════════════════════════════════════════════
        // SHEET 11: Categories
        // ══════════════════════════════════════════════════════════════
        val catSheet = workbook.createSheet("Categories")
        val catHeaders = listOf("Category Type", "Name", "System Category", "Status")
        createHeaderRow(catSheet, 0, catHeaders)
        var catRowIdx = 1
        data.cashCategories.forEach { cc ->
            val row = catSheet.createRow(catRowIdx++)
            writeString(row, 0, "CASH_CATEGORY")
            writeString(row, 1, cc.name)
            writeString(row, 2, if (cc.isSystem) "YES" else "NO")
            writeString(row, 3, if (cc.isArchived) "Archived" else "Active")
        }
        val distinctProductCategories = data.products.map { it.category }.distinct()
        distinctProductCategories.forEach { pc ->
            val row = catSheet.createRow(catRowIdx++)
            writeString(row, 0, "PRODUCT_CATALOG")
            writeString(row, 1, pc)
            writeString(row, 2, "NO")
            writeString(row, 3, "Active")
        }

        // Set standard readable column widths across all sheets (Android-safe without java.awt)
        for (i in 0 until workbook.numberOfSheets) {
            val sheet = workbook.getSheetAt(i)
            val lastCol = sheet.getRow(0)?.lastCellNum?.toInt() ?: 0
            for (col in 0 until lastCol) {
                try {
                    sheet.setColumnWidth(col, 20 * 256)
                } catch (_: Throwable) {
                    // Ignore width adjustment failures on constrained runtimes
                }
            }
        }

        // Write to output file
        FileOutputStream(outputFile).use { out ->
            workbook.write(out)
        }
        workbook.close()
    }
}
