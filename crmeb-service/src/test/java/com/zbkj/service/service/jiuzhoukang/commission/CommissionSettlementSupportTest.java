package com.zbkj.service.service.jiuzhoukang.commission;

import org.junit.Assert;
import org.junit.Test;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;

public class CommissionSettlementSupportTest {
    @Test public void sumsSelectedPendingRecordAmounts() {
        Assert.assertEquals(new BigDecimal("15.00"), CommissionSettlementSupport.requireTotal(Arrays.asList(new BigDecimal("10.00"), new BigDecimal("5.00"))));
    }
    @Test(expected = IllegalArgumentException.class) public void rejectsEmptySettlementSelection() {
        CommissionSettlementSupport.requireTotal(Collections.<BigDecimal>emptyList());
    }
    @Test(expected = IllegalArgumentException.class) public void rejectsNonPositiveRecordAmount() {
        CommissionSettlementSupport.requireTotal(Arrays.asList(new BigDecimal("1.00"), BigDecimal.ZERO));
    }
}