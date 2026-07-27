package com.zbkj.service.service.impl.jiuzhoukang.stock;

import com.zbkj.common.constants.jiuzhoukang.JkBizConstants;
import com.zbkj.common.model.product.StoreProduct;
import com.zbkj.common.model.product.StoreProductAttrValue;
import com.zbkj.common.response.jiuzhoukang.JkProductTradeViewResponse;
import com.zbkj.service.service.StoreProductAttrValueService;
import com.zbkj.service.service.StoreProductService;
import com.zbkj.service.service.jiuzhoukang.context.JkUserContext;
import com.zbkj.service.service.jiuzhoukang.stock.StockAccountService;
import com.zbkj.service.service.jiuzhoukang.stock.StockVisibilityService;
import com.zbkj.service.service.jiuzhoukang.support.JkStockVisibilitySupport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class StockVisibilityServiceImpl implements StockVisibilityService {

    @Autowired private StockAccountService stockAccountService;
    @Autowired private StoreProductService productService;
    @Autowired private StoreProductAttrValueService skuService;

    @Override
    public JkProductTradeViewResponse.StockInfo resolveStock(Integer productId, Integer skuId,
                                                              JkUserContext context, String tradeIdentity) {
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

        // 普通零售与区县代平台订货共享 CRMEB 商品/SKU 库存主账。
        // 代理自身库存、区县代可调拨库存仍来自九州康分级库存账户。
        if (JkBizConstants.ROLE_NORMAL_USER.equals(tradeIdentity)
                || JkBizConstants.ROLE_COUNTY_AGENT.equals(tradeIdentity)) {
            int available = crmebAvailable(productId, skuId);
            stockInfo.setVisibleQty(available);
            stockInfo.setSource(JkBizConstants.ROLE_NORMAL_USER.equals(tradeIdentity)
                    ? JkBizConstants.STOCK_SOURCE_RETAIL
                    : JkBizConstants.STOCK_SOURCE_PLATFORM_ORDERABLE);
            stockInfo.setStockUnavailableReason(available > 0 ? null : "OUT_OF_STOCK");
        }
        return stockInfo;
    }

    private int crmebAvailable(Integer productId, Integer skuId) {
        if (skuId != null) {
            StoreProductAttrValue sku = skuService.getById(skuId);
            if (sku == null || Boolean.TRUE.equals(sku.getIsDel()) || !productId.equals(sku.getProductId())) return 0;
            return sku.getStock() == null ? 0 : Math.max(0, sku.getStock());
        }
        StoreProduct product = productService.getById(productId);
        if (product == null || Boolean.TRUE.equals(product.getIsDel())) return 0;
        return product.getStock() == null ? 0 : Math.max(0, product.getStock());
    }
}