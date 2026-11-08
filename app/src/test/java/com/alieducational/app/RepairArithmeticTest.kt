package com.alieducational.app

import com.alieducational.app.domain.calculation.*
import org.junit.Test
import org.junit.Assert.*

class RepairArithmeticTest {
    @Test fun receiptUsesSnapshotPackQuantityAndExactRate() {
        val line=com.alieducational.app.data.local.entity.SaleLineEntity(id="l",saleId="s",productId="p",productName="Pens",quantityBase=20,isPack=true,unitSellingPricePaise=1500,unitCostSnapshotPaise=1000,lineGrossPaise=30000,lineDiscountAllocatedPaise=0,lineNetPaise=30000,lineProfitPaise=10000,unitLabel="pack:10")
        assertEquals("2 pack(s) of 10",ReceiptUnits.quantity(line))
        assertEquals(15000L,ReceiptUnits.ratePaise(line))
        assertEquals("20 pcs (pack sale)",ReceiptUnits.quantity(line.copy(unitLabel=null)))
    }
    @Test fun proportionalAllocationDoesNotOverflowIntermediateProduct() {
        val results=CostCalculator.allocateDelivery(listOf(PurchaseLineInput("a",1,4_000_000_000L),PurchaseLineInput("b",1,6_000_000_000L)),1_000_000_000L)
        assertEquals(400_000_000L,results[0].allocatedDeliveryPaise)
        assertEquals(600_000_000L,results[1].allocatedDeliveryPaise)
    }
    @Test fun discountAllocationRemainsExactAtLargeValues() {
        val result=CostCalculator.allocateWholeSaleDiscount(listOf(SaleLineInput("a",1,4_000_000_000L,1L),SaleLineInput("b",1,6_000_000_000L,1L)),1_000_000_000L)
        assertEquals(1_000_000_000L,result.sumOf { it.allocatedDiscountPaise })
        assertEquals(9_000_000_000L,result.sumOf { it.netValuePaise })
    }
    @Test fun oversizedPriceIsRejected() {
        assertNull(CostCalculator.calculatePriceFromPercentage(Long.MAX_VALUE,100.0,CostCalculator.PricingMode.MARKUP))
        assertNull(CostCalculator.parseRupeesToPaise("999999999999999999999"))
    }
    @Test fun stockOverflowIsRejected() {
        assertTrue(runCatching { CostCalculator.calculateMovingWeightedAverage(Int.MAX_VALUE,1,1,1) }.isFailure)
    }
    @Test fun markupAndMarginRemainDistinct() {
        assertEquals(12000L,CostCalculator.calculatePriceFromPercentage(10000L,20.0,CostCalculator.PricingMode.MARKUP))
        assertEquals(12500L,CostCalculator.calculatePriceFromPercentage(10000L,20.0,CostCalculator.PricingMode.GROSS_MARGIN))
    }
}
