package com.alieducational.app.data.service

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import com.alieducational.app.data.local.entity.ShopExpenseEntity
import com.alieducational.app.domain.calculation.CostCalculator
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.poi.ss.usermodel.BorderStyle
import org.apache.poi.ss.usermodel.Cell
import org.apache.poi.ss.usermodel.CellStyle
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.DateUtil
import org.apache.poi.ss.usermodel.FillPatternType
import org.apache.poi.ss.usermodel.HorizontalAlignment
import org.apache.poi.ss.usermodel.IndexedColors
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.ss.usermodel.VerticalAlignment
import org.apache.poi.ss.usermodel.WorkbookFactory
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToLong

data class CashTrackerImportPreview(
    val fileName: String,
    val items: List<ShopExpenseEntity> = emptyList(),
    val totalIncomePaise: Long = 0L,
    val totalExpensePaise: Long = 0L,
    val incomeCount: Int = 0,
    val expenseCount: Int = 0,
    val skippedRows: Int = 0,
    val errorMessage: String? = null
)

@Singleton
class CashTrackerExcelService @Inject constructor(
    @ApplicationContext private val context: Context?
) {
    constructor() : this(null)

    private val exportFolder: File
        get() {
            val dir = context?.getExternalFilesDir("exports") ?: File(context?.cacheDir, "exports")
            if (!dir.exists()) {
                dir.mkdirs()
            }
            return dir
        }

    /**
     * Parse an Excel file (.xlsx or .xls) from content Uri into a preview of ShopExpenseEntity objects.
     */
    suspend fun parseExcelFile(uri: Uri, fileName: String): CashTrackerImportPreview = withContext(Dispatchers.IO) {
        try {
            val pfd = context?.contentResolver?.openFileDescriptor(uri, "r")
            val fileSize = pfd?.statSize ?: 0L
            pfd?.close()
            if (fileSize > 10 * 1024 * 1024) {
                return@withContext CashTrackerImportPreview(
                    fileName = fileName,
                    errorMessage = "Excel file exceeds the 10 MB limit (${fileSize / (1024 * 1024)} MB). Please split into smaller files."
                )
            }

            val inputStream: InputStream = context?.contentResolver?.openInputStream(uri)
                ?: return@withContext CashTrackerImportPreview(
                    fileName = fileName,
                    errorMessage = "Cannot access the selected file. Please pick it again."
                )

            inputStream.use { stream ->
                val workbook = WorkbookFactory.create(stream)
                workbook.use { wb ->
                    parseWorkbook(wb, fileName)
                }
            }
        } catch (e: Exception) {
            CashTrackerImportPreview(
                fileName = fileName,
                errorMessage = "Failed to parse Excel file: ${e.localizedMessage ?: "Unknown format error"}"
            )
        }
    }

    /**
     * Parse an in-memory or loaded Workbook into a preview.
     */
    fun parseWorkbook(workbook: org.apache.poi.ss.usermodel.Workbook, fileName: String): CashTrackerImportPreview {
        if (workbook.numberOfSheets == 0) {
            return CashTrackerImportPreview(
                fileName = fileName,
                errorMessage = "Excel workbook contains no sheets."
            )
        }

        val sheet = workbook.getSheetAt(0)
        if (sheet.physicalNumberOfRows < 2) {
            return CashTrackerImportPreview(
                fileName = fileName,
                errorMessage = "The sheet does not contain enough data rows (headers + entries)."
            )
        }

        if (sheet.physicalNumberOfRows > 20000 || sheet.lastRowNum > 20000) {
            val count = maxOf(sheet.physicalNumberOfRows, sheet.lastRowNum)
            return CashTrackerImportPreview(
                fileName = fileName,
                errorMessage = "Excel sheet exceeds the 20,000 row limit ($count rows). Please split into smaller files."
            )
        }

        // Find header row (usually row 0 or 1)
        var headerRowIndex = -1
        var dateCol = -1
        var descCol = -1
        var categoryCol = -1
        var incomeCol = -1
        var expenseCol = -1
        var modeCol = -1
        var singleAmountCol = -1
        var typeCol = -1

        for (r in 0..minOf(5, sheet.lastRowNum)) {
            val row = sheet.getRow(r) ?: continue
            var catCol = -1
            var dCol = -1
            var desCol = -1
            var inCol = -1
            var outCol = -1
            var mCol = -1
            var amtCol = -1
            var tCol = -1

            for (c in 0 until row.lastCellNum) {
                val cell = row.getCell(c) ?: continue
                val text = getCellString(cell).trim().lowercase(Locale.ROOT)
                if (text.isBlank()) continue

                when {
                    text == "category" || text.contains("category") || text.contains("श्रेणी") -> catCol = c
                    text.contains("date") || text.contains("dated") || text.contains("दिनांक") || text.contains("तारीख") -> dCol = c
                    text.contains("desc") || text.contains("particular") ||
                            text.contains("item") || text.contains("name") || text.contains("details") || text.contains("विवरण") -> desCol = c
                    text.contains("cash in") || text.contains("income") || text.contains("credit") ||
                            text == "cr" || text.contains("received") || text.contains("जमा") || text == "in" -> inCol = c
                    text.contains("cash out") || text.contains("expense") || text.contains("debit") ||
                            text == "dr" || text.contains("paid") || text.contains("खर्च") || text == "out" -> outCol = c
                    text.contains("mode") || text.contains("method") || text.contains("type") || text.contains("note") || text.contains("remark") -> mCol = c
                    text.contains("amount") || text.contains("total") || text.contains("₹") || text.contains("rs") -> amtCol = c
                    text == "flow" || text.contains("in/out") || text.contains("income/expense") -> tCol = c
                }
            }

            // Header matches if we found at least description or date, and an income/expense or amount column
            if ((inCol != -1 || outCol != -1 || amtCol != -1) && (desCol != -1 || dCol != -1)) {
                headerRowIndex = r
                categoryCol = catCol
                dateCol = dCol
                descCol = desCol
                incomeCol = inCol
                expenseCol = outCol
                modeCol = mCol
                singleAmountCol = amtCol
                typeCol = tCol
                break
            }
        }

        // Fallback: If no explicit headers recognized, assume standard columns:
        // Col 0: Date, Col 1: Description, Col 2: Cash In, Col 3: Cash Out, Col 4: Payment Mode
        if (headerRowIndex == -1) {
            headerRowIndex = 0
            dateCol = 0
            descCol = 1
            incomeCol = 2
            expenseCol = 3
            modeCol = 4
        }

        val parsedItems = mutableListOf<ShopExpenseEntity>()
        var skipped = 0

        for (r in (headerRowIndex + 1)..sheet.lastRowNum) {
            val row = sheet.getRow(r) ?: continue
            if (isRowEmpty(row)) continue

            val dateTimestamp = if (dateCol != -1) parseCellDate(row.getCell(dateCol)) else System.currentTimeMillis()
            val catText = if (categoryCol != -1) getCellString(row.getCell(categoryCol)).trim() else ""
            val desc = if (descCol != -1) getCellString(row.getCell(descCol)).trim() else ""
            val mode = if (modeCol != -1) getCellString(row.getCell(modeCol)).trim() else ""

            // Exclude summary rows, opening/previous balance, and totals from transactions
            val combinedText = (desc + " " + mode).lowercase(Locale.ROOT)
            if (combinedText.contains("previous balance") || combinedText.contains("opening balance") ||
                combinedText.contains("closing balance") || combinedText.contains("total") ||
                combinedText.contains("subtotal") || combinedText.contains("grand total")
            ) {
                skipped++
                continue
            }

            var incomePaise = 0L
            var expensePaise = 0L

            if (incomeCol != -1) {
                val inAmt = parseCellAmount(row.getCell(incomeCol))
                if (inAmt > 0) incomePaise = (inAmt * 100).roundToLong()
            }
            if (expenseCol != -1) {
                val outAmt = parseCellAmount(row.getCell(expenseCol))
                if (outAmt > 0) expensePaise = (outAmt * 100).roundToLong()
            }

            // If separate income/expense columns weren't both present, check single amount column
            if (incomePaise == 0L && expensePaise == 0L && singleAmountCol != -1) {
                val amt = parseCellAmount(row.getCell(singleAmountCol))
                if (amt > 0) {
                    val typeStr = if (typeCol != -1) getCellString(row.getCell(typeCol)).lowercase(Locale.ROOT) else ""
                    if (typeStr.contains("in") || typeStr.contains("cr") || typeStr.contains("credit") || typeStr.contains("income") || typeStr.contains("jama")) {
                        incomePaise = (amt * 100).roundToLong()
                    } else {
                        expensePaise = (amt * 100).roundToLong()
                    }
                }
            }

            if (incomePaise == 0L && expensePaise == 0L) {
                skipped++
                continue
            }

            val finalCat = when {
                catText.isNotBlank() -> catText
                desc.isNotBlank() -> desc
                else -> "General"
            }
            val finalNotes = when {
                mode.isNotBlank() -> mode
                desc.isNotBlank() && desc != finalCat -> desc
                else -> "Excel Import"
            }

            if (incomePaise > 0L) {
                parsedItems.add(
                    ShopExpenseEntity(
                        category = finalCat,
                        amountPaise = incomePaise,
                        isIncome = true,
                        notes = finalNotes,
                        timestamp = dateTimestamp
                    )
                )
            }

            if (expensePaise > 0L) {
                parsedItems.add(
                    ShopExpenseEntity(
                        category = finalCat,
                        amountPaise = expensePaise,
                        isIncome = false,
                        notes = finalNotes,
                        timestamp = dateTimestamp
                    )
                )
            }

            if (incomePaise == 0L && expensePaise == 0L) {
                skipped++
            }
        }

        if (parsedItems.isEmpty()) {
            return CashTrackerImportPreview(
                fileName = fileName,
                errorMessage = "No valid income or expense entries found in the file. Please ensure amounts are greater than 0."
            )
        }

        val incomeItems = parsedItems.filter { it.isIncome }
        val expenseItems = parsedItems.filter { !it.isIncome }

        return CashTrackerImportPreview(
            fileName = fileName,
            items = parsedItems,
            totalIncomePaise = incomeItems.sumOf { it.amountPaise },
            totalExpensePaise = expenseItems.sumOf { it.amountPaise },
            incomeCount = incomeItems.size,
            expenseCount = expenseItems.size,
            skippedRows = skipped
        )
    }

    /**
     * Export Cash Tracker items to a real multi-sheet Excel workbook (.xlsx):
     * - Summary sheet: metadata, filters, transaction count, inflow, outflow, net movement, category subtotals.
     * - Transactions sheet: frozen header, stable ID, date, time, direction, purpose, category, classification, sanitized notes, source, inflow/outflow numeric cells.
     */
    fun buildCashTrackerWorkbook(
        items: List<ShopExpenseEntity>,
        periodTitle: String = "All Transactions",
        fromDate: Long? = null,
        toDate: Long? = null,
        activeFiltersSummary: String = "All Categories, Active Transactions"
    ): XSSFWorkbook {
        val workbook = XSSFWorkbook()
        val dataFormat = workbook.createDataFormat()
        val currencyFormatIndex = dataFormat.getFormat("#,##,##0.00")

        // ── STYLES ───────────────────────────────────────────
        val headerFont = workbook.createFont().apply {
            bold = true
            fontHeightInPoints = 11.toShort()
            color = IndexedColors.WHITE.index
        }

        val headerStyle = workbook.createCellStyle().apply {
            setFont(headerFont)
            fillForegroundColor = IndexedColors.DARK_BLUE.index
            fillPattern = FillPatternType.SOLID_FOREGROUND
            alignment = HorizontalAlignment.CENTER
            verticalAlignment = VerticalAlignment.CENTER
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

        val normalNumberStyle = workbook.createCellStyle().apply {
            setDataFormat(currencyFormatIndex)
            alignment = HorizontalAlignment.RIGHT
            borderBottom = BorderStyle.THIN
            borderTop = BorderStyle.THIN
            borderLeft = BorderStyle.THIN
            borderRight = BorderStyle.THIN
        }

        val incomeNumberStyle = workbook.createCellStyle().apply {
            setDataFormat(currencyFormatIndex)
            alignment = HorizontalAlignment.RIGHT
            val f = workbook.createFont().apply {
                color = IndexedColors.DARK_GREEN.index
                bold = true
            }
            setFont(f)
            borderBottom = BorderStyle.THIN
            borderTop = BorderStyle.THIN
            borderLeft = BorderStyle.THIN
            borderRight = BorderStyle.THIN
        }

        val expenseNumberStyle = workbook.createCellStyle().apply {
            setDataFormat(currencyFormatIndex)
            alignment = HorizontalAlignment.RIGHT
            val f = workbook.createFont().apply {
                color = IndexedColors.DARK_RED.index
                bold = true
            }
            setFont(f)
            borderBottom = BorderStyle.THIN
            borderTop = BorderStyle.THIN
            borderLeft = BorderStyle.THIN
            borderRight = BorderStyle.THIN
        }

        val totalStyle = workbook.createCellStyle().apply {
            val f = workbook.createFont().apply {
                bold = true
                fontHeightInPoints = 11.toShort()
            }
            setFont(f)
            setDataFormat(currencyFormatIndex)
            fillForegroundColor = IndexedColors.GREY_25_PERCENT.index
            fillPattern = FillPatternType.SOLID_FOREGROUND
            borderBottom = BorderStyle.MEDIUM
            borderTop = BorderStyle.MEDIUM
            borderLeft = BorderStyle.THIN
            borderRight = BorderStyle.THIN
        }

        val sortedItems = items.sortedBy { it.timestamp }
        val activeItems = sortedItems.filter { !it.isVoided }
        val totalInflowPaise = activeItems.filter { it.isIncome }.sumOf { it.amountPaise }
        val totalOutflowPaise = activeItems.filter { !it.isIncome }.sumOf { it.amountPaise }
        val netMovementPaise = totalInflowPaise - totalOutflowPaise

        val dateFormat = SimpleDateFormat("dd-MMM-yyyy", Locale.US)
        val timeFormat = SimpleDateFormat("hh:mm a", Locale.US)

        // ── SHEET 1: SUMMARY ──────────────────────────────────
        val summarySheet = workbook.createSheet("Summary")

        // Title
        val sRow0 = summarySheet.createRow(0)
        sRow0.createCell(0).setCellValue("Ali Educational - Cash In & Out Ledger Report")
        sRow0.getCell(0).cellStyle = workbook.createCellStyle().apply {
            val f = workbook.createFont().apply { bold = true; fontHeightInPoints = 14.toShort() }
            setFont(f)
        }

        val sRow1 = summarySheet.createRow(1)
        sRow1.createCell(0).setCellValue("Notice: Filtered ledger export (not a complete backup or profit statement)")

        var sRowIdx = 3
        fun addSummaryPair(label: String, value: String) {
            val r = summarySheet.createRow(sRowIdx++)
            r.createCell(0).setCellValue(label)
            r.createCell(1).setCellValue(value)
        }
        fun addSummaryNumber(label: String, rupees: Double) {
            val r = summarySheet.createRow(sRowIdx++)
            r.createCell(0).setCellValue(label)
            val c = r.createCell(1)
            c.setCellValue(rupees)
            c.cellStyle = normalNumberStyle
        }

        val nowStr = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US).format(Date())
        addSummaryPair("Export Generation Time:", nowStr)
        addSummaryPair("Selected Period:", periodTitle)
        addSummaryPair("Active Filters:", activeFiltersSummary)
        addSummaryPair("Matching Active Transactions:", "${activeItems.size}")
        addSummaryNumber("Selected Inflow Total (₹):", totalInflowPaise / 100.0)
        addSummaryNumber("Selected Outflow Total (₹):", totalOutflowPaise / 100.0)
        addSummaryNumber("Selected Net Movement (₹):", netMovementPaise / 100.0)

        // Category Subtotals
        sRowIdx++
        val catHeaderRow = summarySheet.createRow(sRowIdx++)
        catHeaderRow.createCell(0).apply { setCellValue("Category"); cellStyle = headerStyle }
        catHeaderRow.createCell(1).apply { setCellValue("Inflow (₹)"); cellStyle = headerStyle }
        catHeaderRow.createCell(2).apply { setCellValue("Outflow (₹)"); cellStyle = headerStyle }
        catHeaderRow.createCell(3).apply { setCellValue("Net Movement (₹)"); cellStyle = headerStyle }

        val catGroups = activeItems.groupBy { it.category }
        catGroups.forEach { (cat, catList) ->
            val r = summarySheet.createRow(sRowIdx++)
            val catIn = catList.filter { it.isIncome }.sumOf { it.amountPaise } / 100.0
            val catOut = catList.filter { !it.isIncome }.sumOf { it.amountPaise } / 100.0
            val catNet = catIn - catOut
            r.createCell(0).apply { setCellValue(cat); cellStyle = normalStyle }
            r.createCell(1).apply { setCellValue(catIn); cellStyle = normalNumberStyle }
            r.createCell(2).apply { setCellValue(catOut); cellStyle = normalNumberStyle }
            r.createCell(3).apply { setCellValue(catNet); cellStyle = normalNumberStyle }
        }

        summarySheet.setColumnWidth(0, 26 * 256)
        summarySheet.setColumnWidth(1, 18 * 256)
        summarySheet.setColumnWidth(2, 18 * 256)
        summarySheet.setColumnWidth(3, 20 * 256)

        // ── SHEET 2: TRANSACTIONS ─────────────────────────────
        val txSheet = workbook.createSheet("Transactions")
        txSheet.createFreezePane(0, 1)

        val txHeaders = listOf(
            "Tx ID",
            "Date",
            "Time",
            "Direction",
            "Purpose",
            "Category",
            "Classification",
            "Description / Notes",
            "Source / Destination",
            "Inflow (₹)",
            "Outflow (₹)",
            "Attachment",
            "Status"
        )

        val thRow = txSheet.createRow(0)
        thRow.heightInPoints = 24f
        txHeaders.forEachIndexed { i, h ->
            thRow.createCell(i).apply {
                setCellValue(h)
                cellStyle = headerStyle
            }
        }

        var tRowIdx = 1
        for (item in sortedItems) {
            val r = txSheet.createRow(tRowIdx++)
            val d = Date(item.timestamp)

            // 0. Tx ID
            r.createCell(0).apply { setCellValue(item.id.take(8)); cellStyle = normalStyle }
            // 1. Date
            r.createCell(1).apply { setCellValue(dateFormat.format(d)); cellStyle = normalStyle }
            // 2. Time
            r.createCell(2).apply { setCellValue(timeFormat.format(d)); cellStyle = normalStyle }
            // 3. Direction
            r.createCell(3).apply { setCellValue(if (item.isIncome) "Cash In" else "Cash Out"); cellStyle = normalStyle }
            // 4. Purpose
            val purpose = when {
                item.isSavingsTransfer -> "Savings Transfer (Pigmy)"
                item.isExpenseRefund -> "Expense Refund"
                item.isIncome -> "Income"
                else -> "Shop Expense"
            }
            r.createCell(4).apply { setCellValue(purpose); cellStyle = normalStyle }
            // 5. Category
            r.createCell(5).apply { setCellValue(item.category); cellStyle = normalStyle }
            // 6. Classification
            r.createCell(6).apply { setCellValue(item.classification); cellStyle = normalStyle }

            // 7. Notes (sanitized against formula injection)
            var rawNotes = item.notes ?: "-"
            if (rawNotes.startsWith("=") || rawNotes.startsWith("+") || rawNotes.startsWith("-") || rawNotes.startsWith("@")) {
                rawNotes = "'$rawNotes"
            }
            r.createCell(7).apply { setCellValue(rawNotes); cellStyle = normalStyle }

            // 8. Payment Source
            r.createCell(8).apply { setCellValue(item.paymentMethod); cellStyle = normalStyle }

            // 9 & 10. Inflow & Outflow (Numeric cells!)
            val amtRupees = item.amountPaise / 100.0
            if (item.isIncome) {
                r.createCell(9).apply { setCellValue(amtRupees); cellStyle = incomeNumberStyle }
                r.createCell(10).apply { setCellValue(0.0); cellStyle = normalNumberStyle }
            } else {
                r.createCell(9).apply { setCellValue(0.0); cellStyle = normalNumberStyle }
                r.createCell(10).apply { setCellValue(amtRupees); cellStyle = expenseNumberStyle }
            }

            // 11. Attachment
            r.createCell(11).apply { setCellValue(if (!item.attachmentUri.isNullOrBlank()) "Yes" else "No"); cellStyle = normalStyle }
            // 12. Status
            r.createCell(12).apply { setCellValue(if (item.isVoided) "VOIDED" else "Active"); cellStyle = normalStyle }
        }

        // Transactions Totals Row
        val totalRow = txSheet.createRow(tRowIdx)
        totalRow.heightInPoints = 22f
        totalRow.createCell(0).apply { setCellValue("TOTAL ACTIVE"); cellStyle = totalStyle }
        for (col in 1..8) {
            totalRow.createCell(col).apply { setCellValue(""); cellStyle = totalStyle }
        }
        totalRow.createCell(9).apply { setCellValue(totalInflowPaise / 100.0); cellStyle = totalStyle }
        totalRow.createCell(10).apply { setCellValue(totalOutflowPaise / 100.0); cellStyle = totalStyle }
        totalRow.createCell(11).apply { setCellValue(""); cellStyle = totalStyle }
        totalRow.createCell(12).apply { setCellValue("${activeItems.size} active"); cellStyle = totalStyle }

        val txWidths = intArrayOf(
            12 * 256, // ID
            14 * 256, // Date
            11 * 256, // Time
            12 * 256, // Direction
            20 * 256, // Purpose
            18 * 256, // Category
            16 * 256, // Classification
            28 * 256, // Notes
            16 * 256, // Source
            16 * 256, // Inflow
            16 * 256, // Outflow
            12 * 256, // Attachment
            12 * 256  // Status
        )
        txWidths.forEachIndexed { i, w -> txSheet.setColumnWidth(i, w) }
        return workbook
    }

    /**
     * Export Cash Tracker items to a real multi-sheet Excel workbook and prepare a share Intent.
     */
    suspend fun exportCashTrackerExcel(
        items: List<ShopExpenseEntity>,
        periodTitle: String = "All Transactions",
        fromDate: Long? = null,
        toDate: Long? = null,
        activeFiltersSummary: String = "All Categories, Active Transactions"
    ): Intent = withContext(Dispatchers.IO) {
        val workbook = buildCashTrackerWorkbook(items, periodTitle, fromDate, toDate, activeFiltersSummary)

        // Meaningful dated filename: Ali_Educational_Cash_YYYY-MM-DD_to_YYYY-MM-DD.xlsx
        val dfFile = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val fromStr = if (fromDate != null && fromDate > 0L) dfFile.format(Date(fromDate)) else "start"
        val toStr = if (toDate != null && toDate < Long.MAX_VALUE) dfFile.format(Date(toDate)) else dfFile.format(Date())
        val fileName = "Ali_Educational_Cash_${fromStr}_to_${toStr}.xlsx"

        val exportFile = File(exportFolder, fileName)
        FileOutputStream(exportFile).use { fos ->
            workbook.write(fos)
        }

        val ctx = context ?: throw IllegalStateException("Context required for export")
        val exportDir = File(ctx.cacheDir, "exports").apply { if (!exists()) mkdirs() }
        val cacheFile = File(exportDir, fileName)
        exportFile.copyTo(cacheFile, overwrite = true)
        workbook.close()

        val shareUri = FileProvider.getUriForFile(
            ctx,
            "${ctx.packageName}.fileprovider",
            cacheFile
        )

        val activeItems = items.filter { !it.isVoided }
        val totalInflowPaise = activeItems.filter { it.isIncome }.sumOf { it.amountPaise }
        val totalOutflowPaise = activeItems.filter { !it.isIncome }.sumOf { it.amountPaise }
        val netMovementPaise = totalInflowPaise - totalOutflowPaise

        val shareMessage = buildString {
            appendLine("📚 *Ali Educational - Cash In & Out Ledger*")
            appendLine("📅 Period: $periodTitle")
            appendLine("🟢 Total Inflow: ${CostCalculator.formatPaiseToRupees(totalInflowPaise)}")
            appendLine("🔴 Total Outflow: ${CostCalculator.formatPaiseToRupees(totalOutflowPaise)}")
            appendLine("⚖️ Selected Net Movement: ${CostCalculator.formatPaiseToRupees(netMovementPaise)}")
            appendLine("📊 Matching Active Transactions: ${activeItems.size}")
            appendLine("Complete spreadsheet (.xlsx) attached.")
        }

        Intent(Intent.ACTION_SEND).apply {
            type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            putExtra(Intent.EXTRA_STREAM, shareUri)
            putExtra(Intent.EXTRA_SUBJECT, "Ali Educational Cash Ledger - $periodTitle")
            putExtra(Intent.EXTRA_TEXT, shareMessage)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    /**
     * Export Cash Tracker items directly to a destination OutputStream (e.g., SAF CreateDocument Uri).
     */
    suspend fun exportCashTrackerToStream(
        items: List<ShopExpenseEntity>,
        outputStream: java.io.OutputStream,
        periodTitle: String = "All Transactions",
        fromDate: Long? = null,
        toDate: Long? = null,
        activeFiltersSummary: String = "All Categories, Active Transactions"
    ) = withContext(Dispatchers.IO) {
        val workbook = buildCashTrackerWorkbook(items, periodTitle, fromDate, toDate, activeFiltersSummary)
        outputStream.use { os ->
            workbook.write(os)
            os.flush()
        }
        workbook.close()
    }

    private fun getCellString(cell: Cell?): String {
        if (cell == null) return ""
        return try {
            when (cell.cellType) {
                CellType.STRING -> cell.stringCellValue
                CellType.NUMERIC -> {
                    if (DateUtil.isCellDateFormatted(cell)) {
                        SimpleDateFormat("dd-MM-yyyy", Locale.US).format(cell.dateCellValue)
                    } else {
                        val num = cell.numericCellValue
                        if (num == num.toLong().toDouble()) num.toLong().toString() else num.toString()
                    }
                }
                CellType.BOOLEAN -> cell.booleanCellValue.toString()
                CellType.FORMULA -> {
                    try {
                        cell.stringCellValue
                    } catch (e: Exception) {
                        try {
                            cell.numericCellValue.toString()
                        } catch (e2: Exception) {
                            ""
                        }
                    }
                }
                else -> ""
            }
        } catch (e: Exception) {
            android.util.Log.w("CashTrackerExcelService", "Error reading cell string value: ${e.message}")
            ""
        }
    }

    private fun parseCellAmount(cell: Cell?): Double {
        if (cell == null) return 0.0
        return try {
            when (cell.cellType) {
                CellType.NUMERIC -> cell.numericCellValue
                CellType.STRING -> {
                    val raw = cell.stringCellValue
                        .replace("₹", "")
                        .replace("Rs.", "", ignoreCase = true)
                        .replace("Rs", "", ignoreCase = true)
                        .replace(",", "")
                        .trim()
                    raw.toDoubleOrNull() ?: 0.0
                }
                CellType.FORMULA -> {
                    try {
                        cell.numericCellValue
                    } catch (e: Exception) {
                        cell.stringCellValue.replace("₹", "").replace(",", "").trim().toDoubleOrNull() ?: 0.0
                    }
                }
                else -> 0.0
            }
        } catch (e: Exception) {
            0.0
        }
    }

    private fun parseCellDate(cell: Cell?): Long {
        if (cell == null) return System.currentTimeMillis()
        try {
            if (cell.cellType == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
                return cell.dateCellValue.time
            }
            val str = getCellString(cell).trim()
            if (str.isBlank()) return System.currentTimeMillis()

            val formats = listOf(
                "dd-MM-yyyy HH:mm:ss",
                "dd/MM/yyyy HH:mm:ss",
                "yyyy-MM-dd HH:mm:ss",
                "dd-MM-yyyy HH:mm",
                "dd/MM/yyyy HH:mm",
                "dd-MMM-yyyy",
                "dd-MM-yyyy",
                "dd/MM/yyyy",
                "yyyy-MM-dd",
                "d/M/yyyy",
                "d-M-yyyy",
                "d/M/yy",
                "d-M-yy"
            )

            for (pattern in formats) {
                try {
                    val sdf = SimpleDateFormat(pattern, Locale.US).apply { isLenient = false }
                    val d = sdf.parse(str)
                    if (d != null) return d.time
                } catch (ignored: Exception) {}
            }
        } catch (ignored: Exception) {}
        return System.currentTimeMillis()
    }

    private fun isRowEmpty(row: Row): Boolean {
        for (c in 0 until row.lastCellNum) {
            val cell = row.getCell(c)
            if (cell != null && cell.cellType != CellType.BLANK && getCellString(cell).isNotBlank()) {
                return false
            }
        }
        return true
    }
}
