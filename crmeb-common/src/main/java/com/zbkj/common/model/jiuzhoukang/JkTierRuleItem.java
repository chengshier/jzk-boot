package com.zbkj.common.model.jiuzhoukang;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

@Data
@Accessors(chain = true)
@TableName("jk_tier_rule_item")
public class JkTierRuleItem implements Serializable {
    private static final long serialVersionUID = 1L;
    @TableId(value = "id", type = IdType.AUTO) private Long id;
    private Long tierRuleId;
    private BigDecimal thresholdMin;
    private BigDecimal thresholdMax;
    private String rewardMode;
    private BigDecimal rewardValue;
    private BigDecimal tierCap;
    private Integer sortNo;
    private Boolean status;
    private Boolean isDeleted;
    private Date createTime;
    private Date updateTime;
}
