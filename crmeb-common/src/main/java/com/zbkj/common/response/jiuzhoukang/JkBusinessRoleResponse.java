package com.zbkj.common.response.jiuzhoukang;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

@Data
public class JkBusinessRoleResponse implements Serializable {
    private Long id;
    private String roleCode;
    private String roleName;
    private Boolean enabled;
    private Boolean needAudit;
    private Boolean allowFrontApply;
    private Integer roleLevel;
    private String remark;
    private Date createTime;
    private Date updateTime;
    private List<String> permissionCodes;
    private List<String> permissionNames;
    private List<String> permissionDisplayList;
}
