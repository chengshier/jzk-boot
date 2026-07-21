package com.zbkj.service.service.jiuzhoukang.commission;

import java.math.BigDecimal;

public interface CommissionFreezeService {
    void freezeCommission(Long userId, String roleCode, BigDecimal amount, String freezeType, String sourceType, Long sourceId, String requestNo, String idempotencyKey, String reason);
    void releaseCommission(Long userId, String roleCode, BigDecimal amount, String releaseType, String sourceType, Long sourceId, String requestNo, String idempotencyKey, String reason);
}
