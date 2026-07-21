package com.zbkj.service.service.jiuzhoukang.commission;

import org.junit.Assert;
import org.junit.Test;

public class WithdrawStateSupportTest {
    @Test
    public void should_allow_only_submitted_to_auditing_then_approved_to_paid() {
        Assert.assertTrue(WithdrawStateSupport.canTransit("SUBMITTED", "AUDITING"));
        Assert.assertTrue(WithdrawStateSupport.canTransit("AUDITING", "APPROVED"));
        Assert.assertTrue(WithdrawStateSupport.canTransit("APPROVED", "PAID"));
        Assert.assertFalse(WithdrawStateSupport.canTransit("PAID", "APPROVED"));
    }
}
