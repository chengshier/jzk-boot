package com.zbkj.service.service.jiuzhoukang.stock;

import com.zbkj.common.request.jiuzhoukang.JkStockActionRequest;

public interface StockFlowService {
    void freezeStock(JkStockActionRequest request);
    void releaseFrozenStock(JkStockActionRequest request);
    void outboundFrozenStock(JkStockActionRequest request);
    void inboundStock(JkStockActionRequest request);
    void writeStockFlow(JkStockActionRequest request, String actionType);
}