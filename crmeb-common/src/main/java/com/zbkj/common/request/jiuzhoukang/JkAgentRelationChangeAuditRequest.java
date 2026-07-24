package com.zbkj.common.request.jiuzhoukang;

import lombok.Data;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
public class JkAgentRelationChangeAuditRequest {
    @NotNull private Long applyId;
    @NotNull private Boolean approved;
    @NotBlank private String requestNo;
    private String remark;
}
