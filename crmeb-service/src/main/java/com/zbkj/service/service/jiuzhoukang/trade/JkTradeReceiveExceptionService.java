package com.zbkj.service.service.jiuzhoukang.trade;

import com.github.pagehelper.PageInfo;
import com.zbkj.common.model.jiuzhoukang.JkTradeReceiveException;
import com.zbkj.common.request.PageParamRequest;
import com.zbkj.common.request.jiuzhoukang.JkTradeReceiveExceptionCreateRequest;
import com.zbkj.common.request.jiuzhoukang.JkTradeReceiveExceptionHandleRequest;
import com.zbkj.common.response.jiuzhoukang.JkTradeReceiveExceptionDetailResponse;

public interface JkTradeReceiveExceptionService {
    JkTradeReceiveExceptionDetailResponse create(Long userId, JkTradeReceiveExceptionCreateRequest request);
    PageInfo<JkTradeReceiveException> listMine(Long userId, String status, PageParamRequest page);
    JkTradeReceiveExceptionDetailResponse detailMine(Long userId, Long id);
    JkTradeReceiveExceptionDetailResponse detailByBusiness(Long userId, String businessType, Long businessId);
    PageInfo<JkTradeReceiveException> listAdmin(String status, String businessType, Long receiverUserId, PageParamRequest page);
    JkTradeReceiveExceptionDetailResponse detailAdmin(Long id);
    JkTradeReceiveExceptionDetailResponse handle(Long operatorId, JkTradeReceiveExceptionHandleRequest request);
}
