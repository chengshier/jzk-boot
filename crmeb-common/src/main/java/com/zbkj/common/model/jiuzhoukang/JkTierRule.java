package com.zbkj.common.model.jiuzhoukang;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/** 阶梯奖励规则主档；档位保存在 jk_tier_rule_item。 */
@Data
@Accessors(chain = true)
@TableName("jk_tier_rule")
public class JkTierRule implements Serializable {
    private static final long serialVersionUID = 1L;
    @TableId(value = "id", type = IdType.AUTO) private Long id;
    private String ruleCode;
    private String ruleName;
    private Long planId;
    private String planCode;
    private Integer planVersionNo;
    private String receiverRoleCode;
    private String periodType;
    private String regionCode;
    private BigDecimal perUserPeriodCap;
    private BigDecimal totalBudget;
    private Integer priority;
    private String publishStatus;
    private Date effectiveStartTime;
    private Date effectiveEndTime;
    private Long publishedBy;
    private Date publishedAt;
    private Boolean status;
    private Boolean isDeleted;
    private Integer version;
    private Date createTime;
    private Date updateTime;
}
