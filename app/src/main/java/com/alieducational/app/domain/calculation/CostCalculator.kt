package com.alieducational.app.domain.calculation

data class PurchaseLineInput(
    val id: String,
    val quantity: Int,
    val unitPricePaise: Long, // in paise
) {
    val totalValuePaise: Long = Math.multiplyExact(quantity.toLong(), unitPricePaise)
}

data class AllocatedDeliveryResult(
    val lineId: String,
    val goodsValuePaise: Long,
    val allocatedDeliveryPaise: Long,
    val totalLandedCostPaise: Long,
    val effectiveUnitCostPaise: Long, // rounded to nearest paise for unit display/storage
)

data class SaleLineInput(
    val id: String,
    val quantity: Int,
    val unitSellingPricePaise: Long,
    val unitCostPaise: Long?, // null if cost unknown
) {
    val grossTotalPaise: Long = Math.multiplyExact(quantity.toLong(), unitSellingPricePaise)
}

data class AllocatedDiscountLineResult(
    val lineId: String,
    val quantity: Int,
    val grossValuePaise: Long,
    val allocatedDiscountPaise: Long,
    val netValuePaise: Long,
    val unitCostPaise: Long?,
    val actualProfitPaise: Long?, // null if unitCost is null
)

object CostCalculator {
    private fun java.math.BigInteger.checkedLong(): Long {
        if (bitLength() > 63) throw ArithmeticException("Amount exceeds supported range")
        return toLong()
    }


    enum class PricingMode {
        MARKUP,        // Markup on cost: P = C * (1 + r)
        GROSS_MARGIN   // Gross margin on selling price: P = C / (1 - r)
    }

    /**
     * Proportional delivery allocation according to each line's purchase value.
     * Line delivery share = total delivery * line purchase value / total purchase value.
     * Rounding differences are reconciled so sum of allocated delivery == total delivery exactly.
     * Rejects non-zero delivery if total goods value is 0.
     */
    fun allocateDelivery(
        lines: List<PurchaseLineInput>,
        totalDeliveryPaise: Long
    ): List<AllocatedDeliveryResult> {
        if (lines.isEmpty()) return emptyList()
        if (totalDeliveryPaise <= 0L) {
            return lines.map {
                val goods = it.totalValuePaise
                AllocatedDeliveryResult(
                    lineId = it.id,
                    goodsValuePaise = goods,
                    allocatedDeliveryPaise = 0L,
                    totalLandedCostPaise = goods,
                    effectiveUnitCostPaise = if (it.quantity > 0) goods / it.quantity else 0L
                )
            }
        }

        val totalGoodsValue = lines.fold(0L) { sum, line -> Math.addExact(sum, line.totalValuePaise) }
        if (totalGoodsValue == 0L) {
            val perLine = totalDeliveryPaise / lines.size
            val remainder = totalDeliveryPaise % lines.size
            return lines.mapIndexed { index, line ->
                val alloc = perLine + if (index == 0) remainder else 0L
                val totalLanded = Math.addExact(line.totalValuePaise, alloc)
                val unitCost = if (line.quantity > 0) (totalLanded + (line.quantity / 2)) / line.quantity else 0L
                AllocatedDeliveryResult(
                    lineId = line.id,
                    goodsValuePaise = line.totalValuePaise,
                    allocatedDeliveryPaise = alloc,
                    totalLandedCostPaise = totalLanded,
                    effectiveUnitCostPaise = unitCost
                )
            }
        }

        // Proportional allocation in exact paise
        val initialAllocations = lines.map { line ->
            val share = java.math.BigInteger.valueOf(totalDeliveryPaise).multiply(java.math.BigInteger.valueOf(line.totalValuePaise)).divide(java.math.BigInteger.valueOf(totalGoodsValue)).checkedLong()
            line to share
        }

        val allocatedSum = initialAllocations.sumOf { it.second }
        val discrepancy = totalDeliveryPaise - allocatedSum

        // Add discrepancy to line with largest purchase value
        val largestIndex = lines.indices.maxByOrNull { lines[it].totalValuePaise } ?: 0

        return initialAllocations.mapIndexed { index, (line, initialShare) ->
            val finalAlloc = if (index == largestIndex) initialShare + discrepancy else initialShare
            val totalLanded = Math.addExact(line.totalValuePaise, finalAlloc)
            val unitCost = if (line.quantity > 0) (totalLanded + (line.quantity / 2)) / line.quantity else 0L

            AllocatedDeliveryResult(
                lineId = line.id,
                goodsValuePaise = line.totalValuePaise,
                allocatedDeliveryPaise = finalAlloc,
                totalLandedCostPaise = totalLanded,
                effectiveUnitCostPaise = unitCost
            )
        }
    }

    /**
     * Calculates selling price in paise from cost and percentage.
     * Markup: P = C * (1 + r)
     * Gross Margin: P = C / (1 - r)
     * Returns null if mode is Gross Margin and percentage >= 100.0 (infinite/invalid price).
     */
    fun calculatePriceFromPercentage(
        costPaise: Long,
        percentage: Double,
        mode: PricingMode
    ): Long? {
        if (costPaise < 0L || !percentage.isFinite()) return null
        val r = percentage / 100.0
        return when (mode) {
            PricingMode.MARKUP -> {
                val p = costPaise.toDouble() * (1.0 + r)
                if (!p.isFinite() || p >= Long.MAX_VALUE.toDouble()) null else if (p < 0.0) 0L else kotlin.math.round(p).toLong()
            }
            PricingMode.GROSS_MARGIN -> {
                if (r >= 1.0) return null // Margin >= 100% cannot produce finite price
                val p = costPaise.toDouble() / (1.0 - r)
                if (!p.isFinite() || p >= Long.MAX_VALUE.toDouble()) null else if (p < 0.0) 0L else kotlin.math.round(p).toLong()
            }
        }
    }

    /**
     * Calculates percentage from cost and selling price.
     * Markup (%): 100 * (P - C) / C. Undefined (null) if C == 0.
     * Gross Margin (%): 100 * (P - C) / P. Undefined (null) if P == 0.
     */
    fun calculatePercentageFromPrice(
        costPaise: Long,
        pricePaise: Long,
        mode: PricingMode
    ): Double? {
        val profitPaise = pricePaise - costPaise
        return when (mode) {
            PricingMode.MARKUP -> {
                if (costPaise <= 0L) null
                else (profitPaise.toDouble() * 100.0) / costPaise.toDouble()
            }
            PricingMode.GROSS_MARGIN -> {
                if (pricePaise <= 0L) null
                else (profitPaise.toDouble() * 100.0) / pricePaise.toDouble()
            }
        }
    }

    /**
     * Moving weighted-average cost implementation per product/variant:
     * new average = (existing stock cost value + incoming goods cost including allocated delivery)
     *               / (existing quantity + incoming quantity)
     * If existing stock is zero or negative, the incoming stock establishes the new average cost.
     */
    fun calculateMovingWeightedAverage(
        existingStockQuantity: Int,
        existingAverageCostPaise: Long,
        incomingQuantity: Int,
        incomingTotalLandedCostPaise: Long
    ): Long {
        if (incomingQuantity <= 0) return existingAverageCostPaise
        if (existingStockQuantity <= 0) {
            return Math.addExact(incomingTotalLandedCostPaise, (incomingQuantity / 2).toLong()) / incomingQuantity
        }

        val existingTotalValue = Math.multiplyExact(existingStockQuantity.toLong(), existingAverageCostPaise)
        val combinedValue = Math.addExact(existingTotalValue, incomingTotalLandedCostPaise)
        val combinedQuantity = Math.addExact(existingStockQuantity, incomingQuantity).toLong()

        return Math.addExact(combinedValue, combinedQuantity / 2) / combinedQuantity
    }

    /**
     * Allocates whole-sale discount proportionally across sale lines by line gross value.
     * Reconciles rounding remainder onto the largest line so sum of discounts == totalDiscount.
     */
    fun allocateWholeSaleDiscount(
        lines: List<SaleLineInput>,
        totalDiscountPaise: Long
    ): List<AllocatedDiscountLineResult> {
        if (lines.isEmpty()) return emptyList()
        val totalGross = lines.fold(0L) { sum, line -> Math.addExact(sum, line.grossTotalPaise) }

        if (totalDiscountPaise <= 0L || totalGross <= 0L) {
            return lines.map { line ->
                val cost = line.unitCostPaise
                val profit = if (cost != null) line.grossTotalPaise - Math.multiplyExact(line.quantity.toLong(), cost) else null
                AllocatedDiscountLineResult(
                    lineId = line.id,
                    quantity = line.quantity,
                    grossValuePaise = line.grossTotalPaise,
                    allocatedDiscountPaise = 0L,
                    netValuePaise = line.grossTotalPaise,
                    unitCostPaise = cost,
                    actualProfitPaise = profit
                )
            }
        }

        val initialAllocations = lines.map { line ->
            val share = java.math.BigInteger.valueOf(totalDiscountPaise).multiply(java.math.BigInteger.valueOf(line.grossTotalPaise)).divide(java.math.BigInteger.valueOf(totalGross)).checkedLong()
            line to share
        }

        val allocatedSum = initialAllocations.sumOf { it.second }
        val discrepancy = totalDiscountPaise - allocatedSum
        val largestIndex = lines.indices.maxByOrNull { lines[it].grossTotalPaise } ?: 0

        return initialAllocations.mapIndexed { index, (line, initialShare) ->
            val finalDiscount = if (index == largestIndex) initialShare + discrepancy else initialShare
            val net = line.grossTotalPaise - finalDiscount
            val cost = line.unitCostPaise
            val profit = if (cost != null) net - Math.multiplyExact(line.quantity.toLong(), cost) else null

            AllocatedDiscountLineResult(
                lineId = line.id,
                quantity = line.quantity,
                grossValuePaise = line.grossTotalPaise,
                allocatedDiscountPaise = finalDiscount,
                netValuePaise = net,
                unitCostPaise = cost,
                actualProfitPaise = profit
            )
        }
    }

    /**
     * Converts a base quantity into familiar "X boxes + Y pieces (total Z pieces)"
     */
    fun formatBoxesAndPieces(totalBaseUnits: Int, packSize: Int): Pair<Int, Int> {
        val safePackSize = if (packSize > 0) packSize else 1
        val boxes = totalBaseUnits / safePackSize
        val pieces = totalBaseUnits % safePackSize
        return Pair(boxes, pieces)
    }

    /**
     * Formats paise to Indian Rupees string e.g. 10050 -> "₹100.50"
     */
    /**
     * Parses a string representation of Rupees (e.g. "123.45", "48", "47.50", "0") into exact paise.
     * Rejects malformed strings, negative amounts, and excess decimal precision (> 2 decimals).
     */
    fun parseRupeesToPaise(input: String?): Long? {
        if (input == null) return null
        val trimmed = input.replace("₹", "").replace(",", "").trim()
        if (!Regex("[0-9]+(\\.[0-9]{0,2})?").matches(trimmed)) return null
        return try {
            java.math.BigDecimal(trimmed).movePointRight(2).longValueExact()
        } catch (_: ArithmeticException) { null }
          catch (_: NumberFormatException) { null }
    }

    /**
     * Formats an integer value according to Indian number grouping system:
     * - Last 3 digits grouped together
     * - Preceding digits grouped in sets of 2
     * e.g. 999 -> "999", 1000 -> "1,000", 12235 -> "12,235", 100000 -> "1,00,000", 1234567 -> "12,34,567"
     */
    fun formatIndianGrouping(value: Long): String {
        val s = kotlin.math.abs(value).toString()
        if (s.length <= 3) return s
        val lastThree = s.takeLast(3)
        val remaining = s.dropLast(3)
        val sb = StringBuilder()
        var i = remaining.length
        while (i > 0) {
            val start = kotlin.math.max(0, i - 2)
            val part = remaining.substring(start, i)
            if (sb.isNotEmpty()) {
                sb.insert(0, ",")
            }
            sb.insert(0, part)
            i -= 2
        }
        return "$sb,$lastThree"
    }

    /**
     * Formats integer quantity with Indian grouping and optional unit (e.g. 1,23,456 pcs).
     */
    fun formatQuantityWithGrouping(qty: Int, unit: String? = null): String {
        val isNegative = qty < 0
        val absQty = if (isNegative) -qty else qty
        val grouped = formatIndianGrouping(absQty.toLong())
        val sign = if (isNegative) "-" else ""
        return if (unit.isNullOrBlank()) "$sign$grouped" else "$sign$grouped $unit"
    }

    fun formatPaiseToRupees(paise: Long): String {
        val isNegative = paise < 0
        val absPaise = if (isNegative) -paise else paise
        val rupees = absPaise / 100
        val remainder = absPaise % 100
        val prefix = if (isNegative) "-₹" else "₹"
        val groupedRupees = formatIndianGrouping(rupees)
        return String.format(java.util.Locale.US, "%s%s.%02d", prefix, groupedRupees, remainder)
    }
}
