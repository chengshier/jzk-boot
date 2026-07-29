package com.zbkj.common.request.jiuzhoukang;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/** 完成或取消异常收货 V2 处理方案。 */
@Data
public class JkReceiveExceptionResolutionActionRequest {
    @NotNull(message = "处理方案ID不能为空") private Long resolutionId;
    @NotBlank(message = "requestNo不能为空") private String requestNo;
    @NotBlank(message = "操作说明不能为空") private String remark;
}
