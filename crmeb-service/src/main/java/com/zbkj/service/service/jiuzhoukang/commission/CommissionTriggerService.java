package com.zbkj.service.service.jiuzhoukang.commission;

import java.math.BigDecimal;

public interface CommissionTriggerService {
    void onRetailOrderCompleted(Long orderId, String orderNo, Long orderInfoId, Long receiverUserId, String receiverRoleCode, BigDecimal orderAmount, String requestNo);
    void onPlatformOrderStockIn(Long platformOrderId, String orderNo, String requestNo);
    void onStockTransferCompleted(Long transferId, String transferNo, String requestNo);
    void onRefundCompleted(Long orderId, String orderNo, String requestNo);
    void onTransferReturnCompleted(Long transferId, String transferNo, String requestNo);
    void onIdentityFrozen(Long userId, String requestNo);
}
