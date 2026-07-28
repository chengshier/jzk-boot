package com.zbkj.common.request.jiuzhoukang;

import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
public class JkStockCheckCreateRequest {
    @NotNull private String requestNo;
    @NotNull private Long stockAccountId;
    private String scopeType;
    private String remark;
}
