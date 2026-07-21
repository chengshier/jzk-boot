package com.zbkj.service.service.jiuzhoukang.support;

import org.junit.Assert;
import org.junit.Test;

public class JkDictLabelHelperTest {

    @Test
    public void resolvesKnownCodesAndKeepsUnknownCodeReadable() {
        Assert.assertEquals("已提交付款凭证", JkDictLabelHelper.label("platform_order_status", "PAYMENT_SUBMITTED"));
        Assert.assertEquals("入库", JkDictLabelHelper.label("stock_flow_type", "INBOUND"));
        Assert.assertEquals("审核通过", JkDictLabelHelper.label("stock_transfer_status", "AUDIT_APPROVED"));
        Assert.assertEquals("UNKNOWN_CODE", JkDictLabelHelper.label("withdraw_status", "UNKNOWN_CODE"));
    }
}
