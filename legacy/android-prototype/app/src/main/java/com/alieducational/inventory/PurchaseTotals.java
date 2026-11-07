package com.alieducational.inventory;

/** Checked purchase preview shared by posting and UI. All monetary inputs are paise. */
public final class PurchaseTotals {
    public final long[] deliveryShares;
    public final long[] landedValues;
    public final long goodsTotal, stockCost, supplierBill, supplierDue, carrierPaid;
    public PurchaseTotals(long[] goods,long delivery,long supplierPayment,boolean deliveryToSupplier,long[] explicitShares) {
        if(goods.length==0 || delivery<0 || supplierPayment<0)throw new IllegalArgumentException("Invalid purchase amounts");
        long sum=0;
        for(long value:goods){if(value<0)throw new IllegalArgumentException("Negative goods value");sum=Math.addExact(sum,value);}
        goodsTotal=sum;
        if(sum==0 && delivery>0){
            if(explicitShares==null || explicitShares.length!=goods.length)throw new IllegalArgumentException("Enter each delivery share");
            long allocated=0;
            for(long share:explicitShares){if(share<0)throw new IllegalArgumentException("Negative delivery share");allocated=Math.addExact(allocated,share);}
            if(allocated!=delivery)throw new IllegalArgumentException("Delivery shares must equal delivery total");
            deliveryShares=explicitShares.clone();
        }else deliveryShares=Money.allocate(delivery,goods);
        landedValues=new long[goods.length];
        for(int i=0;i<goods.length;i++)landedValues[i]=Math.addExact(goods[i],deliveryShares[i]);
        stockCost=Math.addExact(sum,delivery);
        supplierBill=deliveryToSupplier?stockCost:sum;
        if(supplierPayment>supplierBill)throw new IllegalArgumentException("Payment exceeds this supplier bill");
        supplierDue=supplierBill-supplierPayment;
        carrierPaid=deliveryToSupplier?0:delivery;
    }
}
