package com.zbkj.service.service.jiuzhoukang.trade;

import com.github.pagehelper.PageInfo;
import com.zbkj.common.model.jiuzhoukang.JkStockTransferReturn;
import com.zbkj.common.request.PageParamRequest;
import com.zbkj.common.request.jiuzhoukang.*;
import com.zbkj.common.response.jiuzhoukang.JkStockTransferReturnDetailResponse;

public interface StockTransferReturnService {
    JkStockTransferReturn create(Long userId, JkStockTransferReturnCreateRequest request);
    JkStockTransferReturn cancel(Long userId, JkBusinessActionRequest request);
    JkStockTransferReturn audit(Long countyUserId, JkPaymentAuditRequest request);
    JkStockTransferReturn ship(Long userId, Long returnId, JkStockTransferReturnShipRequest request);
    JkStockTransferReturn receive(Long countyUserId, JkBusinessActionRequest request);
    JkStockTransferReturn confirmRefund(Long countyUserId, JkStockTransferReturnRefundRequest request);
    JkStockTransferReturn close(Long countyUserId, JkBusinessActionRequest request);
    PageInfo<JkStockTransferReturn> getFrontList(Long userId, String status, PageParamRequest page);
    PageInfo<JkStockTransferReturn> getHandleList(Long countyUserId, String status, PageParamRequest page);
    JkStockTransferReturnDetailResponse getFrontDetail(Long userId, Long id);
    JkStockTransferReturnDetailResponse getHandleDetail(Long countyUserId, Long id);
}
