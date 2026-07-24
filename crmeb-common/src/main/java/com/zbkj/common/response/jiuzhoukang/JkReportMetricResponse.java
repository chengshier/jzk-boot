package com.zbkj.common.response.jiuzhoukang;

import lombok.Data;
import lombok.experimental.Accessors;
import java.math.BigDecimal;
import java.util.Date;

@Data @Accessors(chain=true)
public class JkReportMetricResponse {
    private Date metricDate;
    private String metricCode;
    private String dimensionType;
    private String dimensionCode;
    private Long dimensionId;
    private String dimensionName;
    private BigDecimal amount;
    private Long count;
}
