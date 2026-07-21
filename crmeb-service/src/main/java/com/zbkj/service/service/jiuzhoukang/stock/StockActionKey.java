package com.zbkj.service.service.jiuzhoukang.stock;

public final class StockActionKey {
    private StockActionKey() {
    }

    public static String build(String businessType, Long businessId, String actionType, Long stockAccountId, Integer productId, Integer skuId) {
        return businessType + ":" + businessId + ":" + actionType + ":" + stockAccountId + ":" + productId + ":" + skuId;
    }
}
