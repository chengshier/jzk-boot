package com.zbkj.common.response.jiuzhoukang;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
public class JkProductTradeViewResponse implements Serializable {
    private Integer productId;
    private Integer skuId;
    private String skuCode;
    private String tradeIdentity;
    private Boolean anonymous;
    private ProductInfo product;
    private PriceInfo price;
    private StockInfo stock;
    private ActionInfo actions;
    private String disabledReason;
    private Boolean defaultSkuSelected;

    @Data
    public static class ProductInfo implements Serializable {
        private Integer productId;
        private Integer skuId;
        private String skuCode;
        private String skuName;
        private String skuText;
        private String image;
        private String storeName;
        private String storeInfo;
        private String unitName;
        private BigDecimal retailPrice;
        private BigDecimal memberPrice;
        private BigDecimal originalPrice;
        private Boolean specType;
    }

    @Data
    public static class PriceInfo implements Serializable {
        private BigDecimal amount;
        private String priceType;
        private BigDecimal originalAmount;
        private Long ruleId;
        private Integer ruleVersion;
        private String fallbackReason;
    }

    @Data
    public static class StockInfo implements Serializable {
        private Integer visibleQty;
        private String source;
        private Long stockAccountId;
        private String stockOwnerName;
        private Integer ownStockQty;
        private Long ownStockAccountId;
        private String stockUnavailableReason;
    }

    @Data
    public static class ActionInfo implements Serializable {
        private Boolean canRetailBuy;
        private Boolean canApplyTransfer;
        private Boolean canOrderFromPlatform;
        private Boolean canTransferToDownline;
        private Boolean canViewStockDetail;
        private String transferDisabledReason;
        private String platformOrderDisabledReason;
        private String downlineTransferDisabledReason;
    }
}
