package com.zbkj.common.model.jiuzhoukang;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Date;

/** 佣金规则命中、未命中和计算解释日志。 */
@Data
@Accessors(chain = true)
@TableName("jk_commission_match_log")
public class JkCommissionMatchLog implements Serializable {
    @TableId(value = "id", type = IdType.AUTO) private Long id;
    private String eventKey;
    private String scenario;
    private String sourceType;
    private Long sourceId;
    private Long sourceItemId;
    private Long receiverUserId;
    private String rewardType;
    private Long ruleId;
    private Integer ruleVersionNo;
    private String matchStatus;
    private String reasonCode;
    private String calculationJson;
    private Date createTime;
}
