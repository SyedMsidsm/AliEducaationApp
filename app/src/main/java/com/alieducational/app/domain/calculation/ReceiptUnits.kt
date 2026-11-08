package com.alieducational.app.domain.calculation

import com.alieducational.app.data.local.entity.SaleLineEntity

/** Older bills without conversion snapshots retain their truthful base-piece count. */
object ReceiptUnits {
    private fun packSize(line: SaleLineEntity): Int? = line.unitLabel?.takeIf { line.isPack && it.startsWith("pack:") }
        ?.removePrefix("pack:")?.toIntOrNull()?.takeIf { it > 0 && line.quantityBase % it == 0 }
    fun quantity(line: SaleLineEntity): String {
        val pack=packSize(line)
        if (pack != null) return "${line.quantityBase / pack} pack(s) of $pack"
        val original=if (line.isPack) "pcs (pack sale)" else line.unitLabel?.takeUnless { it.startsWith("pack:") } ?: "pcs"
        val unit=if (line.quantityBase == 1) when (original.lowercase()) { "copies" -> "copy"; "sheets" -> "sheet"; "pcs" -> "pc"; else -> original } else original
        return "${line.quantityBase} $unit"
    }
    fun ratePaise(line: SaleLineEntity): Long {
        val pack=packSize(line) ?: return line.unitSellingPricePaise
        val packs=line.quantityBase / pack
        return if (packs > 0) line.lineGrossPaise / packs else line.unitSellingPricePaise
    }
}
