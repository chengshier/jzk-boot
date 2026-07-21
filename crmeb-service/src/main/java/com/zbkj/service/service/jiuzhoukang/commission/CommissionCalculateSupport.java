package com.zbkj.service.service.jiuzhoukang.commission;

import java.math.BigDecimal;
import java.math.RoundingMode;
import com.alibaba.fastjson.JSONObject;

/**
 * 佣金计算的无状态支持类；正式业务服务必须保存规则快照后再调用它。
 */
public final class CommissionCalculateSupport {

    private CommissionCalculateSupport() {
    }

    public static BigDecimal calculatePercent(BigDecimal sourceAmount, BigDecimal rate) {
        if (sourceAmount == null || rate == null || sourceAmount.signum() < 0 || rate.signum() < 0) {
            throw new IllegalArgumentException("佣金金额和比例必须为非负数");
        }
        return sourceAmount.multiply(rate).setScale(2, RoundingMode.HALF_UP);
    }

    public static String buildIdempotencyKey(String sourceType, Long sourceId, Long receiverUserId, Long ruleId) {
        if (sourceType == null || sourceType.trim().isEmpty() || sourceId == null || receiverUserId == null || ruleId == null) {
            throw new IllegalArgumentException("佣金幂等键参数不能为空");
        }
        return sourceType + ":" + sourceId + ":" + receiverUserId + ":" + ruleId;
    }

    /** 规则配置仅允许 PERCENT 或 FIXED；金额永远由服务端 BigDecimal 计算。 */
    public static BigDecimal calculateFromRuleConfig(BigDecimal baseAmount, String ruleConfigJson) {
        if (ruleConfigJson == null || ruleConfigJson.trim().isEmpty()) {
            throw new IllegalArgumentException("佣金规则配置不能为空");
        }
        JSONObject config = JSONObject.parseObject(ruleConfigJson);
        String type = config.getString("calculationType");
        if ("PERCENT".equals(type)) {
            return calculatePercent(baseAmount, config.getBigDecimal("commissionRate"));
        }
        if ("FIXED".equals(type)) {
            BigDecimal fixed = config.getBigDecimal("fixedAmount");
            if (fixed == null || fixed.signum() < 0) throw new IllegalArgumentException("固定佣金金额非法");
            return fixed.setScale(2, RoundingMode.HALF_UP);
        }
        throw new IllegalArgumentException("不支持的佣金计算类型");
    }}
