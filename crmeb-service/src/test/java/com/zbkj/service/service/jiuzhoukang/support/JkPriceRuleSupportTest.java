package com.zbkj.service.service.jiuzhoukang.support;

import org.junit.Assert;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;

public class JkPriceRuleSupportTest {

    @Test
    public void should_select_user_rule_before_region_role_and_role_rule() {
        Date now = new Date();
        JkPriceRuleSupport.RuleCandidate roleRule = new JkPriceRuleSupport.RuleCandidate()
                .setRuleId(11L)
                .setRuleVersion(1)
                .setPriceType("FIXED")
                .setMatchLevel(JkPriceRuleSupport.MATCH_LEVEL_ROLE)
                .setFixedPrice(new BigDecimal("498.00"))
                .setEffectiveTime(new Date(now.getTime() - 60000))
                .setExpireTime(new Date(now.getTime() + 60000))
                .setStatus(true);
        JkPriceRuleSupport.RuleCandidate regionRoleRule = new JkPriceRuleSupport.RuleCandidate()
                .setRuleId(12L)
                .setRuleVersion(2)
                .setPriceType("DISCOUNT")
                .setMatchLevel(JkPriceRuleSupport.MATCH_LEVEL_REGION_ROLE)
                .setDiscountRate(new BigDecimal("0.80"))
                .setEffectiveTime(new Date(now.getTime() - 60000))
                .setExpireTime(new Date(now.getTime() + 60000))
                .setStatus(true);
        JkPriceRuleSupport.RuleCandidate userRule = new JkPriceRuleSupport.RuleCandidate()
                .setRuleId(13L)
                .setRuleVersion(3)
                .setPriceType("FIXED")
                .setMatchLevel(JkPriceRuleSupport.MATCH_LEVEL_USER)
                .setFixedPrice(new BigDecimal("458.00"))
                .setEffectiveTime(new Date(now.getTime() - 60000))
                .setExpireTime(new Date(now.getTime() + 60000))
                .setStatus(true);

        JkPriceRuleSupport.ResolvedPrice resolvedPrice = JkPriceRuleSupport.resolvePrice(
                Arrays.asList(roleRule, regionRoleRule, userRule),
                now,
                new BigDecimal("520.00"),
                new BigDecimal("598.00")
        );

        Assert.assertEquals(new BigDecimal("458.00"), resolvedPrice.getAmount());
        Assert.assertEquals(Long.valueOf(13L), resolvedPrice.getRuleId());
        Assert.assertEquals(Integer.valueOf(3), resolvedPrice.getRuleVersion());
        Assert.assertEquals("FIXED", resolvedPrice.getPriceType());
        Assert.assertEquals("USER_RULE", resolvedPrice.getFallbackReason());
    }

    @Test
    public void should_fallback_to_crmeb_member_price_when_no_active_rule_exists() {
        Date now = new Date();
        JkPriceRuleSupport.RuleCandidate expiredRule = new JkPriceRuleSupport.RuleCandidate()
                .setRuleId(21L)
                .setRuleVersion(1)
                .setPriceType("FIXED")
                .setMatchLevel(JkPriceRuleSupport.MATCH_LEVEL_ROLE)
                .setFixedPrice(new BigDecimal("468.00"))
                .setEffectiveTime(new Date(now.getTime() - 120000))
                .setExpireTime(new Date(now.getTime() - 60000))
                .setStatus(true);

        JkPriceRuleSupport.ResolvedPrice resolvedPrice = JkPriceRuleSupport.resolvePrice(
                Collections.singletonList(expiredRule),
                now,
                new BigDecimal("529.00"),
                new BigDecimal("598.00")
        );

        Assert.assertEquals(new BigDecimal("529.00"), resolvedPrice.getAmount());
        Assert.assertNull(resolvedPrice.getRuleId());
        Assert.assertNull(resolvedPrice.getRuleVersion());
        Assert.assertEquals("CRMEB_MEMBER_PRICE", resolvedPrice.getPriceType());
        Assert.assertEquals("NO_ACTIVE_RULE_FALLBACK_MEMBER_PRICE", resolvedPrice.getFallbackReason());
    }
}
