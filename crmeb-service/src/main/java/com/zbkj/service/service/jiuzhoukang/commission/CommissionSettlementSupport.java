package com.zbkj.service.service.jiuzhoukang.commission;

import java.math.BigDecimal;
import java.util.Collection;

/** 结算选择的纯金额校验，账户转账由 CommissionSettleService 完成。 */
public final class CommissionSettlementSupport {
    private CommissionSettlementSupport() { }
    public static BigDecimal requireTotal(Collection<BigDecimal> amounts) {
        if (amounts == null || amounts.isEmpty()) throw new IllegalArgumentException("至少选择一条待结算佣金记录");
        BigDecimal total = BigDecimal.ZERO;
        for (BigDecimal amount : amounts) {
            if (amount == null || amount.signum() <= 0) throw new IllegalArgumentException("待结算佣金金额必须大于零");
            total = total.add(amount);
        }
        return total;
    }
}