package com.alieducational.inventory;
import org.junit.Test;
import static org.junit.Assert.*;
public class PurchaseTotalsTest {
    @Test public void supplierDeliveryIncludedExactlyOnce(){
        PurchaseTotals p=new PurchaseTotals(new long[]{20000,30000},5000,20000,true,null);
        assertArrayEquals(new long[]{22000,33000},p.landedValues);
        assertEquals(55000,p.stockCost);assertEquals(35000,p.supplierDue);assertEquals(0,p.carrierPaid);
    }
    @Test public void separatelyPaidCarrierDoesNotIncreaseSupplierDebt(){
        PurchaseTotals p=new PurchaseTotals(new long[]{20000,30000},5000,20000,false,null);
        assertEquals(55000,p.stockCost);assertEquals(30000,p.supplierDue);assertEquals(5000,p.carrierPaid);
    }
    @Test public void freeGoodsRequireReconciledExplicitShares(){
        PurchaseTotals p=new PurchaseTotals(new long[]{0,0},500,0,true,new long[]{200,300});
        assertArrayEquals(new long[]{200,300},p.landedValues);assertEquals(500,p.supplierDue);
    }
    @Test(expected=IllegalArgumentException.class) public void mismatchedSharesRejected(){
        new PurchaseTotals(new long[]{0,0},500,0,true,new long[]{200,200});
    }
    @Test(expected=IllegalArgumentException.class) public void missingSharesRejected(){
        new PurchaseTotals(new long[]{0},500,0,true,null);
    }
    @Test(expected=IllegalArgumentException.class) public void supplierOverpaymentNotSilentlyDiscarded(){
        new PurchaseTotals(new long[]{100},50,151,true,null);
    }
}
