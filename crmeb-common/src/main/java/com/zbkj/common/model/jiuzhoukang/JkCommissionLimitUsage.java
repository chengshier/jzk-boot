package com.zbkj.common.model.jiuzhoukang;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/** 规则总预算和受益人周期封顶的数据库并发占用行。 */
@Data
@Accessors(chain = true)
@TableName("jk_commission_limit_usage")
public class JkCommissionLimitUsage implements Serializable {
    private static final long serialVersionUID = 1L;
    @TableId(value = "id", type = IdType.AUTO) private Long id;
    private String usageType;
    private Long ruleId;
    private Long userId;
    private String periodKey;
    private BigDecimal limitAmount;
    private BigDecimal usedAmount;
    private Integer version;
    private Date createTime;
    private Date updateTime;
}
