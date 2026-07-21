package com.zbkj.service.service.jiuzhoukang.stock;

import org.junit.Assert;
import org.junit.Test;

public class StockActionKeyTest {

    @Test
    public void buildsStableIdempotencyKeyForOneBusinessStockAction() {
        Assert.assertEquals("PLATFORM_ORDER:10:FREEZE:20:30:40",
                StockActionKey.build("PLATFORM_ORDER", 10L, "FREEZE", 20L, 30, 40));
    }
}
