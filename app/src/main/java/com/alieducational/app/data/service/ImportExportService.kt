package com.alieducational.app.data.service

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.alieducational.app.data.local.AppDatabase
import com.alieducational.app.data.local.dao.ImportBatchDao
import com.alieducational.app.data.local.dao.ProductBarcodeDao
import com.alieducational.app.data.local.dao.ProductDao
import com.alieducational.app.data.local.dao.StockMovementDao
import com.alieducational.app.data.local.entity.ImportBatchEntity
import com.alieducational.app.data.local.entity.ProductBarcodeEntity
import com.alieducational.app.data.local.entity.ProductEntity
import com.alieducational.app.data.local.entity.StockMovementEntity
import com.alieducational.app.data.preferences.AppPreferences
import com.alieducational.app.data.service.`import`.CellReader
import com.alieducational.app.data.service.`import`.CsvReader
import com.alieducational.app.data.service.`import`.ImportParsers
import com.alieducational.app.data.service.`import`.Parsed
import com.alieducational.app.domain.calculation.CostCalculator
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.apache.poi.ss.usermodel.DataFormatter
import org.apache.poi.ss.usermodel.WorkbookFactory
import java.io.InputStream
import java.security.MessageDigest
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

data class SpreadsheetPreview(
    val fileName: String,
    val sheetNames: List<String> = emptyList(),
    val selectedSheetIndex: Int = 0,
    val headers: List<String> = emptyList(),
    val previewRows: List<List<String>> = emptyList(),
    val totalEstimatedRows: Int = 0,
    val errorMessage: String? = null,
    val isCashLedger: Boolean = false,
    val headerRowIndex: Int = 0
)

data class ColumnMapping(
    val nameIndex: Int = -1,
    val categoryIndex: Int = -1,
    val brandIndex: Int = -1,
    val skuIndex: Int = -1,
    val barcodeIndex: Int = -1,
    val baseUnitIndex: Int = -1,
    val packSizeIndex: Int = -1,
    val openingQtyIndex: Int = -1,
    val purchaseCostIndex: Int = -1,
    val sellingPricePieceIndex: Int = -1,
    val sellingPricePackIndex: Int = -1,
    val lowStockIndex: Int = -1,
    val headerRowIndex: Int = 0,
    val wholesalePriceIndex: Int = -1,
    val boxesBroughtIndex: Int = -1
)

enum class RowStatus { NEW, UPDATE, SKIP_DUPLICATE, ERROR }
enum class Severity { ERROR, WARNING }
data class RowIssue(val severity: Severity, val field: String, val message: String)

data class ParsedProductData(
    val name: String,
    val category: String,
    val brand: String?,
    val sku: String?,
    val barcode: String?,
    val baseUnit: String,
    val packSize: Int,
    val quantity: Int,
    val costPaise: Long,
    val sellingPiecePaise: Long,
    val sellingPackPaise: Long,
    val wholesalePaise: Long,
    val lowStock: Int
)

data class PlannedRow(
    val rowNumber: Int,
    val status: RowStatus,
    val issues: List<RowIssue>,
    val matchedProductId: String?,
    val matchedBy: String?, // "SKU" | "Barcode" | "Name"
    val parsedData: ParsedProductData? = null
)

enum class ImportMode {
    NEW_ONLY,
    ADD_STOCK,
    SET_STOCK,
    PRICES_ONLY,
    DETAILS_ONLY
}

data class ImportResult(
    val totalRead: Int,
    val importedCount: Int,
    val skippedDuplicatesCount: Int,
    val updatedCount: Int = 0,
    val updatedDetails: List<String> = emptyList(),
    val skippedDetails: List<String> = emptyList(),
    val errors: List<String> = emptyList(),
    val isSuccess: Boolean = true,
    val failureReason: String? = null,
    val plannedRows: List<PlannedRow> = emptyList(),
    val batchId: String? = null
)

@Singleton
class ImportExportService @Inject constructor(
    @ApplicationContext private val context: Context?,
    private val productDao: ProductDao?,
    private val movementDao: StockMovementDao?,
    private val importBatchDao: ImportBatchDao?,
    private val database: AppDatabase? = null,
    private val preferences: AppPreferences? = null,
    private val productBarcodeDao: ProductBarcodeDao? = null
) {
    constructor() : this(null, null, null, null, null, null, null)

    private val effectiveBarcodeDao: ProductBarcodeDao?
        get() = productBarcodeDao ?: database?.productBarcodeDao()

    suspend fun previewFile(
        uri: Uri,
        fileName: String,
        selectedSheetIndex: Int = 0
    ): SpreadsheetPreview = withContext(Dispatchers.IO) {
        try {
            val pfd = try { context?.contentResolver?.openFileDescriptor(uri, "r") } catch (_: Exception) { null }
            val fileSize = pfd?.statSize ?: 0L
            pfd?.close()
            if (fileSize > 10 * 1024 * 1024) {
                return@withContext SpreadsheetPreview(
                    fileName = fileName,
                    errorMessage = "File exceeds the 10 MB limit (${fileSize / (1024 * 1024)} MB). Please split into smaller files."
                )
            }

            val stream = context?.contentResolver?.openInputStream(uri)
                ?: return@withContext SpreadsheetPreview(
                    fileName = fileName,
                    errorMessage = "Cannot access the selected file. Please select the file again."
                )

            if (fileName.endsWith(".csv", ignoreCase = true)) {
                previewCsv(stream, fileName)
            } else {
                previewExcel(stream, fileName, selectedSheetIndex)
            }
        } catch (e: Exception) {
            val friendlyMsg = mapExceptionToMessage(e)
            SpreadsheetPreview(
                fileName = fileName,
                errorMessage = friendlyMsg
            )
        }
    }

    private fun mapExceptionToMessage(e: Exception): String {
        val msg = e.localizedMessage ?: e.javaClass.simpleName
        return when {
            msg.contains("password", ignoreCase = true) || msg.contains("Encrypted", ignoreCase = true) ->
                "This file is password-protected. Please open it on your computer or phone, remove the password, and try again."
            msg.contains("NotOfficeXmlFileException", ignoreCase = true) || msg.contains("NotOLE2FileException", ignoreCase = true) ->
                "The file format is not a recognized Excel spreadsheet. Please ensure it is a valid .xlsx, .xls, or .csv file."
            msg.contains("EmptyFileException", ignoreCase = true) || msg.contains("empty", ignoreCase = true) ->
                "The selected spreadsheet is completely empty. Please select a file that contains column headings and product rows."
            else ->
                "Could not read spreadsheet: $msg. Please verify the file is not corrupted."
        }
    }

    private fun isCashLedgerHeaders(headers: List<String>, sheetOrFileName: String = ""): Boolean {
        val hLower = headers.map { it.trim().lowercase(Locale.ROOT) }
        val hasIncomeOrExpense = hLower.any {
            it.contains("income") || it.contains("expense") || it.contains("cash in") ||
                    it.contains("cash out") || it.contains("credit") || it.contains("debit")
        }
        val hasBalanceOrDate = hLower.any {
            it.contains("balance") || it.contains("notes") || it.contains("particular") || it.contains("date")
        }
        val isTransactionsContext = sheetOrFileName.contains("transaction", ignoreCase = true) ||
                sheetOrFileName.contains("cash", ignoreCase = true)
        return (hasIncomeOrExpense && (hasBalanceOrDate || isTransactionsContext)) || (isTransactionsContext && hasIncomeOrExpense)
    }

    private fun previewCsv(stream: InputStream, fileName: String): SpreadsheetPreview {
        val rows = try {
            CsvReader.read(stream, maxRows = 25)
        } catch (e: Exception) {
            return SpreadsheetPreview(fileName = fileName, errorMessage = e.message ?: "Failed to read CSV")
        } finally {
            stream.close()
        }

        if (rows.isEmpty()) {
            return SpreadsheetPreview(
                fileName = fileName,
                errorMessage = "CSV file is empty. Please provide a CSV with header columns (e.g. Name, Category, Price, Stock)."
            )
        }

        var headerIndex = 0
        for (i in 0 until minOf(5, rows.size)) {
            if (rows[i].count { it.isNotBlank() } >= 2) {
                headerIndex = i
                break
            }
        }

        val headers = rows[headerIndex]
        val preview = if (rows.size > headerIndex + 1) rows.subList(headerIndex + 1, rows.size) else emptyList()

        if (isCashLedgerHeaders(headers, fileName)) {
            return SpreadsheetPreview(
                fileName = fileName,
                sheetNames = listOf("CSV Data"),
                selectedSheetIndex = 0,
                headers = headers,
                previewRows = preview,
                totalEstimatedRows = rows.size,
                isCashLedger = true,
                errorMessage = "This file contains Cash In/Out transactions, not product stock. Import it from Cash In/Out Tracker.",
                headerRowIndex = headerIndex
            )
        }

        return SpreadsheetPreview(
            fileName = fileName,
            sheetNames = listOf("CSV Data"),
            selectedSheetIndex = 0,
            headers = headers,
            previewRows = preview,
            totalEstimatedRows = rows.size,
            headerRowIndex = headerIndex
        )
    }

    internal fun previewExcel(stream: InputStream, fileName: String, sheetIndex: Int): SpreadsheetPreview {
        val workbook = WorkbookFactory.create(stream)
        val numSheets = workbook.numberOfSheets
        if (numSheets == 0) {
            workbook.close()
            stream.close()
            return SpreadsheetPreview(fileName = fileName, errorMessage = "Excel workbook contains no worksheets.")
        }

        val sheetNames = (0 until numSheets).map { workbook.getSheetName(it) }
        val activeSheetIndex = if (sheetIndex in 0 until numSheets) sheetIndex else 0
        val sheet = workbook.getSheetAt(activeSheetIndex)

        if (sheet.physicalNumberOfRows > 20000 || sheet.lastRowNum > 20000) {
            val rowCount = maxOf(sheet.physicalNumberOfRows, sheet.lastRowNum)
            workbook.close()
            stream.close()
            return SpreadsheetPreview(
                fileName = fileName,
                sheetNames = sheetNames,
                selectedSheetIndex = activeSheetIndex,
                errorMessage = "Excel sheet exceeds the 20,000 row limit ($rowCount rows). Please split into smaller files."
            )
        }

        val evaluator = workbook.creationHelper.createFormulaEvaluator()
        val formatter = DataFormatter()
        val allRows = mutableListOf<Pair<Int, List<String>>>()
        val lastRow = sheet.lastRowNum
        for (i in 0..minOf(lastRow, 25)) {
            val row = sheet.getRow(i) ?: continue
            val rowValues = CellReader.readRow(row, evaluator, formatter)
            if (rowValues.any { it.isNotBlank() }) {
                allRows.add(Pair(i, rowValues))
            }
        }
        workbook.close()
        stream.close()

        if (allRows.isEmpty()) {
            return SpreadsheetPreview(
                fileName = fileName,
                sheetNames = sheetNames,
                selectedSheetIndex = activeSheetIndex,
                errorMessage = "Sheet '${sheetNames[activeSheetIndex]}' contains no data rows."
            )
        }

        var bestRowIdx = 0
        var maxColCount = 0
        for (idx in 0 until minOf(allRows.size, 10)) {
            val cols = allRows[idx].second
            val nonBlank = cols.count { it.isNotBlank() }
            val hasKeywords = cols.any { c ->
                val l = c.lowercase(Locale.ROOT)
                l.contains("name") || l.contains("product") || l.contains("item") ||
                        l.contains("price") || l.contains("cost") || l.contains("stock") ||
                        l.contains("sku") || l.contains("barcode") || l.contains("income") ||
                        l.contains("expense") || l.contains("date")
            }
            if (hasKeywords && nonBlank >= 2) {
                bestRowIdx = idx
                break
            }
            if (nonBlank > maxColCount) {
                maxColCount = nonBlank
                bestRowIdx = idx
            }
        }

        val headers = allRows[bestRowIdx].second
        val detectedHeaderRowIndex = allRows[bestRowIdx].first
        val preview = if (allRows.size > bestRowIdx + 1) {
            allRows.subList(bestRowIdx + 1, allRows.size).map { it.second }
        } else emptyList()

        if (isCashLedgerHeaders(headers, sheetNames[activeSheetIndex]) || isCashLedgerHeaders(headers, fileName)) {
            return SpreadsheetPreview(
                fileName = fileName,
                sheetNames = sheetNames,
                selectedSheetIndex = activeSheetIndex,
                headers = headers,
                previewRows = preview,
                totalEstimatedRows = lastRow + 1,
                isCashLedger = true,
                errorMessage = "This file contains Cash In/Out transactions, not product stock. Import it from Cash In/Out Tracker.",
                headerRowIndex = detectedHeaderRowIndex
            )
        }

        return SpreadsheetPreview(
            fileName = fileName,
            sheetNames = sheetNames,
            selectedSheetIndex = activeSheetIndex,
            headers = headers,
            previewRows = preview,
            totalEstimatedRows = lastRow + 1,
            headerRowIndex = detectedHeaderRowIndex
        )
    }

    suspend fun readRows(uri: Uri, fileName: String, sheetIndex: Int): List<List<String>> = withContext(Dispatchers.IO) {
        val stream = context?.contentResolver?.openInputStream(uri)
            ?: throw IllegalArgumentException("Cannot open file: $fileName")
        if (fileName.endsWith(".csv", ignoreCase = true)) {
            readAllCsv(stream)
        } else {
            readAllExcel(stream, sheetIndex)
        }
    }

    /**
     * Stage 3: Pure planning of the import (NO database writes).
     * Validates fields, checks for duplicates inside the file, matches existing DB products,
     * and produces a PlannedRow for every row.
     */
    suspend fun planImport(
        rows: List<List<String>>,
        mapping: ColumnMapping,
        mode: ImportMode = ImportMode.ADD_STOCK
    ): List<PlannedRow> = withContext(Dispatchers.IO) {
        val pDao = productDao
        val bDao = effectiveBarcodeDao

        val existingProducts = pDao?.getAllProducts()?.first() ?: emptyList()
        val productsBySku = mutableMapOf<String, ProductEntity>()
        val productsByBarcode = mutableMapOf<String, ProductEntity>()
        val productsByName = mutableMapOf<String, ProductEntity>()

        for (p in existingProducts) {
            if (!p.sku.isNullOrBlank()) productsBySku[p.sku] = p
            if (!p.barcode.isNullOrBlank()) productsByBarcode[p.barcode] = p
            if (p.name.isNotBlank()) productsByName[p.name.lowercase(Locale.ROOT)] = p
        }

        val seenSkusInFile = mutableMapOf<String, Int>()
        val seenBarcodesInFile = mutableMapOf<String, Int>()
        val seenNamesInFile = mutableMapOf<String, Int>()

        val planned = mutableListOf<PlannedRow>()
        val startRowIdx = (mapping.headerRowIndex + 1).coerceIn(0, rows.size)

        for (i in startRowIdx until rows.size) {
            val row = rows[i]
            val fileRowNum = i + 1

            if (row.all { it.isBlank() }) continue

            val issues = mutableListOf<RowIssue>()

            val nameRaw = if (mapping.nameIndex in row.indices) row[mapping.nameIndex].trim() else ""
            if (nameRaw.isBlank()) {
                issues.add(RowIssue(Severity.ERROR, "name", "Product name cannot be empty"))
            }

            val skuRaw = if (mapping.skuIndex in row.indices) row[mapping.skuIndex].trim().ifBlank { null } else null

            val barcodeRaw = if (mapping.barcodeIndex in row.indices) row[mapping.barcodeIndex] else null
            val barcode = when (val res = ImportParsers.parseBarcode(barcodeRaw)) {
                is Parsed.Ok -> res.v
                is Parsed.Err -> {
                    issues.add(RowIssue(Severity.ERROR, "barcode", res.msg))
                    null
                }
            }

            val category = if (mapping.categoryIndex in row.indices) row[mapping.categoryIndex].trim().ifBlank { "General" } else "General"
            val brand = if (mapping.brandIndex in row.indices) row[mapping.brandIndex].trim().ifBlank { null } else null
            val baseUnit = if (mapping.baseUnitIndex in row.indices) row[mapping.baseUnitIndex].trim().ifBlank { "piece" } else "piece"

            val packSize = when (val res = ImportParsers.parsePackSize(if (mapping.packSizeIndex in row.indices) row[mapping.packSizeIndex] else null)) {
                is Parsed.Ok -> res.v
                is Parsed.Err -> {
                    issues.add(RowIssue(Severity.ERROR, "packSize", res.msg))
                    1
                }
            }

            val openingQty = when (val res = ImportParsers.parseQty(if (mapping.openingQtyIndex in row.indices) row[mapping.openingQtyIndex] else null, defaultIfEmpty = 0)) {
                is Parsed.Ok -> res.v
                is Parsed.Err -> {
                    issues.add(RowIssue(Severity.ERROR, "quantity", res.msg))
                    0
                }
            }

            val costPaise = when (val res = ImportParsers.parseMoney(if (mapping.purchaseCostIndex in row.indices) row[mapping.purchaseCostIndex] else null, defaultIfEmpty = 0L)) {
                is Parsed.Ok -> res.v
                is Parsed.Err -> {
                    issues.add(RowIssue(Severity.ERROR, "purchaseCost", res.msg))
                    0L
                }
            }

            val sellingPiecePaise = when (val res = ImportParsers.parseMoney(if (mapping.sellingPricePieceIndex in row.indices) row[mapping.sellingPricePieceIndex] else null, defaultIfEmpty = 0L)) {
                is Parsed.Ok -> res.v
                is Parsed.Err -> {
                    issues.add(RowIssue(Severity.ERROR, "sellingPricePiece", res.msg))
                    0L
                }
            }

            val sellingPackDefault = if (sellingPiecePaise > 0L) sellingPiecePaise * packSize else 0L
            val sellingPackPaise = when (val res = ImportParsers.parseMoney(if (mapping.sellingPricePackIndex in row.indices) row[mapping.sellingPricePackIndex] else null, defaultIfEmpty = sellingPackDefault)) {
                is Parsed.Ok -> res.v
                is Parsed.Err -> {
                    issues.add(RowIssue(Severity.ERROR, "sellingPricePack", res.msg))
                    sellingPackDefault
                }
            }

            val wholesalePaise = when (val res = ImportParsers.parseMoney(if (mapping.wholesalePriceIndex in row.indices) row[mapping.wholesalePriceIndex] else null, defaultIfEmpty = 0L)) {
                is Parsed.Ok -> res.v
                is Parsed.Err -> {
                    issues.add(RowIssue(Severity.ERROR, "wholesalePrice", res.msg))
                    0L
                }
            }

            val lowStock = when (val res = ImportParsers.parseQty(if (mapping.lowStockIndex in row.indices) row[mapping.lowStockIndex] else null, defaultIfEmpty = 5)) {
                is Parsed.Ok -> res.v
                is Parsed.Err -> {
                    issues.add(RowIssue(Severity.WARNING, "lowStock", res.msg))
                    5
                }
            }

            if (sellingPiecePaise > 0L && costPaise > 0L && sellingPiecePaise < costPaise) {
                issues.add(RowIssue(Severity.WARNING, "sellingPricePiece", "Selling price (₹${sellingPiecePaise / 100.0}) is below cost (₹${costPaise / 100.0})"))
            }

            // In-file duplicate detection
            if (!barcode.isNullOrBlank()) {
                val prev = seenBarcodesInFile[barcode]
                if (prev != null) {
                    issues.add(RowIssue(Severity.ERROR, "barcode", "Duplicate barcode '$barcode' inside file (same as row $prev)"))
                } else {
                    seenBarcodesInFile[barcode] = fileRowNum
                }
            }

            if (!skuRaw.isNullOrBlank()) {
                val prev = seenSkusInFile[skuRaw]
                if (prev != null) {
                    issues.add(RowIssue(Severity.ERROR, "sku", "Duplicate SKU '$skuRaw' inside file (same as row $prev)"))
                } else {
                    seenSkusInFile[skuRaw] = fileRowNum
                }
            }

            if (nameRaw.isNotBlank()) {
                val nameKey = nameRaw.lowercase(Locale.ROOT)
                val prev = seenNamesInFile[nameKey]
                if (prev != null && skuRaw == null && barcode == null) {
                    issues.add(RowIssue(Severity.WARNING, "name", "Duplicate name '$nameRaw' inside file (same as row $prev)"))
                } else if (prev == null) {
                    seenNamesInFile[nameKey] = fileRowNum
                }
            }

            // Matching against database: Priority: SKU -> Barcode -> Name
            var matchedProduct: ProductEntity? = null
            var matchedBy: String? = null

            if (!skuRaw.isNullOrBlank() && productsBySku.containsKey(skuRaw)) {
                matchedProduct = productsBySku[skuRaw]
                matchedBy = "SKU"
            } else if (!barcode.isNullOrBlank()) {
                if (productsByBarcode.containsKey(barcode)) {
                    matchedProduct = productsByBarcode[barcode]
                    matchedBy = "Barcode"
                } else {
                    // Check auxiliary barcode table if available
                    val barcodeRecord = bDao?.findByBarcode(barcode)
                    if (barcodeRecord != null) {
                        matchedProduct = existingProducts.firstOrNull { it.id == barcodeRecord.productId }
                        if (matchedProduct != null) matchedBy = "Barcode"
                    }
                }
            }

            if (matchedProduct == null && nameRaw.isNotBlank()) {
                val nameKey = nameRaw.lowercase(Locale.ROOT)
                if (productsByName.containsKey(nameKey)) {
                    matchedProduct = productsByName[nameKey]
                    matchedBy = "Name"
                }
            }

            // Check if barcode conflicts with a DIFFERENT product
            if (!barcode.isNullOrBlank()) {
                val existingBarcodeProd = productsByBarcode[barcode]
                    ?: bDao?.findByBarcode(barcode)?.let { rec -> existingProducts.firstOrNull { it.id == rec.productId } }
                if (existingBarcodeProd != null && matchedProduct != null && existingBarcodeProd.id != matchedProduct.id) {
                    issues.add(RowIssue(Severity.ERROR, "barcode", "Barcode '$barcode' is already assigned to '${existingBarcodeProd.name}'"))
                }
            }

            val hasErrors = issues.any { it.severity == Severity.ERROR }
            val status = when {
                hasErrors -> RowStatus.ERROR
                matchedProduct == null -> RowStatus.NEW
                mode == ImportMode.NEW_ONLY -> RowStatus.SKIP_DUPLICATE
                else -> RowStatus.UPDATE
            }

            val parsedData = ParsedProductData(
                name = nameRaw,
                category = category,
                brand = brand,
                sku = skuRaw,
                barcode = barcode,
                baseUnit = baseUnit,
                packSize = packSize,
                quantity = openingQty,
                costPaise = costPaise,
                sellingPiecePaise = sellingPiecePaise,
                sellingPackPaise = sellingPackPaise,
                wholesalePaise = wholesalePaise,
                lowStock = lowStock
            )

            planned.add(
                PlannedRow(
                    rowNumber = fileRowNum,
                    status = status,
                    issues = issues,
                    matchedProductId = matchedProduct?.id,
                    matchedBy = matchedBy,
                    parsedData = parsedData
                )
            )
        }

        planned
    }

    /**
     * Stage 4: Commit import plan in one single database transaction.
     */
    suspend fun commit(
        plan: List<PlannedRow>,
        mode: ImportMode = ImportMode.ADD_STOCK,
        validRowsOnly: Boolean = false,
        fileName: String = "import.xlsx",
        mapping: ColumnMapping? = null
    ): ImportResult = withContext(Dispatchers.IO) {
        val pDao = productDao ?: return@withContext ImportResult(0, 0, 0, isSuccess = false, failureReason = "Database not initialized")
        val mDao = movementDao ?: return@withContext ImportResult(0, 0, 0, isSuccess = false, failureReason = "Database not initialized")
        val bDao = importBatchDao ?: return@withContext ImportResult(0, 0, 0, isSuccess = false, failureReason = "Database not initialized")
        val barcodeDao = effectiveBarcodeDao

        // If not validRowsOnly, any error rolls back the whole batch
        val errorRows = plan.filter { it.status == RowStatus.ERROR }
        if (!validRowsOnly && errorRows.isNotEmpty()) {
            val errorMessages = errorRows.flatMap { row ->
                row.issues.filter { it.severity == Severity.ERROR }.map { "Row ${row.rowNumber}: ${it.message}" }
            }
            return@withContext ImportResult(
                totalRead = plan.size,
                importedCount = 0,
                skippedDuplicatesCount = 0,
                errors = errorMessages,
                isSuccess = false,
                failureReason = "Import rolled back: ${errorMessages.take(5).joinToString("; ")}",
                plannedRows = plan
            )
        }

        val rowsToProcess = plan.filter { row ->
            if (validRowsOnly) row.status == RowStatus.NEW || row.status == RowStatus.UPDATE
            else row.status != RowStatus.ERROR
        }

        val batchId = UUID.randomUUID().toString()
        var importedCount = 0
        var updatedCount = 0
        var skippedCount = plan.count { it.status == RowStatus.SKIP_DUPLICATE }
        if (validRowsOnly) {
            skippedCount += errorRows.size
        }

        val updatedDetails = mutableListOf<String>()
        val skippedDetails = mutableListOf<String>()
        val errors = mutableListOf<String>()

        for (errRow in errorRows) {
            errors.add("Row ${errRow.rowNumber}: ${errRow.issues.joinToString(", ") { it.message }}")
        }

        val block = suspend {
            for (item in rowsToProcess) {
                val data = item.parsedData ?: continue

                when (item.status) {
                    RowStatus.NEW -> {
                        val product = ProductEntity(
                            name = data.name,
                            category = data.category,
                            brand = data.brand,
                            sku = data.sku,
                            barcode = data.barcode,
                            baseUnit = data.baseUnit,
                            packSize = data.packSize,
                            currentSellableStock = if (mode == ImportMode.PRICES_ONLY || mode == ImportMode.DETAILS_ONLY) 0 else data.quantity,
                            movingAvgCostPaise = data.costPaise,
                            sellingPricePiecePaise = data.sellingPiecePaise,
                            sellingPricePackPaise = data.sellingPackPaise,
                            wholesalePricePiecePaise = data.wholesalePaise,
                            lowStockThreshold = data.lowStock
                        )
                        pDao.insertProduct(product)

                        // Register barcode in product_barcodes table
                        if (!data.barcode.isNullOrBlank() && barcodeDao != null) {
                            try {
                                barcodeDao.insertBarcode(
                                    ProductBarcodeEntity(
                                        barcode = data.barcode,
                                        productId = product.id,
                                        unitType = data.baseUnit.uppercase(Locale.ROOT),
                                        packQuantity = data.packSize
                                    )
                                )
                            } catch (_: Exception) {}
                        }

                        // Write opening stock movement
                        if (product.currentSellableStock > 0) {
                            mDao.insertMovement(
                                StockMovementEntity(
                                    productId = product.id,
                                    type = "OPENING_STOCK",
                                    quantityChangeBase = product.currentSellableStock,
                                    unitCostPaise = data.costPaise,
                                    reason = "Catalog Import: $fileName",
                                    referenceId = batchId
                                )
                            )
                        }
                        importedCount++
                    }

                    RowStatus.UPDATE -> {
                        val existing = item.matchedProductId?.let { pDao.getProductById(it) } ?: continue
                        when (mode) {
                            ImportMode.NEW_ONLY -> {
                                skippedCount++
                            }
                            ImportMode.ADD_STOCK -> {
                                val newQty = existing.currentSellableStock + data.quantity
                                val newCost = if (data.costPaise > 0L) {
                                    if (existing.currentSellableStock > 0 && data.quantity > 0) {
                                        val totalVal = (existing.currentSellableStock.toLong() * existing.movingAvgCostPaise) + (data.quantity.toLong() * data.costPaise)
                                        (totalVal / newQty.coerceAtLeast(1)).coerceAtLeast(0L)
                                    } else {
                                        data.costPaise
                                    }
                                } else {
                                    existing.movingAvgCostPaise
                                }

                                val updated = existing.copy(
                                    currentSellableStock = newQty,
                                    movingAvgCostPaise = newCost,
                                    sellingPricePiecePaise = if (data.sellingPiecePaise > 0L) data.sellingPiecePaise else existing.sellingPricePiecePaise,
                                    sellingPricePackPaise = if (data.sellingPackPaise > 0L) data.sellingPackPaise else existing.sellingPricePackPaise,
                                    wholesalePricePiecePaise = if (data.wholesalePaise > 0L) data.wholesalePaise else existing.wholesalePricePiecePaise,
                                    sku = existing.sku ?: data.sku,
                                    barcode = existing.barcode ?: data.barcode,
                                    category = if (existing.category == "General" && data.category != "General") data.category else existing.category,
                                    brand = existing.brand ?: data.brand,
                                    updatedAt = System.currentTimeMillis()
                                )
                                pDao.updateProduct(updated)

                                if (data.quantity > 0) {
                                    mDao.insertMovement(
                                        StockMovementEntity(
                                            productId = existing.id,
                                            type = "STOCK_IN",
                                            quantityChangeBase = data.quantity,
                                            unitCostPaise = data.costPaise,
                                            reason = "Catalog Import (Add Stock): Added +${data.quantity} from $fileName",
                                            referenceId = batchId
                                        )
                                    )
                                }

                                // Register barcode if missing
                                if (!data.barcode.isNullOrBlank() && barcodeDao != null && barcodeDao.findByBarcode(data.barcode) == null) {
                                    try {
                                        barcodeDao.insertBarcode(
                                            ProductBarcodeEntity(
                                                barcode = data.barcode,
                                                productId = existing.id,
                                                unitType = existing.baseUnit.uppercase(Locale.ROOT),
                                                packQuantity = existing.packSize
                                            )
                                        )
                                    } catch (_: Exception) {}
                                }

                                updatedCount++
                                if (updatedDetails.size < 10) {
                                    updatedDetails.add("Row ${item.rowNumber}: '${data.name}' (Stock: ${existing.currentSellableStock} -> $newQty)")
                                }
                            }

                            ImportMode.SET_STOCK -> {
                                val diff = data.quantity - existing.currentSellableStock
                                val newCost = if (data.costPaise > 0L) data.costPaise else existing.movingAvgCostPaise
                                val updated = existing.copy(
                                    currentSellableStock = data.quantity,
                                    movingAvgCostPaise = newCost,
                                    sellingPricePiecePaise = if (data.sellingPiecePaise > 0L) data.sellingPiecePaise else existing.sellingPricePiecePaise,
                                    sellingPricePackPaise = if (data.sellingPackPaise > 0L) data.sellingPackPaise else existing.sellingPricePackPaise,
                                    wholesalePricePiecePaise = if (data.wholesalePaise > 0L) data.wholesalePaise else existing.wholesalePricePiecePaise,
                                    sku = existing.sku ?: data.sku,
                                    barcode = existing.barcode ?: data.barcode,
                                    category = if (existing.category == "General" && data.category != "General") data.category else existing.category,
                                    brand = existing.brand ?: data.brand,
                                    updatedAt = System.currentTimeMillis()
                                )
                                pDao.updateProduct(updated)

                                if (diff != 0) {
                                    mDao.insertMovement(
                                        StockMovementEntity(
                                            productId = existing.id,
                                            type = "STOCK_ADJUSTMENT",
                                            quantityChangeBase = diff,
                                            countedTotalBase = data.quantity,
                                            unitCostPaise = newCost,
                                            reason = "Catalog Import (Set Stock): Adjusted by $diff from $fileName",
                                            referenceId = batchId
                                        )
                                    )
                                }

                                if (!data.barcode.isNullOrBlank() && barcodeDao != null && barcodeDao.findByBarcode(data.barcode) == null) {
                                    try {
                                        barcodeDao.insertBarcode(
                                            ProductBarcodeEntity(
                                                barcode = data.barcode,
                                                productId = existing.id,
                                                unitType = existing.baseUnit.uppercase(Locale.ROOT),
                                                packQuantity = existing.packSize
                                            )
                                        )
                                    } catch (_: Exception) {}
                                }

                                updatedCount++
                                if (updatedDetails.size < 10) {
                                    updatedDetails.add("Row ${item.rowNumber}: '${data.name}' (Stock set to ${data.quantity})")
                                }
                            }

                            ImportMode.PRICES_ONLY -> {
                                val updated = existing.copy(
                                    sellingPricePiecePaise = if (data.sellingPiecePaise > 0L) data.sellingPiecePaise else existing.sellingPricePiecePaise,
                                    sellingPricePackPaise = if (data.sellingPackPaise > 0L) data.sellingPackPaise else existing.sellingPricePackPaise,
                                    wholesalePricePiecePaise = if (data.wholesalePaise > 0L) data.wholesalePaise else existing.wholesalePricePiecePaise,
                                    movingAvgCostPaise = if (data.costPaise > 0L) data.costPaise else existing.movingAvgCostPaise,
                                    updatedAt = System.currentTimeMillis()
                                )
                                pDao.updateProduct(updated)
                                updatedCount++
                            }

                            ImportMode.DETAILS_ONLY -> {
                                val updated = existing.copy(
                                    category = if (data.category != "General") data.category else existing.category,
                                    brand = data.brand ?: existing.brand,
                                    packSize = if (data.packSize > 1) data.packSize else existing.packSize,
                                    lowStockThreshold = data.lowStock,
                                    updatedAt = System.currentTimeMillis()
                                )
                                pDao.updateProduct(updated)
                                updatedCount++
                            }
                        }
                    }

                    RowStatus.SKIP_DUPLICATE -> {
                        skippedCount++
                        if (skippedDetails.size < 10) {
                            skippedDetails.add("Row ${item.rowNumber}: '${data.name}' (duplicate)")
                        }
                    }

                    RowStatus.ERROR -> {
                        // Handled above
                    }
                }
            }

            // Record batch history in import_batches
            try {
                bDao.insertBatch(
                    ImportBatchEntity(
                        id = batchId,
                        fileName = fileName,
                        totalRows = plan.size,
                        importedCount = importedCount + updatedCount,
                        skippedCount = skippedCount
                    )
                )
            } catch (_: Exception) {}
        }

        if (database != null) {
            database.withTransaction { block() }
        } else {
            block()
        }

        ImportResult(
            totalRead = plan.size,
            importedCount = importedCount,
            skippedDuplicatesCount = skippedCount,
            updatedCount = updatedCount,
            updatedDetails = updatedDetails,
            skippedDetails = skippedDetails,
            errors = errors,
            isSuccess = true,
            plannedRows = plan,
            batchId = batchId
        )
    }

    /**
     * Backward-compatible import method used by existing callers.
     */
    suspend fun importProducts(
        uri: Uri,
        fileName: String,
        sheetIndex: Int,
        mapping: ColumnMapping,
        updateExisting: Boolean = false
    ): ImportResult {
        val mode = if (updateExisting) ImportMode.ADD_STOCK else ImportMode.NEW_ONLY
        return importProductsWithMode(uri, fileName, sheetIndex, mapping, mode, validRowsOnly = false)
    }

    suspend fun importProductsWithMode(
        uri: Uri,
        fileName: String,
        sheetIndex: Int,
        mapping: ColumnMapping,
        mode: ImportMode,
        validRowsOnly: Boolean = false
    ): ImportResult = withContext(Dispatchers.IO) {
        try {
            require(preferences == null || preferences.deviceRole.first() == "OWNER") { "Owner authorization required" }

            val pfd = try { context?.contentResolver?.openFileDescriptor(uri, "r") } catch (_: Exception) { null }
            val fileSize = pfd?.statSize ?: 0L
            pfd?.close()
            if (fileSize > 10 * 1024 * 1024) {
                return@withContext ImportResult(
                    0, 0, 0,
                    isSuccess = false,
                    failureReason = "File exceeds the 10 MB limit (${fileSize / (1024 * 1024)} MB). Please split into smaller files."
                )
            }

            val stream = try {
                context?.contentResolver?.openInputStream(uri)
                    ?: return@withContext ImportResult(0, 0, 0, isSuccess = false, failureReason = "Cannot open file stream")
            } catch (e: Exception) {
                return@withContext ImportResult(0, 0, 0, isSuccess = false, failureReason = mapExceptionToMessage(e))
            }

            val rows = try {
                if (fileName.endsWith(".csv", ignoreCase = true)) {
                    readAllCsv(stream)
                } else {
                    readAllExcel(stream, sheetIndex)
                }
            } catch (e: Exception) {
                return@withContext ImportResult(0, 0, 0, isSuccess = false, failureReason = mapExceptionToMessage(e))
            }

            if (rows.size <= 1) {
                return@withContext ImportResult(0, 0, 0, isSuccess = false, failureReason = "No product rows found to import.")
            }

            for (r in rows.take(minOf(10, rows.size))) {
                if (isCashLedgerHeaders(r, fileName)) {
                    return@withContext ImportResult(
                        totalRead = 0,
                        importedCount = 0,
                        skippedDuplicatesCount = 0,
                        isSuccess = false,
                        failureReason = "This file contains Cash In/Out transactions, not product stock. Import it from Cash In/Out Tracker."
                    )
                }
            }

            val batchKey = MessageDigest.getInstance("SHA-256")
                .digest((rows.toString() + mapping.toString() + sheetIndex).toByteArray(Charsets.UTF_8))
                .joinToString("") { "%02x".format(it) }

            if (importBatchDao?.getBatch(batchKey) != null) {
                return@withContext ImportResult(
                    rows.size - 1, 0, rows.size - 1,
                    isSuccess = false,
                    failureReason = "This exact import has already been applied"
                )
            }

            val plan = planImport(rows, mapping, mode)
            commit(plan, mode, validRowsOnly, fileName, mapping)
        } catch (e: Exception) {
            ImportResult(0, 0, 0, isSuccess = false, failureReason = "Import rolled back: ${e.message}")
        }
    }

    internal fun readAllCsv(stream: InputStream): List<List<String>> {
        return CsvReader.read(stream)
    }

    internal fun readAllExcel(stream: InputStream, sheetIndex: Int): List<List<String>> {
        val workbook = WorkbookFactory.create(stream)
        val activeSheetIndex = if (sheetIndex in 0 until workbook.numberOfSheets) sheetIndex else 0
        val sheet = workbook.getSheetAt(activeSheetIndex)

        if (sheet.physicalNumberOfRows > 20000 || sheet.lastRowNum > 20000) {
            val count = maxOf(sheet.physicalNumberOfRows, sheet.lastRowNum)
            workbook.close()
            stream.close()
            throw IllegalArgumentException("Excel sheet exceeds the 20,000 row limit ($count rows). Please split into smaller files.")
        }

        val evaluator = workbook.creationHelper.createFormulaEvaluator()
        val formatter = DataFormatter()
        val rows = mutableListOf<List<String>>()

        for (i in 0..sheet.lastRowNum) {
            val row = sheet.getRow(i) ?: continue
            val rowValues = CellReader.readRow(row, evaluator, formatter)
            if (rowValues.any { it.isNotBlank() }) {
                rows.add(rowValues)
            }
        }
        workbook.close()
        stream.close()
        return rows
    }
}
