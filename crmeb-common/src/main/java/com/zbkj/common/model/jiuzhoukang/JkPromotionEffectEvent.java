package com.zbkj.common.model.jiuzhoukang;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/** 推广效果原始事件。成交类事件只允许后端业务服务写入。 */
@Data
@Accessors(chain = true)
@TableName("jk_promotion_effect_event")
public class JkPromotionEffectEvent implements Serializable {
    private static final long serialVersionUID = 1L;
    @TableId(value = "id", type = IdType.AUTO) private Long id;
    private String eventNo;
    private String sceneCode;
    private Long promoterUserId;
    private Long visitorUserId;
    private String eventType;
    private String sourceType;
    private Long sourceId;
    private Long sourceItemId;
    private String sourceNo;
    private BigDecimal amount;
    private String attributionSnapshotJson;
    private String metadataJson;
    private String requestNo;
    private String idempotencyKey;
    private Date occurredAt;
    private Boolean isDeleted;
    private Date createTime;
}
