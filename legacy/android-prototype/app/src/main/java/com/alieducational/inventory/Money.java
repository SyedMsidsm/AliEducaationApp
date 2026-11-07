package com.alieducational.inventory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;

public final class Money {
    private Money() {}
    public static long parse(String value) {
        if (value == null || value.trim().isEmpty()) throw new IllegalArgumentException("Enter an amount");
        return new BigDecimal(value.replace(",", "").replace("₹", "").trim()).setScale(2, RoundingMode.UNNECESSARY).movePointRight(2).longValueExact();
    }
    public static long nonnegative(String value) { long n=parse(value); if(n<0)throw new IllegalArgumentException("Amount cannot be negative");return n; }
    public static String text(long paise) { return "₹" + String.format(Locale.ENGLISH,"%,.2f", BigDecimal.valueOf(paise,2)); }
    public static String input(long paise) { return BigDecimal.valueOf(paise,2).toPlainString(); }
    public static long fraction(long total,long part,long whole) {
        if(whole<=0)throw new IllegalArgumentException("Allocation requires a positive quantity or value");
        return BigDecimal.valueOf(total).multiply(BigDecimal.valueOf(part)).divide(BigDecimal.valueOf(whole),0,RoundingMode.HALF_UP).longValueExact();
    }
    public static long discount(long original,String percent) {
        BigDecimal p=new BigDecimal(percent);
        if(p.signum()<0||p.compareTo(new BigDecimal("100"))>0)throw new IllegalArgumentException("Discount must be 0 to 100%");
        return BigDecimal.valueOf(original).multiply(p).divide(new BigDecimal("100"),0,RoundingMode.HALF_UP).longValueExact();
    }
    public static long[] allocate(long total,long[] weights) {
        if(total<0)throw new IllegalArgumentException("Allocation total cannot be negative");
        long sum=0;for(long w:weights){if(w<0)throw new IllegalArgumentException("Negative allocation value");sum=Math.addExact(sum,w);}
        if(total==0)return new long[weights.length];
        if(sum==0)throw new IllegalArgumentException("Zero-value goods: choose explicit delivery allocations");
        long[] out=new long[weights.length];long assigned=0,cumulative=0;
        for(int i=0;i<weights.length;i++){cumulative=Math.addExact(cumulative,weights[i]);long target=fraction(total,cumulative,sum);out[i]=target-assigned;assigned=target;}
        return out;
    }
    public static long quantity(String s) { long q=Long.parseLong(s.trim()); if(q<=0||q>100000000)throw new IllegalArgumentException("Enter a positive whole quantity");return q; }
}
