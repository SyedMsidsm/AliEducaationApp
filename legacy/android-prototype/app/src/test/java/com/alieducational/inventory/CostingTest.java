package com.alieducational.inventory;
import org.junit.Test;
import static org.junit.Assert.*;
public class CostingTest {
    @Test public void proportionalDeliveryMatchesPlan(){
        assertArrayEquals(new long[]{2000,3000},Money.allocate(5000,new long[]{20000,30000}));
    }
    @Test public void movingAverageAndSaleMatchPlan(){
        long value=StockCost.receiveValue(10,10000,10,16000);
        assertEquals(26000,value);
        assertEquals(0,StockCost.averagePaise(20,value).compareTo(new java.math.BigDecimal("1300")));
        long cost=StockCost.issueCost(20,value,5);
        long net=10000-Money.discount(10000,"10");
        assertEquals(6500,cost);assertEquals(2500,net-cost);assertEquals(19500,value-cost);
    }
    @Test public void roundingReconcilesAndFinalIssueConsumesResidual(){
        long[] shares=Money.allocate(1,new long[]{1,1,1});
        assertEquals(1,shares[0]+shares[1]+shares[2]);
        long value=100,quantity=3,total=0;
        while(quantity>0){long cost=StockCost.issueCost(quantity,value,1);total+=cost;value-=cost;quantity--;}
        assertEquals(100,total);assertEquals(0,value);
    }
    @Test(expected=IllegalArgumentException.class) public void zeroGoodsRequireExplicitRule(){
        Money.allocate(500,new long[]{0,0});
    }
    @Test(expected=IllegalArgumentException.class) public void shortageCannotInventCost(){
        StockCost.issueCost(3,1000,4);
    }
    @Test(expected=IllegalArgumentException.class) public void negativeDeliveryRejected(){
        Money.allocate(-1,new long[]{1});
    }
}
