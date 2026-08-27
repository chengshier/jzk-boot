package com.zbkj.common.request.jiuzhoukang;

import lombok.Data;
import lombok.experimental.Accessors;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

/** 后台异常收货处理请求。 */
@Data
@Accessors(chain = true)
public class JkTradeReceiveExceptionHandleRequest {
    @NotNull private Long exceptionId;
    /** PROCESSING、PROPOSE_RESOLUTION、REJECTED；RESOLVED 不再允许绕过 V2 双向确认。 */
    @NotBlank private String action;
    @NotBlank private String remark;
    /** RETRY_RECEIVE、PARTIAL_RECEIVE、REFUND、COMPENSATION、REFUND_AND_COMPENSATION、REJECT_ALL。 */
    private String resolutionType;
    private BigDecimal refundAmount;
    private BigDecimal compensationAmount;
}
