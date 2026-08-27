package com.zbkj.common.request.jiuzhoukang;

import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
public class JkStockCheckAuditRequest {
    @NotNull private Long checkId;
    @NotNull private Boolean approved;
    private String remark;
}
