package com.zbkj.common.request.jiuzhoukang;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/** 后台异常收货处理请求。 */
@Data
public class JkTradeReceiveExceptionHandleRequest {
    @NotNull private Long exceptionId;
    /** PROCESSING、RESOLVED 或 REJECTED。 */
    @NotBlank private String action;
    @NotBlank private String remark;
}
