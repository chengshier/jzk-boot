package com.zbkj.common.response.jiuzhoukang;

import lombok.Data;
import lombok.experimental.Accessors;
import java.math.BigDecimal;

/** 仅平台汇总使用；样本少于阈值时 suppressed=true，不返回可识别统计值。 */
@Data @Accessors(chain=true)
public class JkHealthAnonymousSummaryResponse {
    private String regionCode;
    private String dataType;
    private Long userCount;
    private Long recordCount;
    private BigDecimal averageValue;
    private Long alertCount;
    private Boolean suppressed;
    private Integer minimumSampleSize;
}
