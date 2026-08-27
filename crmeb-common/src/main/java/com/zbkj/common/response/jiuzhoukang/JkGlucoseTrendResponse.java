package com.zbkj.common.response.jiuzhoukang;

import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/** 血糖趋势曲线及其统计摘要。 */
@Data
@Accessors(chain = true)
public class JkGlucoseTrendResponse {
    private Date startAt;
    private Date endAt;
    private BigDecimal average;
    private BigDecimal minimum;
    private BigDecimal maximum;
    private Integer count;
    private List<Point> points;

    @Data
    @Accessors(chain = true)
    public static class Point {
        private Date measuredAt;
        private BigDecimal value;
        private String unit;
    }
}
