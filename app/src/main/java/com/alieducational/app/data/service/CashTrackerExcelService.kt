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
            val docs = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
            val dir = File(docs, "AliEducational/Exports")
            if (!dir.exists()) {
                dir.mkdirs()
            }
            return if (dir.exists()) dir else File(context?.getExternalFilesDir(null), "exports").apply { mkdirs() }
        }

    /**
     * Parse an Excel file (.xlsx or .xls) from content Uri into a preview of ShopExpenseEntity objects.
     */
    suspend fun parseExcelFile(uri: Uri, fileName: String): CashTrackerImportPreview = withContext(Dispatchers.IO) {
        try {
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

        // Find header row (usually row 0 or 1)
        var headerRowIndex = -1
        var dateCol = -1
        var descCol = -1
        var incomeCol = -1
        var expenseCol = -1
        var modeCol = -1
        var singleAmountCol = -1
        var typeCol = -1

        for (r in 0..minOf(5, sheet.lastRowNum)) {
            val row = sheet.getRow(r) ?: continue
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
                    text.contains("date") || text.contains("dated") || text.contains("दिनांक") || text.contains("तारीख") -> dCol = c
                    text.contains("desc") || text.contains("particular") || text.contains("category") ||
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

            // Use Notes as description if Category/Description is blank, as common in shop's historical files
            val finalDesc = when {
                desc.isNotBlank() -> desc
                mode.isNotBlank() -> mode
                else -> "Imported Entry"
            }
            val finalNotes = mode.ifBlank { "Excel Import" }

            if (incomePaise > 0L) {
                parsedItems.add(
                    ShopExpenseEntity(
                        category = finalDesc,
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
                        category = finalDesc,
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
     * Export Cash Tracker items to an Excel file and return an ACTION_SEND Intent to share directly.
     */
    suspend fun exportCashTrackerExcel(
        items: List<ShopExpenseEntity>,
        periodTitle: String = "All Transactions"
    ): Intent = withContext(Dispatchers.IO) {
        val workbook = XSSFWorkbook()
        val sheet = workbook.createSheet("Cash Book")

        // Setup Fonts and Styles
        val titleFont = workbook.createFont().apply {
            bold = true
            fontHeightInPoints = 14.toShort()
            color = IndexedColors.WHITE.index
        }
        val titleStyle = workbook.createCellStyle().apply {
            setFont(titleFont)
            fillForegroundColor = IndexedColors.DARK_BLUE.index
            fillPattern = FillPatternType.SOLID_FOREGROUND
            alignment = HorizontalAlignment.CENTER
            verticalAlignment = VerticalAlignment.CENTER
        }

        val headerFont = workbook.createFont().apply {
            bold = true
            fontHeightInPoints = 11.toShort()
            color = IndexedColors.WHITE.index
        }
        val headerStyle = workbook.createCellStyle().apply {
            setFont(headerFont)
            fillForegroundColor = IndexedColors.GREY_80_PERCENT.index
            fillPattern = FillPatternType.SOLID_FOREGROUND
            alignment = HorizontalAlignment.CENTER
            verticalAlignment = VerticalAlignment.CENTER
            borderBottom = BorderStyle.THIN
            borderTop = BorderStyle.THIN
            borderLeft = BorderStyle.THIN
            borderRight = BorderStyle.THIN
        }

        val incomeHeaderStyle = workbook.createCellStyle().apply {
            setFont(headerFont)
            fillForegroundColor = IndexedColors.DARK_GREEN.index
            fillPattern = FillPatternType.SOLID_FOREGROUND
            alignment = HorizontalAlignment.CENTER
            verticalAlignment = VerticalAlignment.CENTER
            borderBottom = BorderStyle.THIN
            borderTop = BorderStyle.THIN
        }

        val expenseHeaderStyle = workbook.createCellStyle().apply {
            setFont(headerFont)
            fillForegroundColor = IndexedColors.DARK_RED.index
            fillPattern = FillPatternType.SOLID_FOREGROUND
            alignment = HorizontalAlignment.CENTER
            verticalAlignment = VerticalAlignment.CENTER
            borderBottom = BorderStyle.THIN
            borderTop = BorderStyle.THIN
        }

        val normalStyle = workbook.createCellStyle().apply {
            borderBottom = BorderStyle.THIN
            borderTop = BorderStyle.THIN
            borderLeft = BorderStyle.THIN
            borderRight = BorderStyle.THIN
        }

        val currencyIncomeStyle = workbook.createCellStyle().apply {
            alignment = HorizontalAlignment.RIGHT
            val font = workbook.createFont().apply {
                color = IndexedColors.DARK_GREEN.index
                bold = true
            }
            setFont(font)
            borderBottom = BorderStyle.THIN
            borderTop = BorderStyle.THIN
            borderLeft = BorderStyle.THIN
            borderRight = BorderStyle.THIN
        }

        val currencyExpenseStyle = workbook.createCellStyle().apply {
            alignment = HorizontalAlignment.RIGHT
            val font = workbook.createFont().apply {
                color = IndexedColors.DARK_RED.index
                bold = true
            }
            setFont(font)
            borderBottom = BorderStyle.THIN
            borderTop = BorderStyle.THIN
            borderLeft = BorderStyle.THIN
            borderRight = BorderStyle.THIN
        }

        val totalStyle = workbook.createCellStyle().apply {
            val font = workbook.createFont().apply {
                bold = true
                fontHeightInPoints = 11.toShort()
            }
            setFont(font)
            fillForegroundColor = IndexedColors.GREY_25_PERCENT.index
            fillPattern = FillPatternType.SOLID_FOREGROUND
            borderBottom = BorderStyle.MEDIUM
            borderTop = BorderStyle.MEDIUM
            borderLeft = BorderStyle.THIN
            borderRight = BorderStyle.THIN
        }

        // Title row
        val titleRow = sheet.createRow(0)
        titleRow.heightInPoints = 28f
        val titleCell = titleRow.createCell(0)
        titleCell.setCellValue("Ali Educational - Cash In & Out Report")
        titleCell.cellStyle = titleStyle

        // Subtitle row (period info)
        val subRow = sheet.createRow(1)
        val exportDateStr = SimpleDateFormat("dd-MMM-yyyy hh:mm a", Locale.US).format(Date())
        subRow.createCell(0).setCellValue("Period: $periodTitle  |  Exported: $exportDateStr")

        // Table Header Row
        val headers = listOf(
            "Date",
            "Time",
            "Description / Particulars",
            "Payment Mode / Notes",
            "Cash In (Income ₹)",
            "Cash Out (Expense ₹)",
            "Running Balance (₹)"
        )

        val headerRow = sheet.createRow(3)
        headerRow.heightInPoints = 24f
        for (i in headers.indices) {
            val cell = headerRow.createCell(i)
            cell.setCellValue(headers[i])
            cell.cellStyle = when (i) {
                4 -> incomeHeaderStyle
                5 -> expenseHeaderStyle
                else -> headerStyle
            }
        }

        // Sort items chronologically for logical running balance
        val sortedItems = items.sortedBy { it.timestamp }
        val dateFormat = SimpleDateFormat("dd-MMM-yyyy", Locale.US)
        val timeFormat = SimpleDateFormat("hh:mm a", Locale.US)

        var runningBalancePaise = 0L
        var totalIncomePaise = 0L
        var totalExpensePaise = 0L
        var currentRowIdx = 4

        for (item in sortedItems) {
            val row = sheet.createRow(currentRowIdx++)
            val d = Date(item.timestamp)

            // Date
            val c0 = row.createCell(0)
            c0.setCellValue(dateFormat.format(d))
            c0.cellStyle = normalStyle

            // Time
            val c1 = row.createCell(1)
            c1.setCellValue(timeFormat.format(d))
            c1.cellStyle = normalStyle

            // Description
            val c2 = row.createCell(2)
            c2.setCellValue(item.category)
            c2.cellStyle = normalStyle

            // Payment Mode / Notes
            val c3 = row.createCell(3)
            c3.setCellValue(item.notes ?: "-")
            c3.cellStyle = normalStyle

            val amountRupees = item.amountPaise / 100.0

            if (item.isIncome) {
                totalIncomePaise += item.amountPaise
                runningBalancePaise += item.amountPaise

                val c4 = row.createCell(4)
                c4.setCellValue(String.format(Locale.US, "%.2f", amountRupees))
                c4.cellStyle = currencyIncomeStyle

                val c5 = row.createCell(5)
                c5.setCellValue("-")
                c5.cellStyle = normalStyle
            } else {
                totalExpensePaise += item.amountPaise
                runningBalancePaise -= item.amountPaise

                val c4 = row.createCell(4)
                c4.setCellValue("-")
                c4.cellStyle = normalStyle

                val c5 = row.createCell(5)
                c5.setCellValue(String.format(Locale.US, "%.2f", amountRupees))
                c5.cellStyle = currencyExpenseStyle
            }

            // Running Balance
            val c6 = row.createCell(6)
            c6.setCellValue(String.format(Locale.US, "%.2f", runningBalancePaise / 100.0))
            c6.cellStyle = normalStyle
        }

        // Totals Row
        val totalRow = sheet.createRow(currentRowIdx)
        totalRow.heightInPoints = 22f

        val t0 = totalRow.createCell(0)
        t0.setCellValue("TOTAL")
        t0.cellStyle = totalStyle

        totalRow.createCell(1).apply { setCellValue(""); cellStyle = totalStyle }
        totalRow.createCell(2).apply { setCellValue("${sortedItems.size} entries"); cellStyle = totalStyle }
        totalRow.createCell(3).apply { setCellValue(""); cellStyle = totalStyle }

        val tIncome = totalRow.createCell(4)
        tIncome.setCellValue(String.format(Locale.US, "%.2f", totalIncomePaise / 100.0))
        tIncome.cellStyle = totalStyle

        val tExpense = totalRow.createCell(5)
        tExpense.setCellValue(String.format(Locale.US, "%.2f", totalExpensePaise / 100.0))
        tExpense.cellStyle = totalStyle

        val tBal = totalRow.createCell(6)
        tBal.setCellValue(String.format(Locale.US, "%.2f", runningBalancePaise / 100.0))
        tBal.cellStyle = totalStyle

        // Explicit column widths in units of 1/256th character width
        // (Avoids POI autoSizeColumn which crashes on Android due to missing java.awt.font.FontRenderContext)
        val columnWidths = intArrayOf(
            14 * 256, // Date (dd-MMM-yyyy)
            11 * 256, // Time (hh:mm a)
            32 * 256, // Description / Particulars
            22 * 256, // Payment Mode / Notes
            16 * 256, // Cash In (₹)
            16 * 256, // Cash Out (₹)
            18 * 256  // Running Balance (₹)
        )
        for (i in columnWidths.indices) {
            sheet.setColumnWidth(i, columnWidths[i])
        }

        val timeStampStr = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val fileName = "CashBook_AliEducational_$timeStampStr.xlsx"

        // 1. Save to public Documents/AliEducational/Exports (visible in My Files)
        val exportFile = File(exportFolder, fileName)
        FileOutputStream(exportFile).use { fos ->
            workbook.write(fos)
        }

        val ctx = context ?: throw IllegalStateException("Context required for export")
        // 2. Also save to cache for guaranteed FileProvider sharing
        val cacheFile = File(ctx.cacheDir, fileName)
        exportFile.copyTo(cacheFile, overwrite = true)
        workbook.close()

        val shareUri = FileProvider.getUriForFile(
            ctx,
            "${ctx.packageName}.fileprovider",
            cacheFile
        )

        val netBalanceRupees = (totalIncomePaise - totalExpensePaise) / 100.0
        val shareMessage = buildString {
            appendLine("📚 *Ali Educational - Cash Book Report*")
            appendLine("📅 Period: $periodTitle")
            appendLine("🟢 Total Income: ₹${String.format(Locale.US, "%,.2f", totalIncomePaise / 100.0)}")
            appendLine("🔴 Total Expense: ₹${String.format(Locale.US, "%,.2f", totalExpensePaise / 100.0)}")
            appendLine("⚖️ Net Balance: ₹${String.format(Locale.US, "%,.2f", netBalanceRupees)}")
            appendLine("📊 Total Transactions: ${sortedItems.size}")
            appendLine("\nComplete spreadsheet (.xlsx) attached.")
        }

        Intent(Intent.ACTION_SEND).apply {
            type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            putExtra(Intent.EXTRA_STREAM, shareUri)
            putExtra(Intent.EXTRA_SUBJECT, "Ali Educational Cash Book - $periodTitle")
            putExtra(Intent.EXTRA_TEXT, shareMessage)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
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
