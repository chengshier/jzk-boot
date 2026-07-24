package com.zbkj.common.request.jiuzhoukang;

import lombok.Data;
import javax.validation.constraints.NotBlank;

@Data
public class JkBusinessRoleSaveRequest {
    private Long id;
    @NotBlank(message = "角色编码不能为空") private String roleCode;
    @NotBlank(message = "角色名称不能为空") private String roleName;
    private String roleType;
    private Integer roleLevel;
    private Boolean needAudit;
    private Boolean allowFrontApply;
    private Boolean enabled;
    private Integer sort;
    private String remark;
}
