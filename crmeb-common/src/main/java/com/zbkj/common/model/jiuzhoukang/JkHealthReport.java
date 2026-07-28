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
@TableName("jk_health_report")
public class JkHealthReport implements Serializable {
    @TableId(value="id",type=IdType.AUTO) private Long id;
    private String reportNo;
    private Long userId;
    private String reportType;
    private Date periodStart;
    private Date periodEnd;
    private Integer recordCount;
    private Integer glucoseCount;
    private BigDecimal averageGlucose;
    private BigDecimal minimumGlucose;
    private BigDecimal maximumGlucose;
    private Integer highCount;
    private Integer lowCount;
    private Integer normalCount;
    private Integer dietCount;
    private Integer exerciseCount;
    private Integer medicineCount;
    private String sourceSummaryJson;
    private String summaryText;
    private Long fileObjectId;
    private String status;
    private Date generatedAt;
    private String requestNo;
    private Boolean isDeleted;
    private Date createTime;
    private Date updateTime;
}
