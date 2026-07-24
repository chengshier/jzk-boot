package com.zbkj.common.response.jiuzhoukang;

import lombok.Data;
import lombok.experimental.Accessors;
import java.math.BigDecimal;

@Data @Accessors(chain=true)
public class JkInventoryAgingResponse {
    private Long stockAccountId;
    private String accountName;
    private String regionCode;
    private Integer productId;
    private Integer skuId;
    private String productName;
    private String skuName;
    private Integer availableQty;
    private Integer maxAgeDays;
    private Integer noOutboundDays;
    private String agingLevel;
    private BigDecimal inventoryCost;
}
