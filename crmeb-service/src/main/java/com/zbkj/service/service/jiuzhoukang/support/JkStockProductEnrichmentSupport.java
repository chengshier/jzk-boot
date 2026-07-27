package com.zbkj.service.service.jiuzhoukang.support;

import cn.hutool.core.util.StrUtil;
import com.zbkj.common.model.product.StoreProduct;
import com.zbkj.common.model.product.StoreProductAttrValue;
import com.zbkj.common.response.jiuzhoukang.JkStockItemResponse;
import com.zbkj.service.dao.StoreProductAttrValueDao;
import com.zbkj.service.dao.StoreProductDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 为九州康库存响应补充 CRMEB 商品/SKU 主数据。
 *
 * <p>九州康库存只保存 productId、skuId 和数量，不建立第二套商品档案。
 * 商品名称、图片、单位、条码、当前价格和成本均从 CRMEB 商品表读取。</p>
 */
@Component
public class JkStockProductEnrichmentSupport {

    @Autowired
    private StoreProductDao productDao;
    @Autowired
    private StoreProductAttrValueDao skuDao;

    public void enrich(List<JkStockItemResponse> rows) {
        if (rows == null || rows.isEmpty()) {
            return;
        }
        Set<Integer> productIds = rows.stream().map(JkStockItemResponse::getProductId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Integer> skuIds = rows.stream().map(JkStockItemResponse::getSkuId)
                .filter(Objects::nonNull).collect(Collectors.toSet());

        Map<Integer, StoreProduct> productMap = productIds.isEmpty() ? Collections.emptyMap()
                : productDao.selectBatchIds(productIds).stream()
                .collect(Collectors.toMap(StoreProduct::getId, item -> item, (a, b) -> a));
        Map<Integer, StoreProductAttrValue> skuMap = skuIds.isEmpty() ? Collections.emptyMap()
                : skuDao.selectBatchIds(skuIds).stream()
                .collect(Collectors.toMap(StoreProductAttrValue::getId, item -> item, (a, b) -> a));

        for (JkStockItemResponse row : rows) {
            StoreProduct product = productMap.get(row.getProductId());
            StoreProductAttrValue sku = skuMap.get(row.getSkuId());
            if (product != null) {
                if (StrUtil.isBlank(row.getProductName()) || "商品已删除".equals(row.getProductName())) {
                    row.setProductName(StrUtil.blankToDefault(product.getStoreName(), "商品未命名"));
                }
                row.setUnitName(product.getUnitName());
            }
            if (sku != null) {
                if (StrUtil.isBlank(row.getSkuName()) || "SKU 已删除".equals(row.getSkuName())) {
                    row.setSkuName(StrUtil.blankToDefault(sku.getSuk(), "默认规格"));
                }
                if (StrUtil.isBlank(row.getSkuText()) || "SKU 已删除".equals(row.getSkuText())) {
                    row.setSkuText(StrUtil.blankToDefault(sku.getAttrValue(), row.getSkuName()));
                }
                if (StrUtil.isBlank(row.getSkuCode())) {
                    row.setSkuCode(sku.getUnique());
                }
            }

            String image = sku != null && StrUtil.isNotBlank(sku.getImage())
                    ? sku.getImage() : (product == null ? null : product.getImage());
            String barCode = sku != null && StrUtil.isNotBlank(sku.getBarCode())
                    ? sku.getBarCode() : (product == null ? null : product.getBarCode());
            BigDecimal retailPrice = sku != null && sku.getPrice() != null
                    ? sku.getPrice() : (product == null ? null : product.getPrice());
            BigDecimal costPrice = sku != null && sku.getCost() != null
                    ? sku.getCost() : (product == null ? null : product.getCost());
            BigDecimal referencePrice = costPrice != null ? costPrice : retailPrice;
            int available = row.getAvailableQty() == null ? 0 : row.getAvailableQty();

            row.setProductImage(image);
            row.setBarCode(barCode);
            row.setRetailPrice(retailPrice);
            row.setCostPrice(costPrice);
            row.setReferencePrice(referencePrice);
            row.setAvailableQuantity(available);
            row.setStockValue(referencePrice == null ? BigDecimal.ZERO
                    : referencePrice.multiply(BigDecimal.valueOf(available)));
        }
    }

    public Map<String, Object> summarize(List<JkStockItemResponse> rows) {
        Map<String, Object> summary = new HashMap<>();
        if (rows == null) {
            rows = Collections.emptyList();
        }
        BigDecimal stockValue = rows.stream().map(JkStockItemResponse::getStockValue)
                .filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
        long productCount = rows.stream().map(JkStockItemResponse::getProductId)
                .filter(Objects::nonNull).distinct().count();
        int quantity = rows.stream().map(JkStockItemResponse::getAvailableQty)
                .filter(Objects::nonNull).mapToInt(Integer::intValue).sum();
        summary.put("stockValue", stockValue);
        summary.put("skuCount", rows.size());
        summary.put("productCount", productCount);
        summary.put("availableQuantity", quantity);
        summary.put("stockValueBasis", "CRMEB_SKU_COST_FALLBACK_RETAIL_PRICE");
        return summary;
    }
}