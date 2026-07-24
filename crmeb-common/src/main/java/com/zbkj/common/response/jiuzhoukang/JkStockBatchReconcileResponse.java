package com.zbkj.common.response.jiuzhoukang;

import lombok.Data;
import lombok.experimental.Accessors;

/** 库存总账与批次账逐商品/SKU 对账结果。 */
@Data @Accessors(chain = true)
public class JkStockBatchReconcileResponse {
    private Long stockItemId;
    private Long stockAccountId;
    private String accountName;
    private String regionCode;
    private Integer productId;
    private Integer skuId;
    private String productName;
    private Integer totalAvailableQty;
    private Integer totalFrozenQty;
    private Integer batchAvailableQty;
    private Integer batchFrozenQty;
    private Integer availableDifference;
    private Integer frozenDifference;
    private String reconcileStatus;
}
