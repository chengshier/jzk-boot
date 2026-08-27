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
@TableName("jk_performance_record")
public class JkPerformanceRecord implements Serializable {
    @TableId(value = "id", type = IdType.AUTO) private Long id;
    private String performanceNo;
    private String sourceType;
    private Long sourceId;
    private String sourceNo;
    private Long sourceItemId;
    private String performanceType;
    private Long ownerUserId;
    private String ownerRoleCode;
    private Long sourceUserId;
    private String sourceRoleCode;
    private Long directParentUserId;
    private Long countyAgentUserId;
    private String regionCode;
    private Integer productId;
    private Integer skuId;
    private Integer quantity;
    private BigDecimal baseAmount;
    private BigDecimal performanceAmount;
    private BigDecimal reversedAmount;
    private String status;
    private Date occurredAt;
    private String requestNo;
    private Long planId;
    private Integer ruleVersionNo;
    private String relationSnapshotJson;
    private String sourceSnapshotJson;
    private Boolean isDeleted;
    private Date createTime;
    private Date updateTime;
}
