package com.zbkj.common.model.jiuzhoukang;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/** 周期内有效终端销售业绩快照；不纳入平台订货和内部库存调拨。 */
@Data
@Accessors(chain = true)
@TableName("jk_performance_period_item")
public class JkPerformancePeriodItem implements Serializable {
    private static final long serialVersionUID = 1L;
    @TableId(value = "id", type = IdType.AUTO) private Long id;
    private Long periodId;
    private Long performanceRecordId;
    private Long ownerUserId;
    private Long sourceUserId;
    private String sourceType;
    private Long sourceId;
    private Long sourceItemId;
    private BigDecimal performanceAmount;
    private BigDecimal refundAmount;
    private BigDecimal netAmount;
    private String relationSnapshotJson;
    private String status;
    private String idempotencyKey;
    private Boolean isDeleted;
    private Date createTime;
    private Date updateTime;
}
