package com.zbkj.common.request.jiuzhoukang;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
public class JkStockCheckCreateRequest {
    @NotBlank(message = "requestNo不能为空") private String requestNo;
    @NotNull(message = "库存账户不能为空") private Long stockAccountId;
    private String checkType;
    private String remark;
}
