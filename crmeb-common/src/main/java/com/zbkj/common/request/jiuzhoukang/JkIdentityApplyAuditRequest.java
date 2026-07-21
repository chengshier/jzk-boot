package com.zbkj.common.request.jiuzhoukang;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

@Data
@ApiModel(value = "JkIdentityApplyAuditRequest对象", description = "九州康身份审核请求")
public class JkIdentityApplyAuditRequest implements Serializable {

    @NotNull(message = "申请ID不能为空")
    private Long applyId;

    @NotBlank(message = "审核动作不能为空")
    @ApiModelProperty(value = "PASS/REJECT", required = true)
    private String auditAction;

    @Length(max = 500, message = "驳回原因长度不能超过500个字符")
    private String rejectReason;

    @Length(max = 1000, message = "审核备注长度不能超过1000个字符")
    private String auditRemark;
}
