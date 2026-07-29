package com.zbkj.service.service.impl.jiuzhoukang.promotion;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import cn.hutool.json.JSONObject;
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
    @Autowired private JkPromotionEffectEventDao eventDao;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public JkPromotionEffectEvent recordOpen(String sceneCode, Long visitorUserId, String requestNo, String metadataJson) {
        if (StrUtil.isBlank(sceneCode)) throw new IllegalArgumentException("推广场景不能为空");
        if (StrUtil.isBlank(requestNo)) throw new IllegalArgumentException("requestNo不能为空");
        String key = "PROMOTION_OPEN:" + requestNo.trim();
        JkPromotionEffectEvent existing = findByKey(key);
        if (existing != null) return existing;
        JkPromotionEffectEvent event = base("OPEN", key, requestNo.trim())
                .setSceneCode(sceneCode.trim()).setVisitorUserId(visitorUserId)
                .setMetadataJson(metadataJson).setSourceType("MINI_PROGRAM_ENTRY")
                .setOccurredAt(new Date());
        return insert(event);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public JkPromotionEffectEvent recordRetailCompleted(JkRetailOrderAttribution attribution,
                                                        BigDecimal amount, Date occurredAt) {
        if (attribution == null || attribution.getId() == null || attribution.getOrderInfoId() == null) {
            throw new IllegalArgumentException("零售订单归属快照不能为空");
        }
        String key = "PROMOTION_RETAIL_COMPLETED:" + attribution.getOrderInfoId();
        JkPromotionEffectEvent existing = findByKey(key);
        if (existing != null) return existing;
        String sceneCode = sceneFromSnapshot(attribution);
        Long promoter = attribution.getDirectParentUserId() != null
                ? attribution.getDirectParentUserId() : attribution.getCountyAgentUserId();
        JkPromotionEffectEvent event = base("RETAIL_COMPLETED", key, key)
                .setSceneCode(sceneCode).setPromoterUserId(promoter)
                .setVisitorUserId(attribution.getBuyerUserId()).setSourceType("RETAIL_ORDER")
                .setSourceId(attribution.getOrderId()).setSourceItemId(attribution.getOrderInfoId())
                .setSourceNo(attribution.getOrderNo()).setAmount(money(amount))
                .setAttributionSnapshotJson(JSONUtil.toJsonStr(attribution))
                .setOccurredAt(occurredAt == null ? new Date() : occurredAt);
        return insert(event);
    }

    @Override
    public List<JkPromotionEffectEvent> list(String sceneCode, Long promoterUserId, String eventType,
                                             Date startTime, Date endTime) {
        LambdaQueryWrapper<JkPromotionEffectEvent> query = new LambdaQueryWrapper<JkPromotionEffectEvent>()
                .eq(JkPromotionEffectEvent::getIsDeleted, false).orderByDesc(JkPromotionEffectEvent::getId);
        if (StrUtil.isNotBlank(sceneCode)) query.eq(JkPromotionEffectEvent::getSceneCode, sceneCode.trim());
        if (promoterUserId != null) query.eq(JkPromotionEffectEvent::getPromoterUserId, promoterUserId);
        if (StrUtil.isNotBlank(eventType)) query.eq(JkPromotionEffectEvent::getEventType, eventType.trim());
        if (startTime != null) query.ge(JkPromotionEffectEvent::getOccurredAt, startTime);
        if (endTime != null) query.lt(JkPromotionEffectEvent::getOccurredAt, endTime);
        return eventDao.selectList(query);
    }

    @Override
    public Map<String, Object> summary(String sceneCode, Long promoterUserId, Date startTime, Date endTime) {
        List<JkPromotionEffectEvent> events = list(sceneCode, promoterUserId, null, startTime, endTime);
        int openCount = 0;
        int completedOrderCount = 0;
        BigDecimal completedAmount = BigDecimal.ZERO;
        Set<Long> visitors = new HashSet<Long>();
        Set<String> scenes = new HashSet<String>();
        Set<Long> promoters = new HashSet<Long>();
        for (JkPromotionEffectEvent event : events) {
            if ("OPEN".equals(event.getEventType())) openCount++;
            if ("RETAIL_COMPLETED".equals(event.getEventType())) {
                completedOrderCount++;
                completedAmount = completedAmount.add(money(event.getAmount()));
            }
            if (event.getVisitorUserId() != null) visitors.add(event.getVisitorUserId());
            if (StrUtil.isNotBlank(event.getSceneCode())) scenes.add(event.getSceneCode());
            if (event.getPromoterUserId() != null) promoters.add(event.getPromoterUserId());
        }
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("openCount", openCount);
        result.put("identifiedVisitorCount", visitors.size());
        result.put("completedOrderCount", completedOrderCount);
        result.put("completedAmount", completedAmount.setScale(2, RoundingMode.HALF_UP));
        result.put("sceneCount", scenes.size());
        result.put("promoterCount", promoters.size());
        result.put("conversionRate", openCount == 0 ? null
                : new BigDecimal(completedOrderCount).multiply(new BigDecimal("100"))
                    .divide(new BigDecimal(openCount), 2, RoundingMode.HALF_UP));
        result.put("notice", "成交事件由后端零售归属快照生成；客户端只能上报打开事件。场景快照缺失时仍按推广人统计，但不伪造场景归因。");
        return result;
    }

    private JkPromotionEffectEvent base(String eventType, String key, String requestNo) {
        return new JkPromotionEffectEvent().setEventNo("PE" + IdWorker.getIdStr())
                .setEventType(eventType).setIdempotencyKey(key).setRequestNo(requestNo)
                .setAmount(BigDecimal.ZERO).setIsDeleted(false).setCreateTime(new Date());
    }

    private JkPromotionEffectEvent insert(JkPromotionEffectEvent event) {
        try {
            eventDao.insert(event);
            return eventDao.selectById(event.getId());
        } catch (DuplicateKeyException duplicate) {
            JkPromotionEffectEvent existing = findByKey(event.getIdempotencyKey());
            if (existing != null) return existing;
            throw duplicate;
        }
    }

    private JkPromotionEffectEvent findByKey(String key) {
        return eventDao.selectOne(new LambdaQueryWrapper<JkPromotionEffectEvent>()
                .eq(JkPromotionEffectEvent::getIdempotencyKey, key).last("limit 1"));
    }

    private String sceneFromSnapshot(JkRetailOrderAttribution attribution) {
        if (StrUtil.isNotBlank(attribution.getRelationSnapshotJson())) {
            try {
                JSONObject snapshot = JSONUtil.parseObj(attribution.getRelationSnapshotJson());
                String sceneCode = snapshot.getStr("promotionSceneCode");
                if (StrUtil.isBlank(sceneCode)) sceneCode = snapshot.getStr("sceneCode");
                if (StrUtil.isNotBlank(sceneCode)) return sceneCode;
            } catch (Exception ignored) { }
        }
        return attribution.getRelationId() == null ? null : "RELATION:" + attribution.getRelationId();
    }

    private BigDecimal money(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP);
    }
}
