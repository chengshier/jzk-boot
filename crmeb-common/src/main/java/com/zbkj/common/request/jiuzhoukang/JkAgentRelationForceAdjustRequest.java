package com.zbkj.common.request.jiuzhoukang;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/** 管理员强制调整上下级关系，必须填写原因并走独立权限。 */
@Data
public class JkAgentRelationForceAdjustRequest {
    @NotNull(message = "下级用户不能为空")
    private Long userId;
    private Long parentUserId;
    @NotBlank(message = "强制调整原因不能为空")
    private String reason;
    private String sourceCode;
    private String remark;
}
