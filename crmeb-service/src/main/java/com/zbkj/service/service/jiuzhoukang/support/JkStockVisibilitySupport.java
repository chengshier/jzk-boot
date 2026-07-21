package com.zbkj.service.service.jiuzhoukang.support;

import com.zbkj.common.constants.jiuzhoukang.JkBizConstants;

import java.util.Collections;
import java.util.List;

public class JkStockVisibilitySupport {

    public static VisibleStock resolveVisibleStock(String tradeIdentity, List<StockBucket> buckets) {
        List<StockBucket> safeBuckets = buckets == null ? Collections.<StockBucket>emptyList() : buckets;
        if (JkBizConstants.ROLE_NORMAL_USER.equals(tradeIdentity)) {
            return fromSource(safeBuckets, JkBizConstants.STOCK_SOURCE_RETAIL);
        }
        if (JkBizConstants.ROLE_MAKER.equals(tradeIdentity) || JkBizConstants.ROLE_PARTNER.equals(tradeIdentity)) {
            return fromSource(safeBuckets, JkBizConstants.STOCK_SOURCE_COUNTY_ALLOCATABLE);
        }
        if (JkBizConstants.ROLE_COUNTY_AGENT.equals(tradeIdentity)) {
            VisibleStock visibleStock = fromSource(safeBuckets, JkBizConstants.STOCK_SOURCE_PLATFORM_ORDERABLE);
            StockBucket ownBucket = findBucket(safeBuckets, JkBizConstants.STOCK_SOURCE_OWN);
            if (ownBucket != null) {
                visibleStock.setOwnStockQty(ownBucket.getQty());
                visibleStock.setOwnStockAccountId(ownBucket.getStockAccountId());
            } else {
                visibleStock.setOwnStockQty(0);
            }
            return visibleStock;
        }
        return fromSource(safeBuckets, JkBizConstants.STOCK_SOURCE_RETAIL);
    }

    private static VisibleStock fromSource(List<StockBucket> safeBuckets, String source) {
        StockBucket bucket = findBucket(safeBuckets, source);
        if (bucket == null) {
            return new VisibleStock()
                    .setSource(source)
                    .setVisibleQty(0)
                    .setStockUnavailableReason("NO_STOCK_ACCOUNT_OR_ITEM");
        }
        return new VisibleStock()
                .setSource(source)
                .setVisibleQty(bucket.getQty())
                .setStockAccountId(bucket.getStockAccountId())
                .setStockOwnerName(bucket.getOwnerName())
                .setStockUnavailableReason(bucket.getUnavailableReason());
    }

    private static StockBucket findBucket(List<StockBucket> buckets, String source) {
        for (StockBucket item : buckets) {
            if (item != null && source.equals(item.getSource())) {
                return item;
            }
        }
        return null;
    }

    public static class StockBucket {
        private String source;
        private Integer qty;
        private Long stockAccountId;
        private String ownerName;
        private String unavailableReason;

        public String getSource() {
            return source;
        }

        public StockBucket setSource(String source) {
            this.source = source;
            return this;
        }

        public Integer getQty() {
            return qty;
        }

        public StockBucket setQty(Integer qty) {
            this.qty = qty;
            return this;
        }

        public Long getStockAccountId() {
            return stockAccountId;
        }

        public StockBucket setStockAccountId(Long stockAccountId) {
            this.stockAccountId = stockAccountId;
            return this;
        }

        public String getOwnerName() {
            return ownerName;
        }

        public StockBucket setOwnerName(String ownerName) {
            this.ownerName = ownerName;
            return this;
        }

        public String getUnavailableReason() {
            return unavailableReason;
        }

        public StockBucket setUnavailableReason(String unavailableReason) {
            this.unavailableReason = unavailableReason;
            return this;
        }
    }

    public static class VisibleStock {
        private String source;
        private Integer visibleQty;
        private Long stockAccountId;
        private String stockOwnerName;
        private Integer ownStockQty;
        private Long ownStockAccountId;
        private String stockUnavailableReason;

        public String getSource() {
            return source;
        }

        public VisibleStock setSource(String source) {
            this.source = source;
            return this;
        }

        public Integer getVisibleQty() {
            return visibleQty;
        }

        public VisibleStock setVisibleQty(Integer visibleQty) {
            this.visibleQty = visibleQty;
            return this;
        }

        public Long getStockAccountId() {
            return stockAccountId;
        }

        public VisibleStock setStockAccountId(Long stockAccountId) {
            this.stockAccountId = stockAccountId;
            return this;
        }

        public String getStockOwnerName() {
            return stockOwnerName;
        }

        public VisibleStock setStockOwnerName(String stockOwnerName) {
            this.stockOwnerName = stockOwnerName;
            return this;
        }

        public Integer getOwnStockQty() {
            return ownStockQty;
        }

        public VisibleStock setOwnStockQty(Integer ownStockQty) {
            this.ownStockQty = ownStockQty;
            return this;
        }

        public Long getOwnStockAccountId() {
            return ownStockAccountId;
        }

        public VisibleStock setOwnStockAccountId(Long ownStockAccountId) {
            this.ownStockAccountId = ownStockAccountId;
            return this;
        }

        public String getStockUnavailableReason() {
            return stockUnavailableReason;
        }

        public VisibleStock setStockUnavailableReason(String stockUnavailableReason) {
            this.stockUnavailableReason = stockUnavailableReason;
            return this;
        }
    }
}
