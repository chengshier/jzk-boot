package com.zbkj.service.service.jiuzhoukang.trade;

import com.zbkj.common.model.jiuzhoukang.JkPlatformOrder;
import com.zbkj.common.model.jiuzhoukang.JkStockTransfer;
import com.zbkj.common.request.jiuzhoukang.JkBusinessActionRequest;

/** 发货与拨货物流信息事务编排。 */
public interface JkTradeLogisticsService {
    JkPlatformOrder shipPlatformOrder(Long operatorId, JkBusinessActionRequest request);
    JkStockTransfer dispatchStockTransfer(Long operatorId, JkBusinessActionRequest request);
}
