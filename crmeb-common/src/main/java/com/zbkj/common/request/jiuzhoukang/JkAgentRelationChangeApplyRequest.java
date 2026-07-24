package com.zbkj.common.request.jiuzhoukang;

import lombok.Data;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
public class JkAgentRelationChangeApplyRequest {
    @NotBlank private String requestNo;
    @NotNull private Long targetParentUserId;
    @NotBlank private String applyReason;
}
