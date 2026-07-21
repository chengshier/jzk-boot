package com.zbkj.service.service.jiuzhoukang.commission;

import java.math.BigDecimal;

/** 提现申请金额的无状态校验，最低金额由九州康独立配置决定。 */
public final class WithdrawValidationSupport {
    private WithdrawValidationSupport() { }

    public static void validateAmount(BigDecimal amount, BigDecimal minimumAmount) {
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("提现金额必须大于零");
        }
        if (minimumAmount != null && minimumAmount.signum() > 0 && amount.compareTo(minimumAmount) < 0) {
            throw new IllegalArgumentException("提现金额低于后台配置的最低提现金额");
        }
    }
}