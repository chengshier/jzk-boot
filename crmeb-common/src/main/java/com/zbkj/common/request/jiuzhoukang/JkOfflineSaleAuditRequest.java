package com.zbkj.common.request.jiuzhoukang;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
public class JkOfflineSaleAuditRequest {
    @NotNull(message = "销售单不能为空") private Long saleId;
    @NotNull(message = "审核结果不能为空") private Boolean approved;
    @NotBlank(message = "审核备注不能为空") private String remark;
    @NotBlank(message = "requestNo不能为空") private String requestNo;
}
