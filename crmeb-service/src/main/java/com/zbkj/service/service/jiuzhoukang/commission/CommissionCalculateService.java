package com.zbkj.service.service.jiuzhoukang.commission;

import com.zbkj.common.model.jiuzhoukang.JkCommissionRecord;
import java.math.BigDecimal;
import java.util.List;

public interface CommissionCalculateService {
    List<JkCommissionRecord> calculateRetailOrder(Long orderId, String orderNo, Long orderInfoId, Long receiverUserId, String receiverRoleCode, BigDecimal orderAmount, String requestNo);
}
