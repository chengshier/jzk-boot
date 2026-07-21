package com.zbkj.service.service.jiuzhoukang.trade;

import com.github.pagehelper.PageInfo;
import com.zbkj.common.model.jiuzhoukang.JkStockTransfer;
import com.zbkj.common.request.PageParamRequest;
import com.zbkj.common.request.jiuzhoukang.JkBusinessActionRequest;
import com.zbkj.common.request.jiuzhoukang.JkPaymentAuditRequest;
import com.zbkj.common.request.jiuzhoukang.JkPaymentVoucherRequest;
import com.zbkj.common.request.jiuzhoukang.JkTradeCreateRequest;
import com.zbkj.common.request.jiuzhoukang.JkTradeDocumentSearchRequest;
import com.zbkj.common.response.jiuzhoukang.JkStockTransferDetailResponse;

public interface StockTransferService {
    JkStockTransfer create(Long userId, JkTradeCreateRequest request);
    JkStockTransfer audit(Long countyUserId, JkPaymentAuditRequest request);
    JkStockTransfer submitVoucher(Long userId, Long transferId, JkPaymentVoucherRequest request);
    JkStockTransfer confirmPayment(Long countyUserId, JkPaymentAuditRequest request);
    JkStockTransfer dispatch(Long countyUserId, JkBusinessActionRequest request);
    JkStockTransfer receive(Long userId, JkBusinessActionRequest request);
    JkStockTransfer close(Long countyUserId, JkBusinessActionRequest request);
    PageInfo<JkStockTransfer> getFrontList(Long userId, JkTradeDocumentSearchRequest request, PageParamRequest pageParamRequest);
    PageInfo<JkStockTransfer> getAdminList(Long countyUserId, JkTradeDocumentSearchRequest request, PageParamRequest pageParamRequest);
    JkStockTransferDetailResponse getFrontDetail(Long userId, Long transferId);
    JkStockTransferDetailResponse getAdminDetail(Long countyUserId, Long transferId);
}
