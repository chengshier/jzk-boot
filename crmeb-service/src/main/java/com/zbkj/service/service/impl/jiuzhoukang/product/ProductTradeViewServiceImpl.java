package com.zbkj.service.service.impl.jiuzhoukang.product;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zbkj.common.constants.Constants;
import com.zbkj.common.constants.jiuzhoukang.JkBizConstants;
import com.zbkj.common.exception.CrmebException;
import com.zbkj.common.model.product.StoreProduct;
import com.zbkj.common.model.product.StoreProductAttrValue;
import com.zbkj.common.response.jiuzhoukang.JkProductTradeViewResponse;
import com.zbkj.service.service.StoreProductAttrValueService;
import com.zbkj.service.service.StoreProductService;
import com.zbkj.service.service.jiuzhoukang.context.JkUserContext;
import com.zbkj.service.service.jiuzhoukang.price.PriceCalculateService;
import com.zbkj.service.service.jiuzhoukang.product.ProductTradeViewService;
import com.zbkj.service.service.jiuzhoukang.stock.StockVisibilityService;
import com.zbkj.service.service.jiuzhoukang.support.JkTradeViewSupport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
public class ProductTradeViewServiceImpl implements ProductTradeViewService {

    @Autowired
    private StoreProductService storeProductService;
    @Autowired
    private StoreProductAttrValueService productAttrValueService;
    @Autowired
    private PriceCalculateService priceCalculateService;
    @Autowired
    private StockVisibilityService stockVisibilityService;

    @Override
    public JkProductTradeViewResponse getTradeView(Integer productId, String skuId, JkUserContext context) {
        StoreProduct product = storeProductService.getById(productId);
        if (product == null) {
            throw new CrmebException("商品不存在");
        }
        String tradeIdentity = JkTradeViewSupport.resolveTradeIdentity(context);
        StoreProductAttrValue selectedSku = resolveSku(productId, skuId);

        JkProductTradeViewResponse response = new JkProductTradeViewResponse();
        response.setProductId(productId);
        response.setSkuId(selectedSku == null ? null : selectedSku.getId());
        response.setSkuCode(selectedSku == null ? null : selectedSku.getUnique());
        response.setTradeIdentity(tradeIdentity);
        response.setAnonymous(context == null || context.getUserId() == null || context.getUserId() <= 0);
        response.setDefaultSkuSelected(StrUtil.isBlank(skuId) && selectedSku != null);
        response.setProduct(buildProductInfo(product, selectedSku));
        response.setPrice(priceCalculateService.calculatePrice(product, selectedSku, context));
        response.setStock(stockVisibilityService.resolveStock(productId, selectedSku == null ? null : selectedSku.getId(), context, tradeIdentity));

        JkTradeViewSupport.ActionFlags actionFlags = JkTradeViewSupport.resolveActionFlags(tradeIdentity, context == null ? null : context.getPermissions());
        if (actionFlags.getCanOrderFromPlatform()
                && response.getStock() != null
                && JkBizConstants.STOCK_SOURCE_PLATFORM_ORDERABLE.equals(response.getStock().getSource())
                && response.getStock().getVisibleQty() != null
                && response.getStock().getVisibleQty() <= 0) {
            actionFlags.setPlatformOrderDisabledReason("OUT_OF_STOCK");
        }
        JkProductTradeViewResponse.ActionInfo actionInfo = new JkProductTradeViewResponse.ActionInfo();
        actionInfo.setCanRetailBuy(actionFlags.getCanRetailBuy());
        actionInfo.setCanApplyTransfer(actionFlags.getCanApplyTransfer());
        actionInfo.setCanOrderFromPlatform(actionFlags.getCanOrderFromPlatform());
        actionInfo.setCanTransferToDownline(actionFlags.getCanTransferToDownline());
        actionInfo.setCanViewStockDetail(actionFlags.getCanViewStockDetail());
        actionInfo.setTransferDisabledReason(actionFlags.getTransferDisabledReason());
        actionInfo.setPlatformOrderDisabledReason(actionFlags.getPlatformOrderDisabledReason());
        actionInfo.setDownlineTransferDisabledReason(actionFlags.getDownlineTransferDisabledReason());
        response.setActions(actionInfo);
        response.setDisabledReason(resolveDisabledReason(actionInfo));
        return response;
    }

    private StoreProductAttrValue resolveSku(Integer productId, String skuId) {
        List<StoreProductAttrValue> skuList = productAttrValueService.getListByProductIdAndType(productId, Constants.PRODUCT_TYPE_NORMAL);
        if (skuList == null || skuList.isEmpty()) {
            return null;
        }
        if (StrUtil.isNotBlank(skuId)) {
            for (StoreProductAttrValue item : skuList) {
                if (skuId.equals(item.getUnique()) || skuId.equals(String.valueOf(item.getId()))) {
                    return item;
                }
            }
            throw new CrmebException("商品规格不存在");
        }
        return skuList.stream()
                .sorted(Comparator.comparing(StoreProductAttrValue::getStock, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(StoreProductAttrValue::getId))
                .findFirst()
                .orElse(skuList.get(0));
    }

    private JkProductTradeViewResponse.ProductInfo buildProductInfo(StoreProduct product, StoreProductAttrValue sku) {
        JkProductTradeViewResponse.ProductInfo productInfo = new JkProductTradeViewResponse.ProductInfo();
        productInfo.setProductId(product.getId());
        productInfo.setSkuId(sku == null ? null : sku.getId());
        productInfo.setSkuCode(sku == null ? null : sku.getUnique());
        productInfo.setSkuName(sku == null ? null : sku.getSuk());
        productInfo.setSkuText(sku == null ? null : sku.getAttrValue());
        productInfo.setImage(sku != null && StrUtil.isNotBlank(sku.getImage()) ? sku.getImage() : product.getImage());
        productInfo.setStoreName(product.getStoreName());
        productInfo.setStoreInfo(product.getStoreInfo());
        productInfo.setUnitName(product.getUnitName());
        productInfo.setRetailPrice(sku == null ? product.getPrice() : sku.getPrice());
        productInfo.setMemberPrice(product.getVipPrice());
        productInfo.setOriginalPrice(sku == null ? product.getOtPrice() : sku.getOtPrice());
        productInfo.setSpecType(product.getSpecType());
        return productInfo;
    }

    private String resolveDisabledReason(JkProductTradeViewResponse.ActionInfo actionInfo) {
        if (Boolean.TRUE.equals(actionInfo.getCanApplyTransfer()) && StrUtil.isNotBlank(actionInfo.getTransferDisabledReason())) {
            return actionInfo.getTransferDisabledReason();
        }
        if (Boolean.TRUE.equals(actionInfo.getCanOrderFromPlatform()) && StrUtil.isNotBlank(actionInfo.getPlatformOrderDisabledReason())) {
            return actionInfo.getPlatformOrderDisabledReason();
        }
        if (Boolean.TRUE.equals(actionInfo.getCanTransferToDownline()) && StrUtil.isNotBlank(actionInfo.getDownlineTransferDisabledReason())) {
            return actionInfo.getDownlineTransferDisabledReason();
        }
        return null;
    }
}



