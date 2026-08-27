package com.zbkj.common.model.jiuzhoukang;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/** 周期业绩主表；关闭后不可重汇总，调整必须通过冲正和补偿周期。 */
@Data
@Accessors(chain = true)
@TableName("jk_performance_period")
public class JkPerformancePeriod implements Serializable {
    private static final long serialVersionUID = 1L;
    @TableId(value = "id", type = IdType.AUTO) private Long id;
    private String periodNo;
    private String periodType;
    private Date startTime;
    private Date endTime;
    private Long planId;
    private Long ruleId;
    private String ownerRoleCode;
    private String regionCode;
    private String status;
    private BigDecimal totalPerformanceAmount;
    private BigDecimal totalRefundAmount;
    private BigDecimal netPerformanceAmount;
    private Integer memberCount;
    private BigDecimal trialRewardAmount;
    private BigDecimal approvedRewardAmount;
    private String snapshotJson;
    private String requestNo;
    private Long createdBy;
    private Long closedBy;
    private Date closedAt;
    private Integer version;
    private Boolean isDeleted;
    private Date createTime;
    private Date updateTime;
}
