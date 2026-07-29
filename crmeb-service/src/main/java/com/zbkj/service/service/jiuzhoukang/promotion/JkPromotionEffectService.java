package com.zbkj.service.service.jiuzhoukang.promotion;

import com.zbkj.common.model.jiuzhoukang.JkPromotionEffectEvent;
import com.zbkj.common.model.jiuzhoukang.JkRetailOrderAttribution;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;

public interface JkPromotionEffectService {
    JkPromotionEffectEvent recordOpen(String sceneCode, Long visitorUserId, String requestNo, String metadataJson);
    JkPromotionEffectEvent recordRetailCompleted(JkRetailOrderAttribution attribution, BigDecimal amount, Date occurredAt);
    List<JkPromotionEffectEvent> list(String sceneCode, Long promoterUserId, String eventType, Date startTime, Date endTime);
    Map<String, Object> summary(String sceneCode, Long promoterUserId, Date startTime, Date endTime);
}
