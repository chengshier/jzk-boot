package com.zbkj.common.request.jiuzhoukang;

import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
public class JkOfflineSaleAuditRequest {
    @NotNull private Long saleId;
    @NotNull private Boolean approved;
    private String remark;
}
