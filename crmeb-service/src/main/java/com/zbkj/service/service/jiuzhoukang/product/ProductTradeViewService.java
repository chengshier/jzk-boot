package com.zbkj.service.service.jiuzhoukang.product;

import com.zbkj.common.response.jiuzhoukang.JkProductTradeViewResponse;
import com.zbkj.service.service.jiuzhoukang.context.JkUserContext;

public interface ProductTradeViewService {
    JkProductTradeViewResponse getTradeView(Integer productId, String skuId, JkUserContext context);
}
