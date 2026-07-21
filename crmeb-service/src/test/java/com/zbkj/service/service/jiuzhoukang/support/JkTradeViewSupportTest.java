package com.zbkj.service.service.jiuzhoukang.support;

import com.zbkj.service.service.jiuzhoukang.context.JkUserContext;
import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;

public class JkTradeViewSupportTest {

    @Test
    public void should_resolve_anonymous_and_normal_user_to_retail_identity() {
        Assert.assertEquals("normal_user", JkTradeViewSupport.resolveTradeIdentity(null));

        JkUserContext context = new JkUserContext();
        context.setPrimaryRoleCode(null);
        Assert.assertEquals("normal_user", JkTradeViewSupport.resolveTradeIdentity(context));
    }

    @Test
    public void should_enable_phase3_actions_when_business_permissions_are_granted() {
        JkTradeViewSupport.ActionFlags makerFlags = JkTradeViewSupport.resolveActionFlags("maker", Collections.singletonList("stock.apply"));
        Assert.assertTrue(makerFlags.getCanApplyTransfer());
        Assert.assertNull(makerFlags.getTransferDisabledReason());
        Assert.assertFalse(makerFlags.getCanRetailBuy());

        JkTradeViewSupport.ActionFlags countyFlags = JkTradeViewSupport.resolveActionFlags("county_agent", java.util.Arrays.asList("stock.platform.order", "stock.transfer.confirm"));
        Assert.assertTrue(countyFlags.getCanOrderFromPlatform());
        Assert.assertNull(countyFlags.getPlatformOrderDisabledReason());
        Assert.assertTrue(countyFlags.getCanTransferToDownline());
        Assert.assertNull(countyFlags.getDownlineTransferDisabledReason());
        Assert.assertTrue(countyFlags.getCanViewStockDetail());
    }
}
