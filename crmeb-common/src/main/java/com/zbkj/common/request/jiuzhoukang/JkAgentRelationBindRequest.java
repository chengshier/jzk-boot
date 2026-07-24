package com.zbkj.common.request.jiuzhoukang;

import lombok.Data;
import javax.validation.constraints.NotNull;

@Data
public class JkAgentRelationBindRequest {
    @NotNull(message = "下级用户不能为空") private Long userId;
    private Long parentUserId;
    private String relationType;
    private String bindSource;
    private String sourceCode;
    private String changeReason;
    private String remark;
}
