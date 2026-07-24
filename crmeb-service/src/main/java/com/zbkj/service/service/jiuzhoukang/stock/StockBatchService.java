package com.zbkj.service.service.jiuzhoukang.stock;

import com.github.pagehelper.PageInfo;
import com.zbkj.common.model.jiuzhoukang.JkStockBatch;
import com.zbkj.common.request.PageParamRequest;
import com.zbkj.common.request.jiuzhoukang.JkStockActionRequest;
import com.zbkj.common.request.jiuzhoukang.JkStockBatchUpdateRequest;
import java.util.Date;

public interface StockBatchService {
    void freeze(JkStockActionRequest request);
    void release(JkStockActionRequest request);
    void outbound(JkStockActionRequest request);
    void inbound(JkStockActionRequest request);
    PageInfo<JkStockBatch> list(Long stockAccountId, Integer productId, Integer skuId, String agingLevel, PageParamRequest page);
    int openingFromStockItems(Long operatorId);
    JkStockBatch updateMetadata(Long operatorId, JkStockBatchUpdateRequest request);
    int ageDays(Date inboundTime);
}
