package com.zbkj.service.service.jiuzhoukang.commission;
import com.zbkj.common.model.jiuzhoukang.JkCommissionReverse;
import java.math.BigDecimal;
public interface CommissionReverseService {
    JkCommissionReverse reverse(Long commissionRecordId, String sourceType, Long sourceId, String sourceNo, String reverseType, BigDecimal amount, String requestNo, Long operatorId, String reason);
}
