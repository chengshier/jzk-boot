package com.zbkj.service.service.impl.jiuzhoukang.promotion;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.zbkj.common.model.jiuzhoukang.JkPromotionEffectEvent;
import com.zbkj.common.model.jiuzhoukang.JkRetailOrderAttribution;
import com.zbkj.service.dao.jiuzhoukang.JkPromotionEffectEventDao;
import com.zbkj.service.service.jiuzhoukang.promotion.JkPromotionEffectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class JkPromotionEffectServiceImpl implements JkPromotionEffectService {
    @Autowired
    private JkPromotionEffectEventDao eventDao;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public JkPromotionEffectEvent recordOpen(String sceneCode,
                                             Long visitorUserId,
                                             String requestNo,
                                             String metadataJson) {
        if (StrUtil.isBlank(sceneCode)) {
            throw new IllegalArgumentException("推广场景不能为空");
        }
        if (StrUtil.isBlank(requestNo)) {
            throw new IllegalArgumentException("requestNo不能为空");
        }
        String normalizedRequestNo = requestNo.trim();
        String key = "PROMOTION_OPEN:" + normalizedRequestNo;
        JkPromotionEffectEvent existing = findByKey(key);
        if (existing != null) {
            return existing;
        }
        JkPromotionEffectEvent event = base("OPEN", key, normalizedRequestNo)
                .setSceneCode(sceneCode.trim())
                .setVisitorUserId(visitorUserId)
                .setMetadataJson(metadataJson)
                .setSourceType("MINI_PROGRAM_ENTRY")
                .setOccurredAt(new Date());
        return insert(event);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public JkPromotionEffectEvent recordRetailCompleted(JkRetailOrderAttribution attribution,
                                                        BigDecimal amount,
                                                        Date occurredAt) {
        validateAttribution(attribution);
        String key = "PROMOTION_RETAIL_COMPLETED:" + attribution.getOrderInfoId();
        JkPromotionEffectEvent existing = findByKey(key);
        if (existing != null) {
            return existing;
        }
        JkPromotionEffectEvent event = base("RETAIL_COMPLETED", key, key)
                .setSceneCode(sceneFromSnapshot(attribution))
                .setPromoterUserId(resolvePromoter(attribution))
                .setVisitorUserId(attribution.getBuyerUserId())
                .setSourceType("RETAIL_ORDER")
                .setSourceId(attribution.getOrderId())
                .setSourceItemId(attribution.getOrderInfoId())
                .setSourceNo(attribution.getOrderNo())
                .setAmount(money(amount))
                .setAttributionSnapshotJson(JSONUtil.toJsonStr(attribution))
                .setOccurredAt(occurredAt == null ? new Date() : occurredAt);
        return insert(event);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public JkPromotionEffectEvent recordRetailRefund(JkRetailOrderAttribution attribution,
                                                     BigDecimal refundAmount,
                                                     BigDecimal beforeRefundedAmount,
                                                     String requestNo,
                                                     Date occurredAt) {
        validateAttribution(attribution);
        if (refundAmount == null || refundAmount.signum() <= 0) {
            throw new IllegalArgumentException("推广退款冲减金额必须大于零");
        }
        if (StrUtil.isBlank(requestNo)) {
            throw new IllegalArgumentException("退款requestNo不能为空");
        }

        String normalizedRequestNo = requestNo.trim();
        String key = "PROMOTION_RETAIL_REFUND:" + attribution.getOrderInfoId() + ":" + normalizedRequestNo;
        JkPromotionEffectEvent existing = findByKey(key);
        if (existing != null) {
            return existing;
        }

        BigDecimal before = money(beforeRefundedAmount);
        BigDecimal refund = money(refundAmount);
        BigDecimal after = before.add(refund).setScale(2, RoundingMode.HALF_UP);
        BigDecimal itemPaid = money(attribution.getItemPaidAmount());
        Map<String, Object> metadata = new LinkedHashMap<String, Object>();
        metadata.put("beforeRefundedAmount", before);
        metadata.put("refundAmount", refund);
        metadata.put("afterRefundedAmount", after);
        metadata.put("itemPaidAmount", itemPaid);
        metadata.put("fullRefund", itemPaid.signum() > 0 && after.compareTo(itemPaid) >= 0);

        JkPromotionEffectEvent event = base("RETAIL_REFUND", key, normalizedRequestNo)
                .setSceneCode(sceneFromSnapshot(attribution))
                .setPromoterUserId(resolvePromoter(attribution))
                .setVisitorUserId(attribution.getBuyerUserId())
                .setSourceType("RETAIL_ORDER_REFUND")
                .setSourceId(attribution.getOrderId())
                .setSourceItemId(attribution.getOrderInfoId())
                .setSourceNo(attribution.getOrderNo())
                .setAmount(refund)
                .setAttributionSnapshotJson(JSONUtil.toJsonStr(attribution))
                .setMetadataJson(JSONUtil.toJsonStr(metadata))
                .setOccurredAt(occurredAt == null ? new Date() : occurredAt);
        return insert(event);
    }

    @Override
    public List<JkPromotionEffectEvent> list(String sceneCode,
                                             Long promoterUserId,
                                             String eventType,
                                             Date startTime,
                                             Date endTime) {
        LambdaQueryWrapper<JkPromotionEffectEvent> query = new LambdaQueryWrapper<JkPromotionEffectEvent>()
                .eq(JkPromotionEffectEvent::getIsDeleted, false)
                .orderByDesc(JkPromotionEffectEvent::getId);
        if (StrUtil.isNotBlank(sceneCode)) {
            query.eq(JkPromotionEffectEvent::getSceneCode, sceneCode.trim());
        }
        if (promoterUserId != null) {
            query.eq(JkPromotionEffectEvent::getPromoterUserId, promoterUserId);
        }
        if (StrUtil.isNotBlank(eventType)) {
            query.eq(JkPromotionEffectEvent::getEventType, eventType.trim());
        }
        if (startTime != null) {
            query.ge(JkPromotionEffectEvent::getOccurredAt, startTime);
        }
        if (endTime != null) {
            query.lt(JkPromotionEffectEvent::getOccurredAt, endTime);
        }
        return eventDao.selectList(query);
    }

    @Override
    public Map<String, Object> summary(String sceneCode,
                                       Long promoterUserId,
                                       Date startTime,
                                       Date endTime) {
        List<JkPromotionEffectEvent> events = list(sceneCode, promoterUserId, null, startTime, endTime);
        int openCount = 0;
        Set<Long> visitors = new HashSet<Long>();
        Set<String> scenes = new HashSet<String>();
        Set<Long> promoters = new HashSet<Long>();
        Map<String, BigDecimal> orderNetAmounts = new LinkedHashMap<String, BigDecimal>();

        for (JkPromotionEffectEvent event : events) {
            if ("OPEN".equals(event.getEventType())) {
                openCount++;
                if (event.getVisitorUserId() != null) {
                    visitors.add(event.getVisitorUserId());
                }
            }
            if ("RETAIL_COMPLETED".equals(event.getEventType())
                    || "RETAIL_REFUND".equals(event.getEventType())) {
                String orderKey = orderKey(event);
                BigDecimal current = orderNetAmounts.get(orderKey);
                if (current == null) {
                    current = BigDecimal.ZERO;
                }
                BigDecimal amount = money(event.getAmount());
                orderNetAmounts.put(orderKey,
                        "RETAIL_REFUND".equals(event.getEventType())
                                ? current.subtract(amount)
                                : current.add(amount));
            }
            if (StrUtil.isNotBlank(event.getSceneCode())) {
                scenes.add(event.getSceneCode());
            }
            if (event.getPromoterUserId() != null) {
                promoters.add(event.getPromoterUserId());
            }
        }

        int completedOrderCount = 0;
        BigDecimal completedAmount = BigDecimal.ZERO;
        for (BigDecimal value : orderNetAmounts.values()) {
            BigDecimal net = money(value).max(BigDecimal.ZERO);
            if (net.signum() > 0) {
                completedOrderCount++;
            }
            completedAmount = completedAmount.add(net);
        }

        BigDecimal openConversionRate = conversionRate(completedOrderCount, openCount);
        BigDecimal visitorConversionRate = conversionRate(completedOrderCount, visitors.size());
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("openCount", openCount);
        result.put("identifiedVisitorCount", visitors.size());
        result.put("completedOrderCount", completedOrderCount);
        result.put("completedAmount", completedAmount.setScale(2, RoundingMode.HALF_UP));
        result.put("sceneCount", scenes.size());
        result.put("promoterCount", promoters.size());
        result.put("openConversionRate", openConversionRate);
        result.put("visitorConversionRate", visitorConversionRate);
        // 兼容旧页面字段，含义固定为打开转化率。
        result.put("conversionRate", openConversionRate);
        result.put("notice", "有效成交按后端完成事件减退款反向事件统计；部分退款只减金额，累计全额退款后订单不再计为有效成交。客户端只能上报打开事件。");
        return result;
    }

    private void validateAttribution(JkRetailOrderAttribution attribution) {
        if (attribution == null || attribution.getId() == null || attribution.getOrderInfoId() == null) {
            throw new IllegalArgumentException("零售订单归属快照不能为空");
        }
    }

    private Long resolvePromoter(JkRetailOrderAttribution attribution) {
        return attribution.getDirectParentUserId() != null
                ? attribution.getDirectParentUserId()
                : attribution.getCountyAgentUserId();
    }

    private String orderKey(JkPromotionEffectEvent event) {
        if (event.getSourceId() != null) {
            return "ID:" + event.getSourceId();
        }
        if (StrUtil.isNotBlank(event.getSourceNo())) {
            return "NO:" + event.getSourceNo();
        }
        return "EVENT:" + event.getEventNo();
    }

    private BigDecimal conversionRate(int numerator, int denominator) {
        if (denominator <= 0) {
            return null;
        }
        return new BigDecimal(numerator)
                .multiply(new BigDecimal("100"))
                .divide(new BigDecimal(denominator), 2, RoundingMode.HALF_UP);
    }

    private JkPromotionEffectEvent base(String eventType, String key, String requestNo) {
        return new JkPromotionEffectEvent()
                .setEventNo("PE" + IdWorker.getIdStr())
                .setEventType(eventType)
                .setIdempotencyKey(key)
                .setRequestNo(requestNo)
                .setAmount(BigDecimal.ZERO)
                .setIsDeleted(false)
                .setCreateTime(new Date());
    }

    private JkPromotionEffectEvent insert(JkPromotionEffectEvent event) {
        try {
            eventDao.insert(event);
            return eventDao.selectById(event.getId());
        } catch (DuplicateKeyException duplicate) {
            JkPromotionEffectEvent existing = findByKey(event.getIdempotencyKey());
            if (existing != null) {
                return existing;
            }
            throw duplicate;
        }
    }

    private JkPromotionEffectEvent findByKey(String key) {
        return eventDao.selectOne(new LambdaQueryWrapper<JkPromotionEffectEvent>()
                .eq(JkPromotionEffectEvent::getIdempotencyKey, key)
                .last("limit 1"));
    }

    private String sceneFromSnapshot(JkRetailOrderAttribution attribution) {
        if (StrUtil.isNotBlank(attribution.getRelationSnapshotJson())) {
            try {
                JSONObject snapshot = JSONUtil.parseObj(attribution.getRelationSnapshotJson());
                String sceneCode = snapshot.getStr("promotionSceneCode");
                if (StrUtil.isBlank(sceneCode)) {
                    sceneCode = snapshot.getStr("sceneCode");
                }
                if (StrUtil.isNotBlank(sceneCode)) {
                    return sceneCode;
                }
            } catch (Exception ignored) {
                // 历史快照格式不兼容时仅回退关系标识，不读取当前关系。
            }
        }
        return attribution.getRelationId() == null
                ? null
                : "RELATION:" + attribution.getRelationId();
    }

    private BigDecimal money(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value)
                .setScale(2, RoundingMode.HALF_UP);
    }
}
