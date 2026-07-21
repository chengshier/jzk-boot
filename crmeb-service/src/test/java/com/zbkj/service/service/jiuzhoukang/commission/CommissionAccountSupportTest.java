package com.zbkj.service.service.jiuzhoukang.commission;

import org.junit.Assert;
import org.junit.Test;
import java.math.BigDecimal;

public class CommissionAccountSupportTest {
    @Test
    public void should_move_pending_commission_to_settled_without_changing_total() {
        CommissionAccountSupport.Balance balance = CommissionAccountSupport.settle(new BigDecimal("25.00"), new BigDecimal("40.00"), new BigDecimal("65.00"), new BigDecimal("25.00"));
        Assert.assertEquals(new BigDecimal("0.00"), balance.getPendingSettleAmount());
        Assert.assertEquals(new BigDecimal("65.00"), balance.getSettledAmount());
        Assert.assertEquals(new BigDecimal("65.00"), balance.getTotalCommissionAmount());
    }

    @Test
    public void should_freeze_and_release_settled_commission_without_changing_total() {
        CommissionAccountSupport.FreezeBalance frozen = CommissionAccountSupport.freeze(new BigDecimal("50.00"), new BigDecimal("10.00"), new BigDecimal("20.00"));
        Assert.assertEquals(new BigDecimal("30.00"), frozen.getSettledAmount());
        Assert.assertEquals(new BigDecimal("30.00"), frozen.getFrozenCommissionAmount());
        CommissionAccountSupport.FreezeBalance released = CommissionAccountSupport.release(frozen.getSettledAmount(), frozen.getFrozenCommissionAmount(), new BigDecimal("20.00"));
        Assert.assertEquals(new BigDecimal("50.00"), released.getSettledAmount());
        Assert.assertEquals(new BigDecimal("10.00"), released.getFrozenCommissionAmount());
    }
}