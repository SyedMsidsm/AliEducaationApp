package com.alieducational.inventory;
import java.math.BigDecimal;
import java.math.RoundingMode;

/** Known, nonnegative inventory only. Costs are total paise, not rounded unit prices. */
public final class StockCost {
    private StockCost(){}
    public static long receiveValue(long oldQuantity,long oldValue,long incomingQuantity,long landedValue) {
        if(oldQuantity<0 || oldValue<0 || incomingQuantity<=0 || landedValue<0)
            throw new IllegalArgumentException("Cost requires resolved nonnegative stock");
        if(oldQuantity==0 && oldValue!=0)throw new IllegalArgumentException("Residual cost needs review");
        return Math.addExact(oldValue,landedValue);
    }
    public static long issueCost(long quantity,long value,long issued) {
        if(quantity<=0 || value<0 || issued<=0 || issued>quantity)
            throw new IllegalArgumentException("Cost unavailable for shortage or invalid quantity");
        return BigDecimal.valueOf(value).multiply(BigDecimal.valueOf(issued))
            .divide(BigDecimal.valueOf(quantity),0,RoundingMode.HALF_UP).longValueExact();
    }
    public static BigDecimal averagePaise(long quantity,long value) {
        if(quantity<=0 || value<0)throw new IllegalArgumentException("Average unavailable");
        return BigDecimal.valueOf(value).divide(BigDecimal.valueOf(quantity),12,RoundingMode.HALF_UP);
    }
}
