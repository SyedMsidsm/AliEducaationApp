package com.alieducational.app.domain.calculation

data class PurchaseLineInput(
    val id: String,
    val quantity: Int,
    val unitPricePaise: Long, // in paise
) {
    val totalValuePaise: Long = quantity * unitPricePaise
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
    val grossTotalPaise: Long = quantity * unitSellingPricePaise
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

    /**
     * Proportional delivery allocation according to each line's purchase value.
     * Line delivery share = total delivery * line purchase value / total purchase value.
     * Rounding differences are reconciled so sum of allocated delivery == total delivery exactly.
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

        val totalGoodsValue = lines.sumOf { it.totalValuePaise }

        // If total goods value is 0 (e.g. all free samples), split delivery equally
        if (totalGoodsValue == 0L) {
            val count = lines.size
            val baseShare = totalDeliveryPaise / count
            var remainder = totalDeliveryPaise % count

            return lines.map { line ->
                val extra = if (remainder > 0) { remainder--; 1L } else 0L
                val alloc = baseShare + extra
                AllocatedDeliveryResult(
                    lineId = line.id,
                    goodsValuePaise = 0L,
                    allocatedDeliveryPaise = alloc,
                    totalLandedCostPaise = alloc,
                    effectiveUnitCostPaise = if (line.quantity > 0) alloc / line.quantity else 0L
                )
            }
        }

        // Proportional allocation
        val initialAllocations = lines.map { line ->
            val share = (totalDeliveryPaise * line.totalValuePaise) / totalGoodsValue
            line to share
        }

        val allocatedSum = initialAllocations.sumOf { it.second }
        val discrepancy = totalDeliveryPaise - allocatedSum

        // Add discrepancy to line with largest purchase value
        val largestIndex = lines.indices.maxByOrNull { lines[it].totalValuePaise } ?: 0

        return initialAllocations.mapIndexed { index, (line, initialShare) ->
            val finalAlloc = if (index == largestIndex) initialShare + discrepancy else initialShare
            val totalLanded = line.totalValuePaise + finalAlloc
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
            return (incomingTotalLandedCostPaise + (incomingQuantity / 2)) / incomingQuantity
        }

        val existingTotalValue = existingStockQuantity.toLong() * existingAverageCostPaise
        val combinedValue = existingTotalValue + incomingTotalLandedCostPaise
        val combinedQuantity = (existingStockQuantity + incomingQuantity).toLong()

        return (combinedValue + (combinedQuantity / 2)) / combinedQuantity
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
        val totalGross = lines.sumOf { it.grossTotalPaise }

        if (totalDiscountPaise <= 0L || totalGross <= 0L) {
            return lines.map { line ->
                val cost = line.unitCostPaise
                val profit = if (cost != null) line.grossTotalPaise - (line.quantity * cost) else null
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
            val share = (totalDiscountPaise * line.grossTotalPaise) / totalGross
            line to share
        }

        val allocatedSum = initialAllocations.sumOf { it.second }
        val discrepancy = totalDiscountPaise - allocatedSum
        val largestIndex = lines.indices.maxByOrNull { lines[it].grossTotalPaise } ?: 0

        return initialAllocations.mapIndexed { index, (line, initialShare) ->
            val finalDiscount = if (index == largestIndex) initialShare + discrepancy else initialShare
            val net = line.grossTotalPaise - finalDiscount
            val cost = line.unitCostPaise
            val profit = if (cost != null) net - (line.quantity * cost) else null

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
    fun formatPaiseToRupees(paise: Long): String {
        val isNegative = paise < 0
        val absPaise = if (isNegative) -paise else paise
        val rupees = absPaise / 100
        val remainder = absPaise % 100
        val prefix = if (isNegative) "-₹" else "₹"
        return String.format("%s%d.%02d", prefix, rupees, remainder)
    }
}