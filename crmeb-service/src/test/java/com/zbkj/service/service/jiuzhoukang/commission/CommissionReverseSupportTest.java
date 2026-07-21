package com.zbkj.service.service.jiuzhoukang.commission;

import org.junit.Assert;
import org.junit.Test;
import java.math.BigDecimal;

public class CommissionReverseSupportTest {
    @Test public void calculatesRemainingFromPriorReverseInsteadOfSettledAmount() {
        Assert.assertEquals(new BigDecimal("80.00"), CommissionReverseSupport.remaining(new BigDecimal("100.00"), new BigDecimal("20.00")));
    }
    @Test(expected = IllegalArgumentException.class) public void rejectsReverseAboveRemainingAmount() {
        CommissionReverseSupport.requireReverseAmount(new BigDecimal("80.00"), new BigDecimal("80.01"));
    }
    @Test public void createsNegativeOffsetOnlyForShortfall() {
        CommissionReverseSupport.FundReverse balance = CommissionReverseSupport.reverseFundAvailable(new BigDecimal("30.00"), new BigDecimal("50.00"), BigDecimal.ZERO);
        Assert.assertEquals(new BigDecimal("0.00"), balance.getAvailableAmount());
        Assert.assertEquals(new BigDecimal("20.00"), balance.getNegativeOffsetAmount());
    }
}