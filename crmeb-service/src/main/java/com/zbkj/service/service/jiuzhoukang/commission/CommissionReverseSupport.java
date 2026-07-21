package com.zbkj.service.service.jiuzhoukang.commission;

import java.math.BigDecimal;

/** 冲正金额的纯计算规则；实际账户更新仍经统一账户服务执行。 */
public final class CommissionReverseSupport {
    private CommissionReverseSupport() { }

    public static BigDecimal remaining(BigDecimal commissionAmount, BigDecimal reversedAmount) {
        BigDecimal total = safe(commissionAmount);
        BigDecimal reversed = safe(reversedAmount);
        if (total.signum() < 0 || reversed.signum() < 0 || reversed.compareTo(total) > 0) {
            throw new IllegalArgumentException("佣金冲正累计金额非法");
        }
        return total.subtract(reversed);
    }

    public static void requireReverseAmount(BigDecimal remainingAmount, BigDecimal reverseAmount) {
        if (reverseAmount == null || reverseAmount.signum() <= 0 || reverseAmount.compareTo(remainingAmount) > 0) {
            throw new IllegalArgumentException("冲正金额超过可冲正佣金");
        }
    }

    public static FundReverse reverseFundAvailable(BigDecimal availableAmount, BigDecimal reverseAmount, BigDecimal negativeOffsetAmount) {
        BigDecimal available = safe(availableAmount);
        BigDecimal offset = safe(negativeOffsetAmount);
        if (available.signum() < 0 || offset.signum() < 0 || reverseAmount == null || reverseAmount.signum() <= 0) {
            throw new IllegalArgumentException("资金账户冲正金额非法");
        }
        BigDecimal applied = available.min(reverseAmount);
        return new FundReverse(available.subtract(applied), offset.add(reverseAmount.subtract(applied)));
    }

    private static BigDecimal safe(BigDecimal amount) { return amount == null ? BigDecimal.ZERO : amount; }

    public static final class FundReverse {
        private final BigDecimal availableAmount;
        private final BigDecimal negativeOffsetAmount;
        FundReverse(BigDecimal availableAmount, BigDecimal negativeOffsetAmount) {
            this.availableAmount = availableAmount;
            this.negativeOffsetAmount = negativeOffsetAmount;
        }
        public BigDecimal getAvailableAmount() { return availableAmount; }
        public BigDecimal getNegativeOffsetAmount() { return negativeOffsetAmount; }
    }
}