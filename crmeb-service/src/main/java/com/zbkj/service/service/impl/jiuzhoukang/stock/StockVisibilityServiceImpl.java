package com.zbkj.service.service.impl.jiuzhoukang.stock;

import com.zbkj.common.response.jiuzhoukang.JkProductTradeViewResponse;
import com.zbkj.service.service.jiuzhoukang.context.JkUserContext;
import com.zbkj.service.service.jiuzhoukang.stock.StockAccountService;
import com.zbkj.service.service.jiuzhoukang.stock.StockVisibilityService;
import com.zbkj.service.service.jiuzhoukang.support.JkStockVisibilitySupport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class StockVisibilityServiceImpl implements StockVisibilityService {

    @Autowired
    private StockAccountService stockAccountService;

    @Override
    public JkProductTradeViewResponse.StockInfo resolveStock(Integer productId, Integer skuId, JkUserContext context, String tradeIdentity) {
        JkStockVisibilitySupport.VisibleStock visibleStock = JkStockVisibilitySupport.resolveVisibleStock(
                tradeIdentity,
                stockAccountService.getVisibleBuckets(productId, skuId, context, tradeIdentity)
        );
        JkProductTradeViewResponse.StockInfo stockInfo = new JkProductTradeViewResponse.StockInfo();
        stockInfo.setVisibleQty(visibleStock.getVisibleQty());
        stockInfo.setSource(visibleStock.getSource());
        stockInfo.setStockAccountId(visibleStock.getStockAccountId());
        stockInfo.setStockOwnerName(visibleStock.getStockOwnerName());
        stockInfo.setOwnStockQty(visibleStock.getOwnStockQty());
        stockInfo.setOwnStockAccountId(visibleStock.getOwnStockAccountId());
        stockInfo.setStockUnavailableReason(visibleStock.getStockUnavailableReason());
        return stockInfo;
    }
}
