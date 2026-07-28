package com.zbkj.service.service.jiuzhoukang.profit;

import com.github.pagehelper.PageInfo;
import com.zbkj.common.model.jiuzhoukang.JkOperationProfitRecord;
import com.zbkj.common.request.PageParamRequest;

import java.math.BigDecimal;

public interface JkOperationProfitService {
    JkOperationProfitRecord record(JkOperationProfitRecord draft);
    BigDecimal summary(Long userId);
    PageInfo<JkOperationProfitRecord> list(Long userId, String sourceType, String status, PageParamRequest page);
    void reverse(String sourceType, Long sourceId, Long sourceItemId, BigDecimal amount, String requestNo, String reason);
}
