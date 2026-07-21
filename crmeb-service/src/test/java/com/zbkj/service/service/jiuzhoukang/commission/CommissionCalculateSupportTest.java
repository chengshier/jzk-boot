package com.zbkj.service.service.jiuzhoukang.commission;

import org.junit.Assert;
import org.junit.Test;

import java.math.BigDecimal;

public class CommissionCalculateSupportTest {

    @Test
    public void should_calculate_percent_commission_with_rule_snapshot_amount() {
        BigDecimal amount = CommissionCalculateSupport.calculatePercent(
                new BigDecimal("199.00"), new BigDecimal("0.15"));

        Assert.assertEquals(new BigDecimal("29.85"), amount);
    }

    @Test
    public void should_build_stable_idempotency_key_for_same_commission_source() {
        String key = CommissionCalculateSupport.buildIdempotencyKey(
                "RETAIL_ORDER", 1001L, 2002L, 3003L);

        Assert.assertEquals("RETAIL_ORDER:1001:2002:3003", key);
    }

    @Test
    public void readsPercentRateFromRuleConfig() {
        Assert.assertEquals(new BigDecimal("12.50"), CommissionCalculateSupport.calculateFromRuleConfig(new BigDecimal("250"), "{\"calculationType\":\"PERCENT\",\"commissionRate\":\"0.05\"}"));
    }
}
