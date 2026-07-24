package com.zbkj.common.model.jiuzhoukang;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/** 第六阶段日汇总指标。业务口径通过 metricCode 和 metricVersion 固化。 */
@Data @Accessors(chain = true) @TableName("jk_report_daily_summary")
public class JkReportDailySummary implements Serializable {
    private static final long serialVersionUID = 1L;
    @TableId(value="id", type=IdType.AUTO) private Long id;
    private Date metricDate;
    private String metricCode;
    private String dimensionType;
    private String dimensionCode;
    private Long dimensionId;
    private BigDecimal metricAmount;
    private Long metricCount;
    private String metricVersion;
    private String snapshotJson;
    private Boolean isDeleted;
    private Date createTime;
    private Date updateTime;
}
