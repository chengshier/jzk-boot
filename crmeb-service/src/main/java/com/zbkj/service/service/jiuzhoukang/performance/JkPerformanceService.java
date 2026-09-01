package com.zbkj.service.service.jiuzhoukang.performance;

import com.github.pagehelper.PageInfo;
import com.zbkj.common.model.jiuzhoukang.JkPerformanceRecord;
import com.zbkj.common.request.PageParamRequest;

import java.math.BigDecimal;

public interface JkPerformanceService {
    JkPerformanceRecord record(JkPerformanceRecord draft);
    BigDecimal summary(Long ownerUserId, String performanceType);
    PageInfo<JkPerformanceRecord> list(Long ownerUserId, String performanceType, String sourceType, String status, PageParamRequest page);
    void reverse(String sourceType, Long sourceId, Long sourceItemId, BigDecimal amount, String requestNo, String reason);
    BigDecimal reverseByRatio(String sourceType, Long sourceId, Long sourceItemId, BigDecimal ratio, String requestNo, String reason);
}
