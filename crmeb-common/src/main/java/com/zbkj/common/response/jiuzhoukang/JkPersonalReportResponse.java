package com.zbkj.common.response.jiuzhoukang;

import lombok.Data;
import lombok.experimental.Accessors;
import java.math.BigDecimal;

/** App 个人经营中心，只返回本人和本人归属快照数据。 */
@Data @Accessors(chain=true)
public class JkPersonalReportResponse {
    private BigDecimal retailPerformanceAmount;
    /** 查询周期内发生的退款，正数展示。 */
    private BigDecimal retailRefundAmount;
    /** 完成销售额减去当期退款发生额。 */
    private BigDecimal retailNetPerformanceAmount;
    private Long retailOrderCount;
    private BigDecimal transferInboundAmount;
    private BigDecimal transferReturnAmount;
    private Integer stockAvailableQty;
    private Integer stockFrozenQty;
    private BigDecimal pendingCommissionAmount;
    private BigDecimal settledCommissionAmount;
    private BigDecimal availableFundAmount;
    private BigDecimal withdrawingAmount;
    private BigDecimal withdrawnAmount;
}
