package com.zbkj.service.service.jiuzhoukang.price;

import com.zbkj.common.model.product.StoreProduct;
import com.zbkj.common.model.product.StoreProductAttrValue;
import com.zbkj.common.response.jiuzhoukang.JkProductTradeViewResponse;
import com.zbkj.service.service.jiuzhoukang.context.JkUserContext;

public interface PriceCalculateService {
    JkProductTradeViewResponse.PriceInfo calculatePrice(StoreProduct product, StoreProductAttrValue sku, JkUserContext context);
}
