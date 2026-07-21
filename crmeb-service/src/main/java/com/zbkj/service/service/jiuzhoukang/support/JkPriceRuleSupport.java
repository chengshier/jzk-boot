package com.zbkj.service.service.jiuzhoukang.support;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

public class JkPriceRuleSupport {

    public static final String MATCH_LEVEL_USER = "USER";
    public static final String MATCH_LEVEL_REGION_ROLE = "REGION_ROLE";
    public static final String MATCH_LEVEL_ROLE = "ROLE";
    public static final String MATCH_LEVEL_ACTIVITY = "ACTIVITY";

    public static ResolvedPrice resolvePrice(List<RuleCandidate> candidates, Date now, BigDecimal memberPrice, BigDecimal retailPrice) {
        List<RuleCandidate> safeCandidates = candidates == null ? Collections.<RuleCandidate>emptyList() : candidates;
        RuleCandidate selected = safeCandidates.stream()
                .filter(item -> item != null && Boolean.TRUE.equals(item.getStatus()))
                .filter(item -> isInEffectiveWindow(item, now))
                .sorted(Comparator.comparingInt(JkPriceRuleSupport::priority).thenComparing(RuleCandidate::getRuleVersion, Comparator.nullsLast(Comparator.reverseOrder())))
                .findFirst()
                .orElse(null);
        if (selected != null) {
            BigDecimal amount = calculateRuleAmount(selected, retailPrice);
            return new ResolvedPrice()
                    .setAmount(amount)
                    .setOriginalAmount(retailPrice)
                    .setRuleId(selected.getRuleId())
                    .setRuleVersion(selected.getRuleVersion())
                    .setPriceType(selected.getPriceType())
                    .setFallbackReason(selected.getMatchLevel() + "_RULE");
        }
        if (memberPrice != null && memberPrice.compareTo(BigDecimal.ZERO) > 0) {
            return new ResolvedPrice()
                    .setAmount(scale(memberPrice))
                    .setOriginalAmount(retailPrice)
                    .setPriceType("CRMEB_MEMBER_PRICE")
                    .setFallbackReason("NO_ACTIVE_RULE_FALLBACK_MEMBER_PRICE");
        }
        return new ResolvedPrice()
                .setAmount(scale(retailPrice))
                .setOriginalAmount(retailPrice)
                .setPriceType("CRMEB_RETAIL_PRICE")
                .setFallbackReason("NO_ACTIVE_RULE_FALLBACK_RETAIL_PRICE");
    }

    private static boolean isInEffectiveWindow(RuleCandidate candidate, Date now) {
        Date compareTime = now == null ? new Date() : now;
        if (candidate.getEffectiveTime() != null && candidate.getEffectiveTime().after(compareTime)) {
            return false;
        }
        return candidate.getExpireTime() == null || !candidate.getExpireTime().before(compareTime);
    }

    private static int priority(RuleCandidate item) {
        if (MATCH_LEVEL_USER.equals(item.getMatchLevel())) {
            return 1;
        }
        if (MATCH_LEVEL_REGION_ROLE.equals(item.getMatchLevel())) {
            return 2;
        }
        if (MATCH_LEVEL_ROLE.equals(item.getMatchLevel())) {
            return 3;
        }
        return 4;
    }

    private static BigDecimal calculateRuleAmount(RuleCandidate selected, BigDecimal retailPrice) {
        if ("DISCOUNT".equals(selected.getPriceType()) && selected.getDiscountRate() != null && retailPrice != null) {
            return scale(retailPrice.multiply(selected.getDiscountRate()));
        }
        if (selected.getFixedPrice() != null) {
            return scale(selected.getFixedPrice());
        }
        return scale(retailPrice);
    }

    private static BigDecimal scale(BigDecimal value) {
        return value == null ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP) : value.setScale(2, RoundingMode.HALF_UP);
    }

    public static class RuleCandidate {
        private Long ruleId;
        private Integer ruleVersion;
        private String priceType;
        private String matchLevel;
        private BigDecimal fixedPrice;
        private BigDecimal discountRate;
        private Date effectiveTime;
        private Date expireTime;
        private Boolean status;

        public Long getRuleId() {
            return ruleId;
        }

        public RuleCandidate setRuleId(Long ruleId) {
            this.ruleId = ruleId;
            return this;
        }

        public Integer getRuleVersion() {
            return ruleVersion;
        }

        public RuleCandidate setRuleVersion(Integer ruleVersion) {
            this.ruleVersion = ruleVersion;
            return this;
        }

        public String getPriceType() {
            return priceType;
        }

        public RuleCandidate setPriceType(String priceType) {
            this.priceType = priceType;
            return this;
        }

        public String getMatchLevel() {
            return matchLevel;
        }

        public RuleCandidate setMatchLevel(String matchLevel) {
            this.matchLevel = matchLevel;
            return this;
        }

        public BigDecimal getFixedPrice() {
            return fixedPrice;
        }

        public RuleCandidate setFixedPrice(BigDecimal fixedPrice) {
            this.fixedPrice = fixedPrice;
            return this;
        }

        public BigDecimal getDiscountRate() {
            return discountRate;
        }

        public RuleCandidate setDiscountRate(BigDecimal discountRate) {
            this.discountRate = discountRate;
            return this;
        }

        public Date getEffectiveTime() {
            return effectiveTime;
        }

        public RuleCandidate setEffectiveTime(Date effectiveTime) {
            this.effectiveTime = effectiveTime;
            return this;
        }

        public Date getExpireTime() {
            return expireTime;
        }

        public RuleCandidate setExpireTime(Date expireTime) {
            this.expireTime = expireTime;
            return this;
        }

        public Boolean getStatus() {
            return status;
        }

        public RuleCandidate setStatus(Boolean status) {
            this.status = status;
            return this;
        }
    }

    public static class ResolvedPrice {
        private BigDecimal amount;
        private BigDecimal originalAmount;
        private Long ruleId;
        private Integer ruleVersion;
        private String priceType;
        private String fallbackReason;

        public BigDecimal getAmount() {
            return amount;
        }

        public ResolvedPrice setAmount(BigDecimal amount) {
            this.amount = amount;
            return this;
        }

        public BigDecimal getOriginalAmount() {
            return originalAmount;
        }

        public ResolvedPrice setOriginalAmount(BigDecimal originalAmount) {
            this.originalAmount = originalAmount;
            return this;
        }

        public Long getRuleId() {
            return ruleId;
        }

        public ResolvedPrice setRuleId(Long ruleId) {
            this.ruleId = ruleId;
            return this;
        }

        public Integer getRuleVersion() {
            return ruleVersion;
        }

        public ResolvedPrice setRuleVersion(Integer ruleVersion) {
            this.ruleVersion = ruleVersion;
            return this;
        }

        public String getPriceType() {
            return priceType;
        }

        public ResolvedPrice setPriceType(String priceType) {
            this.priceType = priceType;
            return this;
        }

        public String getFallbackReason() {
            return fallbackReason;
        }

        public ResolvedPrice setFallbackReason(String fallbackReason) {
            this.fallbackReason = fallbackReason;
            return this;
        }
    }
}
