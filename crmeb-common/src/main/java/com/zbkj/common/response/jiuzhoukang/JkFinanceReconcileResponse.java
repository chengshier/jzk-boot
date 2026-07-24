package com.zbkj.common.response.jiuzhoukang;

import lombok.Data;
import lombok.experimental.Accessors;
import java.math.BigDecimal;

@Data @Accessors(chain=true)
public class JkFinanceReconcileResponse {
    private BigDecimal commissionGenerated;
    private BigDecimal commissionReversed;
    private BigDecimal commissionSettled;
    private BigDecimal fundAvailable;
    private BigDecimal fundWithdrawing;
    private BigDecimal fundWithdrawn;
    private BigDecimal withdrawSubmitted;
    private BigDecimal withdrawPaid;
    private BigDecimal differenceAmount;
    private String reconcileStatus;
}
