package com.zbkj.service.service.jiuzhoukang.stock;

import com.zbkj.common.response.jiuzhoukang.JkProductTradeViewResponse;
import com.zbkj.service.service.jiuzhoukang.context.JkUserContext;

public interface StockVisibilityService {
    JkProductTradeViewResponse.StockInfo resolveStock(Integer productId, Integer skuId, JkUserContext context, String tradeIdentity);
}
