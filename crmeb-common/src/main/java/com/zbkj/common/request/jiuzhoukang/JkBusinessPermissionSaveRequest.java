package com.zbkj.common.request.jiuzhoukang;

import lombok.Data;
import javax.validation.constraints.NotBlank;

@Data
public class JkBusinessPermissionSaveRequest {
    private Long id;
    @NotBlank(message = "权限编码不能为空") private String permissionCode;
    @NotBlank(message = "权限名称不能为空") private String permissionName;
    @NotBlank(message = "模块编码不能为空") private String moduleCode;
    private String permissionType;
    private Boolean enabled;
    private String remark;
}
