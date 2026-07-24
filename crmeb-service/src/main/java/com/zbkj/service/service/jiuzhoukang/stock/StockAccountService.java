package com.zbkj.service.service.jiuzhoukang.stock;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zbkj.common.model.jiuzhoukang.JkStockAccount;
import com.zbkj.common.request.PageParamRequest;
import com.zbkj.common.request.jiuzhoukang.JkStockAccountSearchRequest;
import com.zbkj.common.request.jiuzhoukang.JkStockFlowSearchRequest;
import com.zbkj.common.request.jiuzhoukang.JkStockItemSearchRequest;
import com.zbkj.common.response.jiuzhoukang.JkStockAccountResponse;
import com.zbkj.common.response.jiuzhoukang.JkStockFlowResponse;
import com.zbkj.common.response.jiuzhoukang.JkStockItemResponse;
import com.zbkj.service.service.jiuzhoukang.context.JkUserContext;
import com.zbkj.service.service.jiuzhoukang.support.JkStockVisibilitySupport;

import java.util.List;

public interface StockAccountService extends IService<JkStockAccount> {
    List<JkStockAccountResponse> getAdminAccountList(JkStockAccountSearchRequest request, PageParamRequest pageParamRequest);
    List<JkStockItemResponse> getAdminItemList(JkStockItemSearchRequest request, PageParamRequest pageParamRequest);
    List<JkStockFlowResponse> getAdminFlowList(JkStockFlowSearchRequest request, PageParamRequest pageParamRequest);
    JkStockAccount initializeBusinessAccount(Long userId, String roleCode, String regionCode, String ownerName);
    List<JkStockVisibilitySupport.StockBucket> getVisibleBuckets(Integer productId, Integer skuId, JkUserContext context, String tradeIdentity);
}
