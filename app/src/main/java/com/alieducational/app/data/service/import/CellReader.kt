package com.alieducational.app.data.service.`import`

import org.apache.poi.ss.usermodel.Cell
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.DataFormatter
import org.apache.poi.ss.usermodel.DateUtil
import org.apache.poi.ss.usermodel.FormulaEvaluator
import org.apache.poi.ss.usermodel.Row
import java.text.SimpleDateFormat
import java.util.Locale

object CellReader {
    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT)

    /**
     * Reads a cell using Apache POI DataFormatter and FormulaEvaluator.
     * Preserves displayed format, evaluates formulas, preserves leading zeros from cell format,
     * and avoids silent ".0" corruption on integer numbers.
     */
    fun readCell(cell: Cell?, evaluator: FormulaEvaluator? = null, formatter: DataFormatter? = null): String {
        if (cell == null) return ""
        val fmt = formatter ?: DataFormatter()

        return try {
            when (cell.cellType) {
                CellType.FORMULA -> {
                    if (evaluator != null) {
                        try {
                            val evalVal = evaluator.evaluate(cell)
                            when (evalVal?.cellType) {
                                CellType.NUMERIC -> {
                                    if (DateUtil.isCellDateFormatted(cell)) {
                                        dateFormatter.format(cell.dateCellValue)
                                    } else {
                                        formatNumeric(evalVal.numberValue, fmt.formatCellValue(cell, evaluator))
                                    }
                                }
                                CellType.STRING -> evalVal.stringValue ?: ""
                                CellType.BOOLEAN -> evalVal.booleanValue.toString()
                                else -> fmt.formatCellValue(cell, evaluator)
                            }
                        } catch (_: Exception) {
                            fmt.formatCellValue(cell)
                        }
                    } else {
                        fmt.formatCellValue(cell)
                    }
                }
                CellType.NUMERIC -> {
                    if (DateUtil.isCellDateFormatted(cell)) {
                        dateFormatter.format(cell.dateCellValue)
                    } else {
                        val formatted = fmt.formatCellValue(cell)
                        formatNumeric(cell.numericCellValue, formatted)
                    }
                }
                CellType.STRING -> cell.stringCellValue?.trim() ?: ""
                CellType.BOOLEAN -> cell.booleanCellValue.toString()
                CellType.BLANK -> ""
                CellType.ERROR -> ""
                else -> fmt.formatCellValue(cell)
            }
        } catch (_: Exception) {
            ""
        }.trim()
    }

    private fun formatNumeric(num: Double, formattedText: String): String {
        val trimmed = formattedText.trim()
        if (trimmed.isNotEmpty() && !trimmed.contains("E", ignoreCase = true)) {
            if (trimmed.endsWith(".0") && num == num.toLong().toDouble()) {
                return trimmed.dropLast(2)
            }
            return trimmed
        }
        return if (num == num.toLong().toDouble()) {
            num.toLong().toString()
        } else {
            num.toString()
        }
    }

    fun readRow(row: Row?, evaluator: FormulaEvaluator? = null, formatter: DataFormatter? = null): List<String> {
        if (row == null) return emptyList()
        val lastCell = row.lastCellNum.toInt()
        if (lastCell <= 0) return emptyList()
        return (0 until lastCell).map { idx ->
            readCell(row.getCell(idx), evaluator, formatter)
        }
    }
}
