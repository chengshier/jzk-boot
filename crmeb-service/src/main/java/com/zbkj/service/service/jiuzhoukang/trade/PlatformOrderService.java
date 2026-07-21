package com.zbkj.service.service.jiuzhoukang.trade;

import com.github.pagehelper.PageInfo;
import com.zbkj.common.model.jiuzhoukang.JkPlatformOrder;
import com.zbkj.common.request.PageParamRequest;
import com.zbkj.common.request.jiuzhoukang.JkBusinessActionRequest;
import com.zbkj.common.request.jiuzhoukang.JkPaymentAuditRequest;
import com.zbkj.common.request.jiuzhoukang.JkPaymentVoucherRequest;
import com.zbkj.common.request.jiuzhoukang.JkTradeCreateRequest;
import com.zbkj.common.request.jiuzhoukang.JkTradeDocumentSearchRequest;
import com.zbkj.common.response.jiuzhoukang.JkPlatformOrderDetailResponse;

public interface PlatformOrderService {
    JkPlatformOrder create(Long userId, JkTradeCreateRequest request);
    JkPlatformOrder submitVoucher(Long userId, Long orderId, JkPaymentVoucherRequest request);
    JkPlatformOrder auditPayment(Long adminUserId, JkPaymentAuditRequest request);
    JkPlatformOrder ship(Long adminUserId, JkBusinessActionRequest request);
    JkPlatformOrder receive(Long userId, JkBusinessActionRequest request);
    JkPlatformOrder close(Long adminUserId, JkBusinessActionRequest request);
    PageInfo<JkPlatformOrder> getFrontList(Long userId, JkTradeDocumentSearchRequest request, PageParamRequest pageParamRequest);
    PageInfo<JkPlatformOrder> getAdminList(JkTradeDocumentSearchRequest request, PageParamRequest pageParamRequest);
    JkPlatformOrderDetailResponse getFrontDetail(Long userId, Long orderId);
    JkPlatformOrderDetailResponse getAdminDetail(Long orderId);
}
