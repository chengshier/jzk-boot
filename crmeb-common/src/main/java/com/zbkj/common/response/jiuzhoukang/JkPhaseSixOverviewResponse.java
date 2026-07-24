package com.zbkj.common.response.jiuzhoukang;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class JkPhaseSixOverviewResponse {
    private Long activeIdentityCount;
    private Long stockAccountCount;
    private Long platformOrderCount;
    private Long stockTransferCount;
    private Long transferReturnCount;
    private BigDecimal pendingCommissionAmount;
    private BigDecimal withdrawPendingAmount;
    private Long deadEventCount;
    private Long accountMismatchCount;
    private Long activeHealthAlertCount;
    private Long deniedHealthAccessCount;
    private Long openRiskEventCount;
}
