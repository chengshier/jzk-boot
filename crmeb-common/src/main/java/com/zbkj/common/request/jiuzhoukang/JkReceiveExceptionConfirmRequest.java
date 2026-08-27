package com.zbkj.common.request.jiuzhoukang;

import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
public class JkReceiveExceptionConfirmRequest {
    @NotNull private Long exceptionId;
    @NotNull private Boolean confirmed;
    private String remark;
}
