package com.zbkj.service.service.jiuzhoukang.commission;

import org.junit.Assert;
import org.junit.Test;
import java.math.BigDecimal;

public class WithdrawValidationSupportTest {
    @Test(expected = IllegalArgumentException.class)
    public void rejectsNonPositiveWithdrawalAmount() {
        WithdrawValidationSupport.validateAmount(BigDecimal.ZERO, null);
    }
    @Test(expected = IllegalArgumentException.class)
    public void rejectsAmountBelowConfiguredMinimum() {
        WithdrawValidationSupport.validateAmount(new BigDecimal("9.99"), new BigDecimal("10.00"));
    }
    @Test
    public void acceptsAmountAtConfiguredMinimum() {
        WithdrawValidationSupport.validateAmount(new BigDecimal("10.00"), new BigDecimal("10.00"));
        Assert.assertTrue(true);
    }
}