package com.zbkj.service.service.jiuzhoukang.support;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

public class JkStockVisibilitySupportTest {

    @Test
    public void should_resolve_county_agent_visible_stock_with_platform_and_own_stock() {
        JkStockVisibilitySupport.StockBucket platformBucket = new JkStockVisibilitySupport.StockBucket()
                .setSource("PLATFORM_ORDERABLE")
                .setQty(180)
                .setStockAccountId(9001L)
                .setOwnerName("平台总仓");
        JkStockVisibilitySupport.StockBucket ownBucket = new JkStockVisibilitySupport.StockBucket()
                .setSource("OWN_STOCK")
                .setQty(36)
                .setStockAccountId(9002L)
                .setOwnerName("思明区区县代");

        JkStockVisibilitySupport.VisibleStock visibleStock = JkStockVisibilitySupport.resolveVisibleStock(
                "county_agent",
                Arrays.asList(platformBucket, ownBucket)
        );

        Assert.assertEquals("PLATFORM_ORDERABLE", visibleStock.getSource());
        Assert.assertEquals(Integer.valueOf(180), visibleStock.getVisibleQty());
        Assert.assertEquals(Long.valueOf(9001L), visibleStock.getStockAccountId());
        Assert.assertEquals(Integer.valueOf(36), visibleStock.getOwnStockQty());
        Assert.assertEquals(Long.valueOf(9002L), visibleStock.getOwnStockAccountId());
    }

    @Test
    public void should_return_zero_and_reason_when_stock_account_is_missing() {
        JkStockVisibilitySupport.VisibleStock visibleStock = JkStockVisibilitySupport.resolveVisibleStock(
                "maker",
                Collections.<JkStockVisibilitySupport.StockBucket>emptyList()
        );

        Assert.assertEquals("COUNTY_AGENT_ALLOCATABLE", visibleStock.getSource());
        Assert.assertEquals(Integer.valueOf(0), visibleStock.getVisibleQty());
        Assert.assertEquals("NO_STOCK_ACCOUNT_OR_ITEM", visibleStock.getStockUnavailableReason());
    }
}
