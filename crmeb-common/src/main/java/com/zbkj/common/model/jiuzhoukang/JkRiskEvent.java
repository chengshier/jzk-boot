package com.zbkj.common.model.jiuzhoukang;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;
import java.io.Serializable;
import java.util.Date;

/** 第六阶段风险事件基础表。当前只记录和处置，不自动修改库存、账户或健康数据。 */
@Data
@Accessors(chain = true)
@TableName("jk_risk_event")
public class JkRiskEvent implements Serializable {
    @TableId(value = "id", type = IdType.AUTO) private Long id;
    private String eventNo;
    private String idempotencyKey;
    private String riskType;
    private String riskLevel;
    private String sourceType;
    private Long sourceId;
    private String sourceNo;
    private Long userId;
    private String summary;
    private String detailJson;
    private String status;
    private Long handleUserId;
    private Date handleTime;
    private String handleRemark;
    private Boolean isDeleted;
    private Date createTime;
    private Date updateTime;
    private Integer version;
}
