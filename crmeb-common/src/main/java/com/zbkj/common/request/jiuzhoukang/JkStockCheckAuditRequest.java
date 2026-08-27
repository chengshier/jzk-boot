package com.zbkj.common.request.jiuzhoukang;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
public class JkStockCheckAuditRequest {
    @NotNull(message = "盘点单不能为空") private Long checkId;
    @NotNull(message = "审核结果不能为空") private Boolean approved;
    @NotBlank(message = "requestNo不能为空") private String requestNo;
    @NotBlank(message = "审核备注不能为空") private String remark;
}
