package com.zbkj.service.service.jiuzhoukang.commission;

import lombok.Data;
import lombok.experimental.Accessors;
import java.math.BigDecimal;

/** 账户服务落库前的金额不变量校验。 */
public final class CommissionAccountSupport {
    private CommissionAccountSupport() { }

    public static Balance settle(BigDecimal pending, BigDecimal settled, BigDecimal total, BigDecimal amount) {
        requireNonNegative(pending, "待结算金额"); requireNonNegative(settled, "已结算金额"); requireNonNegative(total, "累计佣金"); requireNonNegative(amount, "结算金额");
        if (pending.compareTo(amount) < 0) throw new IllegalArgumentException("待结算佣金不足");
        return new Balance().setPendingSettleAmount(pending.subtract(amount)).setSettledAmount(settled.add(amount)).setTotalCommissionAmount(total);
    }

    public static FreezeBalance freeze(BigDecimal settled, BigDecimal frozen, BigDecimal amount) {
        requireNonNegative(settled, "已结算金额"); requireNonNegative(frozen, "冻结佣金"); requireNonNegative(amount, "冻结金额");
        if (settled.compareTo(amount) < 0) throw new IllegalArgumentException("可冻结佣金不足");
        return new FreezeBalance().setSettledAmount(settled.subtract(amount)).setFrozenCommissionAmount(frozen.add(amount));
    }

    public static FreezeBalance release(BigDecimal settled, BigDecimal frozen, BigDecimal amount) {
        requireNonNegative(settled, "已结算金额"); requireNonNegative(frozen, "冻结佣金"); requireNonNegative(amount, "解冻金额");
        if (frozen.compareTo(amount) < 0) throw new IllegalArgumentException("冻结佣金不足");
        return new FreezeBalance().setSettledAmount(settled.add(amount)).setFrozenCommissionAmount(frozen.subtract(amount));
    }

    private static void requireNonNegative(BigDecimal amount, String name) { if (amount == null || amount.signum() < 0) throw new IllegalArgumentException(name + "不能为负数"); }

    @Data @Accessors(chain = true)
    public static class FreezeBalance { private BigDecimal settledAmount; private BigDecimal frozenCommissionAmount; }
    @Data @Accessors(chain = true)
    public static class Balance { private BigDecimal pendingSettleAmount; private BigDecimal settledAmount; private BigDecimal totalCommissionAmount; }
}