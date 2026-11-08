package com.alieducational.app.data.service

import com.alieducational.app.domain.calculation.CostCalculator

import android.content.Context
import androidx.room.withTransaction
import kotlinx.coroutines.flow.first
import android.net.Uri
import com.alieducational.app.data.local.dao.ImportBatchDao
import com.alieducational.app.data.local.dao.ProductDao
import com.alieducational.app.data.local.dao.StockMovementDao
import com.alieducational.app.data.local.entity.ImportBatchEntity
import com.alieducational.app.data.local.entity.ProductEntity
import com.alieducational.app.data.local.entity.StockMovementEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.ss.usermodel.WorkbookFactory
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.util.Locale
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
    val isCashLedger: Boolean = false
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
    val lowStockIndex: Int = -1
)

data class ImportResult(
    val totalRead: Int,
    val importedCount: Int,
    val skippedDuplicatesCount: Int,
    val updatedCount: Int = 0,
    val updatedDetails: List<String> = emptyList(),
    val skippedDetails: List<String> = emptyList(),
    val errors: List<String> = emptyList(),
    val isSuccess: Boolean = true,
    val failureReason: String? = null
)

@Singleton
class ImportExportService @Inject constructor(
    @ApplicationContext private val context: Context?,
    private val productDao: ProductDao?,
    private val movementDao: StockMovementDao?,
    private val importBatchDao: ImportBatchDao?,
    private val database: com.alieducational.app.data.local.AppDatabase? = null,
    private val preferences: com.alieducational.app.data.preferences.AppPreferences? = null
) {
    constructor() : this(null, null, null, null, null, null)

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
        val reader = BufferedReader(InputStreamReader(stream))
        val lines = mutableListOf<List<String>>()
        var line: String?

        var lineCount = 0
        while (reader.readLine().also { line = it } != null) {
            lineCount++
            val parts = parseCsvLine(line!!)
            if (parts.any { it.isNotBlank() }) {
                lines.add(parts)
            }
            if (lines.size >= 15) break
        }
        reader.close()

        if (lines.isEmpty()) {
            return SpreadsheetPreview(
                fileName = fileName,
                errorMessage = "CSV file is empty. Please provide a CSV with header columns (e.g. Name, Category, Price, Stock)."
            )
        }

        // Detect header row (first row with multiple non-blank items)
        var headerIndex = 0
        for (i in 0 until minOf(5, lines.size)) {
            if (lines[i].count { it.isNotBlank() } >= 2) {
                headerIndex = i
                break
            }
        }

        val headers = lines[headerIndex]
        val preview = if (lines.size > headerIndex + 1) lines.subList(headerIndex + 1, lines.size) else emptyList()

        if (isCashLedgerHeaders(headers, fileName)) {
            return SpreadsheetPreview(
                fileName = fileName,
                sheetNames = listOf("CSV Data"),
                selectedSheetIndex = 0,
                headers = headers,
                previewRows = preview,
                totalEstimatedRows = lineCount,
                isCashLedger = true,
                errorMessage = "This file contains Cash In/Out transactions, not product stock. Import it from Cash In/Out Tracker."
            )
        }

        return SpreadsheetPreview(
            fileName = fileName,
            sheetNames = listOf("CSV Data"),
            selectedSheetIndex = 0,
            headers = headers,
            previewRows = preview,
            totalEstimatedRows = lineCount
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

        val allRows = mutableListOf<Pair<Int, List<String>>>()
        val lastRow = sheet.lastRowNum
        for (i in 0..minOf(lastRow, 25)) {
            val row = sheet.getRow(i) ?: continue
            val rowValues = (0 until row.lastCellNum).map { getCellString(row, it) }
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

        // Find the best header row (e.g. Row with maximum non-blank columns, or explicit keywords)
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
                errorMessage = "This file contains Cash In/Out transactions, not product stock. Import it from Cash In/Out Tracker."
            )
        }

        return SpreadsheetPreview(
            fileName = fileName,
            sheetNames = sheetNames,
            selectedSheetIndex = activeSheetIndex,
            headers = headers,
            previewRows = preview,
            totalEstimatedRows = lastRow + 1
        )
    }

    suspend fun importProducts(uri: Uri, fileName: String, sheetIndex: Int, mapping: ColumnMapping, updateExisting: Boolean = false): ImportResult {
        return try {
            require(preferences == null || preferences.deviceRole.first() == "OWNER") { "Owner authorization required" }
            if (database != null) database.withTransaction { importProductsInternal(uri, fileName, sheetIndex, mapping, updateExisting) }
            else importProductsInternal(uri, fileName, sheetIndex, mapping, updateExisting)
        } catch (e: Exception) { ImportResult(0, 0, 0, isSuccess = false, failureReason = "Import rolled back: ${e.message}") }
    }

    private suspend fun importProductsInternal(
        uri: Uri,
        fileName: String,
        sheetIndex: Int,
        mapping: ColumnMapping,
        updateExisting: Boolean = false
    ): ImportResult = withContext(Dispatchers.IO) {
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

        val pDao = productDao ?: return@withContext ImportResult(0, 0, 0, isSuccess = false, failureReason = "Database not initialized")
        val mDao = movementDao ?: return@withContext ImportResult(0, 0, 0, isSuccess = false, failureReason = "Database not initialized")
        val bDao = importBatchDao ?: return@withContext ImportResult(0, 0, 0, isSuccess = false, failureReason = "Database not initialized")

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

        // Safety check: block writing products if file is detected as cash-ledger
        val firstFewRows = rows.take(minOf(10, rows.size))
        for (r in firstFewRows) {
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

        val batchKey = java.security.MessageDigest.getInstance("SHA-256").digest((rows.toString() + mapping.toString() + sheetIndex).toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
        if (bDao.getBatch(batchKey) != null) return@withContext ImportResult(rows.size - 1, 0, rows.size - 1, failureReason = "This exact import has already been applied")
        var imported = 0
        var updatedCount = 0
        var skippedDuplicates = 0
        val updatedDetails = mutableListOf<String>()
        val skippedDetails = mutableListOf<String>()
        val errors = mutableListOf<String>()

        // Process rows atomically one by one
        for (i in 1 until rows.size) {
            val row = rows[i]
            val rowNum = i + 1
            val name = if (mapping.nameIndex in row.indices) row[mapping.nameIndex].trim() else ""
            if (name.isBlank()) {
                // If entire row is blank, skip silently; otherwise note missing name
                if (row.any { it.isNotBlank() }) {
                    errors.add("Row $rowNum: Skipped because product name was empty.")
                }
                continue
            }

            val sku = if (mapping.skuIndex in row.indices) row[mapping.skuIndex].trim().ifBlank { null } else null
            val barcode = if (mapping.barcodeIndex in row.indices) row[mapping.barcodeIndex].trim().ifBlank { null } else null

            // Detect existing product by SKU, Barcode, or exact Name
            val existing = (if (sku != null) pDao.getProductBySku(sku) else null)
                ?: (if (barcode != null) pDao.getProductByBarcode(barcode) else null)
                ?: pDao.getProductByName(name)

            val category = if (mapping.categoryIndex in row.indices) row[mapping.categoryIndex].trim().ifBlank { "General" } else "General"
            val brand = if (mapping.brandIndex in row.indices) row[mapping.brandIndex].trim().ifBlank { null } else null
            val baseUnit = if (mapping.baseUnitIndex in row.indices) row[mapping.baseUnitIndex].trim().ifBlank { "piece" } else "piece"
            val packSize = if (mapping.packSizeIndex in row.indices) row[mapping.packSizeIndex].trim().toIntOrNull()?.coerceAtLeast(1) ?: 1 else 1

            val openingQty = if (mapping.openingQtyIndex in row.indices) row[mapping.openingQtyIndex].trim().toIntOrNull() ?: 0 else 0
            val purchaseCostPaise = if (mapping.purchaseCostIndex in row.indices) parseCurrencyToPaise(row[mapping.purchaseCostIndex]) else 0L
            val sellingPiecePaise = if (mapping.sellingPricePieceIndex in row.indices) parseCurrencyToPaise(row[mapping.sellingPricePieceIndex]) else 0L
            val sellingPackPaise = if (mapping.sellingPricePackIndex in row.indices) parseCurrencyToPaise(row[mapping.sellingPricePackIndex]) else sellingPiecePaise * packSize
            val lowStock = if (mapping.lowStockIndex in row.indices) row[mapping.lowStockIndex].trim().toIntOrNull() ?: 5 else 5

            if (existing != null) {
                if (!updateExisting) {
                    skippedDuplicates++
                    if (skippedDetails.size < 10) {
                        val matchField = if (sku != null && existing.sku == sku) "SKU '$sku'"
                        else if (barcode != null && existing.barcode == barcode) "Barcode '$barcode'"
                        else "Name '${existing.name}'"
                        skippedDetails.add("Row $rowNum: '$name' (matches existing $matchField)")
                    }
                    continue
                } else {
                    // Update existing item
                    try {
                        val newQty = existing.currentSellableStock + openingQty
                        val newCost = if (purchaseCostPaise > 0) {
                            if (existing.currentSellableStock > 0 && openingQty > 0) {
                                val totalVal = (existing.currentSellableStock.toLong() * existing.movingAvgCostPaise) + (openingQty.toLong() * purchaseCostPaise)
                                totalVal / newQty.coerceAtLeast(1)
                            } else {
                                purchaseCostPaise
                            }
                        } else {
                            existing.movingAvgCostPaise
                        }

                        val updatedProduct = existing.copy(
                            currentSellableStock = newQty,
                            movingAvgCostPaise = newCost,
                            sellingPricePiecePaise = if (sellingPiecePaise > 0) sellingPiecePaise else existing.sellingPricePiecePaise,
                            sellingPricePackPaise = if (sellingPackPaise > 0) sellingPackPaise else existing.sellingPricePackPaise,
                            sku = existing.sku ?: sku,
                            barcode = existing.barcode ?: barcode,
                            category = if (existing.category == "General" && category != "General") category else existing.category,
                            brand = existing.brand ?: brand,
                            updatedAt = System.currentTimeMillis()
                        )
                        pDao.updateProduct(updatedProduct)

                        if (openingQty > 0) {
                            mDao.insertMovement(
                                StockMovementEntity(
                                    productId = existing.id,
                                    type = "STOCK_ADJUSTMENT",
                                    quantityChangeBase = openingQty,
                                    unitCostPaise = purchaseCostPaise,
                                    reason = "Catalog Import: Added +$openingQty from $fileName"
                                )
                            )
                        }

                        updatedCount++
                        if (updatedDetails.size < 10) {
                            updatedDetails.add("Row $rowNum: '$name' (Stock: ${existing.currentSellableStock} -> $newQty)")
                        }
                    } catch (e: Exception) {
                        errors.add("Row $rowNum ('$name'): Failed to update: ${e.localizedMessage}")
                    }
                    continue
                }
            }

            val product = ProductEntity(
                name = name,
                category = category,
                brand = brand,
                sku = sku,
                barcode = barcode,
                baseUnit = baseUnit,
                packSize = packSize,
                currentSellableStock = openingQty,
                movingAvgCostPaise = purchaseCostPaise,
                sellingPricePiecePaise = sellingPiecePaise,
                sellingPricePackPaise = sellingPackPaise,
                lowStockThreshold = lowStock
            )

            try {
                pDao.insertProduct(product)
                if (openingQty > 0) {
                    mDao.insertMovement(
                        StockMovementEntity(
                            productId = product.id,
                            type = "OPENING_STOCK",
                            quantityChangeBase = openingQty,
                            unitCostPaise = purchaseCostPaise,
                            reason = "Catalog Import: $fileName"
                        )
                    )
                }
                imported++
            } catch (e: Exception) {
                errors.add("Row $rowNum ('$name'): ${e.localizedMessage}")
            }
        }

        require(errors.isEmpty()) { errors.joinToString("; ") }
        // Record import batch history
        try {
            bDao.insertBatch(
                ImportBatchEntity(
                    id = batchKey,
                    fileName = fileName,
                    totalRows = rows.size - 1,
                    importedCount = imported + updatedCount,
                    skippedCount = skippedDuplicates
                )
            )
        } catch (e: Exception) { throw IllegalStateException("Could not record import history", e) }

        return@withContext ImportResult(
            totalRead = rows.size - 1,
            importedCount = imported,
            skippedDuplicatesCount = skippedDuplicates,
            updatedCount = updatedCount,
            updatedDetails = updatedDetails,
            skippedDetails = skippedDetails,
            errors = errors,
            isSuccess = true
        )
    }

    internal fun readAllCsv(stream: InputStream): List<List<String>> {
        val reader = BufferedReader(InputStreamReader(stream))
        val rows = mutableListOf<List<String>>()
        var line: String?
        while (reader.readLine().also { line = it } != null) {
            val parts = parseCsvLine(line!!)
            if (parts.any { it.isNotBlank() }) {
                rows.add(parts)
            }
            if (rows.size > 20000) {
                reader.close()
                throw IllegalArgumentException("CSV file exceeds the 20,000 row limit (20,000+ rows). Please split into smaller files.")
            }
        }
        reader.close()
        return rows
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

        val rows = mutableListOf<List<String>>()

        for (i in 0..sheet.lastRowNum) {
            val row = sheet.getRow(i) ?: continue
            val rowValues = (0 until row.lastCellNum).map { getCellString(row, it) }
            if (rowValues.any { it.isNotBlank() }) {
                rows.add(rowValues)
            }
        }
        workbook.close()
        stream.close()
        return rows
    }

    private fun getCellString(row: Row, colIndex: Int): String {
        val cell = row.getCell(colIndex) ?: return ""
        return try {
            when (cell.cellType) {
                CellType.STRING -> cell.stringCellValue?.trim() ?: ""
                CellType.NUMERIC -> {
                    val num = cell.numericCellValue
                    if (num == num.toLong().toDouble()) num.toLong().toString() else num.toString()
                }
                CellType.BOOLEAN -> cell.booleanCellValue.toString()
                CellType.FORMULA -> {
                    try {
                        cell.stringCellValue?.trim() ?: cell.numericCellValue.toString()
                    } catch (_: Exception) {
                        cell.numericCellValue.toString()
                    }
                }
                else -> ""
            }
        } catch (_: Exception) {
            ""
        }
    }

    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        var curVal = StringBuilder()
        var inQuotes = false

        for (c in line) {
            if (c == '\"') {
                inQuotes = !inQuotes
            } else if (c == ',' && !inQuotes) {
                result.add(curVal.toString().trim())
                curVal = StringBuilder()
            } else {
                curVal.append(c)
            }
        }
        result.add(curVal.toString().trim())
        return result
    }

    private fun parseCurrencyToPaise(str: String): Long {
        val clean = str.replace("₹", "").replace("Rs.", "", ignoreCase = true).replace("Rs", "", ignoreCase = true).replace(",", "").trim()
        return CostCalculator.parseRupeesToPaise(clean) ?: 0L
    }
}
