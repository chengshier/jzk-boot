package com.zbkj.service.service.jiuzhoukang.commission;

public interface CommissionAutoSettleService {
    int settleDue(int limit, Long operatorId, String triggerNo);
}
