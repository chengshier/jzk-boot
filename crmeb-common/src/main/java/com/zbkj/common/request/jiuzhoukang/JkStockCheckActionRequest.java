package com.zbkj.common.request.jiuzhoukang;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class JkStockCheckActionRequest {
    @NotBlank(message = "requestNo不能为空") private String requestNo;
    @NotBlank(message = "操作备注不能为空") private String remark;
}
