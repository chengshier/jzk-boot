package com.zbkj.common.request.jiuzhoukang;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class JkOfflineSaleActionRequest {
    @NotBlank(message = "requestNo不能为空") private String requestNo;
    @NotBlank(message = "原因不能为空") private String reason;
}
