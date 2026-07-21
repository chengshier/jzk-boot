package com.zbkj.service.service.jiuzhoukang.trade;

import org.junit.Assert;
import org.junit.Test;

public class JkTradeStatusSupportTest {

    @Test
    public void marksPlatformOrderApprovedAndShippedAsFrozenReleasable() {
        Assert.assertTrue(JkTradeStatusSupport.platformOrderRequiresFrozenRelease("PAYMENT_APPROVED"));
        Assert.assertFalse(JkTradeStatusSupport.platformOrderRequiresFrozenRelease("CREATED"));
        Assert.assertFalse(JkTradeStatusSupport.platformOrderRequiresFrozenRelease("SHIPPED"));
    }

    @Test
    public void marksTransferApprovedStatesAsFrozenReleasable() {
        Assert.assertTrue(JkTradeStatusSupport.transferRequiresFrozenRelease("AUDIT_APPROVED"));
        Assert.assertTrue(JkTradeStatusSupport.transferRequiresFrozenRelease("PAYMENT_SUBMITTED"));
        Assert.assertTrue(JkTradeStatusSupport.transferRequiresFrozenRelease("PAYMENT_APPROVED"));
        Assert.assertFalse(JkTradeStatusSupport.transferRequiresFrozenRelease("SUBMITTED"));
        Assert.assertFalse(JkTradeStatusSupport.transferRequiresFrozenRelease("TRANSFERRED"));
    }

    @Test
    public void mergesInventoryRejectReasonWithManualRemark() {
        Assert.assertEquals("库存不足", JkTradeStatusSupport.inventoryRejectReason(null));
        Assert.assertEquals("库存不足；人工备注", JkTradeStatusSupport.inventoryRejectReason("人工备注"));
    }
}
