package com.zbkj.service.service.jiuzhoukang.commission;
import com.zbkj.common.model.jiuzhoukang.JkCommissionAccount;
import java.math.BigDecimal;
public interface CommissionAccountService {
 JkCommissionAccount initialize(Long userId, String roleCode, String regionCode);
 JkCommissionAccount creditPending(Long userId, String roleCode, BigDecimal amount, String requestNo, String idempotencyKey);
 JkCommissionAccount settle(Long userId, String roleCode, BigDecimal amount, String requestNo, String idempotencyKey);
 JkCommissionAccount reversePending(Long userId, String roleCode, BigDecimal amount, String requestNo, String idempotencyKey);
 JkCommissionAccount reverseSettled(Long userId, String roleCode, BigDecimal amount, String requestNo, String idempotencyKey);
 JkCommissionAccount reverseFrozen(Long userId, String roleCode, BigDecimal amount, String requestNo, String idempotencyKey);
 JkCommissionAccount reverseSettledOrFrozen(Long userId, String roleCode, BigDecimal amount, String requestNo, String idempotencyKey);
 JkCommissionAccount freezeSettled(Long userId, String roleCode, BigDecimal amount, String sourceType, Long sourceId, String requestNo, String idempotencyKey, String reason);
 JkCommissionAccount releaseFrozen(Long userId, String roleCode, BigDecimal amount, String sourceType, Long sourceId, String requestNo, String idempotencyKey, String reason);
}
