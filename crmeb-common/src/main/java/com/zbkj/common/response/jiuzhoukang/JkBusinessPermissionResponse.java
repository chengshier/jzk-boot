package com.zbkj.common.response.jiuzhoukang;

import lombok.Data;

import java.io.Serializable;

@Data
public class JkBusinessPermissionResponse implements Serializable {
    private Long id;
    private String permissionCode;
    private String permissionName;
    private String moduleCode;
    private String moduleName;
    private String permissionType;
    private String permissionTypeText;
    private Boolean enabled;
    private String remark;
}
