package com.zbkj.service.service.jiuzhoukang.commission;

import com.zbkj.common.model.jiuzhoukang.JkCommissionSettleTask;
import java.math.BigDecimal;
import java.util.List;

public interface CommissionSettleService {
    void settleToFundAccount(Long userId, String roleCode, BigDecimal amount, String taskNo, String requestNo, String idempotencyKey);
    JkCommissionSettleTask settleRecords(List<Long> commissionRecordIds, Long operatorId, String requestNo, String remark);
}